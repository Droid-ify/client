package com.looker.droidify.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.looker.core.common.*
import com.looker.core.common.cache.Cache
import com.looker.core.common.extension.*
import com.looker.core.common.signature.ValidationException
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.model.InstallerType
import com.looker.core.model.Release
import com.looker.core.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.MainActivity
import com.looker.installer.InstallManager
import com.looker.installer.model.installFrom
import com.looker.network.Downloader
import com.looker.network.NetworkResponse
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import javax.inject.Inject
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes
import com.looker.core.common.R.style as styleRes

@AndroidEntryPoint
class DownloadService : ConnectionService<DownloadService.Binder>() {
	companion object {
		private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"
	}

	@Inject
	lateinit var settingsRepository: SettingsRepository

	@Inject
	lateinit var downloader: Downloader

	private val installerType
		get() = settingsRepository.get { installerType }

	@Inject
	lateinit var installer: InstallManager

	sealed class State(val packageName: String) {
		data object Idle : State("")
		data class Connecting(val name: String) : State(name)
		data class Downloading(val name: String, val read: Long, val total: Long?) : State(name)
		data class Error(val name: String) : State(name)
		data class Cancel(val name: String) : State(name)
		data class Success(val name: String, val release: Release) : State(name)
	}

	data class DownloadState(
		val currentItem: State = State.Idle,
		val queue: List<String> = emptyList()
	) {
		infix fun isDownloading(packageName: String): Boolean =
			currentItem.packageName == packageName && (
					currentItem is State.Connecting || currentItem is State.Downloading
					)

		infix fun isComplete(packageName: String): Boolean =
			currentItem.packageName == packageName && (
					currentItem is State.Error
							|| currentItem is State.Cancel
							|| currentItem is State.Success
							|| currentItem is State.Idle
					)
	}

	private val _downloadState = MutableStateFlow(DownloadState())

	private class Task(
		val packageName: String, val name: String, val release: Release,
		val url: String, val authentication: String,
		val isUpdate: Boolean = false
	) {
		val notificationTag: String
			get() = "download-$packageName"
	}

	private data class CurrentTask(val task: Task, val job: Job, val lastState: State)

	private var started = false
	private val tasks = mutableListOf<Task>()
	private var currentTask: CurrentTask? = null

	private val lock = Mutex()

	inner class Binder : android.os.Binder() {
		val downloadState = _downloadState.asStateFlow()
		fun enqueue(
			packageName: String,
			name: String,
			repository: Repository,
			release: Release,
			isUpdate: Boolean = false
		) {
			val task = Task(
				packageName = packageName,
				name = name,
				release = release,
				url = release.getDownloadUrl(repository),
				authentication = repository.authentication,
				isUpdate = isUpdate
			)
			if (Cache.getReleaseFile(this@DownloadService, release.cacheFileName).exists()) {
				lifecycleScope.launch { publishSuccess(task) }
			} else {
				cancelTasks(packageName)
				cancelCurrentTask(packageName)
				notificationManager.cancel(
					task.notificationTag,
					Constants.NOTIFICATION_ID_DOWNLOADING
				)
				tasks += task
				if (currentTask == null) {
					handleDownload()
				} else {
					updateCurrentQueue { add(packageName) }
				}
			}
		}

		fun cancel(packageName: String) {
			cancelTasks(packageName)
			cancelCurrentTask(packageName)
		}
	}

	private val binder = Binder()
	override fun onBind(intent: Intent): Binder = binder

	override fun onCreate() {
		super.onCreate()

		sdkAbove(Build.VERSION_CODES.O) {
			NotificationChannel(
				Constants.NOTIFICATION_CHANNEL_DOWNLOADING,
				getString(stringRes.downloading), NotificationManager.IMPORTANCE_LOW
			)
				.apply { setShowBadge(false) }
				.let(notificationManager::createNotificationChannel)
		}

		lifecycleScope.launch {
			_downloadState
				.filter { currentTask != null }
				.collect {
					delay(400)
					publishForegroundState(false, it.currentItem)
				}
		}
	}

	override fun onDestroy() {
		super.onDestroy()
		cancelTasks(null)
		cancelCurrentTask(null)
	}

