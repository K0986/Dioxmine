package io.github.rhythmcache.dioxamine.adb

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import io.github.rhythmcache.dioxamine.R
import io.github.rhythmcache.dioxamine.adb.builtin.BuiltInActionsTab
import io.github.rhythmcache.dioxamine.adb.discovery.DiscoverySheet
import io.github.rhythmcache.dioxamine.adb.discovery.QrPairingScreen
import io.github.rhythmcache.dioxamine.adb.shell.ShellScreen
import io.github.rhythmcache.dioxamine.core.*
import io.github.rhythmcache.adb.AdbDeviceMode

@Composable
fun AdbScreen(vm: AdbViewModel) {
    val mode = vm.devices[vm.activeDeviceId]?.mode ?: AdbDeviceMode.UNKNOWN

    vm.daemonDialogMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { vm.dismissDaemonDialog() },
            title = { Text(stringResource(R.string.daemon_dialog_title)) },
            text = { Text(message) },
            confirmButton = { TextButton(onClick = { vm.dismissDaemonDialog() }) { Text(stringResource(R.string.btn_ok)) } },
        )
    }

    when (mode) {
        AdbDeviceMode.SIDELOAD, AdbDeviceMode.RESCUE -> Column(Modifier.fillMaxSize()) {
            DeviceConnectorCard(vm, Modifier.padding(16.dp))
            Box(Modifier.weight(1f)) {
                if (mode == AdbDeviceMode.SIDELOAD) SideloadFlashScreen(vm) else RescueScreen(vm)
            }
        }
        else -> BuiltInActionsTab(vm)
    }
}

