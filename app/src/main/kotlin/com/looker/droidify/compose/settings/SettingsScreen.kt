package com.looker.droidify.compose.settings

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.looker.droidify.R
import com.looker.droidify.datastore.Settings
import com.looker.droidify.datastore.extension.autoSyncName
import com.looker.droidify.datastore.extension.installerName
import com.looker.droidify.datastore.extension.proxyName
import com.looker.droidify.datastore.extension.themeName
import com.looker.droidify.datastore.extension.toTime
import com.looker.droidify.datastore.model.AutoSync
import com.looker.droidify.datastore.model.InstallerType
import com.looker.droidify.datastore.model.LegacyInstallerComponent
import com.looker.droidify.datastore.model.ProxyType
import com.looker.droidify.datastore.model.Theme
import com.looker.droidify.utility.common.isIgnoreBatteryEnabled
import com.looker.droidify.utility.common.requestBatteryFreedom
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        viewModel.snackbarStringId.collect { resId ->
            scope.launch {
                snackbarHostState.showSnackbar(message = context.getString(resId))
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = context.getString(R.string.settings),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { padding ->
        when (val state = uiState) {
            is SettingUiState.Loading -> {
                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp)
                ) {
                    Text("Loading settings ...", style = MaterialTheme.typography.bodyMedium)
                }
            }

            is SettingUiState.Success -> {
                val settings = state.settings
                val showBatteryBanner = !context.isIgnoreBatteryEnabled() && settings.autoSync != AutoSync.NEVER

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                ) {
                    if (showBatteryBanner) {
                        BatteryOptimizationBanner(
                            onRequest = {
                                context.requestBatteryFreedom()
                            }
                        )
                    }

                    SettingsContent(settings = settings, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun BatteryOptimizationBanner(onRequest: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.errorContainer)
            .padding(16.dp)
    ) {
        Text(
            text = "Background access required",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Allow ignoring battery optimizations to enable background sync.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onErrorContainer,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onRequest) {
                Text(text = "Allow")
            }
        }
    }
}

@Composable
private fun SettingsContent(
    settings: Settings,
    viewModel: SettingsViewModel,
) {
    val context = LocalContext.current

    // Dialog visibilities
    var showLanguageDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showCleanupDialog by rememberSaveable { mutableStateOf(false) }
    var showAutoSyncDialog by rememberSaveable { mutableStateOf(false) }
    var showInstallerDialog by rememberSaveable { mutableStateOf(false) }
    var showProxyTypeDialog by rememberSaveable { mutableStateOf(false) }
    var showLegacyInstallerDialog by rememberSaveable { mutableStateOf(false) }
    var showProxyHostDialog by rememberSaveable { mutableStateOf(false) }
    var showProxyPortDialog by rememberSaveable { mutableStateOf(false) }

    Column {
        // Language
        SettingRow(
            title = context.getString(R.string.prefs_language_title),
            value = settings.language,
            onClick = { showLanguageDialog = true }
        )
        // Theme
        SettingRow(
            title = context.getString(R.string.theme),
            value = context.themeName(settings.theme),
            onClick = { showThemeDialog = true }
        )
        // Dynamic theme (visible on SnowCake+)
        if (com.looker.droidify.utility.common.SdkCheck.isSnowCake) {
            SettingRow(
                title = context.getString(R.string.material_you),
                value = null,
                onClick = {},
                switchChecked = settings.dynamicTheme,
                onCheckedChange = { viewModel.setDynamicTheme(it) }
            )
        }
        // Home screen swiping
        SettingRow(
            title = context.getString(R.string.home_screen_swiping),
            value = null,
            onClick = {},
            switchChecked = settings.homeScreenSwiping,
            onCheckedChange = { viewModel.setHomeScreenSwiping(it) }
        )
        // Notify updates
        SettingRow(
            title = context.getString(R.string.notify_about_updates),
            value = null,
            onClick = {},
            switchChecked = settings.notifyUpdate,
            onCheckedChange = { viewModel.setNotifyUpdates(it) }
        )
        // Auto update
        SettingRow(
            title = context.getString(R.string.auto_update),
            value = null,
            onClick = {},
            switchChecked = settings.autoUpdate,
            onCheckedChange = { viewModel.setAutoUpdate(it) }
        )
        // Unstable updates
        SettingRow(
            title = context.getString(R.string.unstable_updates),
            value = null,
            onClick = {},
            switchChecked = settings.unstableUpdate,
            onCheckedChange = { viewModel.setUnstableUpdates(it) }
        )
        // Ignore signature
        SettingRow(
            title = context.getString(R.string.ignore_signature),
            value = null,
            onClick = {},
            switchChecked = settings.ignoreSignature,
            onCheckedChange = { viewModel.setIgnoreSignature(it) }
        )
        // Incompatible versions
        SettingRow(
            title = context.getString(R.string.incompatible_versions),
            value = null,
            onClick = {},
            switchChecked = settings.incompatibleVersions,
            onCheckedChange = { viewModel.setIncompatibleUpdates(it) }
        )
        // Cleanup interval
        SettingRow(
            title = context.getString(R.string.cleanup_title),
            value = context.toTime(settings.cleanUpInterval),
            onClick = { showCleanupDialog = true }
        )
        // Force cleanup (visible when interval is INFINITE)
        if (settings.cleanUpInterval == Duration.INFINITE) {
            SettingRow(
                title = context.getString(R.string.force_clean_up),
                value = context.getString(R.string.force_clean_up_DESC),
                onClick = { viewModel.forceCleanup(context) }
            )
        }
        // Auto sync
        SettingRow(
            title = context.getString(R.string.sync_repositories_automatically),
            value = context.autoSyncName(settings.autoSync),
            onClick = { showAutoSyncDialog = true }
        )
        // Installer
        SettingRow(
            title = context.getString(R.string.installer),
            value = context.installerName(settings.installerType),
            onClick = { showInstallerDialog = true }
        )
        // Legacy installer component (only for LEGACY)
        val useLegacyInstaller = settings.installerType == InstallerType.LEGACY
        if (useLegacyInstaller) {
            SettingRow(
                title = context.getString(R.string.legacyInstallerComponent),
                value = settings.legacyInstallerComponent?.let {
                    when (it) {
                        is LegacyInstallerComponent.Component -> "${'$'}{it.clazz} (${ '$' }{it.activity})"
                        LegacyInstallerComponent.Unspecified -> context.getString(R.string.unspecified)
                        LegacyInstallerComponent.AlwaysChoose -> context.getString(R.string.always_choose)
                    }
                } ?: context.getString(R.string.unspecified),
                onClick = { showLegacyInstallerDialog = true }
            )
        }
        // Proxy type
        val allowProxies = settings.proxy.type != ProxyType.DIRECT
        SettingRow(
            title = context.getString(R.string.proxy_type),
            value = context.proxyName(settings.proxy.type),
            onClick = { showProxyTypeDialog = true }
        )
        if (allowProxies) {
            SettingRow(
                title = context.getString(R.string.proxy_host),
                value = settings.proxy.host,
                onClick = { showProxyHostDialog = true }
            )
            SettingRow(
                title = context.getString(R.string.proxy_port),
                value = settings.proxy.port.toString(),
                onClick = { showProxyPortDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Import/Export launchers
        val exportSettingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            if (uri != null) viewModel.exportSettings(uri)
        }
        val importSettingsLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) viewModel.importSettings(uri) else viewModel.createSnackbar(R.string.file_format_error_DESC)
        }
        val exportReposLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.CreateDocument("application/json"),
        ) { uri: Uri? ->
            if (uri != null) viewModel.exportRepos(uri)
        }
        val importReposLauncher = rememberLauncherForActivityResult(
            contract = ActivityResultContracts.OpenDocument(),
        ) { uri: Uri? ->
            if (uri != null) viewModel.importRepos(uri) else viewModel.createSnackbar(R.string.file_format_error_DESC)
        }

        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { importSettingsLauncher.launch(arrayOf("application/json")) }) {
                Text(text = context.getString(R.string.import_settings_title))
            }
            Button(onClick = { exportSettingsLauncher.launch("droidify_settings") }) {
                Text(text = context.getString(R.string.export_settings_title))
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            Button(onClick = { importReposLauncher.launch(arrayOf("application/json")) }) {
                Text(text = context.getString(R.string.import_repos_title))
            }
            Button(onClick = { exportReposLauncher.launch("droidify_repos") }) {
                Text(text = context.getString(R.string.export_repos_title))
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Credits and about
        SettingRow(
            title = context.getString(R.string.special_credits),
            value = "FoxyDroid",
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/kitsunyan/foxy-droid")))
                }.onFailure { viewModel.createSnackbar(R.string.cannot_open_link) }
            }
        )
        SettingRow(
            title = "Droid-ify",
            value = com.looker.droidify.BuildConfig.VERSION_NAME,
            onClick = {
                runCatching {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/Droid-ify/client")))
                }.onFailure { viewModel.createSnackbar(R.string.cannot_open_link) }
            }
        )

        Spacer(modifier = Modifier.height(32.dp))
    }

    // Language dialog
    if (showLanguageDialog) {
        val localeCodes = com.looker.droidify.BuildConfig.DETECTED_LOCALES.toList().toMutableList().apply { add(0, "system") }
        SingleChoiceDialog(
            title = context.getString(R.string.prefs_language_title),
            items = localeCodes,
            selectedIndex = localeCodes.indexOf(settings.language),
            itemText = { code ->
                // basic humanization: just display code, detailed translation is complex; Fragment used translateLocale
                code
            },
            onDismiss = { showLanguageDialog = false },
            onConfirm = { index ->
                val value = localeCodes.getOrNull(index) ?: settings.language
                viewModel.setLanguage(value)
                showLanguageDialog = false
            }
        )
    }
    // Theme dialog
    if (showThemeDialog) {
        val values = Theme.entries
        SingleChoiceDialog(
            title = context.getString(R.string.themes),
            items = values,
            selectedIndex = values.indexOf(settings.theme),
            itemText = { context.themeName(it) },
            onDismiss = { showThemeDialog = false },
            onConfirm = { index ->
                val value = values.getOrNull(index) ?: settings.theme
                viewModel.setTheme(value)
                showThemeDialog = false
            }
        )
    }
    // Cleanup interval dialog
    if (showCleanupDialog) {
        val durations = listOf(6.hours, 12.hours, 18.hours, 1.days, 2.days, Duration.INFINITE)
        SingleChoiceDialog(
            title = context.getString(R.string.cleanup_title),
            items = durations,
            selectedIndex = durations.indexOfFirst { it == settings.cleanUpInterval },
            itemText = { context.toTime(it) },
            onDismiss = { showCleanupDialog = false },
            onConfirm = { index ->
                val value = durations.getOrNull(index) ?: settings.cleanUpInterval
                viewModel.setCleanUpInterval(value)
                showCleanupDialog = false
            }
        )
    }
    // Auto sync dialog
    if (showAutoSyncDialog) {
        val values = AutoSync.entries
        SingleChoiceDialog(
            title = context.getString(R.string.sync_repositories_automatically),
            items = values,
            selectedIndex = values.indexOf(settings.autoSync),
            itemText = { context.autoSyncName(it) },
            onDismiss = { showAutoSyncDialog = false },
            onConfirm = { index ->
                val value = values.getOrNull(index) ?: settings.autoSync
                viewModel.setAutoSync(value)
                showAutoSyncDialog = false
            }
        )
    }
    // Installer dialog
    if (showInstallerDialog) {
        val values = InstallerType.entries
        SingleChoiceDialog(
            title = context.getString(R.string.installer),
            items = values,
            selectedIndex = values.indexOf(settings.installerType),
            itemText = { context.installerName(it) },
            onDismiss = { showInstallerDialog = false },
            onConfirm = { index ->
                val value = values.getOrNull(index) ?: settings.installerType
                viewModel.setInstaller(context, value)
                showInstallerDialog = false
            }
        )
    }
    // Proxy type dialog
    if (showProxyTypeDialog) {
        val values = ProxyType.entries
        SingleChoiceDialog(
            title = context.getString(R.string.proxy_type),
            items = values,
            selectedIndex = values.indexOf(settings.proxy.type),
            itemText = { context.proxyName(it) },
            onDismiss = { showProxyTypeDialog = false },
            onConfirm = { index ->
                val value = values.getOrNull(index) ?: settings.proxy.type
                viewModel.setProxyType(value)
                showProxyTypeDialog = false
            }
        )
    }
    // Legacy installer component dialog
    if (showLegacyInstallerDialog) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = Uri.parse("content://")
            type = "application/vnd.android.package-archive"
        }
        val activities = pm.queryIntentActivities(intent, android.content.pm.PackageManager.MATCH_DEFAULT_ONLY)
        val options: List<LegacyInstallerComponent> = listOf(
            LegacyInstallerComponent.Unspecified,
            LegacyInstallerComponent.AlwaysChoose,
        ) + activities.map {
            LegacyInstallerComponent.Component(
                clazz = it.activityInfo.packageName,
                activity = it.activityInfo.name,
            )
        }
        SingleChoiceDialog(
            title = context.getString(R.string.legacyInstallerComponent),
            items = options,
            selectedIndex = options.indexOf(settings.legacyInstallerComponent ?: LegacyInstallerComponent.Unspecified),
            itemText = {
                when (it) {
                    is LegacyInstallerComponent.Component -> "${'$'}{it.clazz} (${ '$' }{it.activity})"
                    LegacyInstallerComponent.Unspecified -> context.getString(R.string.unspecified)
                    LegacyInstallerComponent.AlwaysChoose -> context.getString(R.string.always_choose)
                }
            },
            onDismiss = { showLegacyInstallerDialog = false },
            onConfirm = { index ->
                val value = options.getOrNull(index)
                viewModel.setLegacyInstallerComponentComponent(value)
                showLegacyInstallerDialog = false
            }
        )
    }
    // Proxy host input dialog
    if (showProxyHostDialog) {
        TextFieldDialog(
            title = context.getString(R.string.proxy_host),
            initial = settings.proxy.host,
            onDismiss = { showProxyHostDialog = false },
            onConfirm = { text ->
                viewModel.setProxyHost(text)
                showProxyHostDialog = false
            }
        )
    }
    // Proxy port input dialog
    if (showProxyPortDialog) {
        TextFieldDialog(
            title = context.getString(R.string.proxy_port),
            initial = settings.proxy.port.toString(),
            onDismiss = { showProxyPortDialog = false },
            onConfirm = { text ->
                viewModel.setProxyPort(text)
                showProxyPortDialog = false
            }
        )
    }
}

