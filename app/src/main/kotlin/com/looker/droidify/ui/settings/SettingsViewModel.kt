package com.looker.droidify.ui.settings

import android.R.attr.text
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.util.Base64
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.looker.core.common.extension.toLocale
import com.looker.core.datastore.Settings
import com.looker.core.datastore.SettingsRepository
import com.looker.core.datastore.get
import com.looker.core.datastore.model.AutoSync
import com.looker.core.datastore.model.InstallerType
import com.looker.core.datastore.model.ProxyType
import com.looker.core.datastore.model.Theme
import com.looker.core.domain.Repository
import com.looker.droidify.database.Database
import com.looker.droidify.database.RepositoryExporter
import com.looker.droidify.service.Connection
import com.looker.droidify.service.SyncService
import com.looker.droidify.ui.Message
import com.looker.droidify.ui.MessageDialog
import com.looker.droidify.utility.extension.screenActivity
import com.looker.droidify.work.CleanUpWorker
import com.looker.installer.installers.shizuku.ShizukuPermissionHandler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import javax.inject.Inject
import kotlin.time.Duration
import com.looker.core.common.R as CommonR


@HiltViewModel
class SettingsViewModel
@Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val shizukuPermissionHandler: ShizukuPermissionHandler,
    private val repositoryExporter: RepositoryExporter
) : ViewModel() {

    private val initialSetting = flow {
        emit(settingsRepository.getInitial())
    }
    val settingsFlow get() = settingsRepository.data

    private val _snackbarStringId = MutableSharedFlow<Int>()
    val snackbarStringId = _snackbarStringId.asSharedFlow()

    fun <T> getSetting(block: Settings.() -> T): Flow<T> = settingsRepository.get(block)

    fun <T> getInitialSetting(block: Settings.() -> T): Flow<T> = initialSetting.map { it.block() }

    fun setLanguage(language: String) {
        viewModelScope.launch {
            val appLocale = LocaleListCompat.create(language.toLocale())
            AppCompatDelegate.setApplicationLocales(appLocale)
            settingsRepository.setLanguage(language)
        }
    }

    fun setTheme(theme: Theme) {
        viewModelScope.launch {
            settingsRepository.setTheme(theme)
        }
    }

    fun setDynamicTheme(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setDynamicTheme(enable)
        }
    }

    fun setHomeScreenSwiping(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setHomeScreenSwiping(enable)
        }
    }

    fun setCleanUpInterval(interval: Duration) {
        viewModelScope.launch {
            settingsRepository.setCleanUpInterval(interval)
        }
    }

    fun forceCleanup(context: Context) {
        viewModelScope.launch {
            CleanUpWorker.force(context)
        }
    }

    fun setAutoSync(autoSync: AutoSync) {
        viewModelScope.launch {
            settingsRepository.setAutoSync(autoSync)
        }
    }

    fun setNotifyUpdates(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableNotifyUpdates(enable)
        }
    }

    fun setAutoUpdate(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.setAutoUpdate(enable)
        }
    }

    fun setUnstableUpdates(enable: Boolean) {
        //viewModelScope.launch { settingsRepository.enableUnstableUpdates(enable) }
    }
    private fun getTextFromClipboard(clipboardManager: ClipboardManager?):String {
        val abc = clipboardManager?.primaryClip
        val item = (abc?.getItemAt(0))
        return item?.text.toString()
    }
    fun setDevApps(enable: Boolean, clipboardManager: ClipboardManager?, context: Context?)
    {
        if(enable&&Database.RepositoryAdapter.getAll().size>1) return
        if(!enable&&(Database.RepositoryAdapter.getAll().size <= 1)) return
        if(clipboardManager==null) return;
        if(context==null) return;
        if(enable)
        {
            var clipboardText : String = getTextFromClipboard(clipboardManager)
            var split = clipboardText.split("|")
            if(split.size!=3)
            {
                Toast.makeText(context, "Invalid Key", Toast.LENGTH_SHORT).show();
                return;
            }
            val digest = MessageDigest.getInstance("SHA-256")
            val hash = digest.digest(split[0].toByteArray(StandardCharsets.UTF_8));
            var finalString = ""
            for (byte in hash) finalString+=byte.toString()
            if(finalString=="68-3959-87101822-2927-51-118-26-21-89-79110-109-4416141192148117-9-103-131243958-4312")
            {
                val address = split[0].split("?fingerprint=")[0]
                val fingerprint = split[0].split("?fingerprint=")[1]
                val username = split[1];
                val password = split[2]
                val authentication = username?.let { u ->
                    password?.let { p ->
                        Base64.encodeToString(
                            "$u:$p".toByteArray(Charset.defaultCharset()),
                            Base64.NO_WRAP
                        )
                    }
                }?.let { "Basic $it" }.orEmpty()
                val repositoryId : Long = 2
                val repository = repositoryId?.let(Database.RepositoryAdapter::get)
                    ?.edit(address, fingerprint, authentication)
                    ?: Repository.newRepository(address, fingerprint, authentication)
                val changedRepository = Database.RepositoryAdapter.put(repository)
                Database.RepositoryAdapter.removeDuplicates()
                Toast.makeText(context, "Successfully activated Dev Apps", Toast.LENGTH_SHORT).show()
            }
            else Toast.makeText(context, "Invalid Key", Toast.LENGTH_SHORT).show()
        }
        else
        {
            val syncConnection = Connection(SyncService::class.java)
            syncConnection.bind(context)
            for (repo in Database.RepositoryAdapter.getAll())
            {
                if(repo.address!="https://raw.githubusercontent.com/ThatFinnDev/fullcodesfdroid/master/repo")
                {
                    syncConnection.binder?.deleteRepository(repo.id)
                    Database.RepositoryAdapter.markAsDeleted(repo.id)
                }
            }
        }
    }
    fun setIncompatibleUpdates(enable: Boolean) {
        viewModelScope.launch {
            settingsRepository.enableIncompatibleVersion(enable)
        }
    }

    fun setProxyType(proxyType: ProxyType) {
        viewModelScope.launch {
            settingsRepository.setProxyType(proxyType)
        }
    }

    fun setProxyHost(proxyHost: String) {
        viewModelScope.launch {
            settingsRepository.setProxyHost(proxyHost)
        }
    }

    fun setProxyPort(proxyPort: String) {
        viewModelScope.launch {
            try {
                settingsRepository.setProxyPort(proxyPort.toInt())
            } catch (e: NumberFormatException) {
                createSnackbar(CommonR.string.proxy_port_error_not_int)
            }
        }
    }

    fun setInstaller(installerType: InstallerType) {
        viewModelScope.launch {
            settingsRepository.setInstallerType(installerType)
            if (installerType == InstallerType.SHIZUKU) handleShizuku()
        }
    }

    fun exportSettings(file: Uri) {
        viewModelScope.launch {
            settingsRepository.export(file)
        }
    }

    fun importSettings(file: Uri) {
        viewModelScope.launch {
            settingsRepository.import(file)
        }
    }

    fun exportRepos(file: Uri) {
        viewModelScope.launch {
            val repos = Database.RepositoryAdapter.getAll()
            repositoryExporter.export(repos, file)
        }
    }

    fun importRepos(file: Uri) {
        viewModelScope.launch {
            val repos = repositoryExporter.import(file)
            Database.RepositoryAdapter.importRepos(repos)
        }
    }

    fun createSnackbar(@StringRes message: Int) {
        viewModelScope.launch {
            _snackbarStringId.emit(message)
        }
    }

    private fun handleShizuku() {
        viewModelScope.launch {
            val state = shizukuPermissionHandler.state.first()
            if (state.isAlive && state.isPermissionGranted) cancel()
            if (state.isInstalled) {
                if (!state.isAlive) {
                    createSnackbar(CommonR.string.shizuku_not_alive)
                }
            } else {
                createSnackbar(CommonR.string.shizuku_not_installed)
            }
        }
    }
}
