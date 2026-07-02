package com.example.ui

import com.example.viewmodel.AppViewModel
import com.example.service.SiCepatVpnService
import com.example.service.XrayCore
import com.example.fmt.XrayConfigGenerator
import androidx.compose.ui.text.style.TextAlign
import android.app.Activity
import android.content.Intent
import android.net.VpnService
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import android.widget.Toast
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import java.util.Collections

enum class DashboardCardType(val title: String) {
    NETWORK_SPEED("Network speed"),
    OUTBOUND_MODE("Outbound mode"),
    INTRANET_IP("Intranet IP"),
    MEMORY_INFO("Memory info"),
    NETWORK("Network"),
    TRAFFIC_USAGE("Traffic usage"),
    VPN("VPN")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DashboardScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val vpnStatus by SiCepatVpnService.vpnStatus.collectAsStateWithLifecycle()
    val connectionMode by SiCepatVpnService.connectionMode.collectAsStateWithLifecycle()
    val byteStats by SiCepatVpnService.byteStats.collectAsStateWithLifecycle()
    var visibleCards by remember { mutableStateOf(DashboardCardType.values().toList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }
    var activeEditProxy by remember { mutableStateOf<ProxyItem?>(null) }
    var qrDialogProxy by remember { mutableStateOf<ProxyItem?>(null) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
            context.startService(intent)
        }
    }



    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Cards") },
            text = {
                Column {
                    DashboardCardType.values().forEach { cardType ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (visibleCards.contains(cardType)) {
                                        visibleCards = visibleCards.filter { it != cardType }
                                    } else {
                                        visibleCards = (visibleCards + cardType).distinct()
                                    }
                                }
                                .padding(vertical = 8.dp)
                        ) {
                            Checkbox(
                                checked = visibleCards.contains(cardType),
                                onCheckedChange = { checked ->
                                    if (checked) {
                                        visibleCards = (visibleCards + cardType).distinct()
                                    } else {
                                        visibleCards = visibleCards.filter { it != cardType }
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(cardType.title, color = MaterialTheme.colorScheme.onSurface)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text("Done")
                }
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyVerticalStaggeredGrid(
            columns = StaggeredGridCells.Fixed(2),
            modifier = Modifier.fillMaxSize().padding(bottom = 80.dp),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalItemSpacing = 12.dp
        ) {
            item(span = StaggeredGridItemSpan.FullLine) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Dashboard",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isEditMode) {
                            IconButton(onClick = { showAddDialog = true }) {
                                Icon(
                                    imageVector = Icons.Default.AddCircleOutline,
                                    contentDescription = "Add",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            IconButton(onClick = { isEditMode = false }) {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = "Save",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        } else {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Active Status",
                                tint = Color(0xFF20DB93),
                                modifier = Modifier.padding(end = 8.dp).size(28.dp)
                            )
                            IconButton(onClick = { isEditMode = true }) {
                                Icon(
                                    imageVector = Icons.Default.Settings,
                                    contentDescription = "Edit Dashboard Layout",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                        }
                    }
                }
            }

            item(span = StaggeredGridItemSpan.FullLine) {
                val activeProxy = viewModel.proxies.find { it.isSelected }
                if (activeProxy != null) {
                    Column(modifier = Modifier.padding(bottom = 12.dp)) {
                        Text(
                            text = "Active Proxy Server",
                            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = activeProxy.name,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = activeProxy.type, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text(
                                        text = activeProxy.latency, 
                                        fontSize = 14.sp, 
                                        fontWeight = FontWeight.Medium,
                                        color = if (activeProxy.latency == "Timeout") Color(0xFFE57373) else MaterialTheme.colorScheme.primary
                                    )
                                }
                                Spacer(modifier = Modifier.height(16.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    val clipboardManager = LocalClipboardManager.current
                                    IconButton(onClick = { activeEditProxy = activeProxy }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { qrDialogProxy = activeProxy }) {
                                        Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.primary)
                                    }
                                    IconButton(onClick = { 
                                        clipboardManager.setText(AnnotatedString(XrayConfigGenerator.generateProxyUri(activeProxy)))
                                        Toast.makeText(context, "Exported config to clipboard!", Toast.LENGTH_SHORT).show()
                                    }) {
                                        Icon(Icons.Default.ContentPaste, contentDescription = "Export Config", tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (vpnStatus == SiCepatVpnService.VpnStatus.CONNECTED && connectionMode == SiCepatVpnService.ConnectionMode.PROXY_ONLY) {
                item(span = StaggeredGridItemSpan.FullLine) {
                    val clipboardManager = LocalClipboardManager.current
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp)
                            .animateItemPlacement(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Info, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Local Proxy Address", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("SOCKS5: 127.0.0.1:10808", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Text("HTTP: 127.0.0.1:10809", color = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString("SOCKS5: 127.0.0.1:10808\nHTTP: 127.0.0.1:10809"))
                                    Toast.makeText(context, "Copied to clipboard", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Copy Configuration")
                            }
                        }
                    }
                }
            }

            visibleCards.forEachIndexed { index, cardType ->
                val onMoveLeft = if (index > 0) { {
                    val mut = visibleCards.toMutableList()
                    Collections.swap(mut, index, index - 1)
                    visibleCards = mut
                } } else null
                
                val onMoveRight = if (index < visibleCards.size - 1) { {
                    val mut = visibleCards.toMutableList()
                    Collections.swap(mut, index, index + 1)
                    visibleCards = mut
                } } else null

                item(
                    span = if (cardType == DashboardCardType.NETWORK_SPEED) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane,
                    key = cardType.name
                ) {
                    val cardModifier = if (cardType == DashboardCardType.NETWORK_SPEED) Modifier.fillMaxWidth().height(160.dp) else Modifier
                    DashboardCard(
                        showCloseButton = isEditMode,
                        onClose = { visibleCards = visibleCards.filter { it != cardType } },
                        onMoveLeft = if (isEditMode) onMoveLeft else null,
                        onMoveRight = if (isEditMode) onMoveRight else null,
                        modifier = cardModifier.animateItemPlacement()
                    ) {
                        when (cardType) {
                            DashboardCardType.NETWORK_SPEED -> NetworkSpeedCard(byteStats)
                            DashboardCardType.OUTBOUND_MODE -> OutboundModeCard(viewModel)
                            DashboardCardType.INTRANET_IP -> IntranetIpCard()
                            DashboardCardType.MEMORY_INFO -> MemoryInfoCard()
                            DashboardCardType.NETWORK -> NetworkCard()
                            DashboardCardType.TRAFFIC_USAGE -> TrafficUsageCard()
                            DashboardCardType.VPN -> VpnOptionCard(connectionMode)
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (vpnStatus == SiCepatVpnService.VpnStatus.DISCONNECTED) {
                    if (connectionMode == SiCepatVpnService.ConnectionMode.PROXY_ONLY) {
                        val startIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
                        context.startService(startIntent)
                    } else {
                        val intent = VpnService.prepare(context)
                        if (intent != null) {
                            vpnLauncher.launch(intent)
                        } else {
                            val startIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
                            context.startService(startIntent)
                        }
                    }
                } else {
                    val stopIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "STOP" }
                    context.startService(stopIntent)
                }
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(16.dp)
                .size(64.dp),
            containerColor = MaterialTheme.colorScheme.primary,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = if (vpnStatus == SiCepatVpnService.VpnStatus.CONNECTED) Icons.Default.Stop else Icons.Default.PlayArrow,
                contentDescription = "Toggle VPN",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(32.dp)
            )
        }
        
        if (activeEditProxy != null) {
            EditProxyScreen(
                editingProxyInstance = activeEditProxy!!,
                proxiesList = viewModel.proxies,
                onDismiss = { activeEditProxy = null }
            )
        }

        if (qrDialogProxy != null) {
            val proxy = qrDialogProxy!!
            val proxyUri = remember(proxy) { XrayConfigGenerator.generateProxyUri(proxy) }
            AlertDialog(
                onDismissRequest = { qrDialogProxy = null },
                title = {
                    Text(
                        text = "QR Code share",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                text = {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = proxy.name,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Box(
                            modifier = Modifier
                                .size(180.dp)
                                .background(Color.White, RoundedCornerShape(8.dp))
                                .padding(8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("QR: ${proxy.name}", color = Color.Black, textAlign = TextAlign.Center, fontSize = 12.sp)
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { qrDialogProxy = null }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DashboardCard(
    modifier: Modifier = Modifier,
    showCloseButton: Boolean = true,
    onMoveLeft: (() -> Unit)? = null,
    onMoveRight: (() -> Unit)? = null,
    onClose: () -> Unit,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.padding(top = 8.dp, bottom = 4.dp)) {
        Card(
            modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        ) {
            content()
        }
        
        if (showCloseButton) {
            Row(modifier = Modifier.align(Alignment.TopEnd).padding(end = 6.dp), verticalAlignment = Alignment.CenterVertically) {
                if (onMoveLeft != null) {
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFE0E0E0)).clickable { onMoveLeft() }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.KeyboardArrowLeft, contentDescription = "Move Left", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(4.dp))
                }
                if (onMoveRight != null) {
                    Box(modifier = Modifier.size(26.dp).clip(CircleShape).background(Color(0xFFE0E0E0)).clickable { onMoveRight() }, contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "Move Right", tint = Color.Black, modifier = Modifier.size(16.dp))
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier
                        .size(26.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF81D4FA))
                        .clickable { onClose() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = Color(0xFF0D47A1),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun OutboundOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth().clickable { onClick() }
    ) {
        RadioButton(
            selected = selected,
            onClick = { onClick() },
            colors = RadioButtonDefaults.colors(
                selectedColor = MaterialTheme.colorScheme.primary,
                unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            ),
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = text, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

private fun formatBytes(bytes: Long): String {
    val unit = 1024.0
    if (bytes < unit) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format("%.1f %sB", bytes / Math.pow(unit, exp.toDouble()), pre)
}

@Composable
fun NetworkSpeedCard(byteStats: SiCepatVpnService.ByteStats) {
    Column(modifier = Modifier.padding(16.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Network speed", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("↑ ${formatBytes(byteStats.txBytes)}/s   ↓ ${formatBytes(byteStats.rxBytes)}/s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.weight(1f))
        Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
        Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
    }
}

@Composable
fun OutboundModeCard(viewModel: AppViewModel) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Outbound mode", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutboundOption("Proxy", selected = (viewModel.globalOutboundMode == "Proxy"), onClick = { viewModel.updateOutboundMode("Proxy") })
        OutboundOption("Block", selected = (viewModel.globalOutboundMode == "Block"), onClick = { viewModel.updateOutboundMode("Block") })
        OutboundOption("Direct", selected = (viewModel.globalOutboundMode == "Direct"), onClick = { viewModel.updateOutboundMode("Direct") })
    }
}

@Composable
fun IntranetIpCard() {
    val localIp = remember { 
        var ip = "127.0.0.1"
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces()
            val ips = mutableListOf<String>()
            while (interfaces.hasMoreElements()) {
                val networkInterface = interfaces.nextElement()
                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()
                    if (!address.isLoopbackAddress && address is java.net.Inet4Address) {
                        address.hostAddress?.let { ips.add(it) }
                    }
                }
            }
            if (ips.isNotEmpty()) {
                ip = ips.first()
            }
        } catch (e: Exception) { }
        ip
    }
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Intranet IP", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(localIp, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun MemoryInfoCard() {
    val context = LocalContext.current
    val memoryInfo = remember { android.app.ActivityManager.MemoryInfo() }
    val activityManager = remember { context.getSystemService(android.content.Context.ACTIVITY_SERVICE) as android.app.ActivityManager }
    var availMemMb by remember { mutableStateOf(0L) }
    
    androidx.compose.runtime.LaunchedEffect(Unit) {
        while(true) {
            activityManager.getMemoryInfo(memoryInfo)
            availMemMb = memoryInfo.availMem / (1024 * 1024)
            kotlinx.coroutines.delay(2000)
        }
    }
    
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Memory Info (Free)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text("$availMemMb MB", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun NetworkCard() {
    var publicIp by remember { mutableStateOf("Checking...") }
    androidx.compose.runtime.LaunchedEffect(Unit) {
        if (publicIp == "Checking...") {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                try {
                    val url = java.net.URL("https://api.ipify.org")
                    val con = url.openConnection() as java.net.HttpURLConnection
                    con.connectTimeout = 3000
                    con.readTimeout = 3000
                    val reader = java.io.BufferedReader(java.io.InputStreamReader(con.inputStream))
                    val ip = reader.readLine()
                    reader.close()
                    publicIp = ip ?: "Unknown"
                } catch (e: Exception) {
                    publicIp = "Timeout/Error"
                }
            }
        }
    }
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Network (Public IP)", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(publicIp, fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun TrafficUsageCard() {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Traffic usage", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.height(24.dp))
        
        val totalTrafficBytes = android.net.TrafficStats.getTotalRxBytes() + android.net.TrafficStats.getTotalTxBytes()
        
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(100.dp)) {
                DonutChart(modifier = Modifier.size(100.dp))
                Text("${formatBytes(totalTrafficBytes)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Used", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatBytes(totalTrafficBytes)}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Sent", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatBytes(android.net.TrafficStats.getTotalTxBytes())}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Received", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${formatBytes(android.net.TrafficStats.getTotalRxBytes())}", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
            }
        }
    }
}

@Composable
fun VpnOptionCard(connectionMode: SiCepatVpnService.ConnectionMode) {
    Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (connectionMode == SiCepatVpnService.ConnectionMode.VPN) "Mode VPN" else "Mode Proxy",
                fontWeight = FontWeight.SemiBold, 
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Options", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Switch(
                checked = connectionMode == SiCepatVpnService.ConnectionMode.VPN,
                onCheckedChange = { checked ->
                    val newMode = if (checked) SiCepatVpnService.ConnectionMode.VPN else SiCepatVpnService.ConnectionMode.PROXY_ONLY
                    SiCepatVpnService.setMode(newMode)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                    checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f)
                )
            )
        }
    }
}

@Composable
fun DonutChart(modifier: Modifier = Modifier) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val trackColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    Canvas(modifier = modifier) {
        drawArc(
            color = trackColor,
            startAngle = 0f,
            sweepAngle = 360f,
            useCenter = false,
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )
        drawArc(
            color = primaryColor,
            startAngle = -90f,
            sweepAngle = 270f,
            useCenter = false,
            style = Stroke(width = 16f, cap = StrokeCap.Round)
        )
    }
}