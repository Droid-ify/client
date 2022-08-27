package com.looker.droidify.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import android.view.ContextThemeWrapper
import androidx.core.app.NotificationCompat
import androidx.core.app.TaskStackBuilder
import com.looker.core_common.Common
import com.looker.core_common.cache.Cache
import com.looker.core_common.formatSize
import com.looker.core_common.hex
import com.looker.core_common.notificationManager
import com.looker.core_common.nullIfEmpty
import com.looker.core_common.result.Result
import com.looker.core_datastore.UserPreferencesRepository
import com.looker.core_datastore.model.InstallerType
import com.looker.core_model.Release
import com.looker.droidify.BuildConfig
import com.looker.droidify.MainActivity
import com.looker.droidify.R
import com.looker.droidify.network.Downloader
import com.looker.droidify.utility.Utils.calculateHash
import com.looker.droidify.utility.extension.android.Android
import com.looker.droidify.utility.extension.android.singleSignature
import com.looker.droidify.utility.extension.android.versionCodeCompat
import com.looker.droidify.utility.extension.app_file.installApk
import com.looker.droidify.utility.extension.resources.getColorFromAttr
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.math.roundToInt
import com.looker.core_common.R.string as stringRes
import com.looker.core_common.R.style as styleRes

@AndroidEntryPoint
class DownloadService : ConnectionService<DownloadService.Binder>() {
	companion object {
		private const val ACTION_CANCEL = "${BuildConfig.APPLICATION_ID}.intent.action.CANCEL"
	}

	val scope = CoroutineScope(Dispatchers.IO)

	@Inject
	lateinit var userPreferencesRepository: UserPreferencesRepository

	sealed class State(val packageName: String, val name: String) {
		object EMPTY : State("", "")
		class Pending(packageName: String, name: String) : State(packageName, name)
		class Connecting(packageName: String, name: String) : State(packageName, name)
		class Downloading(packageName: String, name: String, val read: Long, val total: Long?) :
			State(packageName, name)

		class Success(
			packageName: String, name: String, val release: Release
		) : State(packageName, name)

		class Error(packageName: String, name: String) : State(packageName, name)
		class Cancel(packageName: String, name: String) : State(packageName, name)
	}

	private val mutableDownloadState = MutableStateFlow<State>(State.EMPTY)
	private val mutableStateSubject = MutableSharedFlow<State>()

	private class Task(
		val packageName: String, val name: String, val release: Release,
		val url: String, val authentication: String,
	) {
		val notificationTag: String
			get() = "download-$packageName"
	}

	private data class CurrentTask(val task: Task, val job: Job, val lastState: State)

	private var started = false
	private val tasks = mutableListOf<Task>()
	private var currentTask: CurrentTask? = null

