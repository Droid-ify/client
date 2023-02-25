package com.looker.droidify.service

import android.annotation.SuppressLint
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.job.JobParameters
import android.app.job.JobService
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.util.Log
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.looker.core.common.Constants
import com.looker.core.common.SdkCheck
import com.looker.core.common.extension.asSequence
import com.looker.core.common.extension.getColorFromAttr
import com.looker.core.common.extension.notificationManager
import com.looker.core.common.formatSize
import com.looker.core.common.result.Result
import com.looker.core.common.sdkAbove
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.SortOrder
import com.looker.core.model.ProductItem
import com.looker.core.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.MainActivity
import com.looker.droidify.R
import com.looker.droidify.database.Database
import com.looker.droidify.index.RepositoryUpdater
import com.looker.droidify.utility.Utils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject
import kotlin.math.roundToInt
import com.looker.core.common.R.string as stringRes
import com.looker.core.common.R.style as styleRes

@AndroidEntryPoint
class SyncService : ConnectionService<SyncService.Binder>() {

	companion object {
		private const val TAG = "SyncService"
		private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"

		private val mutableStateSubject = MutableSharedFlow<State>()
		private val mutableFinishState = MutableSharedFlow<Unit>()

		private val finishState = mutableFinishState.asSharedFlow()
	}

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository

	private sealed interface State {
		data class Connecting(val name: String) : State
		data class Syncing(
			val name: String, val stage: RepositoryUpdater.Stage,
			val read: Long, val total: Long?,
		) : State

		object Finishing : State
	}

	private class Task(val repositoryId: Long, val manual: Boolean)
	private data class CurrentTask(
		val task: Task?, val job: kotlinx.coroutines.Job,
		val hasUpdates: Boolean, val lastState: State,
	)

	private enum class Started { NO, AUTO, MANUAL }

	private val scope = CoroutineScope(Dispatchers.Default)

	private var started = Started.NO
	private val tasks = mutableListOf<Task>()
	private var currentTask: CurrentTask? = null

	private val downloadConnection = Connection(DownloadService::class.java)

	enum class SyncRequest { AUTO, MANUAL, FORCE }

