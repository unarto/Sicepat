package com.example.ui

import com.example.viewmodel.AppViewModel
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HysteriaScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var profileName by remember { mutableStateOf(viewModel.hysteriaProfileNameText) }
    var serverIp by remember { mutableStateOf(viewModel.hysteriaServerIpText) }
    var portRange by remember { mutableStateOf(viewModel.hysteriaPortRangeText) }
    var mtu by remember { mutableStateOf(viewModel.hysteriaMtuText) }
    var password by remember { mutableStateOf(viewModel.hysteriaPasswordText) }
    var obfs by remember { mutableStateOf(viewModel.hysteriaObfsText) }
    var bufferSize by remember { mutableStateOf(viewModel.hysteriaBufferSizeText) }
    var showBufferSheet by remember { mutableStateOf(false) }
    var socksAddress by remember { mutableStateOf(viewModel.hysteriaSocksAddressText) }
    var socksPort by remember { mutableStateOf(viewModel.hysteriaSocksPortText) }
    var socksUdp by remember { mutableStateOf(viewModel.hysteriaSocksUdpVal) }
    var autoStartOnBoot by remember { mutableStateOf(viewModel.hysteriaAutoStartOnBootVal) }

    val bufferOptions = listOf("0.5x (Low Buffer)", "1.0x (Default)", "1.5x (High Speed)", "2.0x (Max Speed)", "3.0x (Extreme)")

    if (showBufferSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBufferSheet = false },
            containerColor = Color(0xFF1E2228)
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)) {
                bufferOptions.forEach { option ->
                    val isSelected = option == bufferSize
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                bufferSize = option
                                showBufferSheet = false
                            }
                            .background(if (isSelected) Color(0xFF2E3238) else Color.Transparent)
                            .padding(horizontal = 24.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = option,
                            fontSize = 16.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    val context = LocalContext.current
    var showConfigDialog by remember { mutableStateOf(false) }
    var dialogTabSelected by remember { mutableStateOf("json") } // "json" or "clash"

    if (showConfigDialog) {
        val jsonConfig = """
        {
          "server": "${serverIp.ifBlank { "example.com" }}:${portRange.ifBlank { "6000-19999" }}",
          "auth": "$password",
          "obfs": {
            "type": "custom",
            "custom": "$obfs"
          },
          "socks5": {
            "listen": "$socksAddress:$socksPort"
          },
          "transport": {
            "udp": {
              "hop": $socksUdp
            }
          },
          "bandwidth": {
            "up": "100 mbps",
            "down": "100 mbps"
          },
          "mtu": ${mtu.ifBlank { "9000" }}
        }
        """.trimIndent()

        val yamlConfig = """
        proxies:
          - name: "${profileName.ifBlank { "ZIVPN-Core" }}"
            type: hysteria2
            server: ${serverIp.ifBlank { "example.com" }}
            port: ${portRange.substringBefore("-").ifBlank { "443" }}
            ports: $portRange
            password: "$password"
            obfs: custom
            obfs-password: "$obfs"
            up: "100"
            down: "100"
            socks5-server: $socksAddress
            socks5-port: $socksPort
            udp: $socksUdp
        """.trimIndent()

        AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = {
                Text(
                    text = "Generated Configuration",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Tabs
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF23252E), RoundedCornerShape(8.dp))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dialogTabSelected == "json") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { dialogTabSelected = "json" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Hysteria JSON",
                                color = if (dialogTabSelected == "json") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(6.dp))
                                .background(if (dialogTabSelected == "clash") MaterialTheme.colorScheme.primary else Color.Transparent)
                                .clickable { dialogTabSelected = "clash" }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Clash YAML",
                                color = if (dialogTabSelected == "clash") Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    // Text display block
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .background(Color(0xFF14171E), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        val currentText = if (dialogTabSelected == "json") jsonConfig else yamlConfig
                        LazyColumn(modifier = Modifier.fillMaxWidth()) {
                            item {
                                Text(
                                    text = currentText,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = Color(0xFF81D4FA)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Export File
                    Button(
                        onClick = {
                            try {
                                val configDir = java.io.File(context.filesDir, "config")
                                if (!configDir.exists()) {
                                    configDir.mkdirs()
                                }
                                val outFilename = if (dialogTabSelected == "json") "hysteria_client.json" else "clash_hysteria.yaml"
                                val outFile = java.io.File(configDir, outFilename)
                                val currentText = if (dialogTabSelected == "json") jsonConfig else yamlConfig
                                outFile.writeText(currentText)
                                Toast.makeText(context, "Config saved to config/${outFilename}", Toast.LENGTH_LONG).show()
                            } catch (e: Exception) {
                                Toast.makeText(context, "Gagal menulis file: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C3E50)),
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Export File", fontSize = 11.sp, maxLines = 1)
                    }

                    // Copy Config
                    Button(
                        onClick = {
                            val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clipContents = if (dialogTabSelected == "json") jsonConfig else yamlConfig
                            val clip = android.content.ClipData.newPlainText("Hysteria Config", clipContents)
                            clipboard.setPrimaryClip(clip)
                            Toast.makeText(context, "Config copied to clipboard!", Toast.LENGTH_SHORT).show()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(imageVector = Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Copy", fontSize = 11.sp, maxLines = 1)
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigDialog = false }) {
                    Text("Close", color = MaterialTheme.colorScheme.outline)
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(16.dp)
        )
    }

    Column(modifier = Modifier.fillMaxSize()) {
        ApplicationHeader(
            title = "New Hysteria Profile", 
            onBack = onBack,
            actions = {
                IconButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val jsonConfig = """
                    {
                      "server": "${serverIp.ifBlank { "example.com" }}:${portRange.ifBlank { "6000-19999" }}",
                      "auth": "$password",
                      "obfs": {
                        "custom": "$obfs"
                      },
                      "socks5": {
                        "listen": "$socksAddress:$socksPort"
                      },
                      "mtu": ${mtu.ifBlank { "9000" }}
                    }
                    """.trimIndent()
                    val clip = android.content.ClipData.newPlainText("Hysteria Profile JSON", jsonConfig)
                    clipboard.setPrimaryClip(clip)
                    Toast.makeText(context, "Profil disalin ke clipboard!", Toast.LENGTH_SHORT).show()
                }) {
                    Icon(imageVector = Icons.Default.ContentCopy, contentDescription = "Copy Profile", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = {
                    val clipboard = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                    val clipData = clipboard.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text.toString()
                        try {
                            val json = org.json.JSONObject(text)
                            if (json.has("server")) {
                                val s = json.getString("server")
                                serverIp = s.substringBefore(":")
                                portRange = s.substringAfter(":", "6000-19999")
                            }
                            if (json.has("auth")) {
                                password = json.getString("auth")
                            }
                            if (json.has("obfs")) {
                                val ob = json.getJSONObject("obfs")
                                if (ob.has("custom")) obfs = ob.getString("custom")
                            }
                            if (json.has("socks5")) {
                                val so = json.getJSONObject("socks5")
                                if (so.has("listen")) {
                                    val l = so.getString("listen")
                                    socksAddress = l.substringBefore(":")
                                    socksPort = l.substringAfter(":", "7777")
                                }
                            }
                            if (json.has("mtu")) {
                                mtu = json.get("mtu").toString()
                            }
                            Toast.makeText(context, "Profil berhasil di-import!", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Format clipboard tidak valid!", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        Toast.makeText(context, "Clipboard kosong!", Toast.LENGTH_SHORT).show()
                    }
                }) {
                    Icon(imageVector = Icons.Default.ContentPaste, contentDescription = "Paste Profile", tint = MaterialTheme.colorScheme.onBackground)
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = { profileName = it },
                    label = { Text("Profile Name") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = serverIp,
                    onValueChange = { serverIp = it },
                    label = { Text("Server IP / Domain") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = portRange,
                    onValueChange = { portRange = it },
                    label = { Text("Port Range") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = mtu,
                    onValueChange = { mtu = it },
                    label = { Text("MTU") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = obfs,
                    onValueChange = { obfs = it },
                    label = { Text("Obfs") },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Custom dropdown mimic standard
            item {
                Box(modifier = Modifier.fillMaxWidth().clickable { showBufferSheet = true }) {
                    OutlinedTextField(
                        value = bufferSize,
                        onValueChange = {},
                        readOnly = true,
                        enabled = false,
                        label = { Text("Receive Window (Buffer Size)") },
                        trailingIcon = { Icon(Icons.Default.ArrowDropDown, contentDescription = null) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            disabledTextColor = MaterialTheme.colorScheme.onSurface,
                            disabledBorderColor = MaterialTheme.colorScheme.outline,
                            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            disabledTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Text(
                    text = "Default: 1.0x. Increase for high speed, decrease for ...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp, start = 4.dp)
                )
            }

            item {
                Text(
                    text = "Socks5 Integration (ZIVPN/Xray Core)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
            }
            item {
                OutlinedTextField(
                    value = socksAddress,
                    onValueChange = { socksAddress = it },
                    label = { Text("Socks5 Server IP") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                OutlinedTextField(
                    value = socksPort,
                    onValueChange = { socksPort = it },
                    label = { Text("Socks5 Port") },
                    modifier = Modifier.fillMaxWidth()
                )
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Socks5 UDP", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = socksUdp,
                        onCheckedChange = { socksUdp = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = Color(0xFF2E3238),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            // Auto-start on boot
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Auto-Start on Boot", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface)
                    Switch(
                        checked = autoStartOnBoot,
                        onCheckedChange = { autoStartOnBoot = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = MaterialTheme.colorScheme.primary,
                            uncheckedThumbColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            uncheckedTrackColor = Color(0xFF2E3238),
                            checkedBorderColor = Color.Transparent,
                            uncheckedBorderColor = Color.Transparent
                        )
                    )
                }
            }

            item {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            }

            // Texts at the bottom part as seen in the photo
            item {
                Text(
                    text = "via Shizuku.",
                    fontSize = 12.sp,
                    color = Color(0xFF03A9F4),
                    modifier = Modifier.padding(start = 4.dp, top = 8.dp)
                )
            }

            // Bottom large centered buttons
            item {
                Spacer(modifier = Modifier.height(16.dp))
                // Generate Config File Button
                Button(
                    onClick = { showConfigDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1B385C)),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = null, tint = Color(0xFF80DEEA), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Generate Config File", color = Color(0xFF80DEEA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Profile Button
                Button(
                    onClick = {
                        viewModel.hysteriaProfileNameText = profileName
                        viewModel.hysteriaServerIpText = serverIp
                        viewModel.hysteriaPortRangeText = portRange
                        viewModel.hysteriaMtuText = mtu
                        viewModel.hysteriaPasswordText = password
                        viewModel.hysteriaObfsText = obfs
                        viewModel.hysteriaBufferSizeText = bufferSize
                        viewModel.hysteriaSocksAddressText = socksAddress
                        viewModel.hysteriaSocksPortText = socksPort
                        viewModel.hysteriaSocksUdpVal = socksUdp
                        viewModel.hysteriaAutoStartOnBootVal = autoStartOnBoot
                        
                        viewModel.saveHysteriaConfig(context)
                        Toast.makeText(context, "Profil Hysteria Berhasil Disimpan", Toast.LENGTH_SHORT).show()
                        onBack()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF152A3E)),
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Icon(imageVector = Icons.Default.Save, contentDescription = null, tint = Color(0xFF81D4FA), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Save & Create Profile", color = Color(0xFF81D4FA), fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}
