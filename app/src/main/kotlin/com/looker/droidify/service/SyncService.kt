package com.looker.droidify.service

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.fragment.app.Fragment
import com.looker.core.common.Constants
import com.looker.core.common.SdkCheck
import com.looker.core.common.createNotificationChannel
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.notificationManager
import com.looker.core.common.extension.startSelf
import com.looker.core.common.extension.stopForegroundCompat
import com.looker.core.common.result.Result
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.SettingsRepository
import com.looker.droidify.BuildConfig
import com.looker.droidify.MainActivity
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.model.ProductItem
import com.looker.droidify.model.Repository
import com.looker.droidify.utility.extension.startUpdate
import com.looker.network.DataSize
import com.looker.network.percentBy
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes
import com.looker.core.common.R.style as styleRes
import kotlinx.coroutines.Job as CoroutinesJob

@AndroidEntryPoint
class SyncService : ConnectionService<SyncService.Binder>() {

    companion object {
        private const val MAX_PROGRESS = 100

        private const val NOTIFICATION_UPDATE_SAMPLING = 400L

        private const val MAX_UPDATE_NOTIFICATION = 5
        private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"

        private val syncState = MutableSharedFlow<State>()
        private val onFinishState = MutableSharedFlow<Unit>()
    }

    @Inject
    lateinit var settingsRepository: SettingsRepository

    private sealed class State(val name: String) {
        data class Connecting(val appName: String) : State(appName)
        data class Syncing(
            val appName: String,
            val stage: RepositoryUpdater.Stage,
            val read: DataSize,
            val total: DataSize?
        ) : State(appName)
    }

    private class Task(val repositoryId: Long, val manual: Boolean)
    private data class CurrentTask(
        val task: Task?,
        val job: CoroutinesJob,
        val hasUpdates: Boolean,
        val lastState: State
    )

    private enum class Started { NO, AUTO, MANUAL }

    private var started = Started.NO
    private val tasks = mutableListOf<Task>()
    private var currentTask: CurrentTask? = null

    private var updateNotificationBlockerFragment: WeakReference<Fragment>? = null

    private val downloadConnection = Connection(DownloadService::class.java)
    private val lock = Mutex()

    enum class SyncRequest { AUTO, MANUAL, FORCE }

    inner class Binder : android.os.Binder() {

        val onFinish: SharedFlow<Unit>
            get() = onFinishState.asSharedFlow()

        private fun sync(ids: List<Long>, request: SyncRequest) {
            val cancelledTask =
                cancelCurrentTask { request == SyncRequest.FORCE && it.task?.repositoryId in ids }
            cancelTasks { !it.manual && it.repositoryId in ids }
            val currentIds = tasks.asSequence().map { it.repositoryId }.toSet()
            val manual = request != SyncRequest.AUTO
            tasks += ids.asSequence().filter {
                it !in currentIds &&
                    it != currentTask?.task?.repositoryId
            }.map { Task(it, manual) }
            handleNextTask(cancelledTask?.hasUpdates == true)
            if (request != SyncRequest.AUTO && started == Started.AUTO) {
                started = Started.MANUAL
                startSelf()
                handleSetStarted()
                currentTask?.lastState?.let { publishForegroundState(true, it) }
            }
        }

        fun sync(request: SyncRequest) {
            val ids = Database.RepositoryAdapter.getAll()
                .asSequence().filter { it.enabled }.map { it.id }.toList()
            sync(ids, request)
        }

        fun sync(repository: Repository) {
            if (repository.enabled) {
                sync(listOf(repository.id), SyncRequest.FORCE)
            }
        }

        suspend fun updateAllApps() {
            updateAllAppsInternal()
        }

        fun setUpdateNotificationBlocker(fragment: Fragment?) {
            updateNotificationBlockerFragment = fragment?.let(::WeakReference)
            if (fragment != null) {
                notificationManager?.cancel(Constants.NOTIFICATION_ID_UPDATES)
            }
        }

        fun setEnabled(repository: Repository, enabled: Boolean): Boolean {
            Database.RepositoryAdapter.put(repository.enable(enabled))
            if (enabled) {
                val isRepoInTasks = repository.id != currentTask?.task?.repositoryId &&
                    !tasks.any { it.repositoryId == repository.id }
                if (isRepoInTasks) {
                    tasks += Task(repository.id, true)
                    handleNextTask(false)
                }
            } else {
                cancelTasks { it.repositoryId == repository.id }
                val cancelledTask = cancelCurrentTask {
                    it.task?.repositoryId == repository.id
                }
                handleNextTask(cancelledTask?.hasUpdates == true)
            }
            return true
        }

        fun isCurrentlySyncing(repositoryId: Long): Boolean {
            return currentTask?.task?.repositoryId == repositoryId
        }

        fun deleteRepository(repositoryId: Long): Boolean {
            val repository = Database.RepositoryAdapter.get(repositoryId)
            return repository != null && run {
                setEnabled(repository, false)
                Database.RepositoryAdapter.markAsDeleted(repository.id)
                true
            }
        }

        fun cancelAuto(): Boolean {
            val removed = cancelTasks { !it.manual }
            val currentTask = cancelCurrentTask { it.task?.manual == false }
            handleNextTask(currentTask?.hasUpdates == true)
            return removed || currentTask != null
        }
    }

