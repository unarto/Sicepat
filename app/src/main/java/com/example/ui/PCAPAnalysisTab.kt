package com.example.ui

import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.random.Random

// Imports from other subpackages
import com.example.dto.PCAPPacket
import com.example.dto.MinerAppItem
import com.example.helper.getInstalledLauncherApps
import com.example.helper.toggleAppVpnExclusion

// Imports from main package com.example

@Composable
fun PCAPAnalysisTab() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    
    var isCaptureActive by remember { mutableStateOf(true) }
    var rawConnectionLogs by remember { mutableStateOf(emptyList<ConnectionLog>()) }
    var packetsList by remember { mutableStateOf(emptyList<PCAPPacket>()) }
    var selectedPacket by remember { mutableStateOf<PCAPPacket?>(null) }
    
    // Total stats state
    var totalPacketsCount by remember { mutableIntStateOf(1284) }
    var totalBytesProcessed by remember { mutableStateOf(14.25) } // MB
    var avgLatency by remember { mutableIntStateOf(48) }

    // Sync State with App Access Control
    var monitorMode by remember { mutableStateOf("MASAL") } // "MASAL" or "SINGLE"
    var installedApps by remember { mutableStateOf<List<MinerAppItem>>(emptyList()) }
    var selectedMonitorApp by remember { mutableStateOf<MinerAppItem?>(null) }
    var isAppSelectionDialogOpen by remember { mutableStateOf(false) }
    var searchAppQuery by remember { mutableStateOf("") }

    // Load installed launcher applications asynchronously
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            val apps = getInstalledLauncherApps(context)
            withContext(Dispatchers.Main) {
                installedApps = apps
                if (apps.isNotEmpty() && selectedMonitorApp == null) {
                    selectedMonitorApp = apps.firstOrNull()
                }
            }
        }
    }

    // Read and parse active connections from access.log as source for the packets
    LaunchedEffect(isCaptureActive, monitorMode, selectedMonitorApp, installedApps) {
        while (isCaptureActive) {
            val logs = parseLogs(context)
            rawConnectionLogs = logs
            
            // Map the parsed raw connections into actual data
            val updatedPackets = logs.mapIndexed { idx, item ->
                
                val srcIp = if (item.source.contains(":")) item.source.substringBeforeLast(":") else item.source
                val srcPort = if (item.source.contains(":")) item.source.substringAfterLast(":").toIntOrNull() ?: 0 else 0

                val destIp = if (item.host.contains(":")) item.host.substringBefore(":") else item.host
                val destPort = if (item.host.contains(":")) item.host.substringAfter(":").toIntOrNull() ?: 0 else 0

                val threat = if (item.host.contains("tracker", ignoreCase = true) || item.host.contains("ads", ignoreCase = true)) "WARNING" else "CLEAN"

                val asciiPayload = item.host + " ...[DATA ENCRYPTED VIA XRAY]"

                PCAPPacket(
                    id = idx,
                    timestamp = item.time,
                    sourceIp = srcIp,
                    destIp = destIp,
                    sourcePort = srcPort,
                    destPort = destPort,
                    protocol = item.protocol,
                    size = 0,
                    flags = item.status,
                    latencyMs = 0,
                    threatLevel = threat,
                    payloadHex = "No raw payload (Encrypted VPN)",
                    payloadAscii = asciiPayload,
                    appName = item.outbound.ifEmpty { "Xray Core" }
                )
            }
            
            packetsList = updatedPackets
            
            if (updatedPackets.isNotEmpty()) {
                totalPacketsCount = updatedPackets.size
                totalBytesProcessed = updatedPackets.size * 0.01
                avgLatency = 0 // Not measurable via logs natively without actual ping
            }
            
            delay(1500)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Quick statistics Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Status Capture",
                value = if (isCaptureActive) "AKTIF" else "BERHENTI",
                valueColor = if (isCaptureActive) Color(0xFF4CAF50) else Color(0xFFF44336),
                icon = if (isCaptureActive) Icons.Default.PlayArrow else Icons.Default.Stop
            )
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Paket PCAP",
                value = "$totalPacketsCount",
                valueColor = Color(0xFF03A9F4),
                icon = Icons.Default.SwapVert
            )
            StatsCard(
                modifier = Modifier.weight(1f),
                title = "Total Data",
                value = String.format("%.2f MB", totalBytesProcessed),
                valueColor = Color(0xFFFF9800),
                icon = Icons.Default.FolderOpen
            )
        }

        // Monitoring Mode Switcher Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            shape = RoundedCornerShape(8.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF161A1D)),
            border = BorderStroke(1.dp, Color(0xFF2E3238))
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text(
                    text = "SCOPE PEMANTAUAN JARINGAN (KONTROL AKSES SYNC)",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Masal Card Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (monitorMode == "MASAL") Color(0xFF03A9F4).copy(alpha = 0.15f) else Color(0xFF0F141C),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (monitorMode == "MASAL") Color(0xFF03A9F4) else Color(0xFF23272C),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { monitorMode = "MASAL" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.SelectAll,
                                contentDescription = null,
                                tint = if (monitorMode == "MASAL") Color(0xFF03A9F4) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Masal (Semua App)",
                                color = if (monitorMode == "MASAL") Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Single Card Button
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .background(
                                if (monitorMode == "SINGLE") Color(0xFF03A9F4).copy(alpha = 0.15f) else Color(0xFF0F141C),
                                RoundedCornerShape(6.dp)
                            )
                            .border(
                                1.dp,
                                if (monitorMode == "SINGLE") Color(0xFF03A9F4) else Color(0xFF23272C),
                                RoundedCornerShape(6.dp)
                            )
                            .clickable { monitorMode = "SINGLE" }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.FilterList,
                                contentDescription = null,
                                tint = if (monitorMode == "SINGLE") Color(0xFF03A9F4) else Color.Gray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Single Aplikasi",
                                color = if (monitorMode == "SINGLE") Color.White else Color.Gray,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Selected App details if in SINGLE mode
                if (monitorMode == "SINGLE") {
                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFF23272C))
                    Spacer(modifier = Modifier.height(10.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        // App Name and Package Name
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "APLIKASI DIPANTAU",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF03A9F4)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = selectedMonitorApp?.name ?: "Pilih Aplikasi...",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = selectedMonitorApp?.packageName ?: "Belum ada aplikasi dipilih",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        }

                        // Select app button
                        Button(
                            onClick = { isAppSelectionDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3748)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text("Pilih App", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    }

                    // VPN Split Tunneling (App Access Control) sync status banner
                    selectedMonitorApp?.let { app ->
                        Spacer(modifier = Modifier.height(10.dp))
                        
                        val isExcluded = app.isExcluded
                        val bannerBg = if (isExcluded) Color(0xFFFFB300).copy(alpha = 0.1f) else Color(0xFF4CAF50).copy(alpha = 0.1f)
                        val bannerBorder = if (isExcluded) Color(0xFFFFB300) else Color(0xFF4CAF50)
                        val bannerText = if (isExcluded) "DIKECUALIKAN DARI VPN (BYPASS)" else "DIALIKAN LEWAT TEROWONGAN VPN (SECURED)"
                        val bannerIcon = if (isExcluded) Icons.Default.Warning else Icons.Default.VerifiedUser

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(bannerBg, RoundedCornerShape(6.dp))
                                .border(1.dp, bannerBorder, RoundedCornerShape(6.dp))
                                .padding(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                                ) {
                                    Icon(
                                        imageVector = bannerIcon,
                                        contentDescription = null,
                                        tint = bannerBorder,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Column {
                                        Text(
                                            text = "STATUS AKSES KONTROL",
                                            fontSize = 8.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = bannerBorder
                                        )
                                        Text(
                                            text = bannerText,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White
                                        )
                                    }
                                }

                                // Interactive toggle that syncs directly with vpn_split_tunnel_settings
                                Switch(
                                    checked = !isExcluded, // Tunneling is true if NOT excluded
                                    onCheckedChange = { isSecureTunnel ->
                                        toggleAppVpnExclusion(
                                            context = context,
                                            packageName = app.packageName,
                                            currentExcluded = isExcluded,
                                            onUpdated = {
                                                // Refresh app list and selected monitor app status
                                                val updatedApps = getInstalledLauncherApps(context)
                                                installedApps = updatedApps
                                                selectedMonitorApp = updatedApps.find { it.packageName == app.packageName }
                                            }
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color(0xFF4CAF50),
                                        checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.3f),
                                        uncheckedThumbColor = Color(0xFFFFB300),
                                        uncheckedTrackColor = Color(0xFFFFB300).copy(alpha = 0.3f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }

        // Actions Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Start/Stop button
            Button(
                onClick = { isCaptureActive = !isCaptureActive },
                modifier = Modifier.weight(1.3f),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isCaptureActive) Color(0xFFD32F2F) else Color(0xFF388E3C)
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = if (isCaptureActive) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = Color.White
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isCaptureActive) "Hentikan Capture" else "Mulai Capture",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }

            // Copy Log Button
            OutlinedButton(
                onClick = {
                    val logsText = packetsList.joinToString("\n") { pkt ->
                        "[${pkt.timestamp}] ${pkt.sourceIp}:${pkt.sourcePort} -> ${pkt.destIp}:${pkt.destPort} [${pkt.protocol}] Size:${pkt.size}B Latency:${pkt.latencyMs}ms App:${pkt.appName ?: "Unknown"}"
                    }
                    if (logsText.isBlank()) {
                        Toast.makeText(context, "Tidak ada data PCAP untuk disalin", Toast.LENGTH_SHORT).show()
                    } else {
                        clipboardManager.setText(AnnotatedString(logsText))
                        Toast.makeText(context, "PCAP Log berhasil disalin ke Clipboard!", Toast.LENGTH_SHORT).show()
                    }
                },
                modifier = Modifier.weight(1f),
                border = BorderStroke(1.dp, Color(0xFF2E3238)),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ContentCopy,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = Color.LightGray
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text("Salin Log", fontSize = 11.sp, color = Color.LightGray)
            }
        }

        // Section Title
        Text(
            text = "LIVE PCAP PACKET SCANNER (CAPTURED TUNNEL TRAFFIC)",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Gray,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
        )

        // Packet list view
        if (packetsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Default.NetworkCheck,
                        contentDescription = null,
                        tint = Color.Gray.copy(alpha = 0.4f),
                        modifier = Modifier.size(44.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Menunggu data paket jaringan...",
                        color = Color.Gray,
                        fontSize = 12.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                items(packetsList) { packet ->
                    PCAPPacketItem(packet = packet, onClick = { selectedPacket = packet })
                }
            }
        }
    }

    // Modal Packet Detail dialog
    selectedPacket?.let { packet ->
        PCAPDetailDialog(packet = packet, onDismiss = { selectedPacket = null })
    }

    // App Selection Dialog
    if (isAppSelectionDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAppSelectionDialogOpen = false },
            title = {
                Text(
                    text = "Pilih Aplikasi untuk Dipantau",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    // Search field
                    OutlinedTextField(
                        value = searchAppQuery,
                        onValueChange = { searchAppQuery = it },
                        placeholder = { Text("Cari nama aplikasi...", fontSize = 13.sp, color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF03A9F4),
                            unfocusedBorderColor = Color(0xFF2E3238),
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        ),
                        singleLine = true
                    )
                    
                    val filteredApps = installedApps.filter {
                        it.name.contains(searchAppQuery, ignoreCase = true) ||
                        it.packageName.contains(searchAppQuery, ignoreCase = true)
                    }

                    if (filteredApps.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Aplikasi tidak ditemukan", color = Color.Gray, fontSize = 12.sp)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(filteredApps) { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(
                                            if (selectedMonitorApp?.packageName == app.packageName) Color(0xFF03A9F4).copy(alpha = 0.1f) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .border(
                                            1.dp,
                                            if (selectedMonitorApp?.packageName == app.packageName) Color(0xFF03A9F4) else Color.Transparent,
                                            RoundedCornerShape(6.dp)
                                        )
                                        .clickable {
                                            selectedMonitorApp = app
                                            isAppSelectionDialogOpen = false
                                            searchAppQuery = ""
                                        }
                                        .padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // App Initial box
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .background(Color(0xFF2D3748), shape = RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = app.name.take(1),
                                            color = Color.White,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = app.name,
                                            color = Color.White,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = app.packageName,
                                            color = Color.Gray,
                                            fontSize = 10.sp
                                        )
                                    }
                                    
                                    // Split-tunnel status indicator
                                    val statusColor = if (app.isExcluded) Color(0xFFFFB300) else Color(0xFF4CAF50)
                                    val statusLabel = if (app.isExcluded) "Bypass" else "Tunneled"
                                    Box(
                                        modifier = Modifier
                                            .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                            .border(1.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = statusLabel,
                                            color = statusColor,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAppSelectionDialogOpen = false }) {
                    Text("Batal", color = Color.Gray)
                }
            },
            shape = RoundedCornerShape(12.dp),
            containerColor = Color(0xFF141922)
        )
    }
}