	override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
		if (intent?.action == ACTION_CANCEL) {
			currentTask?.let { binder.cancel(it.task.packageName) }
		}
		return START_NOT_STICKY
	}

	private fun cancelTasks(packageName: String?) {
		tasks.removeAll {
			(packageName == null || it.packageName == packageName) && run {
				updateCurrentState(State.Cancel(it.packageName))
				true
			}
		}
	}

	private fun cancelCurrentTask(packageName: String?) {
		currentTask?.let {
			if (packageName == null || it.task.packageName == packageName) {
				it.job.cancel()
				currentTask = null
				updateCurrentState(State.Cancel(it.task.packageName))
			}
		}
	}

	private sealed interface ErrorType {
		data object IO : ErrorType
		data object Http : ErrorType
		data object SocketTimeout : ErrorType
		data object ConnectionTimeout : ErrorType
		class Validation(val exception: ValidationException) : ErrorType
	}

	private fun showNotificationError(task: Task, errorType: ErrorType) {
		val intent = Intent(this, MainActivity::class.java)
			.setAction(Intent.ACTION_VIEW)
			.setData(Uri.parse("package:${task.packageName}"))
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			.getPendingIntent(this)
		notificationManager.notify(
			task.notificationTag,
			Constants.NOTIFICATION_ID_DOWNLOADING,
			NotificationCompat
				.Builder(this, Constants.NOTIFICATION_CHANNEL_DOWNLOADING)
				.setAutoCancel(true)
				.setSmallIcon(android.R.drawable.stat_notify_error)
				.setColor(
					ContextThemeWrapper(this, styleRes.Theme_Main_Light)
						.getColor(CommonR.color.md_theme_dark_errorContainer)
				)
				.setOnlyAlertOnce(true)
				.setContentIntent(intent)
				.errorNotificationContent(task, errorType)
				.build()
		)
	}

	private fun NotificationCompat.Builder.errorNotificationContent(
		task: Task,
		errorType: ErrorType
	): NotificationCompat.Builder {
		val title = if (errorType is ErrorType.Validation) stringRes.could_not_validate_FORMAT
		else stringRes.could_not_download_FORMAT
		val description = when (errorType) {
			ErrorType.ConnectionTimeout -> getString(stringRes.connection_error_DESC)
			ErrorType.Http -> getString(stringRes.http_error_DESC)
			ErrorType.IO -> getString(stringRes.io_error_DESC)
			ErrorType.SocketTimeout -> getString(stringRes.socket_error_DESC)
			is ErrorType.Validation -> errorType.exception.message
		}
		setContentTitle(getString(title, task.name))
		return setContentText(description)
	}

	private fun showNotificationInstall(task: Task) {
		val intent = Intent(this, MainActivity::class.java)
			.setAction(MainActivity.ACTION_INSTALL)
			.setData(Uri.parse("package:${task.packageName}"))
			.putExtra(MainActivity.EXTRA_CACHE_FILE_NAME, task.release.cacheFileName)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
			.getPendingIntent(this)
		notificationManager.notify(
			task.notificationTag,
			Constants.NOTIFICATION_ID_DOWNLOADING,
			NotificationCompat
				.Builder(this, Constants.NOTIFICATION_CHANNEL_DOWNLOADING)
				.setAutoCancel(true)
				.setOngoing(false)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setColor(
					ContextThemeWrapper(this, styleRes.Theme_Main_Light)
						.getColor(CommonR.color.md_theme_dark_primaryContainer)
				)
				.setOnlyAlertOnce(true)
				.setContentIntent(intent)
				.setContentTitle(getString(stringRes.downloaded_FORMAT, task.name))
				.setContentText(getString(stringRes.tap_to_install_DESC))
				.build()
		)
	}

	private suspend fun publishSuccess(task: Task) {
		val currentInstaller = installerType.first()
		updateCurrentState(State.Success(task.packageName, task.release))
		val autoInstallWithSessionInstaller =
			SdkCheck.canAutoInstall(task.release.targetSdkVersion)
					&& currentInstaller == InstallerType.SESSION
					&& task.isUpdate

		showNotificationInstall(task)
		if (currentInstaller == InstallerType.ROOT
			|| currentInstaller == InstallerType.SHIZUKU
			|| autoInstallWithSessionInstaller
		) {
			val installItem = task.packageName installFrom task.release.cacheFileName
			installer + installItem
		}
	}

	private val stateNotificationBuilder by lazy {
		NotificationCompat
			.Builder(this, Constants.NOTIFICATION_CHANNEL_DOWNLOADING)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setColor(
				ContextThemeWrapper(this, styleRes.Theme_Main_Light)
					.getColor(CommonR.color.md_theme_dark_primaryContainer)
			)
			.addAction(
				0, getString(stringRes.cancel), PendingIntent.getService(
					this,
					0,
					Intent(this, this::class.java).setAction(ACTION_CANCEL),
					PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
				)
			)
	}

	private fun publishForegroundState(force: Boolean, state: State) {
		if (!force && currentTask == null) return
		currentTask = currentTask!!.copy(lastState = state)
		stateNotificationBuilder.downloadingNotificationContent(state)
			?.let { notification ->
				startForeground(
					Constants.NOTIFICATION_ID_DOWNLOADING,
					notification.build()
				)
			} ?: run {
			Log.e("DownloadService", "Invalid Download State: $state")
		}
	}

	private fun NotificationCompat.Builder.downloadingNotificationContent(
		state: State
	): NotificationCompat.Builder? {
		return when (state) {
			is State.Connecting -> {
				setContentTitle(getString(stringRes.downloading_FORMAT, currentTask!!.task.name))
				setContentText(getString(stringRes.connecting))
				setProgress(1, 0, true)
			}

			is State.Downloading -> {
				setContentTitle(getString(stringRes.downloading_FORMAT, currentTask!!.task.name))
				if (state.total != null) {
					setContentText("${state.read.formatSize()} / ${state.total.formatSize()}")
					setProgress(100, state.read percentBy state.total, false)
				} else {
					setContentText(state.read.formatSize())
					setProgress(0, 0, true)
				}
			}

			else -> null
		}
	}

	private fun handleDownload() {
		if (currentTask != null) return
		if (tasks.isEmpty() && started) {
			started = false
			stopForegroundCompat()
			return
		}
		if (!started) {
			started = true
			startSelf()
		}
		val task = tasks.removeFirstOrNull() ?: return
		with(stateNotificationBuilder) {
			setWhen(System.currentTimeMillis())
			setContentIntent(createNotificationIntent(task.packageName))
		}
		val connectionState = State.Connecting(task.packageName)
		val partialReleaseFile =
			Cache.getPartialReleaseFile(this, task.release.cacheFileName)
		val job = lifecycleScope.downloadFile(task, partialReleaseFile)
		currentTask = CurrentTask(task, job, connectionState)
		publishForegroundState(true, connectionState)
		updateCurrentState(State.Connecting(task.packageName))
	}

	private fun createNotificationIntent(packageName: String): PendingIntent? =
		Intent(this, MainActivity::class.java)
			.setAction(Intent.ACTION_VIEW)
			.setData(Uri.parse("package:${packageName}"))
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.getPendingIntent(this)

	private fun CoroutineScope.downloadFile(
		task: Task,
		target: File
	) = launch {
		try {
			val releaseValidator = ReleaseFileValidator(
				context = this@DownloadService,
				packageName = task.packageName,
				release = task.release
			)
			val response = downloader.downloadToFile(
				url = task.url,
				target = target,
				validator = releaseValidator,
				headers = { authentication(task.authentication) }
			) { read, total ->
				ensureActive()
				yield()
				updateCurrentState(State.Downloading(task.packageName, read, total))
			}

			when (response) {
				is NetworkResponse.Success -> {
					val releaseFile = Cache.getReleaseFile(
						this@DownloadService,
						task.release.cacheFileName
					)
					target.renameTo(releaseFile)
					publishSuccess(task)
				}

				is NetworkResponse.Error -> {
					updateCurrentState(State.Error(task.packageName))
					val errorType = when (response) {
						is NetworkResponse.Error.ConnectionTimeout -> ErrorType.ConnectionTimeout
						is NetworkResponse.Error.IO -> ErrorType.IO
						is NetworkResponse.Error.SocketTimeout -> ErrorType.SocketTimeout
						is NetworkResponse.Error.Validation -> ErrorType.Validation(response.exception)
						else -> ErrorType.Http
					}
					showNotificationError(task, errorType)
				}
			}
		} finally {
			lock.withLock { currentTask = null }
			handleDownload()
		}
	}

	private fun updateCurrentState(state: State) {
		_downloadState.update {
			val newQueue =
				if (state.packageName in it.queue) it.queue.updateAsMutable { remove(state.packageName) }
				else it.queue
			it.copy(currentItem = state, queue = newQueue)
		}
	}

	private fun updateCurrentQueue(block: MutableList<String>.() -> Unit) {
		_downloadState.update { state ->
			state.copy(queue = state.queue.updateAsMutable(block))
		}
	}
}
