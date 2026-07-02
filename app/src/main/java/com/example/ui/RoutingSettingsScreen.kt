package com.example.ui

import com.example.viewmodel.AppViewModel
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import androidx.compose.foundation.isSystemInDarkTheme
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog

data class RoutingRule(
    val id: String = java.util.UUID.randomUUID().toString(),
    val name: String,
    val value: String = "",
    val isLocked: Boolean = false,
    val domain: String = "",
    val ip: String = "",
    val port: String = "",
    val protocol: String = "",
    val network: String = "",
    val outbound: String, // "direct", "proxy", "block"
    val isEnabled: Boolean = true
) {
    val displayValue: String
        get() {
            if (value.isNotBlank()) return value
            val parts = mutableListOf<String>()
            if (domain.isNotBlank()) parts.add(domain)
            if (ip.isNotBlank()) parts.add(ip)
            if (port.isNotBlank()) parts.add(port)
            if (protocol.isNotBlank()) parts.add(protocol)
            if (network.isNotBlank()) parts.add(network)
            return parts.joinToString(", ").ifBlank { "no criteria" }
        }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingSettingsScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    val context = LocalContext.current
    val sharedPrefs = remember { context.getSharedPreferences("zivpn_routing_settings", Context.MODE_PRIVATE) }
    
    // Domain strategy state
    var domainStrategy by remember { mutableStateOf(sharedPrefs.getString("domainStrategy", "AsIs") ?: "AsIs") }
    var showOutboundDialog by remember { mutableStateOf(false) }
    var showStrategyDialog by remember { mutableStateOf(false) }

    // Dropdown menu state
    var showMoreMenu by remember { mutableStateOf(false) }

    // Active routing rules state
    val routingRules = remember {
        val rulesList = mutableStateListOf<RoutingRule>()
        val rulesStr = sharedPrefs.getString("routing_rules_json", null)
        if (rulesStr != null) {
            try {
                val arr = JSONArray(rulesStr)
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    rulesList.add(RoutingRule(
                        id = obj.optString("id", java.util.UUID.randomUUID().toString()),
                        name = obj.optString("name", ""),
                        value = obj.optString("value", ""),
                        isLocked = obj.optBoolean("isLocked", false),
                        domain = obj.optString("domain", ""),
                        ip = obj.optString("ip", ""),
                        port = obj.optString("port", ""),
                        protocol = obj.optString("protocol", ""),
                        network = obj.optString("network", ""),
                        outbound = obj.optString("outbound", "proxy"),
                        isEnabled = obj.optBoolean("isEnabled", true)
                    ))
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (rulesList.isEmpty()) {
            rulesList.addAll(listOf(
                RoutingRule(name = "阻断udp443", value = "443", outbound = "block", isEnabled = true),
                RoutingRule(name = "代理Google", value = "[geosite:google]", outbound = "proxy", isEnabled = true),
                RoutingRule(name = "绕过局域网IP", value = "[geoip:private]", outbound = "direct", isEnabled = true),
                RoutingRule(name = "绕过局域网域名", value = "[geosite:private]", outbound = "direct", isEnabled = true),
                RoutingRule(name = "绕过中国公共DNSIP", value = "[geoip:cn]", outbound = "direct", isEnabled = true)
            ))
        }
        rulesList
    }

    val saveRules = {
        val arr = JSONArray()
        for (rule in routingRules) {
            val obj = JSONObject().apply {
                put("id", rule.id)
                put("name", rule.name)
                put("value", rule.value)
                put("isLocked", rule.isLocked)
                put("domain", rule.domain)
                put("ip", rule.ip)
                put("port", rule.port)
                put("protocol", rule.protocol)
                put("network", rule.network)
                put("outbound", rule.outbound)
                put("isEnabled", rule.isEnabled)
            }
            arr.put(obj)
        }
        sharedPrefs.edit().putString("routing_rules_json", arr.toString()).apply()
    }

    // Dialog flags
    var isAddingRule by remember { mutableStateOf(false) }
    var ruleToEdit by remember { mutableStateOf<RoutingRule?>(null) }
    
    // Clipboard simulation
    var showClipboardImportDialog by remember { mutableStateOf(false) }
    var clipboardContent by remember { mutableStateOf("") }

    // QRcode simulation
    var showQrImportDialog by remember { mutableStateOf(false) }
    var qrCodeContent by remember { mutableStateOf("") }

    val themeMode = viewModel.selectedThemeMode
    val isLightTheme = themeMode == "Light" || (themeMode == "Auto" && !isSystemInDarkTheme())

    val backgroundBrush = if (isLightTheme) {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFFFFFFF),
                Color(0xFFFFFFFF),
                Color(0xFF87CEEB)
            )
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFF14171E),
                Color(0xFF14171E)
            )
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundBrush)
    ) {
        if (isAddingRule) {
            RoutingRuleEditScreen(
                rule = null,
                onBack = { isAddingRule = false },
                onSave = { newRule ->
                    routingRules.add(0, newRule)
                    saveRules()
                    isAddingRule = false
                    Toast.makeText(context, "Routing rule added!", Toast.LENGTH_SHORT).show()
                }
            )
        } else if (ruleToEdit != null) {
            RoutingRuleEditScreen(
                rule = ruleToEdit,
                onBack = { ruleToEdit = null },
                onSave = { updatedRule ->
                    val idx = routingRules.indexOfFirst { it.id == updatedRule.id }
                    if (idx != -1) {
                        routingRules[idx] = updatedRule
                    }
                    saveRules()
                    ruleToEdit = null
                    Toast.makeText(context, "Routing rule updated!", Toast.LENGTH_SHORT).show()
                },
                onDelete = {
                    routingRules.removeIf { it.id == ruleToEdit?.id }
                    saveRules()
                    ruleToEdit = null
                    Toast.makeText(context, "Routing rule deleted!", Toast.LENGTH_SHORT).show()
                }
            )
        } else {
            Scaffold(
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = "Routing Settings",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = Color.White
                                )
                            }
                        },
                        actions = {
                            // Add Button
                            IconButton(onClick = { isAddingRule = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Rule",
                            tint = Color.White
                        )
                    }
                    // More Button
                    Box {
                        IconButton(onClick = { showMoreMenu = true }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = "Menu Options",
                                tint = Color.White
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            modifier = Modifier.background(Color(0xFF262A30))
                        ) {
                            DropdownMenuItem(
                                text = { Text("Import predefined rulesets", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.Download, contentDescription = null, tint = Color(0xFF03A9F4)) },
                                onClick = {
                                    showMoreMenu = false
                                    // Append standard helpful rulesets
                                    val adBlock = RoutingRule(name = "Block Ads", value = "[geosite:category-ads-all]", outbound = "block")
                                    val appleDirect = RoutingRule(name = "Apple Direct", value = "[geosite:apple]", outbound = "direct")
                                    val netflixProxy = RoutingRule(name = "Netflix Proxy", value = "[geosite:netflix]", outbound = "proxy")
                                    
                                    routingRules.add(adBlock)
                                    routingRules.add(appleDirect)
                                    routingRules.add(netflixProxy)
                                    saveRules()
                                    
                                    Toast.makeText(context, "Predefined rulesets successfully imported!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import ruleset from clipboard", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ContentPaste, contentDescription = null, tint = Color(0xFF03A9F4)) },
                                onClick = {
                                    showMoreMenu = false
                                    val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                    val primaryClip = clipboardManager.primaryClip
                                    val realContent = if (primaryClip != null && primaryClip.itemCount > 0) {
                                        primaryClip.getItemAt(0).text?.toString() ?: ""
                                    } else {
                                        ""
                                    }
                                    clipboardContent = if (realContent.isNotBlank()) {
                                        realContent
                                    } else {
                                        "{\"name\":\"Clipboard Rule\",\"value\":\"[geosite:custom]\",\"outbound\":\"proxy\"}"
                                    }
                                    showClipboardImportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Import ruleset from QRcode", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.QrCodeScanner, contentDescription = null, tint = Color(0xFF03A9F4)) },
                                onClick = {
                                    showMoreMenu = false
                                    qrCodeContent = "{\"name\":\"QR Code Rule\",\"value\":\"[geoip:custom-range]\",\"outbound\":\"direct\"}"
                                    showQrImportDialog = true
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Export ruleset to clipboard", color = Color.White) },
                                leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = Color(0xFF81D4FA)) },
                                onClick = {
                                    showMoreMenu = false
                                    try {
                                        val jsonArray = org.json.JSONArray()
                                        routingRules.forEach { rule ->
                                            jsonArray.put(org.json.JSONObject().apply {
                                                put("name", rule.name)
                                                put("value", rule.value)
                                                put("outbound", rule.outbound)
                                            })
                                        }
                                        val rulesJson = jsonArray.toString(4)
                                        val clipboardManager = context.getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                                        val clipData = android.content.ClipData.newPlainText("v2rayA Routing Rules", rulesJson)
                                        clipboardManager.setPrimaryClip(clipData)
                                        Toast.makeText(context, "Berhasil mengekspor ${routingRules.size} aturan routing ke Clipboard!", Toast.LENGTH_LONG).show()
                                    } catch (e: java.lang.Exception) {
                                        Toast.makeText(context, "Gagal mengekspor: ${e.message}", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF14171A)
                )
            )
        },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
            // 1. Domain Strategy Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showStrategyDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Domain strategy",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = domainStrategy,
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp
                )
            }

            HorizontalDivider(color = Color(0xFF23282F), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))

            // Outbound Mode Row
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { showOutboundDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Text(
                    text = "Global Outbound Mode",
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = viewModel.globalOutboundMode,
                    color = Color(0xFF90A4AE),
                    fontSize = 14.sp
                )
            }

            // Divider or category section matching screenshot style
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Routing Rule Settings",
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            // 2. Rules list
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                items(routingRules, key = { it.id }) { rule ->
                    RoutingRuleItem(
                        rule = rule,
                        onToggle = { isEnabled ->
                            val index = routingRules.indexOfFirst { it.id == rule.id }
                            if (index != -1) {
                                routingRules[index] = rule.copy(isEnabled = isEnabled)
                                saveRules()
                            }
                        },
                        onEditClick = {
                            ruleToEdit = rule
                        }
                    )
                    HorizontalDivider(color = Color(0xFF23282F), thickness = 1.dp, modifier = Modifier.padding(horizontal = 16.dp))
                }
            }
        }
    }
}

    // Domain Strategy Selector Dialog
    if (showStrategyDialog) {
        val strategies = listOf("AsIs", "IPIfNonMatch", "IPOnDemand")
        AlertDialog(
            onDismissRequest = { showStrategyDialog = false },
            title = { Text("Select Domain Strategy", color = Color.White) },
            text = {
                Column {
                    strategies.forEach { strategy ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    domainStrategy = strategy
                                    sharedPrefs.edit().putString("domainStrategy", strategy).apply()
                                    showStrategyDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (domainStrategy == strategy),
                                onClick = {
                                    domainStrategy = strategy
                                    sharedPrefs.edit().putString("domainStrategy", strategy).apply()
                                    showStrategyDialog = false
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF03A9F4),
                                    unselectedColor = Color(0xFF90A4AE)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = strategy, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStrategyDialog = false }) {
                    Text("Cancel", color = Color(0xFF03A9F4))
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // Outbound Mode Selector Dialog
    if (showOutboundDialog) {
        val options = listOf("Proxy", "Block", "Direct")
        AlertDialog(
            onDismissRequest = { showOutboundDialog = false },
            title = { Text("Select Global Outbound Mode", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    options.forEach { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateOutboundMode(option)
                                    showOutboundDialog = false
                                    Toast.makeText(context, "Outbound mode set to: $option", Toast.LENGTH_SHORT).show()
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = (viewModel.globalOutboundMode == option),
                                onClick = {
                                    viewModel.updateOutboundMode(option)
                                    showOutboundDialog = false
                                    Toast.makeText(context, "Outbound mode set to: $option", Toast.LENGTH_SHORT).show()
                                },
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = Color(0xFF03A9F4),
                                    unselectedColor = Color(0xFF90A4AE)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(text = option, color = Color.White, fontSize = 16.sp)
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showOutboundDialog = false }) {
                    Text("Batal", color = Color(0xFF03A9F4))
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }



    // Clipboard Import Dialog
    if (showClipboardImportDialog) {
        AlertDialog(
            onDismissRequest = { showClipboardImportDialog = false },
            title = { Text("Import from Clipboard", color = Color.White) },
            text = {
                Column {
                    Text("Found ruleset metadata in clipboard:", color = Color(0xFFB0C1D4), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = clipboardContent,
                        onValueChange = { clipboardContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val obj = org.json.JSONObject(clipboardContent)
                            val name = obj.optString("name", "Imported Rule")
                            val value = obj.optString("value", "")
                            val outbound = obj.optString("outbound", "proxy")
                            if (value.isNotEmpty()) {
                                routingRules.add(0, RoutingRule(name = name, value = value, outbound = outbound))
                                Toast.makeText(context, "Ruleset imported successfully!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid rule format", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to parse JSON", Toast.LENGTH_SHORT).show()
                        }
                        showClipboardImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                ) {
                    Text("Import", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClipboardImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }

    // QR Code Import Dialog
    if (showQrImportDialog) {
        AlertDialog(
            onDismissRequest = { showQrImportDialog = false },
            title = { Text("Scan QR Code", color = Color.White) },
            text = {
                Column {
                    Text("Scanned contents from QR Code:", color = Color(0xFFB0C1D4), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = qrCodeContent,
                        onValueChange = { qrCodeContent = it },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        try {
                            val obj = org.json.JSONObject(qrCodeContent)
                            val name = obj.optString("name", "QR Rule")
                            val value = obj.optString("value", "")
                            val outbound = obj.optString("outbound", "direct")
                            if (value.isNotEmpty()) {
                                routingRules.add(0, RoutingRule(name = name, value = value, outbound = outbound))
                                Toast.makeText(context, "Ruleset imported from QR Code!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Invalid rule format", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            Toast.makeText(context, "Failed to parse JSON", Toast.LENGTH_SHORT).show()
                        }
                        showQrImportDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF03A9F4))
                ) {
                    Text("Import", color = Color.Black)
                }
            },
            dismissButton = {
                TextButton(onClick = { showQrImportDialog = false }) {
                    Text("Cancel", color = Color(0xFF90A4AE))
                }
            },
            containerColor = Color(0xFF1E2228),
            shape = RoundedCornerShape(20.dp)
        )
    }
    }
}

@Composable
fun RoutingRuleItem(
    rule: RoutingRule,
    onToggle: (Boolean) -> Unit,
    onEditClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = rule.name,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = rule.displayValue,
                color = Color(0xFF90A4AE),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = rule.outbound,
                color = when (rule.outbound) {
                    "proxy" -> Color(0xFF90CAF9)
                    "block" -> Color(0xFFEF5350)
                    else -> Color(0xFFA5D6A7) // direct
                },
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Edit pencil button
        IconButton(onClick = onEditClick) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Edit Routing Rule",
                tint = Color(0xFF90A4AE),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Switch button
        Switch(
            checked = rule.isEnabled,
            onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF03A9F4),
                uncheckedThumbColor = Color(0xFF78909C),
                uncheckedTrackColor = Color(0xFF262A30),
                checkedBorderColor = Color.Transparent,
                uncheckedBorderColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RoutingRuleEditScreen(
    rule: RoutingRule?, // null means adding new rule
    onBack: () -> Unit,
    onSave: (RoutingRule) -> Unit,
    onDelete: () -> Unit = {}
) {
    var remarks by remember { mutableStateOf(rule?.name ?: "") }
    var isLocked by remember { mutableStateOf(rule?.isLocked ?: false) }
    var domain by remember { mutableStateOf(rule?.domain ?: "") }
    var ip by remember { mutableStateOf(rule?.ip ?: "") }
    var port by remember { mutableStateOf(rule?.port ?: "") }
    var protocol by remember { mutableStateOf(rule?.protocol ?: "") }
    var network by remember { mutableStateOf(rule?.network ?: "") }
    var outbound by remember { mutableStateOf(rule?.outbound ?: "proxy") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Routing Rule Settings",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        onSave(
                            RoutingRule(
                                id = rule?.id ?: java.util.UUID.randomUUID().toString(),
                                name = remarks,
                                isLocked = isLocked,
                                domain = domain,
                                ip = ip,
                                port = port,
                                protocol = protocol,
                                network = network,
                                outbound = outbound,
                                isEnabled = rule?.isEnabled ?: true
                            )
                        )
                    }) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Save",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        },
        containerColor = Color.Transparent
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            // 1. remarks
            UnderlineInputField(
                label = "remarks",
                value = remarks,
                onValueChange = { remarks = it }
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 2. Locked switch row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Locked, keep this rule when import presets",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Normal
                )
                Switch(
                    checked = isLocked,
                    onCheckedChange = { isLocked = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = Color(0xFF03A9F4),
                        uncheckedThumbColor = Color(0xFF78909C),
                        uncheckedTrackColor = Color(0xFF262A30),
                        checkedBorderColor = Color.Transparent,
                        uncheckedBorderColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(18.dp))

            // 3. Header: Routing Rule Settings
            Text(
                text = "Routing Rule Settings",
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 4. domain
            UnderlineInputField(
                label = "domain",
                value = domain,
                onValueChange = { domain = it },
                placeholder = "Separated by commas(,), choose domain or ip"
            )

            // 5. ip
            UnderlineInputField(
                label = "ip",
                value = ip,
                onValueChange = { ip = it },
                placeholder = "Separated by commas(,), choose domain or ip"
            )

            // 6. port
            UnderlineInputField(
                label = "port",
                value = port,
                onValueChange = { port = it }
            )

            // 7. protocol
            UnderlineInputField(
                label = "protocol",
                value = protocol,
                onValueChange = { protocol = it },
                placeholder = "[http,tls,bittorrent]"
            )

            // 8. network
            UnderlineInputField(
                label = "network",
                value = network,
                onValueChange = { network = it },
                placeholder = "[udp|tcp]"
            )

            Spacer(modifier = Modifier.height(10.dp))

            // 9. outboundTag selection
            OutboundDropdownSelector(
                selectedOutbound = outbound,
                onSelect = { outbound = it }
            )

            if (rule != null) {
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onDelete,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete Rule", color = Color.White, fontSize = 16.sp)
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun UnderlineInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = label,
            color = Color(0xFF90A4AE),
            fontSize = 14.sp
        )
        TextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { 
                if (placeholder.isNotEmpty()) {
                    Text(text = placeholder, color = Color(0xFF5F6D7E), fontSize = 15.sp) 
                }
            },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color(0xFF03A9F4),
                unfocusedIndicatorColor = Color(0xFF455A64),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                cursorColor = Color(0xFF03A9F4)
            ),
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = (-4).dp)
        )
    }
}

@Composable
fun OutboundDropdownSelector(
    selectedOutbound: String,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = listOf("proxy", "direct", "block")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = true }
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = "outboundTag",
            color = Color(0xFF90A4AE),
            fontSize = 14.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = selectedOutbound,
                color = Color.White,
                fontSize = 16.sp
            )
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Dropdown Selection",
                tint = Color(0xFF90A4AE)
            )
        }
        HorizontalDivider(color = Color(0xFF455A64), thickness = 1.dp, modifier = Modifier.padding(top = 4.dp))

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .background(Color.White)
        ) {
            options.forEach { opt ->
                DropdownMenuItem(
                    text = { Text(text = opt, color = Color.Black) },
                    onClick = {
                        onSelect(opt)
                        expanded = false
                    }
                )
            }
        }
    }
}
