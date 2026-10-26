package io.github.rhythmcache.dioxamine.adb

import android.widget.Toast
import android.content.ClipData
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalClipboard
import androidx.compose.ui.platform.ClipEntry
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.rhythmcache.dioxamine.R
import io.github.rhythmcache.dioxamine.adb.discovery.DiscoveryDialog
import io.github.rhythmcache.dioxamine.adb.discovery.DiscoveryStep
import io.github.rhythmcache.dioxamine.adb.discovery.QrPairingScreen
import io.github.rhythmcache.dioxamine.core.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DeviceConnectorCard(vm: AdbViewModel, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val clipboard = LocalClipboard.current
    val scope = rememberCoroutineScope()
    var showConnections by rememberSaveable { mutableStateOf(false) }
    var showDiscovery by rememberSaveable { mutableStateOf(false) }
    var manualEntry by rememberSaveable { mutableStateOf(false) }
    var showQrPairing by rememberSaveable { mutableStateOf(false) }
    val active = vm.devices[vm.activeDeviceId]
    val connected = active?.state is ConnectionState.Connected
    val pending = vm.devices.values.firstOrNull { it.state is ConnectionState.Connecting }
    val failed = vm.devices.values.firstOrNull { it.state is ConnectionState.Error }
    val colors = MaterialTheme.colorScheme
    val copyLabel = stringResource(R.string.connection_copy)
    val copiedMessage = stringResource(R.string.connection_copied)

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        colors = CardDefaults.cardColors(containerColor = colors.surfaceContainerHigh),
    ) {
        Column(
            Modifier.background(Brush.linearGradient(listOf(colors.primary.copy(alpha = 0.08f), colors.tertiary.copy(alpha = 0.04f), colors.surfaceContainerHigh)))
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AccentIcon(if (connected) Icons.Filled.PhoneAndroid else Icons.Filled.Devices)
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(stringResource(R.string.connection_eyebrow), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    StatusPill(
                        text = stringResource(when {
                            connected -> R.string.status_connected
                            pending != null -> R.string.status_connecting
                            failed != null -> R.string.connection_failed
                            else -> R.string.connection_offline
                        }),
                        color = when {
                            connected -> colors.secondary
                            failed != null && pending == null -> colors.error
                            else -> colors.onSurfaceVariant
                        },
                    )
                }
            }

            if (connected) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(active.model ?: active.label, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StatusPill(stringResource(if (active.transport == DeviceTransport.USB) R.string.connection_usb else R.string.connection_wifi))
                        active.androidVersion?.let { StatusPill(stringResource(R.string.connection_android, it), colors.tertiary) }
                        active.apiLevel?.let { StatusPill(stringResource(R.string.connection_api, it), colors.onSurfaceVariant) }
                    }
                }
                Surface(color = colors.surfaceContainerLowest.copy(alpha = 0.75f), shape = MaterialTheme.shapes.medium) {
                    Row(Modifier.fillMaxWidth().padding(start = 14.dp, top = 3.dp, bottom = 3.dp, end = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(if (active.isRoot) R.string.connection_root else R.string.connection_shell), style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                            Text(active.id, fontFamily = JetBrainsMono, style = MaterialTheme.typography.bodySmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        IconButton(onClick = {
                            scope.launch {
                                clipboard.setClipEntry(ClipEntry(ClipData.newPlainText(copyLabel, active.id)))
                                if (android.os.Build.VERSION.SDK_INT < 33) {
                                    Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                                }
                            }
                        }) { Icon(Icons.Filled.ContentCopy, stringResource(R.string.connection_copy), Modifier.size(19.dp)) }
                    }
                }
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(R.string.connection_ready_title), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Text(stringResource(R.string.connection_ready_desc), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                }
                if (pending != null) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                    Text(stringResource(R.string.connection_pending), style = MaterialTheme.typography.bodySmall)
                } else if (failed != null) {
                    Text((failed.state as ConnectionState.Error).message, style = MaterialTheme.typography.bodySmall, color = colors.error)
                }
            }
            Button(
                onClick = { showConnections = true },
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            ) {
                Icon(if (connected) Icons.Filled.Tune else Icons.Filled.Add, null, Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
                Text(stringResource(if (connected) R.string.connection_manage else R.string.connection_add), Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(20.dp))
            }
        }
    }

    if (showConnections) {
        ModalBottomSheet(onDismissRequest = { showConnections = false }, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            Column(
                Modifier.fillMaxWidth().imePadding().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(stringResource(R.string.connection_manage), style = MaterialTheme.typography.headlineSmall)
                if (vm.devices.isNotEmpty()) {
                    SectionHeading(stringResource(R.string.connection_session), stringResource(R.string.connection_active_hint))
                    vm.devices.values.toList().forEach { conn ->
                        Surface(shape = MaterialTheme.shapes.medium, color = colors.surfaceContainerHigh) {
                            Column(Modifier.padding(12.dp)) {
                                if (conn.state is ConnectionState.Connected) {
                                    FilterChip(
                                        selected = conn.id == vm.activeDeviceId,
                                        onClick = { vm.setActiveDevice(conn.id) },
                                        label = { Text(conn.model ?: conn.label) },
                                        leadingIcon = { Icon(if (conn.id == vm.activeDeviceId) Icons.Filled.CheckCircle else Icons.Filled.PhoneAndroid, null, Modifier.size(18.dp)) },
                                        modifier = Modifier.heightIn(min = 48.dp),
                                    )
                                }
                                DeviceRow(conn, vm)
                            }
                        }
                    }
                }
                Surface(shape = MaterialTheme.shapes.large, color = colors.surfaceContainerHigh) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AccentIcon(Icons.Filled.Usb, colors.tertiary)
                            Text(stringResource(R.string.connection_usb), style = MaterialTheme.typography.titleMedium)
                        }
                        Text(stringResource(R.string.connection_usb_hint), style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
                    }
                }
                SectionHeading(stringResource(R.string.connection_wifi), stringResource(R.string.connection_wifi_hint))
                Button(onClick = { showConnections = false; manualEntry = false; showDiscovery = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Filled.WifiFind, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.connection_discover))
                }
                FilledTonalButton(onClick = { showConnections = false; showQrPairing = true }, Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                    Icon(Icons.Filled.QrCode2, null, Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.connection_qr))
                }
                TextButton(onClick = { showConnections = false; manualEntry = true; showDiscovery = true }, Modifier.fillMaxWidth().heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.connection_manual))
                }
            }
        }
    }
    if (showDiscovery) {
        DiscoveryDialog(vm, onDismiss = { showDiscovery = false }, initialStep = if (manualEntry) DiscoveryStep.ManualChooseType else DiscoveryStep.DeviceList)
    }
    if (showQrPairing) {
        Dialog(onDismissRequest = { showQrPairing = false }, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Surface(Modifier.fillMaxSize().safeDrawingPadding(), color = colors.background) {
                QrPairingScreen(context.filesDir, onBack = { showQrPairing = false }, onPairedAndDiscoverable = { host, port ->
                    showQrPairing = false
                    vm.connectTls(host, port)
                })
            }
        }
    }
    if (!showConnections && !showDiscovery && !showQrPairing) {
        vm.localAdbPromptTarget?.let { target ->
            AlertDialog(
                onDismissRequest = { vm.dismissLocalAdbPrompt(false) },
                icon = { Icon(Icons.Filled.Devices, null) },
                title = { Text(stringResource(R.string.local_adb_detected_title)) },
                text = { Text(stringResource(R.string.local_adb_detected_msg, target.port)) },
                confirmButton = { Button(onClick = { vm.connectLocalAdb() }) { Text(stringResource(R.string.btn_connect)) } },
                dismissButton = { TextButton(onClick = { vm.dismissLocalAdbPrompt(true) }) { Text(stringResource(R.string.btn_dismiss)) } },
            )
        }
    }
}
