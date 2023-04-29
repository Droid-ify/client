package com.looker.droidify.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import com.looker.core.common.*
import com.looker.core.common.cache.Cache
import com.looker.core.common.extension.*
import com.looker.core.common.result.Result.*
import com.looker.core.data.downloader.NetworkResponse
import com.looker.core.datastore.UserPreferencesRepository
import com.looker.core.datastore.model.InstallerType
import com.looker.core.model.Release
import com.looker.core.model.Repository
import com.looker.droidify.BuildConfig
import com.looker.droidify.MainActivity
import com.looker.droidify.utility.extension.android.getPackageArchiveInfoCompat
import com.looker.installer.Installer
import com.looker.installer.model.installFrom
import dagger.hilt.android.AndroidEntryPoint
import io.ktor.http.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import com.looker.core.common.R as CommonR
import com.looker.core.common.R.string as stringRes
import com.looker.core.common.R.style as styleRes

@AndroidEntryPoint
class DownloadService : ConnectionService<DownloadService.Binder>() {
	companion object {
		private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"
	}

	val scope = CoroutineScope(Dispatchers.Default)

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository

	@Inject
	lateinit var downloader: com.looker.core.data.downloader.Downloader

	private val installerType = flow {
		emit(userPreferencesRepository.fetchInitialPreferences().installerType)
	}

	@Inject
	lateinit var installer: Installer

	sealed class State(val packageName: String) {
		object Idle : State("")
		class Pending(packageName: String) : State(packageName)
		class Connecting(packageName: String) : State(packageName)
		class Downloading(packageName: String, val read: Long, val total: Long?) :
			State(packageName)

		class Error(packageName: String) : State(packageName)
		class Cancel(packageName: String) : State(packageName)
		class Success(packageName: String, val release: Release) : State(packageName)
	}

	private val mutableState = MutableStateFlow<State>(State.Idle)

	private class Task(
		val packageName: String, val name: String, val release: Release,
		val url: String, val authentication: String,
		val updating: Boolean = false
	) {
		val notificationTag: String
			get() = "download-$packageName"
	}

	private data class CurrentTask(val task: Task, val job: Job, val lastState: State)

	private var started = false
	private val tasks = mutableListOf<Task>()
	private var currentTask: CurrentTask? = null

	inner class Binder : android.os.Binder() {
		val stateFlow = mutableState.stateIn(
			scope = scope,
			started = SharingStarted.WhileSubscribed(5_000),
			initialValue = State.Idle
		)

		fun enqueue(
			packageName: String,
			name: String,
			repository: Repository,
			release: Release,
			updating: Boolean = false
		) {
			val task = Task(
				packageName = packageName,
				name = name,
				release = release,
				url = release.getDownloadUrl(repository),
				authentication = repository.authentication,
				updating = updating
			)
			if (Cache.getReleaseFile(this@DownloadService, release.cacheFileName).exists()) {
				scope.launch(Dispatchers.Main) { publishSuccess(task) }
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
					scope.launch { mutableState.emit(State.Pending(packageName)) }
				}
			}
		}

		fun cancel(packageName: String) {
			cancelTasks(packageName)
			cancelCurrentTask(packageName)
			handleDownload()
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
	}

