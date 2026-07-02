package com.example.ui

import android.content.Context
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.io.File

data class ConnectionLog(val id: Int, val time: String, val source: String, val status: String, val protocol: String, val host: String, val outbound: String)

fun parseLogs(context: Context): List<ConnectionLog> {
    val file = File(context.filesDir, "access.log")
    if (!file.exists()) return emptyList()
    return try {
        val lines = file.readLines().takeLast(300).reversed()
        val logs = mutableListOf<ConnectionLog>()
        var idCounter = 0
        for (line in lines) {
            val parts = line.split(" ", limit = 6)
            if (parts.size >= 6) {
                val time = "${parts[0].substringAfterLast("/")} ${parts[1]}"
                val source = parts[2]
                val status = parts[3]
                val destStr = parts[4]
                val protocol = destStr.substringBefore(":")
                val host = destStr.substringAfter(":")
                val outbound = parts.getOrNull(5)?.removeSurrounding("[", "]") ?: ""
                logs.add(ConnectionLog(idCounter++, time, source, status, protocol.uppercase(), host, outbound))
            }
        }
        logs
    } catch (e: Exception) {
        emptyList()
    }
}

@Composable
fun ConnectionsScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var logs by remember { mutableStateOf(emptyList<ConnectionLog>()) }
    var isSearching by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredLogs = remember(logs, searchQuery) {
        if (searchQuery.isBlank()) logs else logs.filter {
            it.host.contains(searchQuery, ignoreCase = true) || 
            it.protocol.contains(searchQuery, ignoreCase = true) ||
            it.status.contains(searchQuery, ignoreCase = true) ||
            it.outbound.contains(searchQuery, ignoreCase = true)
        }
    }

    LaunchedEffect(Unit) {
        while (true) {
            logs = parseLogs(context)
            delay(2000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationHeader(
            title = "Connections", 
            onBack = onBack,
            actions = {
                IconButton(onClick = { isSearching = !isSearching }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = {
                    val textToCopy = logs.joinToString("\n") { "${it.time} ${it.protocol} ${it.host} ${it.status} ${it.outbound}" }
                    clipboardManager.setText(AnnotatedString(textToCopy))
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Connections", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = {
                    try { File(context.filesDir, "access.log").writeText("") } catch (e: Exception) {}
                    logs = emptyList()
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Connections", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        if (isSearching) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search connections...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF03A9F4),
                    unfocusedBorderColor = Color(0xFF2E3238),
                    focusedContainerColor = Color(0xFF161A1D),
                    unfocusedContainerColor = Color(0xFF161A1D)
                ),
                shape = RoundedCornerShape(12.dp)
            )
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(160.dp), contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            drawCircle(color = Color(0xFF03A9F4).copy(alpha = 0.15f), radius = size.minDimension / 2.2f)
                        }
                        Card(
                            modifier = Modifier.size(90.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2228)),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(2.dp, Color(0xFF2E3238))
                        ) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Icon(imageVector = Icons.Default.SwapVert, contentDescription = null, tint = Color(0xFF03A9F4), modifier = Modifier.size(44.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(if (logs.isEmpty()) "No Connections yet" else "No matching connections", fontSize = 16.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredLogs, key = { it.id }) { log ->
                    ConnectionItem(log)
                }
            }
        }
    }
}

@Composable
fun ConnectionItem(log: ConnectionLog) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF161A1D)),
        border = BorderStroke(1.dp, Color(0xFF2E3238))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.background(if (log.status.contains("accepted", true)) Color(0xFF4CAF50).copy(0.2f) else Color(0xFFF44336).copy(0.2f), RoundedCornerShape(4.dp)).padding(horizontal = 6.dp, vertical = 2.dp)) {
                        Text(log.protocol, color = if (log.status.contains("accepted", true)) Color(0xFF4CAF50) else Color(0xFFF44336), fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(log.time, color = Color.Gray, fontSize = 12.sp)
                }
                Text(log.outbound, color = Color(0xFF03A9F4), fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(log.host, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Status: ${log.status}", color = Color.Gray, fontSize = 12.sp)
        }
    }
}
