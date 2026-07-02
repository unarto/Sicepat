package com.example.ui

import com.example.viewmodel.AppViewModel
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.util.PrefManager
import com.example.enums.LanguageOption

@Composable
fun ToolsScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    var activeSubScreen by remember { mutableStateOf<String?>(null) }
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showDisclaimerDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(PrefManager.getLanguage(context).displayName) }

    when (activeSubScreen) {
        "Requests" -> RequestsScreen(onBack = { activeSubScreen = null })
        "Connections" -> ConnectionsScreen(onBack = { activeSubScreen = null })
        "Resources" -> ResourcesScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "hysteria" -> HysteriaScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "Theme" -> ThemeScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "Backup and Recovery" -> BackupAndRecoveryScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "AccessControl" -> AppAccessControlScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "Routing" -> RoutingSettingsScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "Logcat" -> LogcatScreen(onBack = { activeSubScreen = null })
        "Shizuku" -> ShizukuIntegrationScreen(viewModel = viewModel, onBack = { activeSubScreen = null })
        "NetworkMiner" -> NetworkMinerScreen(onBack = { activeSubScreen = null })
        else -> {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Tools",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize()
                ) {
                    item { ToolSectionHeader("More") }
                    item { ToolListItem(Icons.Default.ImportExport, "Requests", "View recently request records") { activeSubScreen = "Requests" } }
                    item { ToolListItem(Icons.Default.WifiTethering, "Connections", "View current connections data") { activeSubScreen = "Connections" } }
                    item { ToolListItem(Icons.Default.Folder, "Resources", "External resource related info") { activeSubScreen = "Resources" } }
                    item { ToolListItem(Icons.Default.RocketLaunch, "hysteria", null) { activeSubScreen = "hysteria" } }
                    item { ToolListItem(Icons.Default.Terminal, "Shizuku Integration", "Check status, request permission and execute elevated shell actions") { activeSubScreen = "Shizuku" } }
                    item { ToolListItem(Icons.Default.TextSnippet, "Logcat", "View historical running dynamic events") { activeSubScreen = "Logcat" } }
                    item { ToolListItem(Icons.Default.NetworkPing, "Network Miner", "Analisis PCAP, Deteksi Ancaman Keamanan & Audit Kepatuhan") { activeSubScreen = "NetworkMiner" } }
                    
                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp)) }
                    
                    item { ToolSectionHeader("Settings") }
                    item { ToolListItem(Icons.Default.Language, "Language", currentLanguage) { showLanguageDialog = true } }
                    item { ToolListItem(Icons.Default.Palette, "Theme", "Set dark mode,adjust the color") { activeSubScreen = "Theme" } }
                    item { ToolListItem(Icons.Default.SettingsBackupRestore, "Backup and Recovery", "Sync data via WebDAV or file") { activeSubScreen = "Backup and Recovery" } }
                    item { ToolListItem(Icons.Default.VpnKey, "AccessControl", "Configure application access proxy") { activeSubScreen = "AccessControl" } }
                    item { ToolListItem(Icons.Default.Route, "Routing settings", "Configure rule-based routing settings") { activeSubScreen = "Routing" } }
                    item { AdvancedConfigurationContent() }

                    item { HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f), modifier = Modifier.padding(vertical = 8.dp)) }
                    
                    item { ToolSectionHeader("Other") }
                    item { ToolListItem(Icons.Default.Gavel, "Disclaimer", null) { showDisclaimerDialog = true } }
                    item { ToolListItem(Icons.Default.Info, "About", null) { showAboutDialog = true } }
                    
                    item { Spacer(modifier = Modifier.height(32.dp)) }
                }
            }

            // Language Selection dialog
            if (showLanguageDialog) {
                AlertDialog(
                    onDismissRequest = { showLanguageDialog = false },
                    title = {
                        Text(
                            text = "Pilih Bahasa / Select Language",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                "Bahasa Indonesia",
                                "English (US)",
                                "System Default"
                            ).forEach { lang ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            currentLanguage = lang
                                            PrefManager.setLanguage(context, LanguageOption.fromDisplayName(lang))
                                            showLanguageDialog = false
                                            Toast.makeText(context, "Bahasa diubah ke: $lang", Toast.LENGTH_SHORT).show()
                                        }
                                        .padding(vertical = 12.dp, horizontal = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    RadioButton(
                                        selected = (currentLanguage == lang),
                                        onClick = {
                                            currentLanguage = lang
                                            PrefManager.setLanguage(context, LanguageOption.fromDisplayName(lang))
                                            showLanguageDialog = false
                                            Toast.makeText(context, "Bahasa diubah ke: $lang", Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = lang,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 15.sp
                                    )
                                }
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { showLanguageDialog = false }) {
                            Text("Batal", color = MaterialTheme.colorScheme.primary)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFF15222E)
                )
            }

            // Disclaimer dialog
            if (showDisclaimerDialog) {
                AlertDialog(
                    onDismissRequest = { showDisclaimerDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Gavel,
                                contentDescription = null,
                                tint = Color(0xFFFFD54F),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Syarat & Ketentuan",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = "Aplikasi SiCepat VPN adalah alat uji jaringan berbasis open-source (Xray/V2Ray Core).",
                                fontSize = 13.sp,
                                color = Color(0xFFECEFF1)
                            )
                            Text(
                                text = "1. Tidak menyediakan server bawaan (user harus memasukkan konfigurasi secara mandiri).",
                                fontSize = 12.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = "2. Penyalahgunaan aplikasi untuk aktivitas ilegal atau melanggar hukum setempat sepenuhnya merupakan tanggung jawab pengguna masing-masing.",
                                fontSize = 12.sp,
                                color = Color(0xFFB0BEC5)
                            )
                            Text(
                                text = "3. Kami tidak bertanggung jawab atas kegagalan koneksi atau kerugian data karena konfigurasi pihak ketiga.",
                                fontSize = 12.sp,
                                color = Color(0xFFB0BEC5)
                            )
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showDisclaimerDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Saya Setuju & Mengerti", color = Color.White, fontWeight = FontWeight.Bold)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFF15222E)
                )
            }

            // About dialog
            if (showAboutDialog) {
                AlertDialog(
                    onDismissRequest = { showAboutDialog = false },
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF90CAF9),
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Tentang SiCepat",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    },
                    text = {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E2F3F)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.size(72.dp)
                            ) {
                                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Security,
                                        contentDescription = null,
                                        tint = Color(0xFF00E676),
                                        modifier = Modifier.size(40.dp)
                                    )
                                }
                            }
                            
                            Text(
                                text = "SiCepat VPN Client",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            
                            Text(
                                text = "Versi 2.5.0-Indigo (Release)",
                                fontSize = 12.sp,
                                color = Color(0xFF00E676),
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Klien VPN handal untuk protokol vmess, vless, trojan, hysteria, dan shadowsocks dengan kecepatan tinggi.",
                                fontSize = 12.sp,
                                color = Color(0xFFB0BEC5),
                                lineHeight = 16.sp,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )

                            HorizontalDivider(color = Color(0xFF233545), thickness = 1.dp)

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                  Text("Inti Mesin:", fontSize = 11.sp, color = Color(0xFF78909C))
                                  Text("Xray-Core v1.8.4", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                  Text("Lisensi:", fontSize = 11.sp, color = Color(0xFF78909C))
                                  Text("GPL-3.0 License", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    },
                    confirmButton = {
                        Button(
                            onClick = { showAboutDialog = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF37474F))
                        ) {
                            Text("Tutup", color = Color.White)
                        }
                    },
                    shape = RoundedCornerShape(16.dp),
                    containerColor = Color(0xFF15222E)
                )
            }
        }
    }
}