	inner class Binder : android.os.Binder() {
		val finish: SharedFlow<Unit>
			get() = finishState

		private fun sync(ids: List<Long>, request: SyncRequest) {
			Log.i(TAG, "Sync Started: ${request.name}")
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
			val ids = Database.RepositoryAdapter.getAll(null)
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

		fun setEnabled(repository: Repository, enabled: Boolean): Boolean {
			Database.RepositoryAdapter.put(repository.enable(enabled))
			if (enabled) {
				if (repository.id != currentTask?.task?.repositoryId && !tasks.any { it.repositoryId == repository.id }) {
					tasks += Task(repository.id, true)
					handleNextTask(false)
				}
			} else {
				cancelTasks { it.repositoryId == repository.id }
				val cancelledTask = cancelCurrentTask { it.task?.repositoryId == repository.id }
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

		sdkAbove(Build.VERSION_CODES.O) {
			NotificationChannel(
				Constants.NOTIFICATION_CHANNEL_SYNCING,
				getString(stringRes.syncing), NotificationManager.IMPORTANCE_LOW
			)
				.apply { setShowBadge(false) }
				.let(notificationManager::createNotificationChannel)
			NotificationChannel(
				Constants.NOTIFICATION_CHANNEL_UPDATES,
				getString(stringRes.updates), NotificationManager.IMPORTANCE_LOW
			)
				.let(notificationManager::createNotificationChannel)
		}
		downloadConnection.bind(this)
		mutableStateSubject.onEach { publishForegroundState(false, it) }.launchIn(scope)
	}

	override fun onDestroy() {
		super.onDestroy()
		scope.cancel()
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
		notificationManager.notify(
			"repository-${repository.id}", Constants.NOTIFICATION_ID_SYNCING, NotificationCompat
				.Builder(this, Constants.NOTIFICATION_CHANNEL_SYNCING)
				.setSmallIcon(android.R.drawable.stat_sys_warning)
				.setColor(
					ContextThemeWrapper(this, styleRes.Theme_Main_Light)
						.getColorFromAttr(android.R.attr.colorPrimary).defaultColor
				)
				.setContentTitle(getString(stringRes.could_not_sync_FORMAT, repository.name))
				.setContentText(
					getString(
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
				)
				.build()
		)
	}

	private val stateNotificationBuilder by lazy {
		NotificationCompat
			.Builder(this, Constants.NOTIFICATION_CHANNEL_SYNCING)
			.setSmallIcon(R.drawable.ic_sync)
			.setColor(
				ContextThemeWrapper(this, styleRes.Theme_Main_Light)
					.getColorFromAttr(android.R.attr.colorPrimary).defaultColor
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
		if (force || currentTask?.lastState != state) {
			currentTask = currentTask?.copy(lastState = state)
			if (started == Started.MANUAL) {
				startForeground(Constants.NOTIFICATION_ID_SYNCING, stateNotificationBuilder.apply {
					when (state) {
						is State.Connecting -> {
							setContentTitle(getString(stringRes.syncing_FORMAT, state.name))
							setContentText(getString(stringRes.connecting))
							setProgress(0, 0, true)
						}
						is State.Syncing -> {
							setContentTitle(getString(stringRes.syncing_FORMAT, state.name))
							when (state.stage) {
								RepositoryUpdater.Stage.DOWNLOAD -> {
									if (state.total != null) {
										setContentText("${state.read.formatSize()} / ${state.total.formatSize()}")
										setProgress(
											100,
											(100f * state.read / state.total).roundToInt(),
											false
										)
									} else {
										setContentText(state.read.formatSize())
										setProgress(0, 0, true)
									}
								}
								RepositoryUpdater.Stage.PROCESS -> {
									val progress =
										state.total?.let { 100f * state.read / it }?.roundToInt()
									setContentText(
										getString(
											stringRes.processing_FORMAT,
											"${progress ?: 0}%"
										)
									)
									setProgress(100, progress ?: 0, progress == null)
								}
								RepositoryUpdater.Stage.MERGE -> {
									val progress = (100f * state.read / (state.total
										?: state.read)).roundToInt()
									setContentText(
										getString(
											stringRes.merging_FORMAT,
											"${state.read} / ${state.total ?: state.read}"
										)
									)
									setProgress(100, progress, false)
								}
								RepositoryUpdater.Stage.COMMIT -> {
									setContentText(getString(stringRes.saving_details))
									setProgress(0, 0, true)
								}
							}
						}
						is State.Finishing -> {
							setContentTitle(getString(stringRes.syncing))
							setContentText(null)
							setProgress(0, 0, true)
						}
					}::class
				}.build())
			}
		}
	}

	private fun handleSetStarted() {
		stateNotificationBuilder.setWhen(System.currentTimeMillis())
	}

	private fun handleNextTask(hasUpdates: Boolean) {
		if (currentTask == null) {
			if (tasks.isNotEmpty()) {
				val task = tasks.removeAt(0)
				val repository = Database.RepositoryAdapter.get(task.repositoryId)
				if (repository != null && repository.enabled) {
					val lastStarted = started
					val newStarted =
						if (task.manual || lastStarted == Started.MANUAL) Started.MANUAL else Started.AUTO
					started = newStarted
					if (newStarted == Started.MANUAL && lastStarted != Started.MANUAL) {
						startSelf()
						handleSetStarted()
					}
					val initialState = State.Connecting(repository.name)
					publishForegroundState(true, initialState)
					scope.launch {
						val unstableUpdates =
							userPreferencesRepository.fetchInitialPreferences().unstableUpdate
						handleFileDownload(
							task = task,
							initialState = initialState,
							hasUpdates = hasUpdates,
							unstableUpdates = unstableUpdates,
							repository = repository
						)
					}
				} else {
					handleNextTask(hasUpdates)
				}
			} else if (started != Started.NO) {
				scope.launch {
					val preference = userPreferencesRepository.fetchInitialPreferences()
					handleUpdates(
						hasUpdates = hasUpdates,
						notifyUpdates = preference.notifyUpdate,
						autoUpdate = preference.autoUpdate
					)
				}
			}
		}
	}

	private suspend fun handleFileDownload(
		task: Task,
		initialState: State,
		hasUpdates: Boolean,
		unstableUpdates: Boolean,
		repository: Repository
	) {
		val job = scope.launch {
			val request = RepositoryUpdater
				.update(
					this@SyncService,
					repository,
					unstableUpdates
				) { stage, progress, total ->
					launch {
						mutableStateSubject.emit(
							State.Syncing(
								repository.name,
								stage,
								progress,
								total
							)
						)
					}
				}
			currentTask = null
			when (request) {
				Result.Loading -> {
					mutableStateSubject.emit(State.Connecting(repository.name))
				}
				is Result.Error -> {
					request.exception?.printStackTrace()
					if (task.manual) showNotificationError(
						repository,
						request.exception as Exception
					)
					handleNextTask(request.data == true || hasUpdates)
				}
				is Result.Success -> handleNextTask(request.data || hasUpdates)
			}
		}
		currentTask = CurrentTask(task, job, hasUpdates, initialState)
	}

	private suspend fun handleUpdates(
		hasUpdates: Boolean,
		notifyUpdates: Boolean,
		autoUpdate: Boolean
	) {
		if (hasUpdates && notifyUpdates) {
			val job = scope.launch {
				val products = async {
					Database.ProductAdapter
						.query(
							installed = true,
							updates = true,
							searchQuery = "",
							section = ProductItem.Section.All,
							order = SortOrder.NAME,
							signal = null
						)
						.use {
							it.asSequence().map(Database.ProductAdapter::transformItem).toList()
						}
				}
				currentTask = null
				handleNextTask(false)
				if (autoUpdate) {
					products.cancel()
					updateAllAppsInternal()
				} else {
					val availableUpdate = products.await()
					displayUpdatesNotification(availableUpdate)
				}
			}
			currentTask = CurrentTask(null, job, true, State.Finishing)
		} else {
			scope.launch { mutableFinishState.emit(Unit) }
			val needStop = started == Started.MANUAL
			started = Started.NO
			if (needStop) {
				@Suppress("DEPRECATION")
				if (SdkCheck.isNougat) stopForeground(STOP_FOREGROUND_REMOVE)
				else stopForeground(true)
				stopSelf()
			}
		}
	}

	private suspend fun updateAllAppsInternal() = withContext(Dispatchers.IO) {
		val products = async {
			Database.ProductAdapter
				.query(
					installed = true,
					updates = true,
					searchQuery = "",
					section = ProductItem.Section.All,
					order = SortOrder.NAME,
					signal = null
				)
				.use {
					it.asSequence().map(Database.ProductAdapter::transformItem).toList()
				}
		}
		products.await().map {
			Database.InstalledAdapter.get(it.packageName, null) to
					Database.RepositoryAdapter.get(it.repositoryId)
		}
			.filter { it.first != null && it.second != null }
			.forEach { (installItem, repo) ->
				val productRepo = Database.ProductAdapter.get(installItem!!.packageName, null)
					.filter { it.repositoryId == repo!!.id }
					.map { async { it to repo!! } }
				Utils.startUpdate(
					installItem.packageName,
					installItem,
					productRepo.awaitAll(),
					downloadConnection
				)
			}
	}

	private fun displayUpdatesNotification(productItems: List<ProductItem>) {
		val maxUpdates = 5
		fun <T> T.applyHack(callback: T.() -> Unit): T = apply(callback)
		notificationManager.notify(
			Constants.NOTIFICATION_ID_UPDATES, NotificationCompat
				.Builder(this, Constants.NOTIFICATION_CHANNEL_UPDATES)
				.setSmallIcon(R.drawable.ic_new_releases)
				.setContentTitle(getString(stringRes.new_updates_available))
				.setContentText(
					resources.getQuantityString(
						R.plurals.new_updates_DESC_FORMAT,
						productItems.size, productItems.size
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
				.setStyle(NotificationCompat.InboxStyle().applyHack {
					for (productItem in productItems.take(maxUpdates)) {
						val builder = SpannableStringBuilder(productItem.name)
						builder.setSpan(
							ForegroundColorSpan(Color.BLACK), 0, builder.length,
							SpannableStringBuilder.SPAN_EXCLUSIVE_EXCLUSIVE
						)
						builder.append(' ').append(productItem.version)
						addLine(builder)
					}
					if (productItems.size > maxUpdates) {
						val summary =
							getString(stringRes.plus_more_FORMAT, productItems.size - maxUpdates)
						if (SdkCheck.isNougat) addLine(summary) else setSummaryText(summary)
					}
				})
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
					binder.finish.collect {
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
	}
}