    private val binder = Binder()
    override fun onBind(intent: Intent): Binder = binder

    override fun onCreate() {
        super.onCreate()

        createNotificationChannel(
            id = Constants.NOTIFICATION_CHANNEL_SYNCING,
            name = getString(stringRes.syncing),
        )
        createNotificationChannel(
            id = Constants.NOTIFICATION_CHANNEL_UPDATES,
            name = getString(stringRes.updates),
        )

        downloadConnection.bind(this)
        lifecycleScope.launch {
            syncState
                .sample(NOTIFICATION_UPDATE_SAMPLING)
                .collectLatest {
                    publishForegroundState(false, it)
                }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        downloadConnection.unbind(this)
        cancelTasks { true }
        cancelCurrentTask { true }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_CANCEL) {
            tasks.clear()
            val cancelledTask = cancelCurrentTask { it.task != null }
            handleNextTask(cancelledTask?.hasUpdates == true)
        }
        return START_NOT_STICKY
    }

    private fun cancelTasks(condition: (Task) -> Boolean): Boolean {
        return tasks.removeAll(condition)
    }

    private fun cancelCurrentTask(condition: ((CurrentTask) -> Boolean)): CurrentTask? {
        return currentTask?.let {
            if (condition(it)) {
                currentTask = null
                it.job.cancel()
                RepositoryUpdater.await()
                it
            } else {
                null
            }
        }
    }

    private fun showNotificationError(repository: Repository, exception: Exception) {
        val description = getString(
            when (exception) {
                is RepositoryUpdater.UpdateException -> when (exception.errorType) {
                    RepositoryUpdater.ErrorType.NETWORK -> stringRes.network_error_DESC
                    RepositoryUpdater.ErrorType.HTTP -> stringRes.http_error_DESC
                    RepositoryUpdater.ErrorType.VALIDATION -> stringRes.validation_index_error_DESC
                    RepositoryUpdater.ErrorType.PARSING -> stringRes.parsing_index_error_DESC
                }

                else -> stringRes.unknown_error_DESC
            }
        )
        notificationManager?.notify(
            "repository-${repository.id}",
            Constants.NOTIFICATION_ID_SYNCING,
            NotificationCompat
                .Builder(this, Constants.NOTIFICATION_CHANNEL_SYNCING)
                .setSmallIcon(android.R.drawable.stat_sys_warning)
                .setColor(
                    ContextThemeWrapper(this, styleRes.Theme_Main_Light)
                        .getColorFromAttr(android.R.attr.colorPrimary).defaultColor
                )
                .setContentTitle(getString(stringRes.could_not_sync_FORMAT, repository.name))
                .setContentText(description)
                .build()
        )
    }

    private val stateNotificationBuilder by lazy {
        NotificationCompat
            .Builder(this, Constants.NOTIFICATION_CHANNEL_SYNCING)
            .setSmallIcon(CommonR.drawable.ic_sync)
            .setColor(
                ContextThemeWrapper(this, styleRes.Theme_Main_Light)
                    .getColorFromAttr(android.R.attr.colorPrimary).defaultColor
            )
            .addAction(
                0,
                getString(stringRes.cancel),
                PendingIntent.getService(
                    this,
                    0,
                    Intent(this, this::class.java).setAction(ACTION_CANCEL),
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )
            )
    }

