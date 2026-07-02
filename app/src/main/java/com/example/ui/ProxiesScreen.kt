package com.example.ui

import androidx.compose.foundation.combinedClickable
import com.example.R
import com.example.viewmodel.AppViewModel
import com.example.fmt.XrayConfigGenerator
import android.content.Context
import com.example.service.SiCepatVpnService
import com.example.service.XrayCore
import org.json.JSONObject
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.viewinterop.AndroidView
import android.view.LayoutInflater
import android.widget.EditText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesScreen(viewModel: AppViewModel) {
    val proxiesList = viewModel.proxies
    val coroutineScope = rememberCoroutineScope()

    var activeAddType by remember { mutableStateOf<String?>(null) }
    var activeEditProxy by remember { mutableStateOf<ProxyItem?>(null) }
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    // Configuration / Custom Settings state
    var selectedStyle by remember { mutableStateOf("Tab") } // "Tab", "List"
    var selectedSort by remember { mutableStateOf("Default") } // "Default", "Delay", "Name"
    var selectedLayout by remember { mutableStateOf("Loose") } // "Loose", "Standard", "Tight"
    var selectedSize by remember { mutableStateOf("Min") } // "Standard", "Shrink", "Min"

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showWireGuardDialog by remember { mutableStateOf(false) }
    var showHysteria2Dialog by remember { mutableStateOf(false) }
    var showVmessDialog by remember { mutableStateOf(false) }
    var showVlessDialog by remember { mutableStateOf(false) }
    var showTrojanDialog by remember { mutableStateOf(false) }
    var showShadowsocksDialog by remember { mutableStateOf(false) }
    var showSocksDialog by remember { mutableStateOf(false) }
    var showHttpDialog by remember { mutableStateOf(false) }
    var selectedTabIndex by remember { mutableStateOf(0) }
    
    var searchQuery by remember { mutableStateOf("") }
    var isSearchActive by remember { mutableStateOf(false) }

    val currentProfileName = remember(viewModel.profiles, selectedTabIndex) {
        if (viewModel.profiles.isNotEmpty() && selectedTabIndex < viewModel.profiles.size) {
            viewModel.profiles[selectedTabIndex].name
        } else {
            ""
        }
    }

    val displayProxies by remember(currentProfileName, selectedStyle, searchQuery) {
        derivedStateOf {
            val filtered = if (selectedStyle == "Tab") {
                proxiesList.filter { it.profileName == currentProfileName }
            } else {
                proxiesList.toList()
            }
            
            if (searchQuery.isNotBlank()) {
                filtered.filter { it.name.contains(searchQuery, ignoreCase = true) || it.type.contains(searchQuery, ignoreCase = true) }
            } else {
                filtered
            }
        }
    }

    // Sort proxies dynamically
    val sortedProxies by remember(selectedSort) {
        derivedStateOf {
            when (selectedSort) {
                "Delay" -> {
                    displayProxies.sortedWith(compareBy {
                        val lat = it.latency
                        if (lat == "⚡") 0
                        else if (lat.endsWith(" ms")) lat.removeSuffix(" ms").toIntOrNull() ?: 9999
                        else 99999 // Timeouts / others go to the end
                    })
                }
                "Name" -> displayProxies.sortedBy { it.name }
                else -> displayProxies // Default
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (activeEditProxy != null) {
            EditProxyScreen(
                editingProxyInstance = activeEditProxy!!,
                proxiesList = proxiesList,
                onDismiss = { activeEditProxy = null }
            )
        } else {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                ProxiesHeader(
                    isSearchActive = isSearchActive,
                    onSearchActiveChange = { isSearchActive = it },
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onSettingsClick = { showSettingsSheet = true },
                    onImportClick = {
                        val clipboardText = clipboardManager.getText()?.text ?: ""
                        if (clipboardText.isNotEmpty()) {
                            val addedCount = viewModel.importProxiesFromText(clipboardText, currentProfileName)
                            if (addedCount > 0) {
                                Toast.makeText(context, "Imported $addedCount proxies from clipboard", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "No valid configs found in clipboard", Toast.LENGTH_SHORT).show()
                            }
                        } else {
                            Toast.makeText(context, "Clipboard empty", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onAddTypeClick = { type ->
                        when (type.lowercase()) {
                            "vmess" -> showVmessDialog = true
                            "vless" -> showVlessDialog = true
                            "trojan" -> showTrojanDialog = true
                            "shadowsocks" -> showShadowsocksDialog = true
                            "histeria2", "hysteria" -> showHysteria2Dialog = true
                            "wireguard" -> showWireGuardDialog = true
                            "socks", "socks5" -> showSocksDialog = true
                            "http" -> showHttpDialog = true
                        }
                    },
                    onDeleteInvalidClick = {
                        if (currentProfileName.isNotBlank() && currentProfileName != "All") {
                            proxiesList.removeAll { it.profileName == currentProfileName && (it.latency == "Timeout" || it.latency == "Waiting") }
                        } else {
                            proxiesList.removeAll { it.latency == "Timeout" || it.latency == "Waiting" }
                        }
                    },
                    onDeleteAllClick = {
                        if (currentProfileName.isNotBlank() && currentProfileName != "All") {
                            proxiesList.removeAll { it.profileName == currentProfileName }
                        } else {
                            proxiesList.clear()
                        }
                    },
                    onExportClick = {
                        val targetList = if (currentProfileName.isNotBlank() && currentProfileName != "All") {
                            proxiesList.filter { it.profileName == currentProfileName }
                        } else {
                            proxiesList.toList()
                        }
                        val allConfigs = targetList.joinToString("\n") { XrayConfigGenerator.generateProxyUri(it) }
                        clipboardManager.setText(AnnotatedString(allConfigs))
                        Toast.makeText(context, "Exported ${targetList.size} configs to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )


                // Tabs (Only show if style == "Tab")
                if (selectedStyle == "Tab" && viewModel.profiles.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = selectedTabIndex,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        divider = {},
                        edgePadding = 0.dp
                    ) {
                        viewModel.profiles.forEachIndexed { index, profile ->
                            Tab(
                                selected = selectedTabIndex == index,
                                onClick = { selectedTabIndex = index },
                                text = { Text(profile.name, fontSize = 16.sp, color = if (selectedTabIndex == index) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (selectedStyle == "Tab") {
                    val gridColumns = when (selectedSize) {
                        "Standard" -> 2
                        "Shrink" -> 3
                        "Min" -> 3 // or 4
                        else -> 2
                    }
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(gridColumns),
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(sortedProxies.size, key = { index -> "${sortedProxies[index].profileName}_${sortedProxies[index].name}_$index" }) { index ->
                            val proxy = sortedProxies[index]
                            ProxyCard(
                                proxy = proxy,
                                layout = selectedLayout,
                                size = selectedSize,
                                isGrid = true,
                                onClick = {
                                    val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                    if (originalIndex != -1) {
                                        for (i in proxiesList.indices) {
                                            proxiesList[i] = proxiesList[i].copy(isSelected = (i == originalIndex))
                                        }
                                        val sharedPrefs = context.getSharedPreferences("sicepat_active_proxy", Context.MODE_PRIVATE)
                                        sharedPrefs.edit()
                                            .putString("name", proxy.name)
                                            .putString("type", proxy.type)
                                            .putString("fullConfig", proxy.fullConfig)
                                            .putString("profileName", proxy.profileName)
                                            .apply()
                                    }
                                },
                                onEdit = {
                                    activeEditProxy = proxy
                                },
                                onDelete = {
                                    val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                    if (originalIndex != -1) {
                                        proxiesList.removeAt(originalIndex)
                                    }
                                }
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp, start = 16.dp, end = 16.dp)
                    ) {
                        items(sortedProxies.size, key = { index -> "${sortedProxies[index].profileName}_${sortedProxies[index].name}_$index" }) { index ->
                            val proxy = sortedProxies[index]
                            ProxyCard(
                                proxy = proxy,
                                layout = selectedLayout,
                                size = selectedSize,
                                isGrid = false,
                                onClick = {
                                    val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                    if (originalIndex != -1) {
                                        for (i in proxiesList.indices) {
                                            proxiesList[i] = proxiesList[i].copy(isSelected = (i == originalIndex))
                                        }
                                        val sharedPrefs = context.getSharedPreferences("sicepat_active_proxy", Context.MODE_PRIVATE)
                                        sharedPrefs.edit()
                                            .putString("name", proxy.name)
                                            .putString("type", proxy.type)
                                            .putString("fullConfig", proxy.fullConfig)
                                            .putString("profileName", proxy.profileName)
                                            .apply()
                                    }
                                },
                                onEdit = {
                                    activeEditProxy = proxy
                                },
                                onDelete = {
                                    val originalIndex = proxiesList.indexOfFirst { it.name == proxy.name }
                                    if (originalIndex != -1) {
                                        proxiesList.removeAt(originalIndex)
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                    }
                }
            }

            // FAB connection
            var isTesting by remember { mutableStateOf(false) }

            FloatingActionButton(
                onClick = { 
                    if (isTesting) return@FloatingActionButton
                    isTesting = true
                    coroutineScope.launch {
                        val displayNames = displayProxies.map { it.name }.toSet()
                        // Mark visible ones as "Testing..."
                        for (i in proxiesList.indices) {
                            if (displayNames.contains(proxiesList[i].name)) {
                                proxiesList[i] = proxiesList[i].copy(latency = "Testing...")
                            }
                        }
                        
                        // Real TCP socket handshake ping check with fallback
                        for (i in proxiesList.indices) {
                            if (displayNames.contains(proxiesList[i].name)) {
                                delay(50)
                                val fullJson = XrayConfigGenerator.generateConfig(
                                    context = context,
                                    overrideName = proxiesList[i].name,
                                    overrideType = proxiesList[i].type,
                                    overrideConfig = proxiesList[i].fullConfig
                                )
                                val realPing = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                                    try {
                                        libv2ray.Libv2ray.measureOutboundDelay(fullJson, "https://www.google.com/generate_204")
                                    } catch (e: Exception) {
                                        -1L
                                    }
                                }
                                
                                val isSuccess = realPing > 0
                                val latencyStr = if (isSuccess) "$realPing ms" else "Timeout"
                                proxiesList[i] = proxiesList[i].copy(
                                    latency = latencyStr,
                                    isGreen = isSuccess
                                )
                            }
                        }
                        isTesting = false
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(64.dp),
                containerColor = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp)
            ) {
                if (isTesting) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.NetworkPing, // Represents ping checking better
                        contentDescription = "Test All Pings",
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        // Bottom Settings Sheet (modal bottom sheet)
        if (showSettingsSheet) {
            ProxySettingsBottomSheet(
                onDismissRequest = { showSettingsSheet = false },
                selectedStyle = selectedStyle,
                onStyleChange = { selectedStyle = it },
                selectedSort = selectedSort,
                onSortChange = { selectedSort = it },
                selectedLayout = selectedLayout,
                onLayoutChange = { selectedLayout = it },
                selectedSize = selectedSize,
                onSizeChange = { selectedSize = it }
            )
        }

        if (showWireGuardDialog) {
            AddWireGuardDialog(
                onDismiss = { showWireGuardDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "WireGuard", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showWireGuardDialog = false
                }
            )
        }

        if (showHysteria2Dialog) {
            AddHysteria2Dialog(
                onDismiss = { showHysteria2Dialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Hysteria2", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showHysteria2Dialog = false
                }
            )
        }

        if (showVmessDialog) {
            AddVmessDialog(
                onDismiss = { showVmessDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Vmess", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showVmessDialog = false
                }
            )
        }

        if (showVlessDialog) {
            AddVlessDialog(
                onDismiss = { showVlessDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Vless", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showVlessDialog = false
                }
            )
        }

        if (showTrojanDialog) {
            AddTrojanDialog(
                onDismiss = { showTrojanDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Trojan", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showTrojanDialog = false
                }
            )
        }

        if (showShadowsocksDialog) {
            AddShadowsocksDialog(
                onDismiss = { showShadowsocksDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Shadowsocks", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showShadowsocksDialog = false
                }
            )
        }

        if (showSocksDialog) {
            AddSocksDialog(
                onDismiss = { showSocksDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "Socks5", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showSocksDialog = false
                }
            )
        }

        if (showHttpDialog) {
            AddHttpDialog(
                onDismiss = { showHttpDialog = false },
                onSave = { name, fullConfig ->
                    proxiesList.add(0, ProxyItem(name, "HTTP", "Waiting", false, fullConfig = fullConfig, profileName = currentProfileName))
                    showHttpDialog = false
                }
            )
        }
    }
}


data class ParsedProxyConfig(
    val remarks: String = "",
    val type: String = "Vless",
    val address: String = "",
    val port: String = "443",
    val uuidOrPassword: String = "",
    val security: String = "none",
    val networkType: String = "tcp",
    val path: String = "",
    val host: String = "",
    val sni: String = "",
    val method: String = "aes-128-gcm",
    val rawUri: String = "",
    val rawJson: String = ""
)

fun parseUri(configUri: String, proxyItem: ProxyItem, context: Context): ParsedProxyConfig {
    val trimmed = configUri.trim()
    var initialJson = "{}"
    try {
        initialJson = XrayConfigGenerator.generateConfig(
            context = context,
            overrideName = proxyItem.name,
            overrideType = proxyItem.type,
            overrideConfig = trimmed
        )
    } catch (e: Exception) {
        // ignore
    }

    if (trimmed.isBlank()) {
        return ParsedProxyConfig(remarks = proxyItem.name, type = proxyItem.type, rawJson = initialJson)
    }

    try {
        if (trimmed.startsWith("{")) {
            val root = JSONObject(trimmed)
            val remarks = root.optString("remarks", proxyItem.name)
            var type = proxyItem.type
            var address = ""
            var port = ""
            var uuidOrPassword = ""
            var security = "none"
            var networkType = "tcp"
            var path = ""
            var host = ""
            var sni = ""
            var method = "aes-128-gcm"

            if (root.has("outbounds")) {
                val outbounds = root.getJSONArray("outbounds")
                if (outbounds.length() > 0) {
                    val mainOutbound = outbounds.getJSONObject(0)
                    type = mainOutbound.optString("protocol", proxyItem.type).replaceFirstChar { it.uppercase() }
                    val settings = mainOutbound.optJSONObject("settings")
                    if (settings != null) {
                        if (settings.has("servers")) {
                            val servers = settings.getJSONArray("servers")
                            if (servers.length() > 0) {
                                val server = servers.getJSONObject(0)
                                address = server.optString("address", "")
                                port = server.optString("port", "")
                                uuidOrPassword = server.optString("password", server.optString("id", ""))
                                method = server.optString("method", "aes-128-gcm")
                            }
                        } else if (settings.has("vnext")) {
                            val vnext = settings.getJSONArray("vnext")
                            if (vnext.length() > 0) {
                                val server = vnext.getJSONObject(0)
                                address = server.optString("address", "")
                                port = server.optString("port", "")
                                val users = server.optJSONArray("users")
                                if (users != null && users.length() > 0) {
                                    uuidOrPassword = users.getJSONObject(0).optString("id", "")
                                }
                            }
                        }
                    }
                    val streamSettings = mainOutbound.optJSONObject("streamSettings")
                    if (streamSettings != null) {
                        networkType = streamSettings.optString("network", "tcp")
                        security = streamSettings.optString("security", "none")
                        val wsSettings = streamSettings.optJSONObject("wsSettings")
                        if (wsSettings != null) {
                            path = wsSettings.optString("path", "")
                            val headers = wsSettings.optJSONObject("headers")
                            if (headers != null) {
                                host = headers.optString("Host", "")
                            }
                        }
                        val grpcSettings = streamSettings.optJSONObject("grpcSettings")
                        if (grpcSettings != null) {
                            path = grpcSettings.optString("serviceName", "")
                        }
                        val tlsSettings = streamSettings.optJSONObject("tlsSettings")
                        if (tlsSettings != null) {
                            sni = tlsSettings.optString("serverName", "")
                        }
                    }
                }
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = type,
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                security = security,
                networkType = networkType,
                path = path,
                host = host,
                sni = sni,
                method = method,
                rawUri = "",
                rawJson = trimmed
            )
        }

        if (trimmed.startsWith("vmess://", ignoreCase = true)) {
            val rawB64 = trimmed.substring(8).trim().substringBefore("#")
            val jsonStr = try {
                String(android.util.Base64.decode(rawB64, android.util.Base64.DEFAULT), Charsets.UTF_8)
            } catch(e: Exception) {
                try {
                    String(android.util.Base64.decode(rawB64, android.util.Base64.URL_SAFE or android.util.Base64.DEFAULT), Charsets.UTF_8)
                } catch(ex: Exception) {
                    ""
                }
            }
            if (jsonStr.isNotBlank()) {
                val json = JSONObject(jsonStr)
                return ParsedProxyConfig(
                    remarks = json.optString("ps", proxyItem.name),
                    type = "Vmess",
                    address = json.optString("add", ""),
                    port = json.optString("port", "443"),
                    uuidOrPassword = json.optString("id", ""),
                    networkType = json.optString("net", "tcp"),
                    path = json.optString("path", ""),
                    host = json.optString("host", ""),
                    sni = json.optString("sni", ""),
                    security = json.optString("tls", "none"),
                    rawUri = trimmed,
                    rawJson = initialJson
                )
            }
        }

        if (trimmed.startsWith("vless://", ignoreCase = true) || trimmed.startsWith("trojan://", ignoreCase = true)) {
            val isVless = trimmed.startsWith("vless://", ignoreCase = true)
            var mainPart = if (isVless) trimmed.substring(8) else trimmed.substring(9)
            var remarks = proxyItem.name
            if (mainPart.contains("#")) {
                try {
                    remarks = java.net.URLDecoder.decode(mainPart.substringAfterLast("#"), "UTF-8")
                } catch(e: Exception) {
                    remarks = mainPart.substringAfterLast("#")
                }
                mainPart = mainPart.substringBeforeLast("#")
            }
            var uuidOrPassword = ""
            if (mainPart.contains("@")) {
                uuidOrPassword = mainPart.substringBefore("@")
                mainPart = mainPart.substringAfter("@")
            }
            var queryStr = ""
            if (mainPart.contains("?")) {
                queryStr = mainPart.substringAfter("?")
                mainPart = mainPart.substringBefore("?")
            }
            var address = ""
            var port = "443"
            if (mainPart.contains(":")) {
                address = mainPart.substringBeforeLast(":")
                port = mainPart.substringAfterLast(":").trim()
            } else {
                address = mainPart
            }

            var security = "none"
            var networkType = "tcp"
            var path = ""
            var host = ""
            var sni = ""

            if (queryStr.isNotEmpty()) {
                val pairs = queryStr.split("&")
                for (pair in pairs) {
                    val idx = pair.indexOf("=")
                    if (idx != -1) {
                        val key = java.net.URLDecoder.decode(pair.substring(0, idx), "UTF-8").lowercase()
                        val value = java.net.URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                        when (key) {
                            "security" -> security = value
                            "type" -> networkType = value
                            "path" -> path = value
                            "host" -> host = value
                            "sni" -> sni = value
                        }
                    }
                }
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = if (isVless) "Vless" else "Trojan",
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                security = security,
                networkType = networkType,
                path = path,
                host = host,
                sni = sni,
                rawUri = trimmed,
                rawJson = initialJson
            )
        }

        if (trimmed.startsWith("ss://", ignoreCase = true)) {
            var mainPart = trimmed.substring(5)
            var remarks = proxyItem.name
            if (mainPart.contains("#")) {
                try {
                    remarks = java.net.URLDecoder.decode(mainPart.substringAfterLast("#"), "UTF-8")
                } catch(e: Exception) {
                    remarks = mainPart.substringAfterLast("#")
                }
                mainPart = mainPart.substringBeforeLast("#")
            }
            var method = "aes-128-gcm"
            var uuidOrPassword = ""
            if (mainPart.contains("@")) {
                val userInfo = mainPart.substringBefore("@")
                mainPart = mainPart.substringAfter("@")
                val decoded = try {
                    String(android.util.Base64.decode(userInfo, android.util.Base64.URL_SAFE or android.util.Base64.DEFAULT), Charsets.UTF_8)
                } catch (e: Exception) {
                    userInfo
                }
                if (decoded.contains(":")) {
                    method = decoded.substringBefore(":")
                    uuidOrPassword = decoded.substringAfter(":")
                } else {
                    uuidOrPassword = decoded
                }
            }
            var address = ""
            var port = "443"
            if (mainPart.contains(":")) {
                address = mainPart.substringBeforeLast(":")
                port = mainPart.substringAfterLast(":").trim()
            } else {
                address = mainPart
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = "Shadowsocks",
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                method = method,
                rawUri = trimmed,
                rawJson = initialJson
            )
        }

        if (trimmed.startsWith("hysteria2://", ignoreCase = true) || trimmed.startsWith("hysteria://", ignoreCase = true)) {
            val isHy2 = trimmed.startsWith("hysteria2://", ignoreCase = true)
            var mainPart = if (isHy2) trimmed.substring(12) else trimmed.substring(11)
            var remarks = proxyItem.name
            if (mainPart.contains("#")) {
                try {
                    remarks = java.net.URLDecoder.decode(mainPart.substringAfterLast("#"), "UTF-8")
                } catch(e: Exception) {
                    remarks = mainPart.substringAfterLast("#")
                }
                mainPart = mainPart.substringBeforeLast("#")
            }
            var uuidOrPassword = ""
            if (mainPart.contains("@")) {
                uuidOrPassword = mainPart.substringBefore("@")
                mainPart = mainPart.substringAfter("@")
            }
            var address = ""
            var port = "443"
            if (mainPart.contains(":")) {
                address = mainPart.substringBeforeLast(":")
                port = mainPart.substringAfterLast(":").trim()
            } else {
                address = mainPart
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = "Hysteria2",
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                rawUri = trimmed,
                rawJson = initialJson
            )
        }

        if (trimmed.startsWith("socks://", ignoreCase = true) || trimmed.startsWith("socks5://", ignoreCase = true)) {
            val isS5 = trimmed.startsWith("socks5://", ignoreCase = true)
            var mainPart = if (isS5) trimmed.substring(9) else trimmed.substring(8)
            var remarks = proxyItem.name
            if (mainPart.contains("#")) {
                try {
                    remarks = java.net.URLDecoder.decode(mainPart.substringAfterLast("#"), "UTF-8")
                } catch(e: Exception) {
                    remarks = mainPart.substringAfterLast("#")
                }
                mainPart = mainPart.substringBeforeLast("#")
            }
            var uuidOrPassword = ""
            if (mainPart.contains("@")) {
                uuidOrPassword = mainPart.substringBefore("@")
                mainPart = mainPart.substringAfter("@")
            }
            var address = ""
            var port = "1080"
            if (mainPart.contains(":")) {
                address = mainPart.substringBeforeLast(":")
                port = mainPart.substringAfterLast(":").trim()
            } else {
                address = mainPart
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = "Socks5",
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                rawUri = trimmed,
                rawJson = initialJson
            )
        }

        if (trimmed.startsWith("http://", ignoreCase = true) || trimmed.startsWith("https://", ignoreCase = true)) {
            val isHttps = trimmed.startsWith("https://", ignoreCase = true)
            var mainPart = if (isHttps) trimmed.substring(8) else trimmed.substring(7)
            var remarks = proxyItem.name
            if (mainPart.contains("#")) {
                try {
                    remarks = java.net.URLDecoder.decode(mainPart.substringAfterLast("#"), "UTF-8")
                } catch(e: Exception) {
                    remarks = mainPart.substringAfterLast("#")
                }
                mainPart = mainPart.substringBeforeLast("#")
            }
            var uuidOrPassword = ""
            if (mainPart.contains("@")) {
                uuidOrPassword = mainPart.substringBefore("@")
                mainPart = mainPart.substringAfter("@")
            }
            var address = ""
            var port = "8080"
            if (mainPart.contains(":")) {
                address = mainPart.substringBeforeLast(":")
                port = mainPart.substringAfterLast(":").trim()
            } else {
                address = mainPart
            }
            return ParsedProxyConfig(
                remarks = remarks,
                type = "HTTP",
                address = address,
                port = port,
                uuidOrPassword = uuidOrPassword,
                rawUri = trimmed,
                rawJson = initialJson
            )
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return ParsedProxyConfig(remarks = proxyItem.name, type = proxyItem.type, rawUri = trimmed, rawJson = initialJson)
}

fun buildUri(p: ParsedProxyConfig): String {
    val remarksEncoded = try {
        java.net.URLEncoder.encode(p.remarks, "UTF-8").replace("+", "%20")
    } catch(e: Exception) {
        p.remarks
    }
    return when (p.type) {
        "Vmess" -> {
            val json = JSONObject()
            json.put("v", "2")
            json.put("ps", p.remarks)
            json.put("add", p.address)
            json.put("port", p.port)
            json.put("id", p.uuidOrPassword)
            json.put("aid", "0")
            json.put("scy", "auto")
            json.put("net", p.networkType)
            json.put("type", "none")
            json.put("host", p.host)
            json.put("path", p.path)
            json.put("tls", p.security)
            json.put("sni", p.sni)
            val b64 = android.util.Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
            "vmess://$b64"
        }
        "Vless", "Trojan" -> {
            val scheme = p.type.lowercase()
            val queryParams = mutableListOf<String>()
            if (p.security.isNotEmpty()) queryParams.add("security=${p.security}")
            if (p.networkType.isNotEmpty()) queryParams.add("type=${p.networkType}")
            if (p.path.isNotEmpty()) {
                val encodedPath = try { java.net.URLEncoder.encode(p.path, "UTF-8") } catch(e: Exception) { p.path }
                queryParams.add("path=$encodedPath")
            }
            if (p.host.isNotEmpty()) {
                val encodedHost = try { java.net.URLEncoder.encode(p.host, "UTF-8") } catch(e: Exception) { p.host }
                queryParams.add("host=$encodedHost")
            }
            if (p.sni.isNotEmpty()) {
                val encodedSni = try { java.net.URLEncoder.encode(p.sni, "UTF-8") } catch(e: Exception) { p.sni }
                queryParams.add("sni=$encodedSni")
            }
            
            val queryStr = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
            val userInfo = if (p.uuidOrPassword.isNotEmpty()) "${p.uuidOrPassword}@" else ""
            "$scheme://$userInfo${p.address}:${p.port}$queryStr#$remarksEncoded"
        }
        "Shadowsocks" -> {
            val userInfoRaw = "${p.method}:${p.uuidOrPassword}"
            val userInfoB64 = android.util.Base64.encodeToString(userInfoRaw.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
            "ss://$userInfoB64@${p.address}:${p.port}#$remarksEncoded"
        }
        "Hysteria2" -> {
            val userInfo = if (p.uuidOrPassword.isNotEmpty()) "${p.uuidOrPassword}@" else ""
            "hysteria2://$userInfo${p.address}:${p.port}#$remarksEncoded"
        }
        "Socks5" -> {
            val userInfo = if (p.uuidOrPassword.isNotEmpty()) "${p.uuidOrPassword}@" else ""
            "socks5://$userInfo${p.address}:${p.port}#$remarksEncoded"
        }
        "HTTP" -> {
            val userInfo = if (p.uuidOrPassword.isNotEmpty()) "${p.uuidOrPassword}@" else ""
            "http://$userInfo${p.address}:${p.port}#$remarksEncoded"
        }
        else -> {
            p.rawUri
        }
    }
}

@Composable
fun EditProxyScreen(
    editingProxyInstance: ProxyItem,
    proxiesList: androidx.compose.runtime.snapshots.SnapshotStateList<ProxyItem>,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    
    // Parse the initial URI
    val parsedConfig = remember(editingProxyInstance) {
        parseUri(editingProxyInstance.fullConfig, editingProxyInstance, context)
    }

    var remarksText by remember { mutableStateOf(parsedConfig.remarks) }
    var typeText by remember { mutableStateOf(parsedConfig.type) }
    var addressText by remember { mutableStateOf(parsedConfig.address) }
    var portText by remember { mutableStateOf(parsedConfig.port) }
    var uuidOrPasswordText by remember { mutableStateOf(parsedConfig.uuidOrPassword) }
    var securityText by remember { mutableStateOf(parsedConfig.security) }
    var networkTypeText by remember { mutableStateOf(parsedConfig.networkType) }
    var pathText by remember { mutableStateOf(parsedConfig.path) }
    var hostText by remember { mutableStateOf(parsedConfig.host) }
    var sniText by remember { mutableStateOf(parsedConfig.sni) }
    var methodText by remember { mutableStateOf(parsedConfig.method) }

    var fullConfigText by remember { mutableStateOf(editingProxyInstance.fullConfig) }
    var fullJsonText by remember { mutableStateOf(parsedConfig.rawJson) }

    val isJsonConfig = remember(editingProxyInstance.fullConfig) {
        editingProxyInstance.fullConfig.trim().startsWith("{")
    }

    var isTypeMenuExpanded by remember { mutableStateOf(false) }
    var isNetMenuExpanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onBackground
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Edit Pengaturan Proxy",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        Card(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                if (isJsonConfig) {
                    // JSON Mode (Auto Detected)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = remarksText,
                            onValueChange = { remarksText = it },
                            label = { Text("Nama Server / Remarks") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = fullConfigText,
                            onValueChange = { fullConfigText = it },
                            label = { Text("Raw Xray JSON Config") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            maxLines = 25
                        )
                    }
                } else {
                    // Standard Form Mode (Auto Detected)
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedTextField(
                            value = remarksText,
                            onValueChange = { remarksText = it },
                            label = { Text("Nama Server / Remarks") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        // Protocol Selector Dropdown
                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedTextField(
                                value = typeText,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Protocol Type") },
                                trailingIcon = {
                                    Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Protocol")
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                            // Transparent clickable box overlay
                            Box(
                                modifier = Modifier
                                    .matchParentSize()
                                    .clickable { isTypeMenuExpanded = true }
                                    .background(Color.Transparent)
                            )
                            DropdownMenu(
                                expanded = isTypeMenuExpanded,
                                onDismissRequest = { isTypeMenuExpanded = false },
                                modifier = Modifier.background(Color(0xFF1E2228))
                            ) {
                                val protocols = listOf("Vmess", "Vless", "Trojan", "Shadowsocks", "Hysteria2", "Socks5", "HTTP")
                                protocols.forEach { proto ->
                                    DropdownMenuItem(
                                        text = { Text(proto, color = Color.White) },
                                        onClick = {
                                            typeText = proto
                                            isTypeMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = addressText,
                            onValueChange = { addressText = it },
                            label = { Text("Server Address") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = portText,
                            onValueChange = { portText = it },
                            label = { Text("Port") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        OutlinedTextField(
                            value = uuidOrPasswordText,
                            onValueChange = { uuidOrPasswordText = it },
                            label = { 
                                Text(
                                    when (typeText) {
                                        "Trojan", "Shadowsocks" -> "Password"
                                        "Vmess", "Vless" -> "UUID"
                                        else -> "Username / Password / Key"
                                    }
                                )
                            },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )

                        if (typeText == "Shadowsocks") {
                            var isMethodMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = methodText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Shadowsocks Encryption Method") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Method")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { isMethodMenuExpanded = true }
                                        .background(Color.Transparent)
                                )
                                DropdownMenu(
                                    expanded = isMethodMenuExpanded,
                                    onDismissRequest = { isMethodMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E2228))
                                ) {
                                    val methods = listOf("aes-128-gcm", "aes-256-gcm", "chacha20-ietf-poly1305")
                                    methods.forEach { m ->
                                        DropdownMenuItem(
                                            text = { Text(m, color = Color.White) },
                                            onClick = {
                                                methodText = m
                                                isMethodMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        // Advanced transport options
                        if (typeText == "Vmess" || typeText == "Vless" || typeText == "Trojan") {
                            Text(
                                text = "Security & Transport Settings",
                                fontWeight = FontWeight.Bold,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(top = 8.dp)
                            )

                            // Network / Transport Selector
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = networkTypeText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Network / Transport Type") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Transport")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { isNetMenuExpanded = true }
                                        .background(Color.Transparent)
                                )
                                DropdownMenu(
                                    expanded = isNetMenuExpanded,
                                    onDismissRequest = { isNetMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E2228))
                                ) {
                                    val transports = listOf("tcp", "ws", "grpc", "h2", "http")
                                    transports.forEach { trans ->
                                        DropdownMenuItem(
                                            text = { Text(trans, color = Color.White) },
                                            onClick = {
                                                networkTypeText = trans
                                                isNetMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            OutlinedTextField(
                                value = pathText,
                                onValueChange = { pathText = it },
                                label = { Text("Path / Service Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = hostText,
                                onValueChange = { hostText = it },
                                label = { Text("Host") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            OutlinedTextField(
                                value = sniText,
                                onValueChange = { sniText = it },
                                label = { Text("SNI") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )

                            // Security Dropdown
                            var isSecurityMenuExpanded by remember { mutableStateOf(false) }
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = securityText,
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Security") },
                                    trailingIcon = {
                                        Icon(Icons.Default.ArrowDropDown, contentDescription = "Select Security")
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Box(
                                    modifier = Modifier
                                        .matchParentSize()
                                        .clickable { isSecurityMenuExpanded = true }
                                        .background(Color.Transparent)
                                )
                                DropdownMenu(
                                    expanded = isSecurityMenuExpanded,
                                    onDismissRequest = { isSecurityMenuExpanded = false },
                                    modifier = Modifier.background(Color(0xFF1E2228))
                                ) {
                                    val securities = listOf("none", "tls")
                                    securities.forEach { sec ->
                                        DropdownMenuItem(
                                            text = { Text(sec, color = Color.White) },
                                            onClick = {
                                                securityText = sec
                                                isSecurityMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Batal", color = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(
                        onClick = {
                            var name = remarksText
                            var type = typeText
                            var finalConfig = fullConfigText

                            if (isJsonConfig) {
                                // Save from JSON mode
                                finalConfig = fullConfigText
                                name = if (remarksText.isNotBlank()) remarksText else editingProxyInstance.name
                                try {
                                    val root = JSONObject(finalConfig)
                                    if (remarksText.isNotBlank() && root.optString("remarks") != remarksText) {
                                        root.put("remarks", remarksText)
                                        finalConfig = root.toString(2)
                                    }
                                    if (root.has("outbounds")) {
                                        val outbounds = root.getJSONArray("outbounds")
                                        if (outbounds.length() > 0) {
                                            val mainOutbound = outbounds.getJSONObject(0)
                                            val protocol = mainOutbound.optString("protocol", "")
                                            if (protocol.isNotEmpty()) {
                                                type = protocol.replaceFirstChar { it.uppercase() }
                                            }
                                        }
                                    }
                                } catch (e: Exception) {
                                    // ignore
                                }
                            } else {
                                // Save from Form
                                val p = ParsedProxyConfig(
                                    remarks = remarksText,
                                    type = typeText,
                                    address = addressText,
                                    port = portText,
                                    uuidOrPassword = uuidOrPasswordText,
                                    security = securityText,
                                    networkType = networkTypeText,
                                    path = pathText,
                                    host = hostText,
                                    sni = sniText,
                                    method = methodText,
                                    rawUri = fullConfigText,
                                    rawJson = fullJsonText
                                )
                                finalConfig = buildUri(p)
                                name = if (remarksText.isNotBlank()) remarksText else editingProxyInstance.name
                                type = typeText
                            }

                            val originalIndex = proxiesList.indexOfFirst { it.name == editingProxyInstance.name }
                            if (originalIndex != -1) {
                                val updatedProxy = proxiesList[originalIndex].copy(
                                    name = name,
                                    type = type,
                                    fullConfig = finalConfig
                                )
                                proxiesList[originalIndex] = updatedProxy
                                
                                // Update sicepat_active_proxy if this was the selected proxy
                                if (updatedProxy.isSelected) {
                                    val sharedPrefs = context.getSharedPreferences("sicepat_active_proxy", Context.MODE_PRIVATE)
                                    sharedPrefs.edit()
                                        .putString("name", updatedProxy.name)
                                        .putString("type", updatedProxy.type)
                                        .putString("fullConfig", updatedProxy.fullConfig)
                                        .putString("profileName", updatedProxy.profileName)
                                        .apply()

                                    // Restart VPN if it is currently connected to apply changes
                                    if (SiCepatVpnService.vpnStatus.value == SiCepatVpnService.VpnStatus.CONNECTED) {
                                        context.startService(android.content.Intent(context, SiCepatVpnService::class.java).apply { action = "STOP" })
                                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                                            context.startService(android.content.Intent(context, SiCepatVpnService::class.java).apply { action = "START" })
                                        }, 1000)
                                    }
                                }
                            }
                            onDismiss()
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Simpan", color = MaterialTheme.colorScheme.onPrimary)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxySettingsBottomSheet(
    onDismissRequest: () -> Unit,
    selectedStyle: String,
    onStyleChange: (String) -> Unit,
    selectedSort: String,
    onSortChange: (String) -> Unit,
    selectedLayout: String,
    onLayoutChange: (String) -> Unit,
    selectedSize: String,
    onSizeChange: (String) -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Color(0xFF16181C),
        contentColor = MaterialTheme.colorScheme.onSurface,
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text(
                text = "Settings",
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(bottom = 24.dp)
            )

            SectionTitle(title = "Style")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsOptionButton(
                    text = "Tab",
                    icon = { tint -> Icon(Icons.Default.Tab, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
                    isSelected = selectedStyle == "Tab",
                    onClick = { onStyleChange("Tab") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "List",
                    icon = { tint -> Icon(Icons.Default.List, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
                    isSelected = selectedStyle == "List",
                    onClick = { onStyleChange("List") },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle(title = "Sort")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsOptionButton(
                    text = "Default",
                    icon = { tint -> Icon(Icons.Default.FilterList, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
                    isSelected = selectedSort == "Default",
                    onClick = { onSortChange("Default") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Delay",
                    icon = { tint -> Icon(Icons.Default.Speed, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
                    isSelected = selectedSort == "Delay",
                    onClick = { onSortChange("Delay") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Name",
                    icon = { tint -> Icon(Icons.Default.SortByAlpha, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp)) },
                    isSelected = selectedSort == "Name",
                    onClick = { onSortChange("Name") },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle(title = "Layout")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsOptionButton(
                    text = "Loose",
                    isSelected = selectedLayout == "Loose",
                    onClick = { onLayoutChange("Loose") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Standard",
                    isSelected = selectedLayout == "Standard",
                    onClick = { onLayoutChange("Standard") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Tight",
                    isSelected = selectedLayout == "Tight",
                    onClick = { onLayoutChange("Tight") },
                    modifier = Modifier.weight(1f)
                )
            }

            SectionTitle(title = "Size")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                SettingsOptionButton(
                    text = "Standard",
                    isSelected = selectedSize == "Standard",
                    onClick = { onSizeChange("Standard") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Shrink",
                    isSelected = selectedSize == "Shrink",
                    onClick = { onSizeChange("Shrink") },
                    modifier = Modifier.weight(1f)
                )
                SettingsOptionButton(
                    text = "Min",
                    isSelected = selectedSize == "Min",
                    onClick = { onSizeChange("Min") },
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxiesHeader(
    isSearchActive: Boolean,
    onSearchActiveChange: (Boolean) -> Unit,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onSettingsClick: () -> Unit,
    onImportClick: () -> Unit,
    onAddTypeClick: (String) -> Unit,
    onDeleteInvalidClick: () -> Unit,
    onDeleteAllClick: () -> Unit,
    onExportClick: () -> Unit
) {
    var showAddMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isSearchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Search proxies...") },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color.Transparent,
                    unfocusedBorderColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                    focusedTextColor = MaterialTheme.colorScheme.onBackground
                ),
                trailingIcon = {
                    IconButton(onClick = {
                        onSearchActiveChange(false)
                        onSearchQueryChange("")
                    }) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Close Search")
                    }
                }
            )
        } else {
            Text(
                text = "Proxies",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onSearchActiveChange(true) }) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = "Search", tint = MaterialTheme.colorScheme.onBackground)
                }
                IconButton(onClick = { onSettingsClick() }) {
                    Icon(imageVector = Icons.Default.Settings, contentDescription = "Settings", tint = MaterialTheme.colorScheme.onBackground)
                }
                Box {
                    IconButton(onClick = { showAddMenu = true }) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add Proxy Menu", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(
                        expanded = showAddMenu,
                        onDismissRequest = { showAddMenu = false },
                        modifier = Modifier.background(Color(0xFF1E2228))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Import dari clipboard", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onImportClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add VMess", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("vmess")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add VLESS", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("vless")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Trojan", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("trojan")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Hysteria2", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("histeria2")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Shadowsocks", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("shadowsocks")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add Wireguard", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("wireguard")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add SOCKS", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("socks")
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Add HTTP", color = Color.White) },
                            leadingIcon = { Icon(Icons.Default.AddBox, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                            onClick = {
                                showAddMenu = false
                                onAddTypeClick("http")
                            }
                        )
                    }
                }
                Box {
                    IconButton(onClick = { showMoreMenu = true }) {
                        Icon(imageVector = Icons.Default.MoreVert, contentDescription = "More Options", tint = MaterialTheme.colorScheme.onBackground)
                    }
                    DropdownMenu(
                        expanded = showMoreMenu,
                        onDismissRequest = { showMoreMenu = false },
                        modifier = Modifier.background(Color(0xFF1E2228))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete invalid all configs", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onDeleteInvalidClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete all configs", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onDeleteAllClick()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Export all configs to clipboard", color = Color.White) },
                            onClick = {
                                showMoreMenu = false
                                onExportClick()
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SectionTitle(title: String) {
    Text(
        text = title,
        fontSize = 15.sp,
        fontWeight = FontWeight.SemiBold,
        color = Color(0xFF90A4AE),
        modifier = Modifier.padding(start = 4.dp, bottom = 10.dp)
    )
}

@Composable
fun SettingsOptionButton(
    text: String,
    icon: @Composable ((Color) -> Unit)? = null,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF1C3A5A) else Color(0xFF1E2228) // Subtle blue container vs dark grey background
    val borderColor = if (isSelected) Color(0xFF81D4FA) else Color(0xFF2E3238) // Highlight cyan border vs dark outline
    val textColor = if (isSelected) Color(0xFF81D4FA) else Color(0xFF90A4AE)

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .border(1.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            icon(textColor)
            Spacer(modifier = Modifier.width(6.dp))
        }
        Text(
            text = text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = textColor
        )
    }
}

data class ProxyItem(val name: String, val type: String, val latency: String, val isSelected: Boolean, val isGreen: Boolean = false, val profileName: String = "", val fullConfig: String = "")

@Composable
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
fun ProxyCard(
    proxy: ProxyItem,
    layout: String,
    size: String,
    isGrid: Boolean = false,
    onClick: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    var showShareMenu by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val containerColor = if (proxy.isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
    val outlineColor = Color.Transparent

    // Dynamic padding and gaps inside Card based on "Layout"
    val cardPadding = when (layout) {
        "Loose" -> 16.dp
        "Standard" -> 12.dp
        "Tight" -> 8.dp
        else -> 16.dp
    }
    val spacingBetween = when (layout) {
        "Loose" -> 8.dp
        "Standard" -> 6.dp
        "Tight" -> 4.dp
        else -> 8.dp
    }

    // Dynamic font size inside Card based on "Size"
    val titleSize = when (size) {
        "Standard" -> 16.sp
        "Shrink" -> 14.sp
        "Min" -> 12.sp
        else -> 12.sp
    }
    val metaSize = when (size) {
        "Standard" -> 14.sp
        "Shrink" -> 12.sp
        "Min" -> 11.sp
        else -> 11.sp
    }

    val cardShape = RoundedCornerShape(16.dp)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = { showShareMenu = true }
            )
            .border(1.dp, outlineColor, cardShape),
        shape = cardShape,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(cardPadding)
            ) {
                Text(
                    text = proxy.name,
                    fontSize = titleSize,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(spacingBetween))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(text = proxy.type, fontSize = metaSize, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        text = proxy.latency, 
                        fontSize = metaSize, 
                        fontWeight = FontWeight.Medium,
                        color = if (proxy.latency == "Timeout") Color(0xFFE57373) else if (proxy.isSelected || proxy.isGreen) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
                    )
                }
                
                if (proxy.isSelected) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onEdit) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { showQrDialog = true }) {
                            Icon(Icons.Default.QrCode, contentDescription = "QR Code", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = { 
                            clipboardManager.setText(AnnotatedString(XrayConfigGenerator.generateProxyUri(proxy)))
                            Toast.makeText(context, "Exported config to clipboard!", Toast.LENGTH_SHORT).show()
                        }) {
                            Icon(Icons.Default.ContentPaste, contentDescription = "Export Config", tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }

            DropdownMenu(
                expanded = showShareMenu,
                onDismissRequest = { showShareMenu = false },
                modifier = Modifier.background(MaterialTheme.colorScheme.surface)
            ) {
                DropdownMenuItem(
                    text = { Text("Edit", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showShareMenu = false; onEdit() }
                )
                DropdownMenuItem(
                    text = { Text("Delete", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showShareMenu = false; showDeleteDialog = true }
                )
                DropdownMenuItem(
                    text = { Text("QR Code", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.QrCode, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { showShareMenu = false; showQrDialog = true }
                )
                DropdownMenuItem(
                    text = { Text("Export Config", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = { 
                        showShareMenu = false
                        clipboardManager.setText(AnnotatedString(XrayConfigGenerator.generateProxyUri(proxy)))
                        Toast.makeText(context, "Exported config to clipboard!", Toast.LENGTH_SHORT).show()
                    }
                )
                DropdownMenuItem(
                    text = { Text("Export Full JSON", color = MaterialTheme.colorScheme.onSurface) },
                    leadingIcon = { Icon(Icons.Default.DataObject, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    onClick = {
                        showShareMenu = false
                        val fullConfigJson = try {
                            XrayConfigGenerator.generateConfig(
                                context = context,
                                overrideName = proxy.name,
                                overrideType = proxy.type,
                                overrideConfig = proxy.fullConfig
                            )
                        } catch (e: Exception) {
                            "Error generating config: ${e.message}"
                        }
                        clipboardManager.setText(AnnotatedString(fullConfigJson))
                        Toast.makeText(context, "Full Xray JSON config copied!", Toast.LENGTH_LONG).show()
                    }
                )
            }
        }
    }

    // QR Code Dialog
    if (showQrDialog) {
        val proxyUri = remember(proxy) { XrayConfigGenerator.generateProxyUri(proxy) }
        AlertDialog(
            onDismissRequest = { showQrDialog = false },
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

                    // Draw actual simulated QR Code
                    Box(
                        modifier = Modifier
                            .size(180.dp)
                            .background(Color.White, RoundedCornerShape(8.dp))
                            .padding(8.dp)
                            .align(Alignment.CenterHorizontally),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "QR Code Placeholder",
                            modifier = Modifier.size(120.dp),
                            tint = Color.Black
                        )
                    }

                    // Display truncated URI or base64 config with a copy button
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (proxyUri.length > 35) proxyUri.substring(0, 32) + "..." else proxyUri,
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(proxyUri))
                                    Toast.makeText(context, "Config copied to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ContentPaste,
                                    contentDescription = "Copy URI",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Text(
                        text = "Scan this QR code using another client app to import the profile easily.",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showQrDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text("Done")
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = {
                Text(
                    text = "Delete Proxy?",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFE57373)
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${proxy.name}'? This action cannot be undone.",
                    fontSize = 15.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE57373))
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.primary)
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }
}

@Composable
fun AddWireGuardDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_wireguard, null, false)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New WireGuard Server"
                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "51820"
                        val port = portStr.toIntOrNull() ?: 51820
                        val fullConfig = "wireguard://$address:$port#${java.net.URLEncoder.encode(name, "UTF-8")}"
                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddHysteria2Dialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_hysteria2, null, false)
                        
                        val spStreamSecurity = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)
                        val laySni = view.findViewById<android.view.View>(R.id.lay_sni)
                        val layAllowInsecure = view.findViewById<android.view.View>(R.id.lay_allow_insecure)

                        spStreamSecurity?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
                            override fun onItemSelected(parent: android.widget.AdapterView<*>?, viewItem: android.view.View?, position: Int, id: Long) {
                                val selected = parent?.getItemAtPosition(position)?.toString() ?: ""
                                if (selected == "none") {
                                    laySni?.visibility = android.view.View.GONE
                                    layAllowInsecure?.visibility = android.view.View.GONE
                                } else {
                                    laySni?.visibility = android.view.View.VISIBLE
                                    layAllowInsecure?.visibility = android.view.View.VISIBLE
                                }
                            }

                            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
                        }

                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Hysteria2 Server"
                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "443"
                        val port = portStr.toIntOrNull() ?: 443
                        val password = view.findViewById<EditText>(R.id.et_id)?.text?.toString() ?: ""
                        val obfsPassword = view.findViewById<EditText>(R.id.et_obfs_password)?.text?.toString() ?: ""
                        val obfs = view.findViewById<EditText>(R.id.et_obfs)?.text?.toString() ?: ""
                        val sni = view.findViewById<EditText>(R.id.et_sni)?.text?.toString() ?: ""
                        val insecure = view.findViewById<android.widget.Spinner>(R.id.sp_allow_insecure)?.selectedItem?.toString()?.lowercase() == "true"
                        
                        val queryParams = mutableListOf<String>()
                        if (sni.isNotBlank()) queryParams.add("sni=${java.net.URLEncoder.encode(sni, "UTF-8")}")
                        if (insecure) queryParams.add("insecure=1")
                        if (obfs.isNotBlank()) queryParams.add("obfs=${java.net.URLEncoder.encode(obfs, "UTF-8")}")
                        if (obfsPassword.isNotBlank()) queryParams.add("obfs-password=${java.net.URLEncoder.encode(obfsPassword, "UTF-8")}")
                        
                        val queryStr = if (queryParams.isNotEmpty()) "?" + queryParams.joinToString("&") else ""
                        val fullConfig = "hysteria2://$password@$address:$port$queryStr#${java.net.URLEncoder.encode(name, "UTF-8")}"
                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

fun setupTlsVisibility(view: android.view.View) {
    val spStreamSecurity = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)
    val laySni = view.findViewById<android.view.View>(R.id.lay_sni)
    val layStreamFingerprint = view.findViewById<android.view.View>(R.id.lay_stream_fingerprint)
    val layStreamAlpn = view.findViewById<android.view.View>(R.id.lay_stream_alpn)
    val layAllowInsecure = view.findViewById<android.view.View>(R.id.lay_allow_insecure)
    val layEchConfigList = view.findViewById<android.view.View>(R.id.lay_ech_config_list)
    val layEchForceQuery = view.findViewById<android.view.View>(R.id.lay_ech_force_query)
    val layPinnedCa256 = view.findViewById<android.view.View>(R.id.lay_pinned_ca256)
    val layPublicKey = view.findViewById<android.view.View>(R.id.lay_public_key)
    val layShortId = view.findViewById<android.view.View>(R.id.lay_short_id)
    val laySpiderX = view.findViewById<android.view.View>(R.id.lay_spider_x)
    val layMldsa65Verify = view.findViewById<android.view.View>(R.id.lay_mldsa65_verify)

    val updateVisibility = { selected: String ->
        when (selected) {
            "none" -> {
                laySni?.visibility = android.view.View.GONE
                layStreamFingerprint?.visibility = android.view.View.GONE
                layStreamAlpn?.visibility = android.view.View.GONE
                layAllowInsecure?.visibility = android.view.View.GONE
                layEchConfigList?.visibility = android.view.View.GONE
                layEchForceQuery?.visibility = android.view.View.GONE
                layPinnedCa256?.visibility = android.view.View.GONE
                layPublicKey?.visibility = android.view.View.GONE
                layShortId?.visibility = android.view.View.GONE
                laySpiderX?.visibility = android.view.View.GONE
                layMldsa65Verify?.visibility = android.view.View.GONE
            }
            "tls" -> {
                laySni?.visibility = android.view.View.VISIBLE
                layStreamFingerprint?.visibility = android.view.View.VISIBLE
                layStreamAlpn?.visibility = android.view.View.VISIBLE
                layAllowInsecure?.visibility = android.view.View.VISIBLE
                layEchConfigList?.visibility = android.view.View.VISIBLE
                layEchForceQuery?.visibility = android.view.View.VISIBLE
                layPinnedCa256?.visibility = android.view.View.VISIBLE
                
                layPublicKey?.visibility = android.view.View.GONE
                layShortId?.visibility = android.view.View.GONE
                laySpiderX?.visibility = android.view.View.GONE
                layMldsa65Verify?.visibility = android.view.View.GONE
            }
            "reality" -> {
                laySni?.visibility = android.view.View.VISIBLE
                layStreamFingerprint?.visibility = android.view.View.VISIBLE
                layStreamAlpn?.visibility = android.view.View.VISIBLE
                layPublicKey?.visibility = android.view.View.VISIBLE
                layShortId?.visibility = android.view.View.VISIBLE
                laySpiderX?.visibility = android.view.View.VISIBLE
                layMldsa65Verify?.visibility = android.view.View.VISIBLE
                
                layAllowInsecure?.visibility = android.view.View.GONE
                layEchConfigList?.visibility = android.view.View.GONE
                layEchForceQuery?.visibility = android.view.View.GONE
                layPinnedCa256?.visibility = android.view.View.GONE
            }
        }
    }

    spStreamSecurity?.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: android.widget.AdapterView<*>?, viewItem: android.view.View?, position: Int, id: Long) {
            val selected = parent?.getItemAtPosition(position)?.toString() ?: ""
            updateVisibility(selected)
        }

        override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
    }

    // Trigger initial state mapping based on whatever starts selected in the Spinner
    val initialSelected = spStreamSecurity?.selectedItem?.toString() ?: "none"
    updateVisibility(initialSelected)
}

@Composable
fun AddVmessDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_vmess, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Vmess Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "443"
                        val port = portStr.toIntOrNull() ?: 443
                        val uuid = view.findViewById<EditText>(R.id.et_uuid)?.text?.toString() ?: ""
                        val net = view.findViewById<android.widget.Spinner>(R.id.sp_network)?.selectedItem?.toString()?.lowercase() ?: "ws"
                        val security = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)?.selectedItem?.toString()?.lowercase() ?: "none"
                        val path = view.findViewById<EditText>(R.id.et_path)?.text?.toString() ?: ""
                        val host = view.findViewById<EditText>(R.id.et_request_host)?.text?.toString() ?: ""
                        val sni = view.findViewById<EditText>(R.id.et_sni)?.text?.toString() ?: ""

                        val json = JSONObject().apply {
                            put("v", "2")
                            put("ps", name)
                            put("add", address)
                            put("port", port)
                            put("id", uuid)
                            put("aid", 0)
                            put("scy", "auto")
                            put("net", net)
                            put("type", "none")
                            put("host", host)
                            put("path", path)
                            put("tls", security)
                            put("sni", sni)
                        }
                        val base64 = android.util.Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                        val fullConfig = "vmess://$base64"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddVlessDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_vless, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Vless Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "443"
                        val port = portStr.toIntOrNull() ?: 443
                        val uuid = view.findViewById<EditText>(R.id.et_uuid_vless)?.text?.toString() ?: ""
                        val net = view.findViewById<android.widget.Spinner>(R.id.sp_network)?.selectedItem?.toString()?.lowercase() ?: "ws"
                        val security = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)?.selectedItem?.toString()?.lowercase() ?: "none"
                        val path = view.findViewById<EditText>(R.id.et_path)?.text?.toString() ?: ""
                        val host = view.findViewById<EditText>(R.id.et_request_host)?.text?.toString() ?: ""
                        val sni = view.findViewById<EditText>(R.id.et_sni)?.text?.toString() ?: ""

                        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                        val encodedHost = java.net.URLEncoder.encode(host, "UTF-8")
                        val encodedSni = java.net.URLEncoder.encode(sni, "UTF-8")
                        val fullConfig = "vless://$uuid@$address:$port?security=$security&type=$net&path=$encodedPath&host=$encodedHost&sni=$encodedSni#${java.net.URLEncoder.encode(name, "UTF-8")}"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddTrojanDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_trojan, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Trojan Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "443"
                        val port = portStr.toIntOrNull() ?: 443
                        val password = view.findViewById<EditText>(R.id.et_password_trojan)?.text?.toString() ?: ""
                        val net = view.findViewById<android.widget.Spinner>(R.id.sp_network)?.selectedItem?.toString()?.lowercase() ?: "ws"
                        val security = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)?.selectedItem?.toString()?.lowercase() ?: "none"
                        val path = view.findViewById<EditText>(R.id.et_path)?.text?.toString() ?: ""
                        val host = view.findViewById<EditText>(R.id.et_request_host)?.text?.toString() ?: ""
                        val sni = view.findViewById<EditText>(R.id.et_sni)?.text?.toString() ?: ""

                        val encodedPath = java.net.URLEncoder.encode(path, "UTF-8")
                        val encodedHost = java.net.URLEncoder.encode(host, "UTF-8")
                        val encodedSni = java.net.URLEncoder.encode(sni, "UTF-8")
                        val fullConfig = "trojan://$password@$address:$port?security=$security&type=$net&path=$encodedPath&host=$encodedHost&sni=$encodedSni#${java.net.URLEncoder.encode(name, "UTF-8")}"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddShadowsocksDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_shadowsocks, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Shadowsocks Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "443"
                        val port = portStr.toIntOrNull() ?: 443
                        val method = view.findViewById<android.widget.Spinner>(R.id.sp_method_ss)?.selectedItem?.toString() ?: "aes-128-gcm"
                        val password = view.findViewById<EditText>(R.id.et_password_ss)?.text?.toString() ?: ""

                        // Shadowsocks standard format: ss://Base64(method:password)@address:port#remarks
                        val rawUserInfo = "$method:$password"
                        val base64UserInfo = android.util.Base64.encodeToString(rawUserInfo.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP or android.util.Base64.URL_SAFE)
                        val fullConfig = "ss://$base64UserInfo@$address:$port#${java.net.URLEncoder.encode(name, "UTF-8")}"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddSocksDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_socks, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New Socks Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "1080"
                        val port = portStr.toIntOrNull() ?: 1080
                        val username = view.findViewById<EditText>(R.id.et_username_socks)?.text?.toString() ?: ""
                        val password = view.findViewById<EditText>(R.id.et_password_socks)?.text?.toString() ?: ""
                        val security = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)?.selectedItem?.toString()?.lowercase() ?: "none"

                        // Socks5 format: socks5://[username:password@]address:port?security=none
                        val authPart = if (username.isNotBlank() || password.isNotBlank()) "$username:$password@" else ""
                        val fullConfig = "socks5://$authPart$address:$port?security=$security#${java.net.URLEncoder.encode(name, "UTF-8")}"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun AddHttpDialog(
    onDismiss: () -> Unit,
    onSave: (name: String, fullConfig: String) -> Unit
) {
    var rawView by remember { mutableStateOf<android.view.View?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        text = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 450.dp)
            ) {
                AndroidView(
                    modifier = Modifier.fillMaxWidth(),
                    factory = { context ->
                        val view = LayoutInflater.from(context).inflate(R.layout.layout_http, null, false)
                        setupTlsVisibility(view)
                        rawView = view
                        view
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val view = rawView
                    if (view != null) {
                        val etRemarks = view.findViewById<EditText>(R.id.et_remarks)
                        val remarksText = etRemarks?.text?.toString() ?: ""
                        val name = if (remarksText.isNotBlank()) remarksText else "New HTTP Server"

                        val address = view.findViewById<EditText>(R.id.et_address)?.text?.toString() ?: ""
                        val portStr = view.findViewById<EditText>(R.id.et_port)?.text?.toString() ?: "80"
                        val port = portStr.toIntOrNull() ?: 80
                        val username = view.findViewById<EditText>(R.id.et_username_http)?.text?.toString() ?: ""
                        val password = view.findViewById<EditText>(R.id.et_password_http)?.text?.toString() ?: ""
                        val security = view.findViewById<android.widget.Spinner>(R.id.sp_stream_security)?.selectedItem?.toString()?.lowercase() ?: "none"

                        // HTTP format: http://[username:password@]address:port?security=none
                        val authPart = if (username.isNotBlank() || password.isNotBlank()) "$username:$password@" else ""
                        val fullConfig = "http://$authPart$address:$port?security=$security#${java.net.URLEncoder.encode(name, "UTF-8")}"

                        onSave(name, fullConfig)
                    } else {
                        onDismiss()
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Save", color = MaterialTheme.colorScheme.onPrimary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = Color(0xFF1E2228),
        shape = RoundedCornerShape(20.dp)
    )
}



