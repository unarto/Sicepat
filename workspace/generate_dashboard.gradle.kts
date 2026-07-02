import java.io.File

rootProject {
    tasks.register("generateDashboard") {
        doLast {
            val file = file("app/src/main/java/com/example/DashboardScreen.kt")
            val code = """package com.example

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
fun DashboardScreen() {
    val context = LocalContext.current
    val vpnStatus by SiCepatVpnService.vpnStatus.collectAsStateWithLifecycle()
    val byteStats by SiCepatVpnService.byteStats.collectAsStateWithLifecycle()
    var visibleCards by remember { mutableStateOf(DashboardCardType.values().toList()) }
    var showAddDialog by remember { mutableStateOf(false) }
    var isEditMode by remember { mutableStateOf(false) }

    val vpnLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val intent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
            context.startService(intent)
        }
    }

    fun formatBytes(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "${'$'}bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp.toDouble()), pre)
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
            // Header
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
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = MaterialTheme.colorScheme.onBackground,
                                    modifier = Modifier.size(26.dp)
                                )
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
                            DashboardCardType.NETWORK_SPEED -> {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Speed, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Network speed", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Text("↑ ${'$'}{formatBytes(byteStats.txBytes)}/s   ↓ ${'$'}{formatBytes(byteStats.rxBytes)}/s", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    Box(modifier = Modifier.fillMaxWidth().height(2.dp).background(MaterialTheme.colorScheme.primary))
                                    Box(modifier = Modifier.fillMaxWidth().height(30.dp).background(MaterialTheme.colorScheme.primary.copy(alpha=0.2f)))
                                }
                            }
                            DashboardCardType.OUTBOUND_MODE -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.CallSplit, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Outbound mode", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    OutboundOption("Rule", selected = true)
                                    OutboundOption("Global", selected = false)
                                    OutboundOption("Direct", selected = false)
                                }
                            }
                            DashboardCardType.INTRANET_IP -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Computer, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Intranet IP", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("100.81.47.188", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            DashboardCardType.MEMORY_INFO -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.Memory, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Memory info", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("611.3 MB", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            DashboardCardType.NETWORK -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text("Network", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                        Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("103.127.132.166", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                            DashboardCardType.TRAFFIC_USAGE -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.DataUsage, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Traffic usage", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                                        DonutChart(modifier = Modifier.size(60.dp))
                                    }
                                    Spacer(modifier = Modifier.height(24.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("↑ ${'$'}{formatBytes(byteStats.txBytes)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text("↓ ${'$'}{formatBytes(byteStats.rxBytes)}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            DashboardCardType.VPN -> {
                                Column(modifier = Modifier.padding(16.dp).fillMaxWidth()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(Icons.Default.VpnKey, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("VPN", fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                                        Text("Options", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Switch(
                                            checked = vpnStatus == SiCepatVpnService.VpnStatus.CONNECTED,
                                            onCheckedChange = { checked ->
                                                if (checked) {
                                                    val intent = VpnService.prepare(context)
                                                    if (intent != null) {
                                                        vpnLauncher.launch(intent)
                                                    } else {
                                                        val startIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
                                                        context.startService(startIntent)
                                                    }
                                                } else {
                                                    val stopIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "STOP" }
                                                    context.startService(stopIntent)
                                                }
                                            },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = MaterialTheme.colorScheme.primaryContainer,
                                                checkedTrackColor = MaterialTheme.colorScheme.primary.copy(alpha=0.5f)
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                if (vpnStatus == SiCepatVpnService.VpnStatus.DISCONNECTED) {
                    val intent = VpnService.prepare(context)
                    if (intent != null) {
                        vpnLauncher.launch(intent)
                    } else {
                        val startIntent = Intent(context, SiCepatVpnService::class.java).apply { action = "START" }
                        context.startService(startIntent)
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
fun OutboundOption(text: String, selected: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 4.dp).fillMaxWidth()
    ) {
        RadioButton(
            selected = selected,
            onClick = { /*TODO*/ },
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
}"""
            file.writeText(code)
            println("Replaced!")
        }
    }
}
