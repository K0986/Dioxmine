package io.github.rhythmcache.dioxamine.adb.builtin

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.material3.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import io.github.rhythmcache.dioxamine.adb.shell.ShellScreen
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rhythmcache.dioxamine.R
import io.github.rhythmcache.dioxamine.adb.AdbViewModel

import io.github.rhythmcache.dioxamine.adb.builtin.filemanager.FileManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.filemanager.FileManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.logcat.LogcatScreen
import io.github.rhythmcache.dioxamine.adb.builtin.logcat.LogcatTile
import io.github.rhythmcache.dioxamine.adb.builtin.misc.MiscScreen
import io.github.rhythmcache.dioxamine.adb.builtin.misc.MiscTile
import io.github.rhythmcache.dioxamine.adb.builtin.packagemanager.PackageManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.packagemanager.PackageManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.processmanager.ProcessManagerScreen
import io.github.rhythmcache.dioxamine.adb.builtin.processmanager.ProcessManagerTile
import io.github.rhythmcache.dioxamine.adb.builtin.reboot.RebootScreen
import io.github.rhythmcache.dioxamine.adb.builtin.reboot.RebootTile
import io.github.rhythmcache.dioxamine.adb.builtin.remotecontrol.RemoteControlScreen
import io.github.rhythmcache.dioxamine.adb.builtin.remotecontrol.RemoteControlTile
import io.github.rhythmcache.dioxamine.adb.builtin.screencap.ScreencapScreen
import io.github.rhythmcache.dioxamine.adb.builtin.screencap.ScreencapTile
import io.github.rhythmcache.dioxamine.adb.builtin.touchpad.TouchpadScreen
import io.github.rhythmcache.dioxamine.adb.builtin.touchpad.TouchpadTile

enum class BuiltInSubScreen {
    TilesList, DeviceInfo, RemoteControl, Touchpad, FileManager, PackageManager,
    ProcessManager, Logcat, Misc, Screenshot, Reboot, Terminal,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BuiltInActionsTab(vm: AdbViewModel) {
    var activeSubScreen by rememberSaveable { mutableStateOf(BuiltInSubScreen.TilesList) }
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val overviewGridState = rememberLazyGridState()
    val isConnected = vm.activeClient() != null

    // Return to tools list when on a sub-screen
    BackHandler(enabled = activeSubScreen != BuiltInSubScreen.TilesList) {
        activeSubScreen = BuiltInSubScreen.TilesList
    }

    when (activeSubScreen) {
        BuiltInSubScreen.TilesList -> OverviewDashboard(
            vm = vm,
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            gridState = overviewGridState,
            onOpenTool = { activeSubScreen = it },
        )
        BuiltInSubScreen.Terminal -> Column(Modifier.fillMaxSize()) {
            TopAppBar(
                title = { Text(stringResource(R.string.tool_terminal)) },
                navigationIcon = { IconButton(onClick = { activeSubScreen = BuiltInSubScreen.TilesList }) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.cd_nav_back))
                } },
                windowInsets = WindowInsets(0, 0, 0, 0),
            )
            ShellScreen(vm)
        }
        BuiltInSubScreen.DeviceInfo -> {
            DeviceInformationDetailScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.RemoteControl -> {
            RemoteControlScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList },
                onOpenTouchpad = { activeSubScreen = BuiltInSubScreen.Touchpad }
            )
        }
        BuiltInSubScreen.Touchpad -> {
            TouchpadScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.FileManager -> {
            FileManagerScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.PackageManager -> {
            PackageManagerScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.ProcessManager -> {
            ProcessManagerScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.Logcat -> {
            LogcatScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.Misc -> {
            MiscScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.Screenshot -> {
            ScreencapScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
        BuiltInSubScreen.Reboot -> {
            RebootScreen(
                vm = vm,
                onBack = { activeSubScreen = BuiltInSubScreen.TilesList }
            )
        }
    }
}