	override fun onDestroy() {
		super.onDestroy()

		scope.cancel()
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
				scope.launch { mutableState.emit(State.Cancel(it.packageName)) }
				true
			}
		}
	}

	private fun cancelCurrentTask(packageName: String?) {
		currentTask?.let {
			if (packageName == null || it.task.packageName == packageName) {
				currentTask = null
				scope.launch {
					mutableState.emit(
						State.Cancel(
							it.task.packageName
						)
					)
				}
				it.job.cancel()
			}
		}
	}

	private enum class ValidationError { INTEGRITY, FORMAT, METADATA, SIGNATURE, PERMISSIONS }

	private sealed interface ErrorType {
		object IO : ErrorType
		object Http : ErrorType
		class Validation(val validateError: ValidationError) : ErrorType
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
				.apply {
					errorNotificationContent(task, errorType)
				}
				.build())
	}

	private fun NotificationCompat.Builder.errorNotificationContent(
		task: Task,
		errorType: ErrorType
	) {
		when (errorType) {
			is ErrorType.IO -> {
				setContentTitle(
					getString(
						stringRes.could_not_download_FORMAT,
						task.name
					)
				)
				setContentText(getString(stringRes.io_error_DESC))
			}

			is ErrorType.Http -> {
				setContentTitle(
					getString(
						stringRes.could_not_download_FORMAT,
						task.name
					)
				)
				setContentText(getString(stringRes.http_error_DESC))
			}

			is ErrorType.Validation -> {
				setContentTitle(
					getString(
						stringRes.could_not_validate_FORMAT,
						task.name
					)
				)
				setContentText(
					getString(
						when (errorType.validateError) {
							ValidationError.INTEGRITY -> stringRes.integrity_check_error_DESC
							ValidationError.FORMAT -> stringRes.file_format_error_DESC
							ValidationError.METADATA -> stringRes.invalid_metadata_error_DESC
							ValidationError.SIGNATURE -> stringRes.invalid_signature_error_DESC
							ValidationError.PERMISSIONS -> stringRes.invalid_permissions_error_DESC
						}
					)
				)
			}
		}::class
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
		mutableState.emit(State.Success(task.packageName, task.release))
		val autoInstallWithSessionInstaller =
			SdkCheck.canAutoInstall(task.release.targetSdkVersion)
					&& currentInstaller == InstallerType.SESSION
					&& task.updating

		showNotificationInstall(task)
		if (currentInstaller == InstallerType.ROOT
			|| currentInstaller == InstallerType.SHIZUKU
			|| autoInstallWithSessionInstaller
		) {
			val installItem = task.packageName installFrom task.release.cacheFileName
			installer + installItem
		}
	}

	private suspend fun validatePackage(
		task: Task,
		file: File
	): ValidationError? = withContext(Dispatchers.IO) {
		val hash = try {
			val hashType = task.release.hashType.ifEmpty { "SHA256" }
			val digest = MessageDigest.getInstance(hashType)
			file.inputStream().use {
				val bytes = ByteArray(8 * 1024)
				generateSequence { it.read(bytes) }.takeWhile { it >= 0 }
					.forEach { digest.update(bytes, 0, it) }
				digest.digest().hex()
			}
		} catch (e: Exception) {
			""
		}
		var validationError: ValidationError? = null
		if (hash.isEmpty() || hash != task.release.hash) validationError = ValidationError.INTEGRITY
		yield()
		val packageInfo = packageManager.getPackageArchiveInfoCompat(file.path)
			?: return@withContext ValidationError.FORMAT
		if (packageInfo.packageName != task.packageName || packageInfo.versionCodeCompat != task.release.versionCode) validationError =
			ValidationError.METADATA
		yield()
		val signature = packageInfo.singleSignature?.calculateHash().orEmpty()
		if (signature.isEmpty() || signature != task.release.signature) validationError =
			ValidationError.SIGNATURE
		yield()
		val permissions = packageInfo.permissions?.asSequence().orEmpty().map { it.name }.toSet()
		if (!task.release.permissions.containsAll(permissions)) validationError =
			ValidationError.PERMISSIONS
		yield()
		validationError
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
		currentTask = currentTask?.copy(lastState = state)
		startForeground(
			Constants.NOTIFICATION_ID_DOWNLOADING,
			stateNotificationBuilder.apply {
				when (state) {
					is State.Connecting -> {
						setContentTitle(
							getString(stringRes.downloading_FORMAT, currentTask?.task?.name)
						)
						setContentText(getString(stringRes.connecting))
						setProgress(1, 0, true)
					}

					is State.Downloading -> {
						setContentTitle(
							getString(stringRes.downloading_FORMAT, currentTask?.task?.name)
						)
						if (state.total != null) {
							setContentText("${state.read.formatSize()} / ${state.total.formatSize()}")
							setProgress(100, state.read percentBy state.total, false)
						} else {
							setContentText(state.read.formatSize())
							setProgress(0, 0, true)
						}
					}

					else -> throw IllegalStateException()
				}::class
			}.build()
		)
		mutableState.tryEmit(state)
	}

	private fun handleDownload() {
		if (currentTask != null) return
		if (tasks.isEmpty() && started) {
			started = false
			@Suppress("DEPRECATION")
			if (SdkCheck.isNougat) stopForeground(STOP_FOREGROUND_REMOVE)
			else stopForeground(true)
			stopSelf()
			return
		}
		if (!started) {
			started = true
			startSelf()
		}
		val task = tasks.removeAt(0)
		val intent = Intent(this, MainActivity::class.java)
			.setAction(Intent.ACTION_VIEW)
			.setData(Uri.parse("package:${task.packageName}"))
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			.getPendingIntent(this)
		stateNotificationBuilder.setWhen(System.currentTimeMillis())
		stateNotificationBuilder.setContentIntent(intent)
		publishForegroundState(true, State.Connecting(task.packageName))
		val partialReleaseFile =
			Cache.getPartialReleaseFile(this, task.release.cacheFileName)
		val job = scope.launch {
			val response = downloader.downloadToFile(
				url = task.url,
				target = partialReleaseFile,
				headers = mapOf(HttpHeaders.Authorization to task.authentication)
			) { read, total ->
				publishForegroundState(false, State.Downloading(task.packageName, read, total))
			}
			currentTask = null
			yield()
			when (response) {
				NetworkResponse.Success -> {
					val validationError = validatePackage(task, partialReleaseFile)
					if (validationError == null) {
						val releaseFile = Cache.getReleaseFile(
							this@DownloadService,
							task.release.cacheFileName
						)
						partialReleaseFile.renameTo(releaseFile)
						publishSuccess(task)
					} else {
						partialReleaseFile.delete()
						showNotificationError(task, ErrorType.Validation(validationError))
						mutableState.emit(State.Error(task.packageName))
					}
				}

				is NetworkResponse.Error -> {
					showNotificationError(task, ErrorType.Http)
					mutableState.emit(State.Error(task.packageName))
				}
			}
			yield()
			handleDownload()
		}
		currentTask = CurrentTask(task, job, State.Connecting(task.packageName))
	}
}