    private fun publishForegroundState(force: Boolean, state: State) {
        if (force || currentTask?.lastState != state) {
            currentTask = currentTask?.copy(lastState = state)
            if (started == Started.MANUAL) {
                startForeground(
                    Constants.NOTIFICATION_ID_SYNCING,
                    stateNotificationBuilder.apply {
                        setContentTitle(getString(stringRes.syncing_FORMAT, state.name))
                        when (state) {
                            is State.Connecting -> {
                                setContentText(getString(stringRes.connecting))
                                setProgress(0, 0, true)
                            }

                            is State.Syncing -> {
                                when (state.stage) {
                                    RepositoryUpdater.Stage.DOWNLOAD -> {
                                        if (state.total != null) {
                                            setContentText("${state.read} / ${state.total}")
                                            setProgress(
                                                MAX_PROGRESS,
                                                state.read percentBy state.total,
                                                false
                                            )
                                        } else {
                                            setContentText(state.read.toString())
                                            setProgress(0, 0, true)
                                        }
                                    }

                                    RepositoryUpdater.Stage.PROCESS -> {
                                        val progress = (state.read percentBy state.total)
                                            .takeIf { it != -1 }
                                        setContentText(
                                            getString(
                                                stringRes.processing_FORMAT,
                                                "${progress ?: 0}%"
                                            )
                                        )
                                        setProgress(MAX_PROGRESS, progress ?: 0, progress == null)
                                    }

                                    RepositoryUpdater.Stage.MERGE -> {
                                        val progress = (state.read percentBy state.total)
                                        setContentText(
                                            getString(
                                                stringRes.merging_FORMAT,
                                                "${state.read.value} / ${state.total?.value ?: state.read.value}"
                                            )
                                        )
                                        setProgress(MAX_PROGRESS, progress, false)
                                    }

                                    RepositoryUpdater.Stage.COMMIT -> {
                                        setContentText(getString(stringRes.saving_details))
                                        setProgress(0, 0, true)
                                    }
                                }
                            }
                        }::class
                    }.build()
                )
            }
        }
    }

    private fun handleSetStarted() {
        stateNotificationBuilder.setWhen(System.currentTimeMillis())
    }

    private fun handleNextTask(hasUpdates: Boolean) {
        if (currentTask != null) return
        if (tasks.isEmpty()) {
            if (started != Started.NO) {
                lifecycleScope.launch {
                    val setting = settingsRepository.getInitial()
                    handleUpdates(
                        hasUpdates = hasUpdates,
                        notifyUpdates = setting.notifyUpdate,
                        autoUpdate = setting.autoUpdate
                    )
                }
            }
            return
        }
        val task = tasks.removeFirst()
        val repository = Database.RepositoryAdapter.get(task.repositoryId)
        if (repository == null || !repository.enabled) handleNextTask(hasUpdates)
        val lastStarted = started
        val newStarted = if (task.manual || lastStarted == Started.MANUAL) {
            Started.MANUAL
        } else {
            Started.AUTO
        }
        started = newStarted
        if (newStarted == Started.MANUAL && lastStarted != Started.MANUAL) {
            startSelf()
            handleSetStarted()
        }
        val initialState = State.Connecting(repository!!.name)
        publishForegroundState(true, initialState)
        lifecycleScope.launch {
            val unstableUpdates =
                settingsRepository.getInitial().unstableUpdate
            val downloadJob = downloadFile(
                task = task,
                repository = repository,
                hasUpdates = hasUpdates,
                unstableUpdates = unstableUpdates
            )
            currentTask = CurrentTask(task, downloadJob, hasUpdates, initialState)
        }
    }

    private fun CoroutineScope.downloadFile(
        task: Task,
        repository: Repository,
        hasUpdates: Boolean,
        unstableUpdates: Boolean
    ): CoroutinesJob = launch(Dispatchers.Default) {
        var passedHasUpdates = hasUpdates
        try {
            val response = RepositoryUpdater.update(
                this@SyncService,
                repository,
                unstableUpdates
            ) { stage, progress, total ->
                launch {
                    syncState.emit(
                        State.Syncing(
                            appName = repository.name,
                            stage = stage,
                            read = DataSize(progress),
                            total = total?.let { DataSize(it) }
                        )
                    )
                }
            }
            passedHasUpdates = when (response) {
                is Result.Error -> {
                    response.exception?.let {
                        it.printStackTrace()
                        if (task.manual) showNotificationError(repository, it as Exception)
                    }
                    response.data == true || hasUpdates
                }

                is Result.Success -> response.data || hasUpdates
            }
        } finally {
            withContext(NonCancellable) {
                lock.withLock { currentTask = null }
                handleNextTask(passedHasUpdates)
            }
        }
    }

    private suspend fun handleUpdates(
        hasUpdates: Boolean,
        notifyUpdates: Boolean,
        autoUpdate: Boolean
    ) {
        try {
            if (!hasUpdates || !notifyUpdates) {
                onFinishState.emit(Unit)
                val needStop = started == Started.MANUAL
                started = Started.NO
                if (needStop) stopForegroundCompat()
                return
            }
            val blocked = updateNotificationBlockerFragment?.get()?.isAdded == true
            val updates = Database.ProductAdapter.getUpdates()
            if (!blocked && updates.isNotEmpty()) {
                displayUpdatesNotification(updates)
                if (autoUpdate) updateAllAppsInternal()
            }
            handleUpdates(hasUpdates = false, notifyUpdates = true, autoUpdate = autoUpdate)
        } finally {
            withContext(NonCancellable) {
                lock.withLock { currentTask = null }
                handleNextTask(false)
            }
        }
    }

