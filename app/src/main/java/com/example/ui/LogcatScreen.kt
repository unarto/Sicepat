package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

data class LogEntry(
    val level: String,
    val message: String,
    val timestamp: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogcatScreen(onBack: () -> Unit) {
    var isRunning by remember { mutableStateOf(true) }
    val logsList = remember { mutableStateListOf<LogEntry>() }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }
    val context = LocalContext.current

    // Add initial logs and start reading logcat
    LaunchedEffect(isRunning) {
        if (isRunning) {
            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                var process: java.lang.Process? = null
                try {
                    // Start reading live stream continuously
                    process = Runtime.getRuntime().exec(arrayOf("logcat", "-v", "threadtime"))
                    val liveReader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                    while (isRunning) {
                        val liveLine = liveReader.readLine() ?: break
                        val matchTime = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+([A-Z])/(.*?)\(\s*\d+\):\s+(.*)""").find(liveLine)
                        val matchThreadTime = Regex("""^(\d{2}-\d{2}\s\d{2}:\d{2}:\d{2}\.\d{3})\s+\d+\s+\d+\s+([A-Z])\s+(.*?):\s+(.*)""").find(liveLine)
                        
                        var levelChar = "I"
                        var tag = ""
                        var msg = liveLine
                        var time = ""

                        if (matchTime != null) {
                            time = matchTime.groupValues[1]
                            levelChar = matchTime.groupValues[2]
                            tag = matchTime.groupValues[3].trim()
                            msg = matchTime.groupValues[4]
                        } else if (matchThreadTime != null) {
                            time = matchThreadTime.groupValues[1]
                            levelChar = matchThreadTime.groupValues[2]
                            tag = matchThreadTime.groupValues[3].trim()
                            msg = matchThreadTime.groupValues[4]
                        } else {
                            // If it's a continuation line, try to extract level from it if it contains [Debug] etc
                        }
                        
                        val level = when {
                            msg.contains("[Debug]", ignoreCase = true) -> "Debug"
                            msg.contains("[Info]", ignoreCase = true) -> "Info"
                            msg.contains("[Warning]", ignoreCase = true) -> "Warning"
                            msg.contains("[Error]", ignoreCase = true) -> "Error"
                            else -> when (levelChar) {
                                "V" -> "Verbose"
                                "D" -> "Debug"
                                "I" -> "Info"
                                "W" -> "Warning"
                                "E" -> "Error"
                                "F" -> "Fatal"
                                else -> "Info"
                            }
                        }
                        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                            logsList.add(LogEntry(level, liveLine, time))
                            if (logsList.size > 2000) {
                                logsList.removeAt(0)
                            }
                        }
                    }
                } catch (e: Exception) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                        logsList.add(LogEntry("Error", "Failed to read logcat: ${e.message}", "00:00:00.000"))
                    }
                } finally {
                    process?.destroy()
                }
            }
        }
    }

    var filterLevel by remember { mutableStateOf("ALL") } // "ALL" or "ERROR"

    // Filtered logs list matching search query and level filter
    val filteredLogs = remember(searchQuery, filterLevel, logsList.size) {
        val baseList = if (filterLevel == "ERROR") {
            logsList.filter { it.level.equals("Error", ignoreCase = true) || it.level.equals("Fatal", ignoreCase = true) }
        } else {
            logsList.toList()
        }
        
        if (searchQuery.isBlank()) {
            baseList
        } else {
            baseList.filter {
                it.message.contains(searchQuery, ignoreCase = true) ||
                it.level.contains(searchQuery, ignoreCase = true)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationHeader(
            title = "System Logs", 
            onBack = onBack,
            actions = {
                IconButton(onClick = { isSearchActive = !isSearchActive }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = { 
                    isRunning = !isRunning
                    Toast.makeText(context, "Log streaming ${if (isRunning) "started" else "stopped"}", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(
                        imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                        contentDescription = if (isRunning) "Stop" else "Start",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
                IconButton(onClick = {
                    if (filteredLogs.isEmpty()) {
                        Toast.makeText(context, "No logs to copy", Toast.LENGTH_SHORT).show()
                    } else {
                        val allLogsText = filteredLogs.joinToString("\n") { it.message }
                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clipData = android.content.ClipData.newPlainText("Logcat Logs", allLogsText)
                        clipboardManager.setPrimaryClip(clipData)
                        Toast.makeText(context, "Logs copied to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Logs", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = {
                    logsList.clear()
                    Toast.makeText(context, "Logs cleared", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = "Clear Logs", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search logs...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF03A9F4),
                    unfocusedBorderColor = Color(0xFF2E3238),
                    focusedContainerColor = Color(0xFF161A1D),
                    unfocusedContainerColor = Color(0xFF161A1D)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(12.dp)
            )
        }

        // Custom filter selection row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val filterOptions = listOf(
                "ALL" to "Semua Log",
                "ERROR" to "Hanya Error"
            )
            filterOptions.forEach { (key, label) ->
                val isSelected = filterLevel == key
                val backgroundColor = if (isSelected) Color(0xFF03A9F4).copy(alpha = 0.15f) else Color(0xFF161A1D)
                val borderColor = if (isSelected) Color(0xFF03A9F4) else Color(0xFF2E3238)
                val textColor = if (isSelected) Color(0xFF03A9F4) else Color.Gray

                Box(
                    modifier = Modifier
                        .background(backgroundColor, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .border(1.dp, borderColor, androidx.compose.foundation.shape.RoundedCornerShape(20.dp))
                        .clickable { filterLevel = key }
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (key == "ERROR") {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Error Filter",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        } else {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "All Filter",
                                tint = textColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = label,
                            color = textColor,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }

        if (filteredLogs.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    text = if (searchQuery.isEmpty()) {
                        if (filterLevel == "ERROR") "Tidak ada log Error" else "No logs recorded"
                    } else "No logs matching criteria",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 15.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                items(filteredLogs) { log ->
                    LogItemView(log = log)
                }
            }
        }
    }
}

@Composable
fun LogItemView(log: LogEntry) {
    Text(
        text = log.message,
        color = Color(0xFFEEEEEE),
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal,
        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        lineHeight = 16.sp
    )
}