@Composable
fun DeviceRow(conn: DeviceConnection, vm: AdbViewModel) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(
                if (conn.transport == DeviceTransport.USB) Icons.Filled.Usb else Icons.Filled.Wifi,
                contentDescription = null,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Column {
                Text(conn.label, style = MaterialTheme.typography.bodySmall)
                Text(
                    when (conn.state) {
                        is ConnectionState.Connected -> {
                            when (conn.mode) {
                                AdbDeviceMode.SIDELOAD -> stringResource(R.string.status_connected_mode, stringResource(R.string.mode_sideload))
                                AdbDeviceMode.RECOVERY -> {
                                    if (conn.androidVersion != null && conn.apiLevel != null) {
                                        stringResource(R.string.status_connected_recovery_version_api, conn.androidVersion, conn.apiLevel)
                                    } else if (conn.androidVersion != null) {
                                        stringResource(R.string.status_connected_recovery_version, conn.androidVersion)
                                    } else if (conn.apiLevel != null) {
                                        stringResource(R.string.status_connected_recovery_api, conn.apiLevel)
                                    } else {
                                        stringResource(R.string.status_connected_mode, stringResource(R.string.mode_recovery))
                                    }
                                }
                                AdbDeviceMode.RESCUE -> stringResource(R.string.status_connected_mode, stringResource(R.string.mode_rescue))
                                else -> {
                                    if (conn.androidVersion != null && conn.apiLevel != null) {
                                        stringResource(R.string.status_connected_version_api, conn.androidVersion, conn.apiLevel)
                                    } else if (conn.androidVersion != null) {
                                        stringResource(R.string.status_connected_version, conn.androidVersion)
                                    } else if (conn.apiLevel != null) {
                                        stringResource(R.string.status_connected_api, conn.apiLevel)
                                    } else {
                                        stringResource(R.string.status_connected)
                                    }
                                }
                            }
                        }
                        is ConnectionState.Connecting -> stringResource(R.string.status_connecting)
                        is ConnectionState.Error -> conn.state.message
                        else -> ""
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = when (conn.state) {
                        is ConnectionState.Error -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            when (conn.state) {
                is ConnectionState.Connecting -> CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                is ConnectionState.Connected -> {
                    var menuExpanded by remember { mutableStateOf(false) }
                    var showTcpDialog by remember { mutableStateOf(false) }
                    var tcpPortInput by remember { mutableStateOf("5555") }

                    Box {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Filled.MoreVert, contentDescription = stringResource(R.string.cd_daemon_options), modifier = Modifier.size(18.dp))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false }
                        ) {
                            if (conn.transport == DeviceTransport.USB) {
                                DropdownMenuItem(
                                    text = { Text(stringResource(R.string.action_switch_tcpip), style = MaterialTheme.typography.bodyMedium) },
                                    leadingIcon = { Icon(Icons.Filled.Wifi, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                    onClick = {
                                        menuExpanded = false
                                        showTcpDialog = true
                                    }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text(stringResource(if (conn.isRoot) R.string.action_unroot_daemon else R.string.action_root_daemon), style = MaterialTheme.typography.bodyMedium) },
                                leadingIcon = { Icon(if (conn.isRoot) Icons.Filled.LockOpen else Icons.Filled.AdminPanelSettings, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                onClick = {
                                    menuExpanded = false
                                    vm.toggleRoot(conn.id)
                                }
                            )
                        }
                    }

                    if (showTcpDialog) {
                        AlertDialog(
                            onDismissRequest = { showTcpDialog = false },
                            title = { Text(stringResource(R.string.dialog_tcpip_title)) },
                            text = {
                                Column {
                                    Text(stringResource(R.string.dialog_tcpip_msg))
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = tcpPortInput,
                                        onValueChange = { tcpPortInput = it.filter(Char::isDigit).take(5) },
                                        singleLine = true,
                                        label = { Text(stringResource(R.string.label_port)) },
                                        isError = !isValidPort(tcpPortInput),
                                        supportingText = { if (!isValidPort(tcpPortInput)) Text(stringResource(R.string.err_invalid_port_range)) },
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            },
                            confirmButton = {
                                TextButton(onClick = {
                                    showTcpDialog = false
                                    vm.switchTcpip(conn.id, tcpPortInput.toInt())
                                }, enabled = isValidPort(tcpPortInput)) {
                                    Text(stringResource(R.string.btn_restart))
                                }
                            },
                            dismissButton = {
                                TextButton(onClick = { showTcpDialog = false }) {
                                    Text(stringResource(R.string.btn_cancel))
                                }
                            }
                        )
                    }

                    IconButton(onClick = { vm.disconnect(conn.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove), modifier = Modifier.size(18.dp))
                    }
                }
                else -> {
                    IconButton(onClick = { vm.disconnect(conn.id) }) {
                        Icon(Icons.Filled.Close, contentDescription = stringResource(R.string.cd_remove), modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}





@Composable
fun SideloadFlashScreen(vm: AdbViewModel) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var pickedUri by remember { mutableStateOf<android.net.Uri?>(null) }
    var pickedFileName by remember { mutableStateOf<String?>(null) }

    val pickFileLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.OpenDocument()
    ) { uri ->
        pickedUri = uri
        pickedFileName = uri?.let { FileUtils.resolveDisplayName(context.contentResolver, it) }
    }

    var isStartingFlash by remember { mutableStateOf(false) }

    LaunchedEffect(pickedUri, vm.flashState) {
        if (vm.flashState !is FlashUiState.Idle) {
            isStartingFlash = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(56.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.mode_sideload), style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.sideload_description),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(20.dp))

        when (val state = vm.flashState) {
            is FlashUiState.Idle -> {
                OutlinedButton(
                    onClick = { pickFileLauncher.launch(arrayOf("application/zip")) },
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
                ) {
                    Text(pickedFileName ?: stringResource(R.string.btn_choose_file))
                }
                if (pickedFileName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        pickedFileName!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        if (pickedUri != null && !isStartingFlash) {
                            isStartingFlash = true
                            vm.startFlash(pickedUri!!, context.contentResolver)
                        }
                    },
                    enabled = pickedUri != null && !isStartingFlash,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        disabledContainerColor = if (isStartingFlash) MaterialTheme.colorScheme.primary.copy(alpha = 0.38f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
                        disabledContentColor = if (isStartingFlash) MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    )
                ) {
                    if (isStartingFlash) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_starting))
                    } else {
                        Icon(Icons.Filled.SystemUpdate, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.btn_sideload))
                    }
                }
            }
            is FlashUiState.Running -> {
                LinearProgressIndicator(
                    progress = { state.percent / 100f },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(8.dp))
                Text("${state.percent.toInt()}% - ${formatFileSize(state.bytesTransferred)} / ${formatFileSize(state.totalBytes)}")
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { vm.cancelFlash() }) { Text(stringResource(R.string.btn_cancel)) }
            }
            is FlashUiState.Success -> {
                Icon(Icons.Filled.CheckCircle, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.msg_flash_complete))
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { pickedUri = null; pickedFileName = null; vm.resetFlashState() }) { Text(stringResource(R.string.btn_done)) }
            }
            is FlashUiState.Error -> {
                Icon(Icons.Filled.Error, contentDescription = null, modifier = Modifier.size(40.dp), tint = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(state.message, color = MaterialTheme.colorScheme.error, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = { pickedUri = null; pickedFileName = null; vm.resetFlashState() }) { Text(stringResource(R.string.btn_try_again)) }
            }
        }
    }
}

@Composable
fun RescueScreen(vm: AdbViewModel) {
    var showWipeConfirm by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.mode_rescue), style = MaterialTheme.typography.titleMedium)

        SideloadFlashScreen(vm)

        HorizontalDivider()

        OutlinedButton(
            onClick = { showWipeConfirm = true },
            shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
        ) {
            Icon(Icons.Filled.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.btn_wipe_userdata))
        }

        vm.nativeOutputs["wipe"]?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
    }

    if (showWipeConfirm) {
        AlertDialog(
            onDismissRequest = { showWipeConfirm = false },
            title = { Text(stringResource(R.string.dialog_wipe_userdata_title)) },
            text = { Text(stringResource(R.string.dialog_wipe_userdata_msg)) },
            confirmButton = {
                TextButton(onClick = { vm.rescueWipeUserdata(); showWipeConfirm = false }) { Text(stringResource(R.string.btn_wipe)) }
            },
            dismissButton = { TextButton(onClick = { showWipeConfirm = false }) { Text(stringResource(R.string.btn_cancel)) } }
        )
    }
}
