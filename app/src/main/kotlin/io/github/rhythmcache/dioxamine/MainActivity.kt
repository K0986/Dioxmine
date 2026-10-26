package io.github.rhythmcache.dioxamine

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.view.KeyEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.rhythmcache.dioxamine.adb.*
import io.github.rhythmcache.dioxamine.core.*
import io.github.rhythmcache.dioxamine.fastboot.FastbootScreen
import io.github.rhythmcache.dioxamine.fastboot.FastbootViewModel
import io.github.rhythmcache.dioxamine.fastboot.ListenForFastbootDevices
import io.github.rhythmcache.dioxamine.scrcpy.ScrcpyScreen
import io.github.rhythmcache.dioxamine.settings.SettingsScreen
import io.github.rhythmcache.dioxamine.plugin.PluginsScreen
import io.github.rhythmcache.adb.AdbDeviceMode
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import java.io.File

// ---------------------------------------------------------------------------
// Navigation
// ---------------------------------------------------------------------------

enum class Tab(@StringRes val labelRes: Int, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    ADB(R.string.nav_overview, Icons.Filled.Dashboard),
    SCRCPY(R.string.nav_mirror, Icons.AutoMirrored.Filled.ScreenShare),
    FASTBOOT(R.string.nav_flasher, Icons.Filled.Bolt),
    PLUGINS(R.string.tab_plugins, Icons.Filled.Extension),
    SETTINGS(R.string.tab_settings, Icons.Filled.Settings)
}

