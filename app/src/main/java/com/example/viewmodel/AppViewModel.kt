package com.example.viewmodel

import com.example.dto.DiagnosticLog
import com.example.util.AppDatabase
import com.example.util.DiagnosticLogRepository
import com.example.ui.ProfileItem
import com.example.ui.ProxyItem
import android.app.Application
import android.content.Context
import org.json.JSONObject
import org.json.JSONArray
import android.util.Base64
import java.net.URLDecoder
import androidx.lifecycle.AndroidViewModel
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.Credentials

import com.google.gson.Gson
import java.io.InputStream
import java.io.OutputStream

data class BackupData(
    val profiles: List<ProfileItem>,
    val proxies: List<ProxyItem>
)

class AppViewModel(application: Application) : AndroidViewModel(application) {
    private val database = AppDatabase.getDatabase(application)
    private val logRepository = DiagnosticLogRepository(database.diagnosticLogDao())

    val diagnosticLogs: StateFlow<List<DiagnosticLog>> = logRepository.allLogs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun insertDiagnosticLog(command: String, output: String, isSuccess: Boolean) {
        viewModelScope.launch {
            val log = DiagnosticLog(command = command, output = output, isSuccess = isSuccess)
            logRepository.insert(log)
        }
    }

    fun deleteDiagnosticLog(id: Int) {
        viewModelScope.launch {
            logRepository.deleteById(id)
        }
    }

    fun clearAllDiagnosticLogs() {
        viewModelScope.launch {
            logRepository.clearAll()
        }
    }

    val profiles = mutableStateListOf<ProfileItem>()
    
    val proxies = mutableStateListOf<ProxyItem>()
    
    val activeProfileName: String
        get() = profiles.find { it.isSelected }?.name ?: ""

    // Hysteria Local Config for Tools navigation
    var hysteriaProfileNameText: String = ""
    var hysteriaServerIpText: String = ""
    var hysteriaPortRangeText: String = "6000-19999"
    var hysteriaMtuText: String = "9000"
    var hysteriaPasswordText: String = ""
    var hysteriaObfsText: String = "hu`hqb`c"
    var hysteriaBufferSizeText: String = "1.0x (Default)"
    var hysteriaSocksAddressText: String = "127.0.0.1"
    var hysteriaSocksPortText: String = "7777"
    var hysteriaSocksUdpVal: Boolean = false
    var hysteriaAutoStartOnBootVal: Boolean = false

    // Shizuku Settings
    var shizukuUseForCore: Boolean = false
    var shizukuRoutingMode: String = "VpnService" // options: "VpnService", "Transparent Proxy"
    var shizukuBypassDns: Boolean = false
    var shizukuListenPort: String = "10808"

    // Outbound Mode State
    var globalOutboundMode by mutableStateOf("Proxy")

    // Theme Mode State
    var selectedThemeMode by mutableStateOf("Dark") // options: "Auto", "Light", "Dark"

    // Backup & Recovery State
    var webdavAddress by mutableStateOf("")
    var webdavAccount by mutableStateOf("")
    var webdavPassword by mutableStateOf("")
    var webdavIsBound by mutableStateOf(false)
    var recoveryStrategy by mutableStateOf("Compatible") // "Compatible" or "Override"

    init {
        loadHysteriaConfig(application)
        loadShizukuConfig(application)
        loadOutboundMode()
        loadThemeMode()
        loadBackupConfig(application)
        syncHysteriaProxiesList()
        loadActiveProxy(application)
    }

    fun loadActiveProxy(context: Context) {
        val sharedPrefs = context.getSharedPreferences("sicepat_active_proxy", Context.MODE_PRIVATE)
        val name = sharedPrefs.getString("name", "") ?: ""
        val profileName = sharedPrefs.getString("profileName", "") ?: ""
        if (name.isNotEmpty()) {
            val idx = proxies.indexOfFirst { it.name == name && (profileName.isEmpty() || it.profileName == profileName) }
            if (idx != -1) {
                for (i in proxies.indices) {
                    proxies[i] = proxies[i].copy(isSelected = (i == idx))
                }
            }
        }
    }

