package io.github.rhythmcache.dioxamine.adb.builtin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import io.github.rhythmcache.dioxamine.R
import io.github.rhythmcache.dioxamine.adb.AdbViewModel
import io.github.rhythmcache.dioxamine.adb.DeviceConnectorCard
import io.github.rhythmcache.dioxamine.core.*

private data class DashboardTool(val screen: BuiltInSubScreen, val title: Int, val description: Int, val icon: ImageVector, val group: Int)

private val dashboardTools = listOf(
    DashboardTool(BuiltInSubScreen.Terminal, R.string.tool_terminal, R.string.tool_terminal_desc, Icons.Filled.Terminal, 0),
    DashboardTool(BuiltInSubScreen.FileManager, R.string.tool_files, R.string.tool_files_desc, Icons.Filled.FolderOpen, 0),
    DashboardTool(BuiltInSubScreen.PackageManager, R.string.tool_apps, R.string.tool_apps_desc, Icons.Filled.Widgets, 0),
    DashboardTool(BuiltInSubScreen.DeviceInfo, R.string.tool_device, R.string.tool_device_desc, Icons.Filled.PhoneAndroid, 1),
    DashboardTool(BuiltInSubScreen.ProcessManager, R.string.tool_processes, R.string.tool_processes_desc, Icons.Filled.Insights, 1),
    DashboardTool(BuiltInSubScreen.Logcat, R.string.tool_logs, R.string.tool_logs_desc, Icons.AutoMirrored.Filled.Article, 1),
    DashboardTool(BuiltInSubScreen.RemoteControl, R.string.tool_remote, R.string.tool_remote_desc, Icons.Filled.SettingsRemote, 2),
    DashboardTool(BuiltInSubScreen.Touchpad, R.string.tool_touchpad, R.string.tool_touchpad_desc, Icons.Filled.Mouse, 2),
    DashboardTool(BuiltInSubScreen.Screenshot, R.string.tool_screenshot, R.string.tool_screenshot_desc, Icons.Filled.Screenshot, 2),
    DashboardTool(BuiltInSubScreen.Misc, R.string.tool_tuning, R.string.tool_tuning_desc, Icons.Filled.Tune, 2),
    DashboardTool(BuiltInSubScreen.Reboot, R.string.tool_reboot, R.string.tool_reboot_desc, Icons.Filled.RestartAlt, 2),
)

@Composable
fun OverviewDashboard(
    vm: AdbViewModel,
    query: String,
    onQueryChange: (String) -> Unit,
    gridState: LazyGridState,
    onOpenTool: (BuiltInSubScreen) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val connected = vm.activeClient() != null
    val localizedTools = dashboardTools.map { Triple(it, stringResource(it.title), stringResource(it.description)) }
    val matching = localizedTools.filter { (_, title, description) ->
        query.isBlank() || "$title $description".contains(query.trim(), ignoreCase = true)
    }
    val groups = listOf(R.string.tools_group_manage, R.string.tools_group_diagnose, R.string.tools_group_control)
    val fontScale = LocalDensity.current.fontScale

    BoxWithConstraints(Modifier.fillMaxSize()) {
        val minimumTileWidth = if (fontScale > 1.3f) 240.dp else 148.dp
        val availableWidth = with(LocalDensity.current) { constraints.maxWidth.toDp() }
        val columnCount = ((availableWidth - 28.dp) / (minimumTileWidth + 12.dp)).toInt().coerceIn(1, 3)
        LazyVerticalGrid(
            columns = GridCells.Fixed(columnCount),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item(key = "heading", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(bottom = 4.dp)) {
                    Text(stringResource(R.string.overview_title), style = MaterialTheme.typography.headlineMedium)
                    Text(stringResource(R.string.overview_subtitle), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            item(key = "connection", span = { GridItemSpan(maxLineSpan) }) { DeviceConnectorCard(vm) }
            item(key = "toolkit", span = { GridItemSpan(maxLineSpan) }) {
                SectionHeading(stringResource(R.string.tools_heading), if (!connected) stringResource(R.string.tools_offline_hint) else null)
            }
            item(key = "search", span = { GridItemSpan(maxLineSpan) }) {
                OutlinedTextField(
                    value = query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(stringResource(R.string.tools_search)) },
                    leadingIcon = { Icon(Icons.Filled.Search, null) },
                    trailingIcon = if (query.isNotEmpty()) {
                        { IconButton(onClick = { onQueryChange("") }) { Icon(Icons.Filled.Close, stringResource(R.string.tools_clear_search)) } }
                    } else null,
                    shape = MaterialTheme.shapes.medium,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { keyboard?.hide() }),
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    ),
                )
            }
            if (matching.isEmpty()) {
                item(key = "no_results", span = { GridItemSpan(maxLineSpan) }) {
                    Column(Modifier.fillMaxWidth().padding(vertical = 28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AccentIcon(Icons.Filled.SearchOff)
                        Text(stringResource(R.string.tools_empty_title), style = MaterialTheme.typography.titleMedium)
                        Text(stringResource(R.string.tools_empty_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        TextButton(onClick = { onQueryChange("") }) { Text(stringResource(R.string.tools_clear_search)) }
                    }
                }
            }
            groups.forEachIndexed { index, heading ->
                val toolsInGroup = matching.filter { it.first.group == index }
                if (toolsInGroup.isNotEmpty()) {
                    item(key = "group_$index", span = { GridItemSpan(maxLineSpan) }) { SectionHeading(stringResource(heading)) }
                    items(toolsInGroup, key = { it.first.screen.name }) { (tool, title, description) ->
                        ToolCard(
                            title = title,
                            description = description,
                            icon = tool.icon,
                            enabled = connected,
                            tint = when (index) {
                                1 -> MaterialTheme.colorScheme.secondary
                                2 -> MaterialTheme.colorScheme.tertiary
                                else -> MaterialTheme.colorScheme.primary
                            },
                            onClick = { keyboard?.hide(); onOpenTool(tool.screen) },
                        )
                    }
                }
            }
        }
    }
}