    private suspend fun updateAllAppsInternal() {
        Database.ProductAdapter
            .getUpdates()
            // Update Droid-ify the last
            .sortedBy { if (it.packageName == packageName) 1 else -1 }
            .map {
                Database.InstalledAdapter.get(it.packageName, null) to
                    Database.RepositoryAdapter.get(it.repositoryId)
            }
            .filter { it.first != null && it.second != null }
            .forEach { (installItem, repo) ->
                val productRepo = Database.ProductAdapter.get(installItem!!.packageName, null)
                    .filter { it.repositoryId == repo!!.id }
                    .map { it to repo!! }
                downloadConnection.startUpdate(
                    installItem.packageName,
                    installItem,
                    productRepo
                )
            }
    }

    private fun displayUpdatesNotification(productItems: List<ProductItem>) {
        fun <T> T.applyHack(callback: T.() -> Unit): T = apply(callback)
        notificationManager?.notify(
            Constants.NOTIFICATION_ID_UPDATES,
            NotificationCompat
                .Builder(this, Constants.NOTIFICATION_CHANNEL_UPDATES)
                .setSmallIcon(CommonR.drawable.ic_new_releases)
                .setContentTitle(getString(stringRes.new_updates_available))
                .setContentText(
                    resources.getQuantityString(
                        CommonR.plurals.new_updates_DESC_FORMAT,
                        productItems.size,
                        productItems.size
                    )
                )
                .setColor(
                    ContextThemeWrapper(this, styleRes.Theme_Main_Light)
                        .getColorFromAttr(android.R.attr.colorPrimary).defaultColor
                )
                .setContentIntent(
                    PendingIntent.getActivity(
                        this,
                        0,
                        Intent(this, MainActivity::class.java)
                            .setAction(MainActivity.ACTION_UPDATES),
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                )
                .setStyle(
                    NotificationCompat.InboxStyle().applyHack {
                        for (productItem in productItems.take(MAX_UPDATE_NOTIFICATION)) {
                            val builder = SpannableStringBuilder(productItem.name)
                            builder.setSpan(
                                ForegroundColorSpan(Color.BLACK),
                                0,
                                builder.length,
                                SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
                            )
                            builder.append(' ').append(productItem.version)
                            addLine(builder)
                        }
                        if (productItems.size > MAX_UPDATE_NOTIFICATION) {
                            val summary =
                                getString(
                                    stringRes.plus_more_FORMAT,
                                    productItems.size - MAX_UPDATE_NOTIFICATION
                                )
                            if (SdkCheck.isNougat) addLine(summary) else setSummaryText(summary)
                        }
                    }
                )
                .build()
        )
    }

    @SuppressLint("SpecifyJobSchedulerIdRange")
    class Job : JobService() {
        private val jobScope = CoroutineScope(Dispatchers.Default)
        private var syncParams: JobParameters? = null
        private val syncConnection =
            Connection(SyncService::class.java, onBind = { connection, binder ->
                jobScope.launch {
                    binder.onFinish.collect {
                        val params = syncParams
                        if (params != null) {
                            syncParams = null
                            connection.unbind(this@Job)
                            jobFinished(params, false)
                        }
                    }
                }
                binder.sync(SyncRequest.AUTO)
            }, onUnbind = { _, binder ->
                binder.cancelAuto()
                jobScope.cancel()
                val params = syncParams
                if (params != null) {
                    syncParams = null
                    jobFinished(params, true)
                }
            })

        override fun onStartJob(params: JobParameters): Boolean {
            syncParams = params
            syncConnection.bind(this)
            return true
        }

        override fun onStopJob(params: JobParameters): Boolean {
            syncParams = null
            jobScope.cancel()
            val reschedule = syncConnection.binder?.cancelAuto() == true
            syncConnection.unbind(this)
            return reschedule
        }

        companion object {
            fun create(
                context: Context,
                periodMillis: Long,
                networkType: Int,
                isCharging: Boolean,
                isBatteryLow: Boolean
            ): JobInfo = JobInfo.Builder(
                Constants.JOB_ID_SYNC,
                ComponentName(context, Job::class.java)
            ).apply {
                setRequiredNetworkType(networkType)
                sdkAbove(sdk = Build.VERSION_CODES.O) {
                    setRequiresCharging(isCharging)
                    setRequiresBatteryNotLow(isBatteryLow)
                    setRequiresStorageNotLow(true)
                }
                if (SdkCheck.isNougat) {
                    setPeriodic(periodMillis, JobInfo.getMinFlexMillis())
                } else {
                    setPeriodic(periodMillis)
                }
            }.build()
        }
    }
}