@Composable
fun ToolSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
    )
}

@Composable
fun ToolListItem(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplicationHeader(
    title: String,
    onBack: () -> Unit,
    actions: @Composable (RowScope.() -> Unit)? = null
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
        },
        actions = actions ?: {},
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
fun AdvancedConfigurationContent() {
    val context = LocalContext.current
    val sharedPrefs = context.getSharedPreferences("sicepat_advanced_settings", Context.MODE_PRIVATE)

    // Speed Test & Connection Info Settings
    var trueDelayUrl by remember { mutableStateOf(sharedPrefs.getString("trueDelayUrl", "https://www.gstatic.com/generate_204") ?: "https://www.gstatic.com/generate_204") }
    var connInfoUrl by remember { mutableStateOf(sharedPrefs.getString("connInfoUrl", "https://api.ip.sb/geoip") ?: "https://api.ip.sb/geoip") }

    // VPN Settings
    var preferIpv6 by remember { mutableStateOf(sharedPrefs.getBoolean("preferIpv6", false)) }
    var enableLocalDns by remember { mutableStateOf(sharedPrefs.getBoolean("enableLocalDns", true)) } // default to true based on user config
    var enableFakeDns by remember { mutableStateOf(sharedPrefs.getBoolean("enableFakeDns", false)) }
    var appendHttpProxy by remember { mutableStateOf(sharedPrefs.getBoolean("appendHttpProxy", false)) }
    var vpnDns by remember { mutableStateOf(sharedPrefs.getString("vpnDns", "94.140.14.14") ?: "94.140.14.14") }
    var vpnBypassLan by remember { mutableStateOf(sharedPrefs.getString("vpnBypassLan", "Bypass") ?: "Bypass") }
    var vpnInterfaceAddr by remember { mutableStateOf(sharedPrefs.getString("vpnInterfaceAddr", "10.10.14.x") ?: "10.10.14.x") }
    var vpnMtu by remember { mutableStateOf(sharedPrefs.getString("vpnMtu", "1500") ?: "1500") }
    var enableHevTun by remember { mutableStateOf(sharedPrefs.getBoolean("enableHevTun", true)) }
    var hevTunLogLevel by remember { mutableStateOf(sharedPrefs.getString("hevTunLogLevel", "info") ?: "info") }
    var tcpUdpTimeout by remember { mutableStateOf(sharedPrefs.getString("tcpUdpTimeout", "300,60") ?: "300,60") }

    // Core Settings
    var enableSniffing by remember { mutableStateOf(sharedPrefs.getBoolean("enableSniffing", true)) }
    var enableRouteOnly by remember { mutableStateOf(sharedPrefs.getBoolean("enableRouteOnly", false)) }
    var allowConnectionsLan by remember { mutableStateOf(sharedPrefs.getBoolean("allowConnectionsLan", false)) }
    var allowInsecure by remember { mutableStateOf(sharedPrefs.getBoolean("allowInsecure", true)) }
    var localProxyPort by remember { mutableStateOf(sharedPrefs.getString("localProxyPort", "10808") ?: "10808") }
    var remoteDns by remember { mutableStateOf(sharedPrefs.getString("remoteDns", "https://dns.adguard-dns.com/dns-query") ?: "https://dns.adguard-dns.com/dns-query") }
    var domesticDns by remember { mutableStateOf(sharedPrefs.getString("domesticDns", "94.140.15.15") ?: "94.140.15.15") }
    var dnsHosts by remember { mutableStateOf(sharedPrefs.getString("dnsHosts", "dns.adguard-dns.com:94.140.14.14") ?: "dns.adguard-dns.com:94.140.14.14") }
    var logLevel by remember { mutableStateOf(sharedPrefs.getString("logLevel", "debug") ?: "debug") }
    var outboundDomainPreResolve by remember { mutableStateOf(sharedPrefs.getString("outboundDomainPreResolve", "Resolve and add to DNS Hosts") ?: "Resolve and add to DNS Hosts") }

    // Mux Settings
    var enableMux by remember { mutableStateOf(sharedPrefs.getBoolean("enableMux", false)) }
    var tcpConnections by remember { mutableStateOf(sharedPrefs.getString("tcpConnections", "8") ?: "8") }
    var xudpConnections by remember { mutableStateOf(sharedPrefs.getString("xudpConnections", "8") ?: "8") }
    var handlingQuicMux by remember { mutableStateOf(sharedPrefs.getString("handlingQuicMux", "reject") ?: "reject") }

    // Fragment Settings
    var enableFragment by remember { mutableStateOf(sharedPrefs.getBoolean("enableFragment", false)) }
    var fragmentLength by remember { mutableStateOf(sharedPrefs.getString("fragmentLength", "50-100") ?: "50-100") }
    var fragmentInterval by remember { mutableStateOf(sharedPrefs.getString("fragmentInterval", "10-20") ?: "10-20") }
    var fragmentPackets by remember { mutableStateOf(sharedPrefs.getString("fragmentPackets", "tlshello") ?: "tlshello") }

    // Dialog state
    var activeTextFieldKey by remember { mutableStateOf<String?>(null) }
    var activeTextFieldTitle by remember { mutableStateOf("") }
    var activeTextFieldValue by remember { mutableStateOf("") }

    // Choice Dialog state
    var activeChoiceFieldKey by remember { mutableStateOf<String?>(null) }
    var activeChoiceFieldTitle by remember { mutableStateOf("") }
    var activeChoiceOptions by remember { mutableStateOf<List<String>>(emptyList()) }
    var activeChoiceValue by remember { mutableStateOf("") }

    // Text field Edit Dialog
    if (activeTextFieldKey != null) {
        AlertDialog(
            onDismissRequest = { activeTextFieldKey = null },
            title = {
                Text(
                    text = activeTextFieldTitle,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                OutlinedTextField(
                    value = activeTextFieldValue,
                    onValueChange = { activeTextFieldValue = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = Color.LightGray
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        val trimmedVal = activeTextFieldValue.trim()
                        when (activeTextFieldKey) {
                            "trueDelayUrl" -> trueDelayUrl = trimmedVal
                            "connInfoUrl" -> connInfoUrl = trimmedVal
                            "vpnDns" -> vpnDns = trimmedVal
                            "vpnMtu" -> vpnMtu = trimmedVal
                            "tcpUdpTimeout" -> tcpUdpTimeout = trimmedVal
                            "localProxyPort" -> localProxyPort = trimmedVal
                            "remoteDns" -> remoteDns = trimmedVal
                            "domesticDns" -> domesticDns = trimmedVal
                            "dnsHosts" -> dnsHosts = trimmedVal
                            "outboundDomainPreResolve" -> outboundDomainPreResolve = trimmedVal
                            "tcpConnections" -> tcpConnections = trimmedVal
                            "xudpConnections" -> xudpConnections = trimmedVal
                            "fragmentLength" -> fragmentLength = trimmedVal
                            "fragmentInterval" -> fragmentInterval = trimmedVal
                        }
                        activeTextFieldKey?.let { key ->
                            sharedPrefs.edit().putString(key, trimmedVal).apply()
                        }
                        Toast.makeText(context, "Setting diperbarui", Toast.LENGTH_SHORT).show()
                        activeTextFieldKey = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.onPrimary)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { activeTextFieldKey = null }
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary)
                }
            }
        )
    }

    // Radio Selection Dialog
    if (activeChoiceFieldKey != null) {
        AlertDialog(
            onDismissRequest = { activeChoiceFieldKey = null },
            title = {
                Text(
                    text = activeChoiceFieldTitle,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    activeChoiceOptions.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeChoiceValue = option
                                    when (activeChoiceFieldKey) {
                                        "hevTunLogLevel" -> hevTunLogLevel = option
                                        "logLevel" -> logLevel = option
                                        "vpnBypassLan" -> vpnBypassLan = option
                                        "vpnInterfaceAddr" -> vpnInterfaceAddr = option
                                        "outboundDomainPreResolve" -> outboundDomainPreResolve = option
                                        "handlingQuicMux" -> handlingQuicMux = option
                                        "fragmentPackets" -> fragmentPackets = option
                                    }
                                    activeChoiceFieldKey?.let { key ->
                                        sharedPrefs.edit().putString(key, option).apply()
                                    }
                                    Toast.makeText(context, "$activeChoiceFieldTitle diperbarui", Toast.LENGTH_SHORT).show()
                                    activeChoiceFieldKey = null
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (activeChoiceValue == option),
                                onClick = {
                                    activeChoiceValue = option
                                    when (activeChoiceFieldKey) {
                                        "hevTunLogLevel" -> hevTunLogLevel = option
                                        "logLevel" -> logLevel = option
                                        "vpnBypassLan" -> vpnBypassLan = option
                                        "vpnInterfaceAddr" -> vpnInterfaceAddr = option
                                        "outboundDomainPreResolve" -> outboundDomainPreResolve = option
                                        "handlingQuicMux" -> handlingQuicMux = option
                                        "fragmentPackets" -> fragmentPackets = option
                                    }
                                    activeChoiceFieldKey?.let { key ->
                                        sharedPrefs.edit().putString(key, option).apply()
                                    }
                                    Toast.makeText(context, "$activeChoiceFieldTitle diperbarui", Toast.LENGTH_SHORT).show()
                                    activeChoiceFieldKey = null
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text(
                                text = option,
                                fontSize = 16.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(
                    onClick = { activeChoiceFieldKey = null }
                ) {
                    Text("Batal", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                }
            }
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        // ==================== SPEED TEST SETTINGS ====================
        SettingsSectionHeader("Speed Test & Info Settings")

        SettingsTextRow(
            title = "True delay test url",
            value = trueDelayUrl,
            onClick = {
                activeTextFieldKey = "trueDelayUrl"
                activeTextFieldTitle = "True delay test url"
                activeTextFieldValue = trueDelayUrl
            },
            icon = Icons.Default.Timer
        )

        SettingsTextRow(
            title = "Current connection info test url",
            value = connInfoUrl,
            onClick = {
                activeTextFieldKey = "connInfoUrl"
                activeTextFieldTitle = "Current connection info test url"
                activeTextFieldValue = connInfoUrl
            },
            icon = Icons.Default.PermDeviceInformation
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ==================== 1. VPN SETTINGS ====================
        SettingsSectionHeader("VPN Settings")

        SettingsCheckboxRow(
            title = "Prefer IPv6",
            subtitle = "Enable IPv6 routes and Prefer IPv6 addresses",
            checked = preferIpv6,
            onCheckedChange = {
                preferIpv6 = it
                sharedPrefs.edit().putBoolean("preferIpv6", it).apply()
            },
            icon = Icons.Default.Dns
        )

        SettingsCheckboxRow(
            title = "Enable local DNS",
            subtitle = "DNS processed by core's DNS module (Recommended if you need routing bypassing LAN and mainland addresses)",
            checked = enableLocalDns,
            onCheckedChange = {
                enableLocalDns = it
                sharedPrefs.edit().putBoolean("enableLocalDns", it).apply()
            },
            icon = Icons.Default.GpsFixed
        )

        SettingsCheckboxRow(
            title = "Enable fake DNS",
            subtitle = "Local DNS returns fake IP addresses (faster, but it may not work for some apps)",
            checked = enableFakeDns,
            onCheckedChange = {
                enableFakeDns = it
                sharedPrefs.edit().putBoolean("enableFakeDns", it).apply()
            },
            icon = Icons.Default.Public
        )

        SettingsCheckboxRow(
            title = "Append HTTP Proxy to VPN",
            subtitle = "HTTP proxy will be used directly from (browser/ some supported apps), without going through the virtual NIC device (Android 10+)",
            checked = appendHttpProxy,
            onCheckedChange = {
                appendHttpProxy = it
                sharedPrefs.edit().putBoolean("appendHttpProxy", it).apply()
            },
            icon = Icons.Default.Http
        )

        SettingsTextRow(
            title = "VPN DNS (only IPv4/v6)",
            value = vpnDns,
            onClick = {
                activeTextFieldKey = "vpnDns"
                activeTextFieldTitle = "VPN DNS (only IPv4/v6)"
                activeTextFieldValue = vpnDns
            },
            icon = Icons.Default.Domain
        )

        SettingsTextRow(
            title = "Does VPN bypass LAN",
            value = vpnBypassLan,
            onClick = {
                activeChoiceFieldKey = "vpnBypassLan"
                activeChoiceFieldTitle = "Does VPN bypass LAN"
                activeChoiceOptions = listOf("Follow config", "Bypass", "Not Bypass")
                activeChoiceValue = vpnBypassLan
            },
            icon = Icons.Default.Lan
        )

        SettingsTextRow(
            title = "VPN Interface Address",
            value = vpnInterfaceAddr,
            onClick = {
                activeChoiceFieldKey = "vpnInterfaceAddr"
                activeChoiceFieldTitle = "VPN Interface Address"
                activeChoiceOptions = listOf("10.10.14.x", "10.1.0.x", "10.0.0.x", "172.31.0.x", "172.20.0.x", "172.16.0.x", "192.168.100.x")
                activeChoiceValue = vpnInterfaceAddr
            },
            icon = Icons.Default.SettingsInputComponent
        )

        SettingsTextRow(
            title = "VPN MTU (default 1500)",
            value = vpnMtu,
            onClick = {
                activeTextFieldKey = "vpnMtu"
                activeTextFieldTitle = "VPN MTU (default 1500)"
                activeTextFieldValue = vpnMtu
            },
            icon = Icons.Default.Straighten
        )

        SettingsCheckboxRow(
            title = "Enable Hev TUN Feature",
            subtitle = "When enabled, TUN will use hev-socks5-tunnel; otherwise, it will use xray-core.",
            checked = enableHevTun,
            onCheckedChange = {
                enableHevTun = it
                sharedPrefs.edit().putBoolean("enableHevTun", it).apply()
            },
            icon = Icons.Default.ToggleOn
        )

        SettingsTextRow(
            title = "Hev Tun Log Level",
            value = hevTunLogLevel,
            onClick = {
                activeChoiceFieldKey = "hevTunLogLevel"
                activeChoiceFieldTitle = "Hev Tun Log Level"
                activeChoiceOptions = listOf("error", "warn", "info", "debug")
                activeChoiceValue = hevTunLogLevel
            },
            icon = Icons.Default.FormatListBulleted
        )

        SettingsTextRow(
            title = "Hev Tun read/write timeout (seconds)\n(tcp,udp default 300,60)",
            value = tcpUdpTimeout,
            onClick = {
                activeTextFieldKey = "tcpUdpTimeout"
                activeTextFieldTitle = "Hev Tun read/write timeout (seconds) (tcp,udp default 300,60)"
                activeTextFieldValue = tcpUdpTimeout
            },
            icon = Icons.Default.HourglassEmpty
        )

        // ==================== 2. CORE SETTINGS ====================
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionHeader("Core Settings")

        SettingsCheckboxRow(
            title = "Enable Sniffing",
            subtitle = "Try sniff domain from the packet (default on)",
            checked = enableSniffing,
            onCheckedChange = {
                enableSniffing = it
                sharedPrefs.edit().putBoolean("enableSniffing", it).apply()
            },
            icon = Icons.Default.Search
        )

        SettingsCheckboxRow(
            title = "Enable routeOnly",
            subtitle = "Use the sniffed domain name for routing only, and keep the target address as the IP address.",
            checked = enableRouteOnly,
            onCheckedChange = {
                enableRouteOnly = it
                sharedPrefs.edit().putBoolean("enableRouteOnly", it).apply()
            },
            icon = Icons.Default.AltRoute
        )

        SettingsCheckboxRow(
            title = "Allow connections from the LAN",
            subtitle = "Other devices can connect to proxy by your IP address through local proxy. Only enable in trusted networks to avoid unauthorized connections",
            checked = allowConnectionsLan,
            onCheckedChange = {
                allowConnectionsLan = it
                sharedPrefs.edit().putBoolean("allowConnectionsLan", it).apply()
            },
            icon = Icons.Default.WifiTethering
        )

        SettingsCheckboxRow(
            title = "allowInsecure",
            subtitle = "When TLS is selected, allow insecure connections by default",
            checked = allowInsecure,
            onCheckedChange = {
                allowInsecure = it
                sharedPrefs.edit().putBoolean("allowInsecure", it).apply()
            },
            icon = Icons.Default.NoEncryption
        )

        SettingsTextRow(
            title = "Local proxy port",
            value = localProxyPort,
            onClick = {
                activeTextFieldKey = "localProxyPort"
                activeTextFieldTitle = "Local proxy port"
                activeTextFieldValue = localProxyPort
            },
            icon = Icons.Default.SettingsInputComponent
        )

        SettingsTextRow(
            title = "Remote DNS (udp/tcp/https/quic)(Optional)",
            value = remoteDns,
            onClick = {
                activeTextFieldKey = "remoteDns"
                activeTextFieldTitle = "Remote DNS (udp/tcp/https/quic)(Optional)"
                activeTextFieldValue = remoteDns
            },
            icon = Icons.Default.Cloud
        )

        SettingsTextRow(
            title = "Domestic DNS (Optional)",
            value = domesticDns,
            onClick = {
                activeTextFieldKey = "domesticDns"
                activeTextFieldTitle = "Domestic DNS (Optional)"
                activeTextFieldValue = domesticDns
            },
            icon = Icons.Default.Home
        )

        SettingsTextRow(
            title = "DNS hosts (Format: domain:address,...)",
            value = dnsHosts,
            onClick = {
                activeTextFieldKey = "dnsHosts"
                activeTextFieldTitle = "DNS hosts (Format: domain:address,...)"
                activeTextFieldValue = dnsHosts
            },
            icon = Icons.Default.ListAlt
        )

        SettingsTextRow(
            title = "Log Level",
            value = logLevel,
            onClick = {
                activeChoiceFieldKey = "logLevel"
                activeChoiceFieldTitle = "Log Level"
                activeChoiceOptions = listOf("debug", "info", "warning", "error", "none")
                activeChoiceValue = logLevel
            },
            icon = Icons.Default.BugReport
        )

        SettingsTextRow(
            title = "Outbound domain pre-resolve method",
            value = outboundDomainPreResolve,
            onClick = {
                activeChoiceFieldKey = "outboundDomainPreResolve"
                activeChoiceFieldTitle = "Outbound domain pre-resolve method"
                activeChoiceOptions = listOf("Do not resolve", "Resolve and add to DNS Hosts", "Resolve and replace domain")
                activeChoiceValue = outboundDomainPreResolve
            },
            icon = Icons.Default.AdsClick
        )

        // ==================== 3. MUX SETTINGS ====================
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionHeader("Mux Settings")

        SettingsCheckboxRow(
            title = "Enable Mux",
            subtitle = "Faster, but it may cause unstable connectivity. Customize how to handle TCP, UDP and QUIC below",
            checked = enableMux,
            onCheckedChange = {
                enableMux = it
                sharedPrefs.edit().putBoolean("enableMux", it).apply()
            },
            icon = Icons.Default.MergeType
        )

        SettingsTextRow(
            title = "TCP connections (range -1 to 1024)",
            value = tcpConnections,
            onClick = {
                activeTextFieldKey = "tcpConnections"
                activeTextFieldTitle = "TCP connections (range -1 to 1024)"
                activeTextFieldValue = tcpConnections
            },
            icon = Icons.Default.CompareArrows
        )

        SettingsTextRow(
            title = "XUDP connections (range -1 to 1024)",
            value = xudpConnections,
            onClick = {
                activeTextFieldKey = "xudpConnections"
                activeTextFieldTitle = "XUDP connections (range -1 to 1024)"
                activeTextFieldValue = xudpConnections
            },
            icon = Icons.Default.SwapCalls
        )

        SettingsTextRow(
            title = "Handling of QUIC in mux tunnel",
            value = handlingQuicMux,
            onClick = {
                activeChoiceFieldKey = "handlingQuicMux"
                activeChoiceFieldTitle = "Handling of QUIC in mux tunnel"
                activeChoiceOptions = listOf("reject", "allow", "skip")
                activeChoiceValue = handlingQuicMux
            },
            icon = Icons.Default.RemoveCircleOutline
        )

        // ==================== 4. FRAGMENT SETTINGS ====================
        Spacer(modifier = Modifier.height(8.dp))
        SettingsSectionHeader("Fragment Settings")

        SettingsCheckboxRow(
            title = "Enable Fragment",
            subtitle = "Enable TLS/TCP payloads fragmenting",
            checked = enableFragment,
            onCheckedChange = {
                enableFragment = it
                sharedPrefs.edit().putBoolean("enableFragment", it).apply()
            },
            icon = Icons.Default.ViewAgenda
        )

        SettingsTextRow(
            title = "Fragment Length (min-max)",
            value = fragmentLength,
            onClick = {
                activeTextFieldKey = "fragmentLength"
                activeTextFieldTitle = "Fragment Length (min-max)"
                activeTextFieldValue = fragmentLength
            },
            icon = Icons.Default.Height
        )

        SettingsTextRow(
            title = "Fragment Interval (min-max)",
            value = fragmentInterval,
            onClick = {
                activeTextFieldKey = "fragmentInterval"
                activeTextFieldTitle = "Fragment Interval (min-max)"
                activeTextFieldValue = fragmentInterval
            },
            icon = Icons.Default.AccessTime
        )

        SettingsTextRow(
            title = "Fragment Packets",
            value = fragmentPackets,
            onClick = {
                activeChoiceFieldKey = "fragmentPackets"
                activeChoiceFieldTitle = "Fragment Packets"
                activeChoiceOptions = listOf("tlshello", "1-2", "1-3", "1-5")
                activeChoiceValue = fragmentPackets
            },
            icon = Icons.Default.Assignment
        )

        Spacer(modifier = Modifier.height(40.dp))
    }
}

@Composable
fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
    )
}

@Composable
fun SettingsCheckboxRow(
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f).padding(end = 16.dp)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!subtitle.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 18.sp
                )
            }
        }
        Checkbox(
            checked = checked,
            onCheckedChange = { onCheckedChange(it) },
            colors = CheckboxDefaults.colors(
                checkedColor = MaterialTheme.colorScheme.primary,
                uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                checkmarkColor = MaterialTheme.colorScheme.onPrimary
            )
        )
    }
}

@Composable
fun SettingsTextRow(
    title: String,
    value: String? = null,
    onClick: () -> Unit,
    icon: ImageVector? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (!value.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = value,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
