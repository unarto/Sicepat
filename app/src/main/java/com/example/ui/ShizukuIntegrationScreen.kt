package com.example.ui

import com.example.viewmodel.AppViewModel
import com.example.dto.DiagnosticLog
import com.example.service.XrayCore
import android.content.Context
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import rikka.shizuku.Shizuku
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShizukuIntegrationScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var isInstalled by remember { mutableStateOf(false) }
    var isRunning by remember { mutableStateOf(false) }
    var hasPermission by remember { mutableStateOf(false) }
    var shizukuVersion by remember { mutableStateOf(-1) }
    var terminalOutput by remember { mutableStateOf("No diagnostic commands executed yet.") }
    var isExecuting by remember { mutableStateOf(false) }

    // Shizuku settings screen state
    var useForCore by remember { mutableStateOf(viewModel.shizukuUseForCore) }
    var routingMode by remember { mutableStateOf(viewModel.shizukuRoutingMode) }
    var bypassDns by remember { mutableStateOf(viewModel.shizukuBypassDns) }
    var listenPort by remember { mutableStateOf(viewModel.shizukuListenPort) }

    // Core status
    var coreActive by remember { mutableStateOf(XrayCore.isRunning) }
    var coreShizukuActive by remember { mutableStateOf(XrayCore.isShizukuModeActive) }

    // Custom Command input state
    var customCommand by remember { mutableStateOf("getprop ro.product.model") }

    // Logic to reload/update state
    fun checkShizukuState() {
        isInstalled = try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }

        isRunning = try {
            Shizuku.pingBinder()
        } catch (e: Throwable) {
            false
        }

        hasPermission = if (isRunning) {
            try {
                Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }
        } else {
            false
        }

        shizukuVersion = if (isRunning) {
            try {
                Shizuku.getVersion()
            } catch (e: Throwable) {
                -1
            }
        } else {
            -1
        }
        
        coreActive = XrayCore.isRunning
        coreShizukuActive = XrayCore.isShizukuModeActive
    }

    // Set up request permission result listener
    val permissionListener = remember {
        Shizuku.OnRequestPermissionResultListener { _, grantResult ->
            hasPermission = grantResult == PackageManager.PERMISSION_GRANTED
            if (hasPermission) {
                Toast.makeText(context, "Shizuku permission granted!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Shizuku permission denied!", Toast.LENGTH_SHORT).show()
            }
            checkShizukuState()
        }
    }

    DisposableEffect(Unit) {
        checkShizukuState()
        try {
            Shizuku.addRequestPermissionResultListener(permissionListener)
        } catch (e: Throwable) {
            // Shizuku not running or older version
        }
        onDispose {
            try {
                Shizuku.removeRequestPermissionResultListener(permissionListener)
            } catch (e: Throwable) {
                // ignore
            }
        }
    }

    // Dynamic core tracker loop
    LaunchedEffect(Unit) {
        while (true) {
            coreActive = XrayCore.isRunning
            coreShizukuActive = XrayCore.isShizukuModeActive
            delay(1500)
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F1A24))) {
        Column(modifier = Modifier.fillMaxSize()) {
            ApplicationHeader(
                title = "Shizuku Integration",
                onBack = onBack,
                actions = {
                    IconButton(onClick = { 
                        checkShizukuState()
                        Toast.makeText(context, "Status reloaded", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Reload Status", tint = Color.White)
                    }
                }
            )

            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Intro Text helper
                item {
                    Text(
                        text = "Shizuku allows selected applications to utilize specialized system APIs directly via ADB or Root permissions.",
                        fontSize = 14.sp,
                        color = Color(0xFFB0BEC5),
                        lineHeight = 20.sp,
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                }

                // Shizuku Manager info Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.SettingsInputAntenna,
                                    contentDescription = null,
                                    tint = if (isInstalled) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shizuku Manager App",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                     )
                                    Text(
                                        text = if (isInstalled) "Installed on your device" else "Not found. Please install the Shizuku app",
                                        fontSize = 13.sp,
                                        color = if (isInstalled) Color(0xFF81C784) else Color(0xFFFF8A80)
                                    )
                                }
                            }

                            if (!isInstalled) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                             val playStoreIntent = android.content.Intent(
                                                 android.content.Intent.ACTION_VIEW,
                                                 android.net.Uri.parse("market://details?id=moe.shizuku.privileged.api")
                                             )
                                             context.startActivity(playStoreIntent)
                                        } catch (e: Exception) {
                                             val webIntent = android.content.Intent(
                                                 android.content.Intent.ACTION_VIEW,
                                                 android.net.Uri.parse("https://github.com/RikkaApps/Shizuku")
                                             )
                                             context.startActivity(webIntent)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Get Shizuku App", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Shizuku Service Running Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (isRunning) Icons.Default.CloudQueue else Icons.Default.CloudOff,
                                    contentDescription = null,
                                    tint = if (isRunning) Color(0xFF4CAF50) else Color(0xFFFF5252),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Shizuku Service Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (isRunning) "Running (API v$shizukuVersion)" else "Not Running / Dormant",
                                        fontSize = 13.sp,
                                        color = if (isRunning) Color(0xFF81C784) else Color(0xFFFF8A80)
                                    )
                                }
                            }

                            if (isInstalled && !isRunning) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            val launchIntent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
                                            if (launchIntent != null) {
                                                context.startActivity(launchIntent)
                                            } else {
                                                Toast.makeText(context, "Failed to launch Shizuku app", Toast.LENGTH_SHORT).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.Launch, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Open Shizuku Manager", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Shizuku App Permission Card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (hasPermission) Icons.Default.VerifiedUser else Icons.Default.GppMaybe,
                                    contentDescription = null,
                                    tint = if (hasPermission) Color(0xFF4CAF50) else Color(0xFFFF9800),
                                    modifier = Modifier.size(28.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "Connection Permission",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = if (hasPermission) "Authorized" else "Needs Authorization",
                                        fontSize = 13.sp,
                                        color = if (hasPermission) Color(0xFF81C784) else Color(0xFFFFB74D)
                                    )
                                }
                            }

                            if (isRunning && !hasPermission) {
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        try {
                                            Shizuku.requestPermission(1001)
                                        } catch (e: Throwable) {
                                            Toast.makeText(context, "Error requesting: ${e.message}", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Authorize Connection", fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }

                // Shizuku VPN & Xray Core Integration Configuration Panel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VpnKey,
                                    contentDescription = null,
                                    tint = if (useForCore) Color(0xFF4CAF50) else Color(0xFF90A4AE),
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Core VPN & Xray Integration",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = Color.White
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(14.dp))
                            
                            // 1. Enable Shizuku for Core Toggle
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Jalankan Core lewat Shizuku",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "Execute XrayCore under elevated shell process",
                                        fontSize = 12.sp,
                                        color = Color(0xFFB0BEC5)
                                    )
                                }
                                Switch(
                                    checked = useForCore,
                                    onCheckedChange = { useForCore = it },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFF4CAF50)
                                    )
                                )
                            }
                            
                            AnimatedVisibility(visible = useForCore) {
                                Column(modifier = Modifier.padding(top = 16.dp)) {
                                    Divider(color = Color(0xFF233545), thickness = 1.dp)
                                    Spacer(modifier = Modifier.height(12.dp))
                                    
                                    // 2. Routing Mode Selector
                                    Text(
                                        text = "Shizuku Routing Engine",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF90CAF9),
                                        modifier = Modifier.padding(bottom = 6.dp)
                                    )
                                    
                                    listOf("VpnService" to "Standard Virtual interface TUN mapping", 
                                           "Transparent Proxy" to "Elevated IPTable REDIRECT rules (No local TUN context)").forEach { (mode, desc) ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { routingMode = mode }
                                                .padding(vertical = 4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            RadioButton(
                                                selected = routingMode == mode,
                                                onClick = { routingMode = mode },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF1E88E5))
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(text = mode, fontSize = 14.sp, color = Color.White, fontWeight = FontWeight.Medium)
                                                Text(text = desc, fontSize = 11.sp, color = Color(0xFFB0BEC5))
                                            }
                                        }
                                    }
                                    
                                    Spacer(modifier = Modifier.height(14.dp))
                                    
                                    // 3. Bypass DNS (Transparent Proxy only)
                                    if (routingMode == "Transparent Proxy") {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "Bypass DNS (Port 53)",
                                                    fontSize = 13.sp,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Medium
                                                )
                                                Text(
                                                    text = "Exempt UDP/TCP port 53 inquiries from redirect",
                                                    fontSize = 11.sp,
                                                    color = Color(0xFFB0BEC5)
                                                )
                                            }
                                            Switch(
                                                checked = bypassDns,
                                                onCheckedChange = { bypassDns = it },
                                                colors = SwitchDefaults.colors(
                                                    checkedThumbColor = Color.White,
                                                    checkedTrackColor = Color(0xFF1E88E5)
                                                )
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(14.dp))
                                    }
                                    
                                    // 4. Custom Socks Listening Port
                                    OutlinedTextField(
                                        value = listenPort,
                                        onValueChange = { listenPort = it },
                                        label = { Text("Local Socks listen port") },
                                        placeholder = { Text("default: 10808") },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedTextColor = Color.White,
                                            unfocusedTextColor = Color.White,
                                            focusedBorderColor = Color(0xFF1E88E5),
                                            unfocusedBorderColor = Color(0xFF233545)
                                        )
                                    )
                                    
                                    Spacer(modifier = Modifier.height(12.dp))
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            
                            // 5. Action: Simpan Konfigurasi
                            Button(
                                onClick = {
                                    viewModel.shizukuUseForCore = useForCore
                                    viewModel.shizukuRoutingMode = routingMode
                                    viewModel.shizukuBypassDns = bypassDns
                                    viewModel.shizukuListenPort = listenPort.ifBlank { "10808" }
                                    viewModel.saveShizukuConfig(context)
                                    Toast.makeText(context, "Shizuku Configuration Saved Successfully!", Toast.LENGTH_SHORT).show()
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Simpan Konfigurasi Shizuku", fontSize = 13.sp)
                            }
                        }
                    }
                }

                // Shizuku Active Core monitoring status
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Running Core Engine",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 8.dp)
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(androidx.compose.foundation.shape.CircleShape)
                                            .background(if (coreActive) Color(0xFF4CAF50) else Color(0xFFFF5252))
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (coreActive) "Core VPN Running" else "Core VPN Stopped",
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                                if (coreActive) {
                                    Text(
                                        text = if (coreShizukuActive) "Elevated (Shizuku)" else "Standard (Sandboxed)",
                                        fontSize = 12.sp,
                                        color = if (coreShizukuActive) Color(0xFF81C784) else Color(0xFF90A4AE),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }

                // Shizuku elevated diagnostic shell command panel
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Elevated Diagnostic Shell",
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp,
                                color = Color.White,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                            Text(
                                text = "Verify your elevated ADB Binder access by sending dynamic system queries directly via Shizuku.",
                                fontSize = 13.sp,
                                color = Color(0xFF90A4AE),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            // Perintah Kustom (Custom Command Input) Section
                            Text(
                                text = "Perintah Kustom (Custom Command)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.padding(bottom = 6.dp)
                            )

                            OutlinedTextField(
                                value = customCommand,
                                onValueChange = { customCommand = it },
                                placeholder = { Text("Contoh: pm list packages -3", color = Color.Gray) },
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth(),
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.Terminal,
                                        contentDescription = null,
                                        tint = Color(0xFF90A4AE)
                                    )
                                },
                                trailingIcon = {
                                    if (customCommand.isNotEmpty()) {
                                        IconButton(onClick = { customCommand = "" }) {
                                            Icon(
                                                imageVector = Icons.Default.Clear,
                                                contentDescription = "Hapus Perintah",
                                                tint = Color.Gray
                                            )
                                        }
                                    }
                                },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    focusedBorderColor = Color(0xFF1E88E5),
                                    unfocusedBorderColor = Color(0xFF233545),
                                    focusedLabelColor = Color(0xFF1E88E5),
                                    unfocusedLabelColor = Color(0xFF90A4AE)
                                )
                            )

                            Spacer(modifier = Modifier.height(10.dp))

                            // Custom command execution button
                            Button(
                                onClick = {
                                    if (customCommand.isBlank()) {
                                        Toast.makeText(context, "Perintah kustom kosong!", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!isRunning) {
                                        Toast.makeText(context, "Shizuku service is not running", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    if (!hasPermission) {
                                        Toast.makeText(context, "Permission is not authorized", Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    isExecuting = true
                                    coroutineScope.launch {
                                        val output = withContext(Dispatchers.IO) {
                                            try {
                                                val method = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                                                val process = method.invoke(null, arrayOf("sh", "-c", customCommand), null, null) as java.lang.Process
                                                val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                                                val errorReader = java.io.BufferedReader(java.io.InputStreamReader(process.errorStream))
                                                val sb = java.lang.StringBuilder()
                                                var line: String?
                                                while (reader.readLine().also { line = it } != null) {
                                                    sb.append(line).append("\n")
                                                }
                                                while (errorReader.readLine().also { line = it } != null) {
                                                    sb.append("[Error] ").append(line).append("\n")
                                                }
                                                process.waitFor()
                                                sb.toString().ifBlank { "[Selesai] Perintah berhasil dijalankan tanpa output." }
                                            } catch (e: Exception) {
                                                "Gagal eksekusi: ${e.message}"
                                            }
                                        }
                                        terminalOutput = output
                                        val isSuccess = !output.contains("Gagal eksekusi:") && !output.contains("[Error]")
                                        viewModel.insertDiagnosticLog(customCommand, output, isSuccess)
                                        isExecuting = false
                                    }
                                },
                                enabled = !isExecuting && customCommand.isNotBlank(),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4CAF50)),
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                if (isExecuting) {
                                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Jalankan Perintah Kustom", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                            Divider(color = Color(0xFF233545), thickness = 1.dp)
                            Spacer(modifier = Modifier.height(12.dp))

                            Text(
                                text = "Pintasan Diagnostik Cepat (Shortcuts)",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF90CAF9),
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            // Command row triggers
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Button(
                                    onClick = {
                                        if (!isRunning) {
                                            Toast.makeText(context, "Shizuku service is not running", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!hasPermission) {
                                            Toast.makeText(context, "Permission is not authorized", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isExecuting = true
                                        coroutineScope.launch {
                                            val output = withContext(Dispatchers.IO) {
                                                try {
                                                    val method = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                                                    val process = method.invoke(null, arrayOf("sh", "-c", "id; whoami; getprop ro.build.version.release"), null, null) as java.lang.Process
                                                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                                                    val sb = java.lang.StringBuilder()
                                                    var line: String?
                                                    while (reader.readLine().also { line = it } != null) {
                                                        sb.append(line).append("\n")
                                                    }
                                                    process.waitFor()
                                                    sb.toString().ifBlank { "Executed successfully with no response." }
                                                } catch (e: Exception) {
                                                    "Execution error: ${e.message}"
                                                }
                                            }
                                            terminalOutput = output
                                            val isSuccess = !output.contains("Execution error:")
                                            viewModel.insertDiagnosticLog("id; whoami; getprop ro.build.version.release", output, isSuccess)
                                            isExecuting = false
                                        }
                                    },
                                    enabled = !isExecuting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0D47A1)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Terminal, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("ADB Ping", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (!isRunning) {
                                            Toast.makeText(context, "Shizuku service is not running", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!hasPermission) {
                                            Toast.makeText(context, "Permission is not authorized", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isExecuting = true
                                        coroutineScope.launch {
                                            val output = withContext(Dispatchers.IO) {
                                                try {
                                                    val method = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                                                    val process = method.invoke(null, arrayOf("sh", "-c", "ip route show; ip addr"), null, null) as java.lang.Process
                                                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                                                    val sb = java.lang.StringBuilder()
                                                    var line: String?
                                                    while (reader.readLine().also { line = it } != null) {
                                                        sb.append(line).append("\n")
                                                    }
                                                    process.waitFor()
                                                    sb.toString().ifBlank { "Executed successfully with no response." }
                                                } catch (e: Exception) {
                                                    "Execution error: ${e.message}"
                                                }
                                            }
                                            terminalOutput = output
                                            val isSuccess = !output.contains("Execution error:")
                                            viewModel.insertDiagnosticLog("ip route show; ip addr", output, isSuccess)
                                            isExecuting = false
                                        }
                                    },
                                    enabled = !isExecuting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF263238)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.NetworkWifi, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Routes", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Button(
                                    onClick = {
                                        if (!isRunning) {
                                            Toast.makeText(context, "Shizuku service is not running", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        if (!hasPermission) {
                                            Toast.makeText(context, "Permission is not authorized", Toast.LENGTH_SHORT).show()
                                            return@Button
                                        }
                                        isExecuting = true
                                        coroutineScope.launch {
                                            val output = withContext(Dispatchers.IO) {
                                                try {
                                                    val method = Shizuku::class.java.getMethod("newProcess", Array<String>::class.java, Array<String>::class.java, String::class.java)
                                                    val process = method.invoke(null, arrayOf("sh", "-c", "iptables -t nat -L -v -n"), null, null) as java.lang.Process
                                                    val reader = java.io.BufferedReader(java.io.InputStreamReader(process.inputStream))
                                                    val sb = java.lang.StringBuilder()
                                                    var line: String?
                                                    while (reader.readLine().also { line = it } != null) {
                                                        sb.append(line).append("\n")
                                                    }
                                                    process.waitFor()
                                                    sb.toString().ifBlank { "No iptables nat rules active or permission error" }
                                                } catch (e: Exception) {
                                                    "Execution error: ${e.message}"
                                                }
                                            }
                                            terminalOutput = output
                                            val isSuccess = !output.contains("Execution error:")
                                            viewModel.insertDiagnosticLog("iptables -t nat -L -v -n", output, isSuccess)
                                            isExecuting = false
                                        }
                                    },
                                    enabled = !isExecuting,
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD84315)),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.weight(1.1f),
                                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                                ) {
                                    if (isExecuting) {
                                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    } else {
                                        Icon(Icons.Default.Security, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("IPTable NAT", fontSize = 11.sp, maxLines = 1)
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Terminal display block
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(min = 120.dp, max = 240.dp)
                                    .background(Color(0xFF0A1015), RoundedCornerShape(8.dp))
                                    .padding(12.dp)
                            ) {
                                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                                    item {
                                        Text(
                                            text = terminalOutput,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = Color(0xFF00E676),
                                            lineHeight = 16.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Riwayat Diagnostik (Diagnostic History Card)
                item {
                    val logs by viewModel.diagnosticLogs.collectAsStateWithLifecycle()
                    val clipboardManager = LocalClipboardManager.current

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF15222E)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.History,
                                        contentDescription = null,
                                        tint = Color(0xFF90CAF9),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Riwayat Diagnostik (${logs.size})",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White
                                    )
                                }
                                if (logs.isNotEmpty()) {
                                    IconButton(
                                        onClick = { viewModel.clearAllDiagnosticLogs() }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.DeleteForever,
                                            contentDescription = "Hapus Semua Riwayat",
                                            tint = Color(0xFFEF5350)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = "Penyimpanan log eksekusi lokal yang persisten. Klik pada log untuk memperluas.",
                                fontSize = 12.sp,
                                color = Color(0xFF90A4AE),
                                modifier = Modifier.padding(bottom = 12.dp)
                            )

                            if (logs.isEmpty()) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .background(Color(0xFF0A1015), RoundedCornerShape(8.dp))
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "Belum ada riwayat diagnostik tersimpan.",
                                        fontSize = 13.sp,
                                        color = Color(0xFF607D8B)
                                    )
                                }
                            } else {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    logs.forEach { log ->
                                        var isExpanded by remember(log.id) { mutableStateOf(false) }
                                        val formattedDate = remember(log.timestamp) {
                                            try {
                                                val sdf = SimpleDateFormat("HH:mm:ss - dd MMM yyyy", Locale.getDefault())
                                                sdf.format(Date(log.timestamp))
                                            } catch (e: Exception) {
                                                "Just now"
                                            }
                                        }

                                        Card(
                                            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A1015)),
                                            shape = RoundedCornerShape(8.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { isExpanded = !isExpanded }
                                        ) {
                                            Column(modifier = Modifier.padding(12.dp)) {
                                                Row(
                                                    modifier = Modifier.fillMaxWidth(),
                                                    horizontalArrangement = Arrangement.SpaceBetween,
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Row(
                                                        verticalAlignment = Alignment.CenterVertically,
                                                        modifier = Modifier.weight(1f)
                                                    ) {
                                                        Icon(
                                                            imageVector = if (log.isSuccess) Icons.Default.CheckCircle else Icons.Default.Error,
                                                            contentDescription = null,
                                                            tint = if (log.isSuccess) Color(0xFF4CAF50) else Color(0xFFEF5350),
                                                            modifier = Modifier.size(16.dp)
                                                        )
                                                        Spacer(modifier = Modifier.width(8.dp))
                                                        Text(
                                                            text = log.command,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontSize = 12.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFF90CAF9),
                                                            maxLines = 1,
                                                            modifier = Modifier.weight(1f)
                                                        )
                                                    }
                                                    Text(
                                                        text = formattedDate,
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF78909C),
                                                        modifier = Modifier.padding(start = 8.dp)
                                                    )
                                                }

                                                AnimatedVisibility(
                                                    visible = isExpanded,
                                                    enter = expandVertically() + fadeIn(),
                                                    exit = shrinkVertically() + fadeOut()
                                                ) {
                                                    Column(
                                                        modifier = Modifier
                                                            .fillMaxWidth()
                                                            .padding(top = 10.dp)
                                                    ) {
                                                        Divider(color = Color(0xFF233545), thickness = 1.dp)
                                                        Spacer(modifier = Modifier.height(8.dp))

                                                        Text(
                                                            text = "Detail Output Log:",
                                                            fontWeight = FontWeight.Bold,
                                                            fontSize = 11.sp,
                                                            color = Color(0xFF90A4AE)
                                                        )
                                                        Spacer(modifier = Modifier.height(4.dp))

                                                        Box(
                                                            modifier = Modifier
                                                                .fillMaxWidth()
                                                                .background(Color(0xFF070B0E), RoundedCornerShape(6.dp))
                                                                .padding(8.dp)
                                                        ) {
                                                            Text(
                                                                text = log.output,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 11.sp,
                                                                color = Color(0xFF00E676),
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }

                                                        Spacer(modifier = Modifier.height(10.dp))

                                                        Row(
                                                            modifier = Modifier.fillMaxWidth(),
                                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                            verticalAlignment = Alignment.CenterVertically
                                                        ) {
                                                            Button(
                                                                onClick = {
                                                                    clipboardManager?.setText(AnnotatedString(log.output))
                                                                    Toast.makeText(context, "Output berhasil dicopy ke clipboard", Toast.LENGTH_SHORT).show()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E88E5)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Salin", fontSize = 11.sp)
                                                            }

                                                            Button(
                                                                onClick = {
                                                                    customCommand = log.command
                                                                    terminalOutput = log.output
                                                                    Toast.makeText(context, "Command dimuat ke terminal input", Toast.LENGTH_SHORT).show()
                                                                },
                                                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F)),
                                                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                                                                shape = RoundedCornerShape(6.dp),
                                                                modifier = Modifier.height(32.dp)
                                                            ) {
                                                                Icon(Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(12.dp))
                                                                Spacer(modifier = Modifier.width(4.dp))
                                                                Text("Muat Perintah", fontSize = 11.sp)
                                                            }

                                                            Spacer(modifier = Modifier.weight(1f))

                                                            IconButton(
                                                                onClick = { viewModel.deleteDiagnosticLog(log.id) },
                                                                modifier = Modifier.size(32.dp)
                                                            ) {
                                                                Icon(
                                                                    imageVector = Icons.Default.Delete,
                                                                    contentDescription = "Hapus Log ini",
                                                                    tint = Color(0xFFEF5350),
                                                                    modifier = Modifier.size(16.dp)
                                                                )
                                                            }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(30.dp)) }
            }
        }
    }
}