	inner class Binder : android.os.Binder() {
		val stateSubject = mutableStateSubject.asSharedFlow()

		fun enqueue(
			packageName: String,
			name: String,
			repository: com.looker.core_model.Repository,
			release: Release
		) {
			val task = Task(
				packageName,
				name,
				release,
				release.getDownloadUrl(repository),
				repository.authentication
			)
			if (Cache.getReleaseFile(this@DownloadService, release.cacheFileName).exists()) {
				runBlocking { publishSuccess(task) }
			} else {
				cancelTasks(packageName)
				cancelCurrentTask(packageName)
				notificationManager.cancel(task.notificationTag, Common.NOTIFICATION_ID_DOWNLOADING)
				tasks += task
				if (currentTask == null) {
					handleDownload()
				} else {
					scope.launch { mutableStateSubject.emit(State.Pending(packageName, name)) }
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

		if (Android.sdk(26)) {
			NotificationChannel(
				Common.NOTIFICATION_CHANNEL_DOWNLOADING,
				getString(stringRes.downloading), NotificationManager.IMPORTANCE_LOW
			)
				.apply { setShowBadge(false) }
				.let(notificationManager::createNotificationChannel)
		}

		mutableDownloadState.onEach { publishForegroundState(false, it) }.launchIn(scope)
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
				scope.launch { mutableStateSubject.emit(State.Cancel(it.packageName, it.name)) }
				true
			}
		}
	}

	private fun cancelCurrentTask(packageName: String?) {
		currentTask?.let {
			if (packageName == null || it.task.packageName == packageName) {
				currentTask = null
				scope.launch {
					mutableStateSubject.emit(
						State.Cancel(
							it.task.packageName,
							it.task.name
						)
					)
				}
				it.job.cancel()
			}
		}
	}

	private enum class ValidationError { INTEGRITY, FORMAT, METADATA, SIGNATURE, PERMISSIONS }

	private sealed class ErrorType {
		object Network : ErrorType()
		object Http : ErrorType()
		class Validation(val validateError: ValidationError) : ErrorType()
	}

	private fun showNotificationError(task: Task, errorType: ErrorType) {
		val intent = Intent(this, MainActivity::class.java)
			.setAction(Intent.ACTION_VIEW)
			.setData(Uri.parse("package:${task.packageName}"))
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
			addNextIntentWithParentStack(intent)
			getPendingIntent(
				0,
				if (Android.sdk(31)) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
				else PendingIntent.FLAG_UPDATE_CURRENT
			)
		}
		notificationManager.notify(task.notificationTag,
			Common.NOTIFICATION_ID_DOWNLOADING,
			NotificationCompat
				.Builder(this, Common.NOTIFICATION_CHANNEL_DOWNLOADING)
				.setAutoCancel(true)
				.setSmallIcon(android.R.drawable.stat_sys_warning)
				.setColor(
					ContextThemeWrapper(this, styleRes.Theme_Main_Light)
						.getColorFromAttr(R.attr.colorPrimary).defaultColor
				)
				.setContentIntent(resultPendingIntent)
				.apply {
					when (errorType) {
						is ErrorType.Network -> {
							setContentTitle(
								getString(
									stringRes.could_not_download_FORMAT,
									task.name
								)
							)
							setContentText(getString(stringRes.network_error_DESC))
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
				.build())
	}

	private fun showNotificationInstall(task: Task) {
		val intent = Intent(this, MainActivity::class.java)
			.setAction(MainActivity.ACTION_INSTALL)
			.setData(Uri.parse("package:$task.packageName"))
			.putExtra(MainActivity.EXTRA_CACHE_FILE_NAME, task.release.cacheFileName)
			.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
			addNextIntentWithParentStack(intent)
			getPendingIntent(
				0,
				if (Android.sdk(31)) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
				else PendingIntent.FLAG_UPDATE_CURRENT
			)
		}
		notificationManager.notify(
			task.notificationTag, Common.NOTIFICATION_ID_DOWNLOADING, NotificationCompat
				.Builder(this, Common.NOTIFICATION_CHANNEL_DOWNLOADING)
				.setAutoCancel(true)
				.setSmallIcon(android.R.drawable.stat_sys_download_done)
				.setColor(
					ContextThemeWrapper(this, styleRes.Theme_Main_Light)
						.getColorFromAttr(R.attr.colorPrimary).defaultColor
				)
				.setContentIntent(resultPendingIntent)
				.setContentTitle(getString(stringRes.downloaded_FORMAT, task.name))
				.setContentText(getString(stringRes.tap_to_install_DESC))
				.build()
		)
	}

	private suspend fun publishSuccess(task: Task) {
		mutableStateSubject.emit(State.Success(task.packageName, task.name, task.release))
		val installerType =
			userPreferencesRepository.fetchInitialPreferences().installerType
		if (installerType == InstallerType.ROOT || installerType == InstallerType.SHIZUKU) {
			task.packageName.installApk(
				this@DownloadService,
				task.release.cacheFileName,
				installerType
			)
		} else showNotificationInstall(task)
	}

	private fun validatePackage(task: Task, file: File): ValidationError? {
		val hash = try {
			val hashType = task.release.hashType.nullIfEmpty() ?: "SHA256"
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
		return if (hash.isEmpty() || hash != task.release.hash) {
			ValidationError.INTEGRITY
		} else {
			val packageInfo = try {
				packageManager.getPackageArchiveInfo(
					file.path,
					Android.PackageManager.signaturesFlag
				)
			} catch (e: Exception) {
				e.printStackTrace()
				null
			}
			if (packageInfo == null) {
				ValidationError.FORMAT
			} else if (packageInfo.packageName != task.packageName ||
				packageInfo.versionCodeCompat != task.release.versionCode
			) {
				ValidationError.METADATA
			} else {
				val signature = packageInfo.singleSignature?.calculateHash.orEmpty()
				if (signature.isEmpty() || signature != task.release.signature) {
					ValidationError.SIGNATURE
				} else {
					val permissions =
						packageInfo.permissions?.asSequence().orEmpty().map { it.name }.toSet()
					if (!task.release.permissions.containsAll(permissions)) {
						ValidationError.PERMISSIONS
					} else {
						null
					}
				}
			}
		}
	}

	private val stateNotificationBuilder by lazy {
		NotificationCompat
			.Builder(this, Common.NOTIFICATION_CHANNEL_DOWNLOADING)
			.setSmallIcon(android.R.drawable.stat_sys_download)
			.setColor(
				ContextThemeWrapper(this, styleRes.Theme_Main_Light)
					.getColorFromAttr(R.attr.colorPrimary).defaultColor
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

	private suspend fun publishForegroundState(force: Boolean, state: State) =
		withContext(Dispatchers.Default) {
			if (force || currentTask != null) {
				currentTask = currentTask?.copy(lastState = state)
				startForeground(Common.NOTIFICATION_ID_SYNCING, stateNotificationBuilder.apply {
					when (state) {
						is State.Connecting -> {
							setContentTitle(getString(stringRes.downloading_FORMAT, state.name))
							setContentText(getString(stringRes.connecting))
							setProgress(1, 0, true)
						}
						is State.Downloading -> {
							setContentTitle(getString(stringRes.downloading_FORMAT, state.name))
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
						is State.Pending, is State.Success, is State.Error, is State.Cancel -> {
							throw IllegalStateException()
						}
						State.EMPTY -> setProgress(1, 0, true)
					}::class
				}.build())
				mutableStateSubject.emit(state)
			}
		}

	private fun handleDownload() {
		if (currentTask == null) {
			if (tasks.isNotEmpty()) {
				val task = tasks.removeAt(0)
				if (!started) {
					started = true
					startSelf()
				}
				val initialState = State.Connecting(task.packageName, task.name)
				val intent = Intent(this, MainActivity::class.java)
					.setAction(Intent.ACTION_VIEW)
					.setData(Uri.parse("package:${task.packageName}"))
					.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
				val resultPendingIntent: PendingIntent? = TaskStackBuilder.create(this).run {
					addNextIntentWithParentStack(intent)
					getPendingIntent(
						0,
						if (Android.sdk(31)) PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
						else PendingIntent.FLAG_UPDATE_CURRENT
					)
				}
				stateNotificationBuilder.setWhen(System.currentTimeMillis())
				stateNotificationBuilder.setContentIntent(resultPendingIntent)
				scope.launch { publishForegroundState(true, initialState) }
				val partialReleaseFile =
					Cache.getPartialReleaseFile(this, task.release.cacheFileName)
				val job = scope.launch {
					val result = Downloader.downloadFile(
						task.url,
						partialReleaseFile,
						"",
						"",
						task.authentication
					) { read, total ->
						launch {
							mutableDownloadState.emit(
								State.Downloading(
									task.packageName,
									task.name,
									read,
									total
								)
							)
						}
					}
					currentTask = null
					when (result) {
						Result.Loading -> {
							launch {
								mutableStateSubject.emit(
									State.Connecting(
										packageName = task.packageName,
										name = task.name
									)
								)
							}
						}
						is Result.Error -> result.exception?.printStackTrace()
						is Result.Success -> {
							if (!result.data.success) {
								showNotificationError(task, ErrorType.Http)
								launch {
									mutableStateSubject.emit(
										State.Error(
											task.packageName,
											task.name
										)
									)
								}
							} else {
								val validationError = validatePackage(task, partialReleaseFile)
								if (validationError == null) {
									val releaseFile =
										Cache.getReleaseFile(
											this@DownloadService,
											task.release.cacheFileName
										)
									partialReleaseFile.renameTo(releaseFile)
									publishSuccess(task)
								} else {
									partialReleaseFile.delete()
									showNotificationError(
										task,
										ErrorType.Validation(validationError)
									)
									launch {
										mutableStateSubject.emit(
											State.Error(
												task.packageName,
												task.name
											)
										)
									}
								}
							}
						}
					}
					handleDownload()
				}
				currentTask = CurrentTask(task, job, initialState)
			} else if (started) {
				started = false
				stopForeground(true)
				stopSelf()
			}
		}
	}
}