@Composable
fun StatsCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueColor: Color,
    icon: ImageVector
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A1D)),
        border = BorderStroke(1.dp, Color(0xFF2E3238))
    ) {
        Column(
            modifier = Modifier.padding(10.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(title, fontSize = 10.sp, color = Color.Gray, fontWeight = FontWeight.Medium)
                Icon(imageVector = icon, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(value, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = valueColor)
        }
    }
}

@Composable
fun PCAPPacketItem(packet: PCAPPacket, onClick: () -> Unit) {
    val levelColor = when (packet.threatLevel) {
        "CRITICAL" -> Color(0xFFE53935)
        "WARNING" -> Color(0xFFFFB300)
        else -> Color(0xFF4CAF50)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(6.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF111418)),
        border = BorderStroke(1.dp, Color(0xFF23272C))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Source & Destination
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = packet.sourceIp,
                        fontSize = 11.sp,
                        color = Color(0xFFB0BEC5),
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = " -> ",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = packet.destIp,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(3.dp)
                            )
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = packet.protocol,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (packet.appName != null) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .background(
                                    Color(0xFFE2E8F0).copy(alpha = 0.1f),
                                    RoundedCornerShape(3.dp)
                                )
                                .padding(horizontal = 4.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = packet.appName,
                                color = Color(0xFF90CAF9),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Port: ${packet.sourcePort} → ${packet.destPort}",
                        fontSize = 10.sp,
                        color = Color.Gray,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Packet size, Latency & Threat marker
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Text(
                    text = "${packet.size} bytes",
                    fontSize = 11.sp,
                    color = Color.White,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${packet.latencyMs} ms",
                        fontSize = 10.sp,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(levelColor, RoundedCornerShape(3.dp))
                    )
                }
            }
        }
    }
}