    fun syncHysteriaProxiesList() {
        val name = if (hysteriaProfileNameText.isNotBlank()) hysteriaProfileNameText else "ZIVPN-Core"
        val activeProfile = activeProfileName
        
        // Remove existing items with name "ZIVPN-Core" or same name/profileName to prevent duplicate cards
        proxies.removeAll { it.name == "ZIVPN-Core" || (it.name == name && it.profileName == activeProfile) }
        
        val socksProxy = ProxyItem(
            name = name,
            type = "Socks5",
            latency = "- ms",
            isSelected = true,
            isGreen = true,
            profileName = activeProfile
        )
        
        // De-select other proxy entries to keep this one selected
        for (i in proxies.indices) {
            proxies[i] = proxies[i].copy(isSelected = false)
        }
        
        proxies.add(0, socksProxy)
    }

    fun saveHysteriaConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_hysteria_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putString("profile_name", hysteriaProfileNameText)
            putString("server_ip", hysteriaServerIpText)
            putString("port_range", hysteriaPortRangeText)
            putString("mtu", hysteriaMtuText)
            putString("password", hysteriaPasswordText)
            putString("obfs", hysteriaObfsText)
            putString("buffer_size", hysteriaBufferSizeText)
            putString("socks_address", hysteriaSocksAddressText)
            putString("socks_port", hysteriaSocksPortText)
            putBoolean("socks_udp", hysteriaSocksUdpVal)
            putBoolean("auto_start", hysteriaAutoStartOnBootVal)
            apply()
        }
        syncHysteriaProxiesList()
    }

    fun loadHysteriaConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_hysteria_settings", Context.MODE_PRIVATE)
        hysteriaProfileNameText = sharedPrefs.getString("profile_name", "") ?: ""
        hysteriaServerIpText = sharedPrefs.getString("server_ip", "") ?: ""
        hysteriaPortRangeText = sharedPrefs.getString("port_range", "6000-19999") ?: "6000-19999"
        hysteriaMtuText = sharedPrefs.getString("mtu", "9000") ?: "9000"
        hysteriaPasswordText = sharedPrefs.getString("password", "") ?: ""
        hysteriaObfsText = sharedPrefs.getString("obfs", "hu`hqb`c") ?: "hu`hqb`c"
        hysteriaBufferSizeText = sharedPrefs.getString("buffer_size", "1.0x (Default)") ?: "1.0x (Default)"
        hysteriaSocksAddressText = sharedPrefs.getString("socks_address", "127.0.0.1") ?: "127.0.0.1"
        hysteriaSocksPortText = sharedPrefs.getString("socks_port", "7777") ?: "7777"
        hysteriaSocksUdpVal = sharedPrefs.getBoolean("socks_udp", false)
        hysteriaAutoStartOnBootVal = sharedPrefs.getBoolean("auto_start", false)
    }

    fun saveShizukuConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().apply {
            putBoolean("use_for_core", shizukuUseForCore)
            putString("routing_mode", shizukuRoutingMode)
            putBoolean("bypass_dns", shizukuBypassDns)
            putString("listen_port", shizukuListenPort)
            apply()
        }
    }

    fun loadShizukuConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE)
        shizukuUseForCore = sharedPrefs.getBoolean("use_for_core", false)
        shizukuRoutingMode = sharedPrefs.getString("routing_mode", "VpnService") ?: "VpnService"
        shizukuBypassDns = sharedPrefs.getBoolean("bypass_dns", false)
        shizukuListenPort = sharedPrefs.getString("listen_port", "10808") ?: "10808"
    }

    fun loadOutboundMode() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("zivpn_routing_settings", Context.MODE_PRIVATE)
        globalOutboundMode = sharedPrefs.getString("global_outbound_mode", "Proxy") ?: "Proxy"
    }

    fun updateOutboundMode(mode: String) {
        globalOutboundMode = mode
        val sharedPrefs = getApplication<Application>().getSharedPreferences("zivpn_routing_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("global_outbound_mode", mode).apply()
    }

    fun loadThemeMode() {
        val sharedPrefs = getApplication<Application>().getSharedPreferences("zivpn_theme_settings", Context.MODE_PRIVATE)
        selectedThemeMode = sharedPrefs.getString("theme_mode", "Dark") ?: "Dark"
    }

    fun updateThemeMode(mode: String) {
        selectedThemeMode = mode
        val sharedPrefs = getApplication<Application>().getSharedPreferences("zivpn_theme_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit().putString("theme_mode", mode).apply()
    }

    fun loadBackupConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_backup_settings", Context.MODE_PRIVATE)
        webdavAddress = sharedPrefs.getString("webdav_address", "") ?: ""
        webdavAccount = sharedPrefs.getString("webdav_account", "") ?: ""
        webdavPassword = sharedPrefs.getString("webdav_password", "") ?: ""
        webdavIsBound = sharedPrefs.getBoolean("webdav_is_bound", false)
        recoveryStrategy = sharedPrefs.getString("recovery_strategy", "Compatible") ?: "Compatible"
    }

    fun saveBackupConfig(context: Context) {
        val sharedPrefs = context.getSharedPreferences("zivpn_backup_settings", Context.MODE_PRIVATE)
        sharedPrefs.edit()
            .putString("webdav_address", webdavAddress)
            .putString("webdav_account", webdavAccount)
            .putString("webdav_password", webdavPassword)
            .putBoolean("webdav_is_bound", webdavIsBound)
            .putString("recovery_strategy", recoveryStrategy)
            .apply()
    }

    fun bindWebdav(context: Context, address: String, account: String, password: String) {
        webdavAddress = address
        webdavAccount = account
        webdavPassword = password
        webdavIsBound = true
        saveBackupConfig(context)
    }

    fun unbindWebdav(context: Context) {
        webdavAddress = ""
        webdavAccount = ""
        webdavPassword = ""
        webdavIsBound = false
        saveBackupConfig(context)
    }

    fun updateRecoveryStrategy(context: Context, strategy: String) {
        recoveryStrategy = strategy
        saveBackupConfig(context)
    }

    fun syncBackupToWebdav(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!webdavIsBound || webdavAddress.isBlank()) {
                onResult(false, "WebDAV is not configured or bound.")
                return@launch
            }

            val backup = BackupData(
                profiles = profiles.toList(),
                proxies = proxies.toList()
            )
            val json = Gson().toJson(backup)

            val targetUrl = when {
                webdavAddress.endsWith(".json", ignoreCase = true) -> webdavAddress
                webdavAddress.endsWith("/") -> "${webdavAddress}sicepatxray_backup.json"
                else -> "${webdavAddress}/sicepatxray_backup.json"
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val credential = Credentials.basic(webdavAccount, webdavPassword)
                    val body = RequestBody.create("application/json; charset=utf-8".toMediaTypeOrNull(), json)

                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("Authorization", credential)
                        .put(body)
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            Result.success("Backup uploaded successfully!")
                        } else {
                            Result.failure(Exception("Server returned HTTP ${response.code}: ${response.message}"))
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) {
                onResult(true, result.getOrThrow())
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }

    fun restoreBackupFromWebdav(context: Context, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            if (!webdavIsBound || webdavAddress.isBlank()) {
                onResult(false, "WebDAV is not configured or bound.")
                return@launch
            }

            val targetUrl = when {
                webdavAddress.endsWith(".json", ignoreCase = true) -> webdavAddress
                webdavAddress.endsWith("/") -> "${webdavAddress}sicepatxray_backup.json"
                else -> "${webdavAddress}/sicepatxray_backup.json"
            }

            val result = withContext(Dispatchers.IO) {
                try {
                    val client = OkHttpClient.Builder()
                        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                        .build()

                    val credential = Credentials.basic(webdavAccount, webdavPassword)
                    val request = Request.Builder()
                        .url(targetUrl)
                        .header("Authorization", credential)
                        .get()
                        .build()

                    client.newCall(request).execute().use { response ->
                        if (response.isSuccessful) {
                            val json = response.body?.string() ?: ""
                            if (json.isNotBlank()) {
                                Result.success(json)
                            } else {
                                Result.failure(Exception("Backup file is empty."))
                            }
                        } else {
                            Result.failure(Exception("Server returned HTTP ${response.code}: ${response.message}"))
                        }
                    }
                } catch (e: Exception) {
                    Result.failure(e)
                }
            }

            if (result.isSuccess) {
                val json = result.getOrThrow()
                try {
                    val backup = Gson().fromJson(json, BackupData::class.java)
                    if (recoveryStrategy == "Override") {
                        profiles.clear()
                        proxies.clear()
                    }
                    var importedProfilesCount = 0
                    var importedProxiesCount = 0
                    if (backup.profiles != null) {
                        for (p in backup.profiles) {
                            if (profiles.none { it.name == p.name }) {
                                profiles.add(p)
                                importedProfilesCount++
                            }
                        }
                    }
                    if (backup.proxies != null) {
                        for (p in backup.proxies) {
                            if (proxies.none { it.name == p.name }) {
                                proxies.add(p)
                                importedProxiesCount++
                            }
                        }
                    }
                    onResult(true, "Successfully restored $importedProfilesCount profiles and $importedProxiesCount proxies!")
                } catch (e: Exception) {
                    onResult(false, "Failed to parse backup data: ${e.message}")
                }
            } else {
                onResult(false, result.exceptionOrNull()?.message ?: "Unknown error")
            }
        }
    }
        
    fun performExport(outputStream: OutputStream?): Boolean {
        if (outputStream == null) return false
        return try {
            val backup = BackupData(
                profiles = profiles.toList(),
                proxies = proxies.toList()
            )
            val json = Gson().toJson(backup)
            outputStream.write(json.toByteArray())
            outputStream.close()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun performImport(inputStream: InputStream?, strategy: String): Boolean {
        if (inputStream == null) return false
        return try {
            val json = inputStream.bufferedReader().use { it.readText() }
            val backup = Gson().fromJson(json, BackupData::class.java)
            if (strategy == "Override") {
                profiles.clear()
                proxies.clear()
            }
            if (backup.profiles != null) {
                for (p in backup.profiles) {
                    if (profiles.none { it.name == p.name }) profiles.add(p)
                }
            }
            if (backup.proxies != null) {
                for (p in backup.proxies) {
                    if (proxies.none { it.name == p.name }) proxies.add(p)
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
        
    fun selectProfile(index: Int) {
        for (i in profiles.indices) {
            profiles[i] = profiles[i].copy(isSelected = (i == index))
        }
    }

    fun importProxiesFromText(text: String, profileName: String): Int {
        val parsedProxies = parseSubscriptionContent(text, profileName)
        proxies.addAll(0, parsedProxies)
        return parsedProxies.size
    }

    fun addProxy(proxy: ProxyItem) {
        proxies.add(0, proxy.copy(profileName = activeProfileName))
    }

    fun addProfile(name: String, url: String = "", source: String = "Manual") {
        profiles.add(0, ProfileItem(name, "Just now", profiles.isEmpty(), optionalUrl = url))
    }

    fun addProfileWithProxies(name: String, url: String = "", source: String = "URL", customProxies: List<ProxyItem>) {
        val countInfo = if (customProxies.size == 1) "1 Server" else "${customProxies.size} Server"
        profiles.add(0, ProfileItem(name, "Selesai: $countInfo", profiles.isEmpty(), optionalUrl = url))
        if (customProxies.isNotEmpty()) {
            proxies.addAll(0, customProxies.map { it.copy(profileName = name) })
        }
    }

    suspend fun downloadResource(url: String, fileName: String, context: android.content.Context): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        val body = response.body
                        if (body != null) {
                            val file = java.io.File(context.filesDir, fileName)
                            java.io.FileOutputStream(file).use { out ->
                                out.write(body.bytes())
                            }
                            return@withContext true
                        }
                    }
                }
                false
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
    }

    suspend fun downloadConfigFromUrl(url: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                val client = OkHttpClient.Builder()
                    .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                    .build()
                val request = Request.Builder().url(url).build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) response.body?.string() else null
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun parseConfigToProxies(content: String, profileName: String): List<ProxyItem> {
        return parseSubscriptionContent(content, profileName)
    }

    fun updateProfile(oldIndex: Int, updated: ProfileItem) {
        val oldName = profiles[oldIndex].name
        profiles[oldIndex] = updated
        if (oldName != updated.name) {
            val toUpdate = proxies.filter { it.profileName == oldName }
            proxies.removeAll(toUpdate.toSet())
            proxies.addAll(toUpdate.map { it.copy(profileName = updated.name) })
        }
    }

    fun removeProfile(index: Int) {
        val toRemove = profiles[index]
        profiles.removeAt(index)
        val toRemoveProxies = proxies.filter { it.profileName == toRemove.name }
        proxies.removeAll(toRemoveProxies.toSet())
        if (toRemove.isSelected && profiles.isNotEmpty()) {
            profiles[0] = profiles[0].copy(isSelected = true)
        }
    }

    fun updateAllProfiles(onComplete: (String) -> Unit) {
        viewModelScope.launch {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
                .build()

            var updatedCount = 0
            var failCount = 0
            val sbMessage = java.lang.StringBuilder()

            for (i in profiles.indices) {
                val profile = profiles[i]
                val url = profile.optionalUrl.trim()
                if (url.startsWith("http://", ignoreCase = true) || url.startsWith("https://", ignoreCase = true)) {
                    val result = withContext(Dispatchers.IO) {
                        try {
                            val request = Request.Builder().url(url).build()
                            client.newCall(request).execute().use { response ->
                                if (response.isSuccessful) {
                                    val bodyText = response.body?.string() ?: ""
                                    Result.success(bodyText)
                                } else {
                                    Result.failure(Exception("HTTP ${response.code}"))
                                }
                            }
                        } catch (e: Exception) {
                            Result.failure(e)
                        }
                    }

                    if (result.isSuccess) {
                        val body = result.getOrNull() ?: ""
                        // Parse network response to get valid VPN proxies
                        val parsedProxies = parseSubscriptionContent(body, profile.name)
                        
                        if (parsedProxies.isNotEmpty()) {
                            // Suppress old proxies for this profile and insert the active server nodes
                            val oldProxies = proxies.filter { it.profileName == profile.name }
                            proxies.removeAll(oldProxies.toSet())
                            proxies.addAll(parsedProxies)
                            
                            profiles[i] = profile.copy(
                                date = "Selesai: ${parsedProxies.size} Server"
                            )
                            sbMessage.append("✓ ${profile.name}: ${parsedProxies.size} server diunduh.\n")
                        } else {
                            profiles[i] = profile.copy(
                                date = "Selesai: Terhubung (${body.length} B)"
                            )
                            sbMessage.append("✓ ${profile.name}: Unduh sukses (${body.length} B).\n")
                        }
                        updatedCount++
                    } else {
                        val errorMsg = result.exceptionOrNull()?.message ?: "Koneksi gagal"
                        profiles[i] = profile.copy(
                            date = "Gagal: $errorMsg"
                        )
                        sbMessage.append("✗ ${profile.name}: Gagal ($errorMsg)\n")
                        failCount++
                    }
                } else {
                    // Standard update / local simulation if profile has no subscription URL configured
                    profiles[i] = profile.copy(date = "Updated just now")
                }
            }

            val finalSummary = if (updatedCount == 0 && failCount == 0) {
                "Semua profil lokal berhasil diperbarui."
            } else {
                "Sinkronisasi Selesai!\nSelesai: $updatedCount | Gagal: $failCount\n\n${sbMessage.toString().trim()}"
            }
            onComplete(finalSummary)
        }
    }

    private fun tryDecodeBase64(input: String): String {
        val clean = input.replace("\\s".toRegex(), "")
        if (clean.isEmpty()) return ""
        for (flag in listOf(Base64.DEFAULT, Base64.NO_PADDING, Base64.URL_SAFE, Base64.NO_WRAP)) {
            try {
                val bytes = Base64.decode(clean, flag)
                val decoded = String(bytes, Charsets.UTF_8)
                if (decoded.isNotEmpty() && (
                    decoded.contains("://") || 
                    decoded.contains("{") || 
                    decoded.contains("\n") || 
                    decoded.contains("ps=")
                )) {
                    return decoded
                }
            } catch (e: Exception) {
                // Try next flag
            }
        }
        return ""
    }

    fun parseSubscriptionContent(rawContent: String, profileName: String): List<ProxyItem> {
        val list = mutableListOf<ProxyItem>()
        var text = rawContent.trim()
        
        // 1. Try to detect if the entire text is a single JSON config array or object
        if (text.startsWith("[") && text.endsWith("]")) {
            try {
                val array = JSONArray(text)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    val remarks = item.optString("remarks", "JSON Config ${i + 1}")
                    var type = "JSON"
                    if (item.has("outbounds")) {
                        val outbounds = item.getJSONArray("outbounds")
                        if (outbounds.length() > 0) {
                            val mainOutbound = outbounds.getJSONObject(0)
                            val protocol = mainOutbound.optString("protocol", "")
                            if (protocol.isNotEmpty()) {
                                type = protocol.replaceFirstChar { it.uppercase() }
                            }
                        }
                    }
                    list.add(ProxyItem(remarks, type, "⚡", false, isGreen = true, profileName = profileName, fullConfig = item.toString(2)))
                }
                if (list.isNotEmpty()) return list
            } catch (e: Exception) {
                // Fall through
            }
        }

        if (text.startsWith("{") && text.endsWith("}")) {
            try {
                val root = JSONObject(text)
                val remarks = root.optString("remarks", "JSON Config")
                var type = "JSON"
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
                list.add(ProxyItem(remarks, type, "⚡", false, isGreen = true, profileName = profileName, fullConfig = text))
                return list
            } catch (e: Exception) {
                // Fall through to line-by-line split
            }
        }

        // 2. Try robust Base64 decoding if the text is not JSON and doesn't start with known protocol schemes
        val isKnownProtocol = text.startsWith("vmess://", ignoreCase = true) ||
                text.startsWith("vless://", ignoreCase = true) ||
                text.startsWith("ss://", ignoreCase = true) ||
                text.startsWith("trojan://", ignoreCase = true) ||
                text.startsWith("hysteria", ignoreCase = true) ||
                text.startsWith("socks", ignoreCase = true) ||
                text.startsWith("http", ignoreCase = true)
        
        if (!text.startsWith("{") && !text.startsWith("[") && !isKnownProtocol) {
            val decoded = tryDecodeBase64(text)
            if (decoded.isNotEmpty()) {
                text = decoded
            }
        }

        // 3. Process line-by-line
        val lines = text.split('\n', '\r').map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            try {
                if (line.startsWith("{") && line.endsWith("}")) {
                    try {
                        val root = JSONObject(line)
                        val remarks = root.optString("remarks", "JSON Config")
                        var type = "JSON"
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
                        list.add(ProxyItem(remarks, type, "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                    } catch (e: Exception) {
                        // skip or ignore
                    }
                } else if (line.startsWith("[")) {
                    try {
                        val array = JSONArray(line)
                        for (i in 0 until array.length()) {
                            val item = array.getJSONObject(i)
                            val remarks = item.optString("remarks", "JSON Config")
                            var type = "JSON"
                            if (item.has("outbounds")) {
                                val outbounds = item.getJSONArray("outbounds")
                                if (outbounds.length() > 0) {
                                    val mainOutbound = outbounds.getJSONObject(0)
                                    val protocol = mainOutbound.optString("protocol", "")
                                    if (protocol.isNotEmpty()) {
                                        type = protocol.replaceFirstChar { it.uppercase() }
                                    }
                                }
                            }
                            list.add(ProxyItem(remarks, type, "⚡", false, isGreen = true, profileName = profileName, fullConfig = item.toString(2)))
                        }
                    } catch (e: Exception) {
                        // skip
                    }
                } else if (line.startsWith("vmess://", ignoreCase = true)) {
                    val configStr = line.substring(8).trim()
                    val decodedConfig = try {
                        val bytes = Base64.decode(configStr, Base64.DEFAULT)
                        String(bytes, Charsets.UTF_8)
                    } catch (e: Exception) {
                        ""
                    }
                    var name = "Vmess Server"
                    if (decodedConfig.isNotEmpty()) {
                        val psMatch = "\"ps\"\\s*:\\s*\"([^\"]+)\"".toRegex().find(decodedConfig)
                        if (psMatch != null) {
                            name = psMatch.groupValues[1]
                        }
                    }
                    list.add(ProxyItem(name, "Vmess", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("vless://", ignoreCase = true)) {
                    val name = extractRemarks(line, "Vless Server")
                    list.add(ProxyItem(name, "Vless", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("ss://", ignoreCase = true)) {
                    val name = extractRemarks(line, "Shadowsocks Server")
                    list.add(ProxyItem(name, "Shadowsocks", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("trojan://", ignoreCase = true)) {
                    val name = extractRemarks(line, "Trojan Server")
                    list.add(ProxyItem(name, "Trojan", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("hysteria://", ignoreCase = true) || line.startsWith("hysteria2://", ignoreCase = true)) {
                    val name = extractRemarks(line, "Hysteria Server")
                    list.add(ProxyItem(name, "Hysteria2", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("socks5://", ignoreCase = true) || line.startsWith("socks://", ignoreCase = true)) {
                    val name = extractRemarks(line, "Socks Server")
                    list.add(ProxyItem(name, "Socks5", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                } else if (line.startsWith("http://", ignoreCase = true) || line.startsWith("https://", ignoreCase = true)) {
                    val name = extractRemarks(line, "HTTP Server")
                    list.add(ProxyItem(name, "HTTP", "⚡", false, isGreen = true, profileName = profileName, fullConfig = line))
                }
            } catch (e: Exception) {
                // Skip faulty line
            }
        }
        return list
    }

    private fun extractRemarks(url: String, defaultName: String): String {
        val hashIdx = url.indexOf('#')
        if (hashIdx != -1 && hashIdx < url.length - 1) {
            val rawRemarks = url.substring(hashIdx + 1)
            return try {
                URLDecoder.decode(rawRemarks, "UTF-8")
            } catch (e: Exception) {
                rawRemarks
            }
        }
        try {
            val clean = url.substringBefore('?').substringBefore('#')
            val parts = clean.split("@")
            if (parts.size > 1) {
                val hostPart = parts[1].substringBefore('/')
                return hostPart
            }
        } catch (e: Exception) {}
        return defaultName
    }

    fun sortProfilesAlphabetically() {
        val sortedList = profiles.sortedBy { it.name.lowercase() }
        profiles.clear()
        profiles.addAll(sortedList)
    }
}