@Composable
private fun <T> SingleChoiceDialog(
    title: String,
    items: List<T>,
    selectedIndex: Int,
    itemText: (T) -> String,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var currentIndex by rememberSaveable { mutableStateOf(selectedIndex.coerceIn(0, items.lastIndex.coerceAtLeast(0))) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { currentIndex = index }
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = itemText(item), modifier = Modifier.weight(1f))
                        androidx.compose.material3.RadioButton(
                            selected = currentIndex == index,
                            onClick = { currentIndex = index }
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(currentIndex) }) {
                Text(text = stringResourceSafe(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResourceSafe(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun TextFieldDialog(
    title: String,
    initial: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var text by rememberSaveable { mutableStateOf(initial) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = {
            androidx.compose.material3.TextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(text) }) {
                Text(text = stringResourceSafe(android.R.string.ok))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResourceSafe(android.R.string.cancel))
            }
        }
    )
}

@Composable
private fun stringResourceSafe(id: Int): String {
    return LocalContext.current.resources.getString(id)
}

@Composable
private fun SettingRow(
    title: String,
    value: String?,
    onClick: () -> Unit,
    switchChecked: Boolean? = null,
    onCheckedChange: ((Boolean) -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            if (value != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (switchChecked != null && onCheckedChange != null) {
            androidx.compose.material3.Switch(
                checked = switchChecked,
                onCheckedChange = onCheckedChange,
            )
        }
    }
}
