package com.looker.droidify.work

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.core_common.Common.NOTIFICATION_CHANNEL_DOWNLOADING
import com.looker.core_common.Common.NOTIFICATION_ID_DOWNLOADING
import com.looker.core_datastore.UserPreferencesRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import com.looker.core_common.R.drawable as drawableRes
import com.looker.core_common.R.string as stringRes

@HiltWorker
class DownloadWorker @AssistedInject constructor(
	@Assisted context: Context,
	@Assisted workerParams: WorkerParameters,
	private val userPreferencesRepository: UserPreferencesRepository
) : CoroutineWorker(context, workerParams) {

	private val notificationManager =
		context.getSystemService(Context.NOTIFICATION_SERVICE) as
				NotificationManager


	override suspend fun doWork(): Result {
		val inputUrl = inputData.getString(KEY_INPUT_URL)
			?: return Result.failure()
		val outputFile = inputData.getString(KEY_OUTPUT_FILE_NAME)
			?: return Result.failure()
		val progress = "Starting Download"
		setForeground(createForegroundInfo(progress))
		download(inputUrl, outputFile)
		return Result.success()
	}

	private fun download(inputUrl: String, outputFile: String) {

	}

	private fun createForegroundInfo(notificationTitle: String, progress: Int = 0): ForegroundInfo {
		val cancelText = applicationContext.getString(stringRes.cancel)

		val cancelIntent = WorkManager.getInstance(applicationContext)
			.createCancelPendingIntent(id)

		val builder =
			NotificationCompat.Builder(applicationContext, NOTIFICATION_CHANNEL_DOWNLOADING)
				.setContentTitle(notificationTitle)
				.setTicker(notificationTitle)
				.setSmallIcon(drawableRes.ic_download)
				.setProgress(100, progress, false)
				.setOngoing(true)
				.addAction(drawableRes.ic_cancel, cancelText, cancelIntent)
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			createChannel().also {
				builder.setChannelId(it.id)
			}
		}
		return if (Build.VERSION_CODES.Q >= Build.VERSION.SDK_INT) {
			ForegroundInfo(
				NOTIFICATION_ID_DOWNLOADING,
				builder.build(),
				FOREGROUND_SERVICE_TYPE_DATA_SYNC
			)
		} else {
			ForegroundInfo(
				NOTIFICATION_ID_DOWNLOADING,
				builder.build()
			)
		}
	}

	@RequiresApi(Build.VERSION_CODES.O)
	private fun createChannel() = NotificationChannel(
		NOTIFICATION_CHANNEL_DOWNLOADING,
		applicationContext.getString(stringRes.downloading),
		NotificationManager.IMPORTANCE_DEFAULT
	).also { channel -> notificationManager.createNotificationChannel(channel) }

	companion object {
		const val KEY_INPUT_URL = "KEY_INPUT_URL"
		const val KEY_OUTPUT_FILE_NAME = "KEY_OUTPUT_FILE_NAME"

		private val downloadQueue = mutableSetOf<DownloadFile>()

		fun addDownloadQueue(downloadFile: DownloadFile) {
			downloadQueue.add(downloadFile)
		}
	}

	data class DownloadFile(
		val fileName: String,
		val fileUrl: String
	)
}