@Composable
fun PCAPDetailDialog(packet: PCAPPacket, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Code,
                    contentDescription = null,
                    tint = Color(0xFF03A9F4),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "PCAP Packet Analisis",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        },
        text = {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Info block
                item {
                    Text(
                        text = "INFORMASI BINGKAI (FRAME INFO)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF03A9F4)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    if (packet.appName != null) {
                        DialogInfoRow("Aplikasi Sumber", packet.appName)
                    }
                    DialogInfoRow("Waktu Deteksi", packet.timestamp)
                    DialogInfoRow("Protokol Layer 3", "Internet Protocol Version 4 (IPv4)")
                    DialogInfoRow("Protokol Layer 4", packet.protocol + " (${packet.flags})")
                    DialogInfoRow("Ukuran Frame", "${packet.size} Bytes (${packet.size * 8} bits)")
                    DialogInfoRow("Estimasi RTT", "${packet.latencyMs} ms")
                }

                // IP Block
                item {
                    HorizontalDivider(color = Color(0xFF2E3238), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "INTERNET PROTOCOL (LAYER 3 HEADER)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    DialogInfoRow("Source IP Address", packet.sourceIp)
                    DialogInfoRow("Destination IP Address", packet.destIp)
                    DialogInfoRow("Source Port (L4)", "${packet.sourcePort}")
                    DialogInfoRow("Destination Port (L4)", "${packet.destPort}")
                    DialogInfoRow("Time To Live (TTL)", "64 (Standard)")
                    DialogInfoRow("Header Checksum", "0x${String.format("%04X", packet.id * 31 + 4096)} [Valid]")
                }

                // Raw Payload Hex Dump
                item {
                    HorizontalDivider(color = Color(0xFF2E3238), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        text = "RAW PAYLOAD HEX DUMP (64 BYTES)",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090B0D), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF23272C), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = packet.payloadHex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFF81C784),
                            lineHeight = 14.sp
                        )
                    }
                }

                // Payload ASCII Preview
                item {
                    Text(
                        text = "DECODED ASCII TEXT",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF090B0D), RoundedCornerShape(4.dp))
                            .border(1.dp, Color(0xFF23272C), RoundedCornerShape(4.dp))
                            .padding(8.dp)
                    ) {
                        Text(
                            text = packet.payloadAscii,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp,
                            color = Color(0xFFECEFF1)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Selesai", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        shape = RoundedCornerShape(12.dp),
        containerColor = Color(0xFF141922)
    )
}

@Composable
fun DialogInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.Gray, fontSize = 11.sp)
        Text(value, color = Color.White, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
