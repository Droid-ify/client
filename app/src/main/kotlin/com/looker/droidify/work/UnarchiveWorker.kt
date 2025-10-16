package com.looker.droidify.work

import android.content.Context
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ALL_USERS
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_ID
import android.content.pm.PackageInstaller.EXTRA_UNARCHIVE_PACKAGE_NAME
import android.content.pm.PackageManager
import android.content.pm.PackageManager.MATCH_ARCHIVED_PACKAGES
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.OutOfQuotaPolicy
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.looker.droidify.data.model.PackageName
import com.looker.droidify.database.Database
import com.looker.droidify.datastore.SettingsRepository
import com.looker.droidify.installer.InstallManager
import com.looker.droidify.installer.model.InstallItem
import com.looker.droidify.model.Product
import com.looker.droidify.model.Repository
import com.looker.droidify.service.Connection
import com.looker.droidify.service.DownloadService
import com.looker.droidify.utility.common.extension.calculateHash
import com.looker.droidify.utility.common.extension.singleSignature
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

@HiltWorker
@RequiresApi(Build.VERSION_CODES.VANILLA_ICE_CREAM)
class UnarchiveWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val installManager: InstallManager
) : CoroutineWorker(context, workerParams) {
    companion object {
        private const val TAG = "CleanUpWorker"

        fun updateNow(context: Context, packageName: String, unarchiveId: Int, allUsers: Boolean) {
            val data = Data.Builder()
                .putString(EXTRA_UNARCHIVE_PACKAGE_NAME, packageName)
                .putInt(EXTRA_UNARCHIVE_ID, unarchiveId)
                .putBoolean(EXTRA_UNARCHIVE_ALL_USERS, allUsers)
                .build()
            val workRequest = OneTimeWorkRequestBuilder<UnarchiveWorker>()
                .setInputData(data)
                .setExpedited(OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST)
                .build()
            WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    suspend fun downloadProductAndWait(
        context: Context,
        packageName: String,
        product: Product,
        repository: Repository
    ): DownloadService.DownloadState {
        lateinit var connection: Connection<DownloadService.Binder, DownloadService>
        val binder = suspendCancellableCoroutine { cont ->
            val c = Connection(
                serviceClass = DownloadService::class.java,
                onBind = { _, binder ->
                    cont.resume(binder) { cause, _, _ -> connection.unbind(context) }
                }
            )
            connection = c
            c.bind(context)
        }

        binder.enqueue(
            packageName,
            product.name,
            repository,
            product.releases.first()
        )

        // Wait until the download completes
        val downloadResult = binder.downloadState.last()
        connection.unbind(context)
        return downloadResult
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val packageName = inputData.getString(EXTRA_UNARCHIVE_PACKAGE_NAME) ?: return@withContext Result.failure()
        val unarchiveId = inputData.getInt(EXTRA_UNARCHIVE_ID, -1)

        val packageManger = applicationContext.packageManager
        val packageInfo = packageManger.getPackageInfo(
            packageName,
            PackageManager.PackageInfoFlags.of(MATCH_ARCHIVED_PACKAGES)
        )
        val sig = packageInfo.singleSignature?.calculateHash()

        val product = Database.ProductAdapter.getArchivedApp(packageName)
            .filter { it.compatible && (sig == null || it.signatures.contains(sig)) }
            .maxByOrNull { it.versionCode }
        if (product == null) {
            Log.e(TAG, "doWork: failed to find a matching app for $packageName")
            return@withContext Result.failure()
        }
        val repository = Database.RepositoryAdapter.get(product.repositoryId)
            ?: return@withContext Result.failure()


        val result = downloadProductAndWait(applicationContext,
            packageName,
            product,
            repository,
        )

        if (result.currentItem !is DownloadService.State.Success) {
            Log.e(TAG, "doWork: failed to download ", )
            return@withContext Result.failure()
        }

        installManager.install(
            InstallItem(
                PackageName(packageName),
                result.currentItem.release.cacheFileName,
                unarchiveId,
            )
        )
        Result.success()
    }
}
