package com.looker.droidify.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.looker.core.common.formatSize
import com.looker.core.common.percentBy
import com.looker.downloader.Downloader
import com.looker.downloader.KtorDownloader
import com.looker.downloader.model.DownloadItem
import com.looker.downloader.model.DownloadState
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.ktor.client.HttpClient
import io.ktor.client.engine.okhttp.OkHttp
import rikka.shizuku.SystemServiceHelper.getSystemService
import java.io.File
import com.looker.core.common.R as CommonR

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParameters: WorkerParameters
) : CoroutineWorker(context, workerParameters) {
	private val client = HttpClient(OkHttp)
	private val downloader: Downloader = KtorDownloader(client)
	override suspend fun doWork(): Result {
		val url = inputData.getString(INPUT_URL) ?: return Result.failure()
		val name = inputData.getString(INPUT_NAME) ?: return Result.failure()
		val file = inputData.getString(INPUT_FILE_LOCATION) ?: return Result.failure()
		val item = DownloadItem(name, url, File(file))
		setForeground(createForegroundInfo(name, DownloadState.Pending))
		download(item)
		return Result.success()
	}

	private suspend fun download(downloadItem: DownloadItem) {
		downloader.download(downloadItem).collect {
			setForegroundAsync(createForegroundInfo(downloadItem.name, it))
		}
	}

	private fun createForegroundInfo(appName: String, state: DownloadState): ForegroundInfo {
		val downloadedTitle =
			applicationContext.getString(CommonR.string.downloaded_FORMAT, appName)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) createNotificationChannel()

		val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
			.setContentTitle(downloadedTitle)
			.setOnlyAlertOnce(true)
			.apply {
				when (state) {
					is DownloadState.Error -> {
						setAutoCancel(true)
						val errorTitle = applicationContext.getString(CommonR.string.network_error_DESC)
						setContentTitle(errorTitle)
						setContentTitle(errorTitle)
						setSmallIcon(android.R.drawable.ic_dialog_alert)
						setProgress(0, 0, false)
						clearActions()
					}
					DownloadState.Pending -> {
						setAutoCancel(false)
						setContentText(applicationContext.getString(CommonR.string.connecting))
						setProgress(0, 0, true)
						setSmallIcon(android.R.drawable.stat_sys_download)
						clearActions()
					}
					is DownloadState.Progress -> {
						setAutoCancel(false)
						setOngoing(true)
						setSmallIcon(android.R.drawable.stat_sys_download)
						val cancel = applicationContext.getString(CommonR.string.cancel)
						val cancelIntent = WorkManager.getInstance(applicationContext)
							.createCancelPendingIntent(id)
						addAction(CommonR.drawable.ic_cancel, cancel, cancelIntent)
						val description =
							"${state.current.formatSize()} / ${state.total.formatSize()}"
						val percentDownloaded = state.current percentBy state.total
						setProgress(100, percentDownloaded, false)
						this@DownloadWorker.setProgressAsync(workDataOf(Progress to percentDownloaded))
						setContentText(description)
					}
					is DownloadState.Success -> {
						setAutoCancel(true)
						val installDesc = applicationContext.getString(CommonR.string.tap_to_install_DESC)
						setContentText(installDesc)
						setSmallIcon(android.R.drawable.stat_sys_download_done)
						setProgress(0, 0, false)
						clearActions()
					}
				}
			}.build()
		return ForegroundInfo(NOTIFICATION_ID, notification)
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createNotificationChannel() {
		val channel = NotificationChannel(
			CHANNEL_ID,
			"Download Notification",
			NotificationManager.IMPORTANCE_DEFAULT
		).apply {
			description = "Shows notification for download"
		}
		val notificationManager: NotificationManager? =
			getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

		notificationManager?.createNotificationChannel(channel)
	}

	companion object {
		const val Progress = "Progress"
		private const val TAG = "DownloadWorker"
		private const val CHANNEL_ID = "DownloadChannel"
		private const val NOTIFICATION_ID = 69420

		private const val INPUT_URL = "input_url"
		private const val INPUT_NAME = "input_name"
		private const val INPUT_FILE_LOCATION = "input_file_name"

		fun Context.enqueueDownload(item: DownloadItem) {
			WorkManager.getInstance(this).apply {
				val request = OneTimeWorkRequestBuilder<DownloadWorker>()
					.setInputData(
						Data.Builder()
							.putAll(
								mapOf(
									INPUT_URL to item.url,
									INPUT_NAME to item.name,
									INPUT_FILE_LOCATION to item.file.path
								)
							)
							.build()
					)
					.build()
				enqueueUniqueWork(
					item.id,
					ExistingWorkPolicy.APPEND,
					request
				)
			}
		}
	}
}