class MainActivity : AppCompatActivity() {
    companion object {
        // Owner-token pattern: prevents a disposing/unmounting screen from clobbering a newly active
        // screen's key interceptor during overlapping Compose transitions or recomposition.
        private var interceptorOwner: Any? = null
        private var activeInterceptor: ((KeyEvent) -> Boolean)? = null

        fun setKeyEventInterceptor(owner: Any, interceptor: (KeyEvent) -> Boolean) {
            interceptorOwner = owner
            activeInterceptor = interceptor
        }

        fun clearKeyEventInterceptor(owner: Any) {
            if (interceptorOwner === owner) {
                interceptorOwner = null
                activeInterceptor = null
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (activeInterceptor?.invoke(event) == true) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        installSplashScreen()
        enableEdgeToEdge()

        val prefs = getSharedPreferences("settings", Context.MODE_PRIVATE)
        if (prefs.getBoolean("keep_alive_enabled", false)) {
            DioxForegroundService.start(this)
        }

        setContent {
            val context = LocalContext.current
            val prefsState = remember { context.getSharedPreferences("settings", Context.MODE_PRIVATE) }
            var currentAppTheme by remember {
                mutableStateOf(
                    runCatching { AppTheme.valueOf(prefsState.getString("theme_mode", "DARK") ?: "DARK") }
                        .getOrDefault(AppTheme.DARK)
                )
            }

            var currentUseMonet by remember {
                mutableStateOf(prefsState.getBoolean("use_monet", false))
            }

            DisposableEffect(Unit) {
                val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
                    if (key == "theme_mode") {
                        currentAppTheme = runCatching { AppTheme.valueOf(prefs.getString("theme_mode", "DARK") ?: "DARK") }
                            .getOrDefault(AppTheme.DARK)
                    } else if (key == "use_monet") {
                        currentUseMonet = prefs.getBoolean("use_monet", false)
                    }
                }
                prefsState.registerOnSharedPreferenceChangeListener(listener)
                onDispose { prefsState.unregisterOnSharedPreferenceChangeListener(listener) }
            }

            DioxamineTheme(appTheme = currentAppTheme, useMonet = currentUseMonet) {
                DioxamineApp(keyDir = filesDir)
            }
        }
    }
}

@Composable
fun DioxamineApp(keyDir: File) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(Tab.ADB) }
    val vm: AdbViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AdbViewModel(keyDir, context.applicationContext) as T
        }
    })
    val fastbootVm: FastbootViewModel = viewModel()

    val coroutineScope = rememberCoroutineScope()
    val pluginRepo = remember { io.github.rhythmcache.dioxamine.plugin.PluginRepository(context.applicationContext, coroutineScope) }
    val permissionStore = remember { io.github.rhythmcache.dioxamine.plugin.PluginPermissionStore(context.applicationContext) }
    val permissionGate = remember { io.github.rhythmcache.dioxamine.plugin.PluginPermissionGate(store = permissionStore) }
    val dialogGate = remember { io.github.rhythmcache.dioxamine.plugin.PluginDialogGate() }
    val safBridge = remember { io.github.rhythmcache.dioxamine.plugin.PluginSafBridge(context.applicationContext) }

    io.github.rhythmcache.dioxamine.plugin.PluginPermissionDialogHost(permissionGate)
    io.github.rhythmcache.dioxamine.plugin.PluginDialogHost(dialogGate)
    io.github.rhythmcache.dioxamine.plugin.PluginSafLauncherHost(safBridge)

    ListenForUsbDevices(vm)
    ListenForFastbootDevices(fastbootVm)

    val adbConnectedCount = vm.devices.values.count { it.state is ConnectionState.Connected }
    val fastbootConnectedCount = if (fastbootVm.isConnected) 1 else fastbootVm.devices.size

    LaunchedEffect(adbConnectedCount, fastbootConnectedCount) {
        DioxForegroundService.updateDeviceCounts(context, adbConnectedCount, fastbootConnectedCount)
    }

    var isScrcpyFullScreen by remember { mutableStateOf(false) }
    var isPluginActive by remember { mutableStateOf(false) }

    DisposableEffect(isScrcpyFullScreen) {
        val activity = context as? ComponentActivity
        val window = activity?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, window.decorView)
            if (isScrcpyFullScreen) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val activityOnDispose = context as? ComponentActivity
            val windowOnDispose = activityOnDispose?.window
            if (windowOnDispose != null) {
                WindowCompat.getInsetsController(windowOnDispose, windowOnDispose.decorView)
                .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    val hideBottomBar = isScrcpyFullScreen || isPluginActive

    // Navigate back to ADB home tab before exiting app
    BackHandler(enabled = selectedTab != Tab.ADB && !hideBottomBar) {
        selectedTab = Tab.ADB
    }

    // Exit Scrcpy fullscreen on back gesture
    BackHandler(enabled = isScrcpyFullScreen) {
        isScrcpyFullScreen = false
    }

    val stateHolder = rememberSaveableStateHolder()
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val availableWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val useRail = availableWidth >= 720.dp && !hideBottomBar
        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            bottomBar = {
                if (!hideBottomBar && !useRail) {
                    Column {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                        NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, tonalElevation = 0.dp) {
                            Tab.entries.forEach { tab ->
                                NavigationBarItem(
                                    selected = selectedTab == tab,
                                    onClick = { selectedTab = tab },
                                    icon = { Icon(tab.icon, contentDescription = null) },
                                    label = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = MaterialTheme.colorScheme.primary,
                                        selectedTextColor = MaterialTheme.colorScheme.primary,
                                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                                    ),
                                )
                            }
                        }
                    }
                }
            },
        ) { padding ->
            Row(
                modifier = if (hideBottomBar) Modifier.fillMaxSize()
                    else Modifier.padding(padding).consumeWindowInsets(padding).imePadding().fillMaxSize(),
            ) {
                if (useRail) {
                    NavigationRail(containerColor = MaterialTheme.colorScheme.surfaceContainerLowest, modifier = Modifier.fillMaxHeight()) {
                        Spacer(Modifier.height(24.dp))
                        Tab.entries.forEach { tab ->
                            NavigationRailItem(
                                selected = selectedTab == tab,
                                onClick = { selectedTab = tab },
                                icon = { Icon(tab.icon, null) },
                                label = { Text(stringResource(tab.labelRes), style = MaterialTheme.typography.labelSmall.copy(letterSpacing = 0.sp)) },
                                modifier = Modifier.padding(vertical = 8.dp),
                            )
                        }
                    }
                }
                Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!hideBottomBar) WorkspaceHeader()
                    // Keep forms readable on tablets while allowing mirroring to use all space.
                    Box(Modifier.weight(1f).then(if (selectedTab == Tab.SCRCPY || hideBottomBar) Modifier.fillMaxWidth() else Modifier.widthIn(max = 1000.dp).fillMaxWidth())) {
                        stateHolder.SaveableStateProvider(selectedTab.name) {
                            when (selectedTab) {
                                Tab.ADB -> AdbScreen(vm)
                                Tab.SCRCPY -> ScrcpyScreen(vm, onFullScreenChange = { isScrcpyFullScreen = it }, onConnectDevice = { selectedTab = Tab.ADB })
                                Tab.FASTBOOT -> FastbootScreen(fastbootVm)
                                Tab.PLUGINS -> PluginsScreen(
                                    vm = vm,
                                    fastbootVm = fastbootVm,
                                    pluginRepo = pluginRepo,
                                    permissionGate = permissionGate,
                                    dialogGate = dialogGate,
                                    safBridge = safBridge,
                                    onPluginActiveChange = { isPluginActive = it },
                                )
                                Tab.SETTINGS -> SettingsScreen(vm)
                            }
                        }
                    }
                }
            }
        }
    }
}
