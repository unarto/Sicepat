package com.example.service

import com.example.MainActivity
import com.example.fmt.XrayConfigGenerator
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.VpnService
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.ByteBuffer

/**
 * SiCepatVpnService manages the VPN lifecycle and packet routing.
 */
class SiCepatVpnService : VpnService() {

    companion object {
        private const val TAG = "SiCepatVpnService"
        
        private val _vpnStatus = MutableStateFlow(VpnStatus.DISCONNECTED)
        val vpnStatus = _vpnStatus.asStateFlow()

        private val _connectionMode = MutableStateFlow(ConnectionMode.VPN)
        val connectionMode = _connectionMode.asStateFlow()

        private val _byteStats = MutableStateFlow(ByteStats(0, 0))
        val byteStats = _byteStats.asStateFlow()

        fun setMode(mode: ConnectionMode) {
            _connectionMode.value = mode
        }
    }

    private val NOTIFICATION_ID = 8811
    private val CHANNEL_ID = "sicepat_vpn_channel"

    private var vpnInterface: ParcelFileDescriptor? = null
    private var vpnThread: Job? = null
    private var logRotationJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
    }

    private fun startVpnInternal() {
        _vpnStatus.value = VpnStatus.CONNECTING
        
        logRotationJob = serviceScope.launch {
            while (true) {
                kotlinx.coroutines.delay(60 * 1000L) // check every minute
                rotateLogs()
            }
        }
        
        vpnThread = serviceScope.launch {
            try {
                // Start Xray Core in background Dispatchers.IO
                val configJson = XrayConfigGenerator.generateConfig(applicationContext)
                XrayCore.startCore(applicationContext, configJson)

                if (_connectionMode.value == ConnectionMode.VPN) {
                    establishVpn()
                } else {
                    _vpnStatus.value = VpnStatus.CONNECTED
                }
                
                // Only start tunnel loop if in VPN mode, otherwise just keep Xray running for local proxy
                runVpnLoop()
            } catch (e: Exception) {
                Log.e(TAG, "VPN Error", e)
                internalStopVpn()
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "SiCepat VPN Service"
            val descriptionText = "Menunjukkan status koneksi VPN SiCepat"
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun getNotification(text: String): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }
            PendingIntent.getActivity(this, 0, notificationIntent, flags)
        }

        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }

        return builder
            .setContentTitle("SiCepat VPN")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateStatsNotification(rxBytes: Long, txBytes: Long) {
        val rxStr = formatBytes(rxBytes)
        val txStr = formatBytes(txBytes)
        val statsText = "Aktif | Download: $rxStr | Upload: $txStr"
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(NOTIFICATION_ID, getNotification(statsText))
    }

    private fun formatBytes(bytes: Long): String {
        val unit = 1024.0
        if (bytes < unit) return "$bytes B"
        val exp = (Math.log(bytes.toDouble()) / Math.log(unit)).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format("%.1f %sB", bytes / Math.pow(unit, exp.toDouble()), pre)
    }

    enum class VpnStatus {
        DISCONNECTED, CONNECTING, CONNECTED
    }

    enum class ConnectionMode {
        VPN, PROXY_ONLY
    }

    data class ByteStats(val rxBytes: Long, val txBytes: Long)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START") {
            startVpn()
        } else if (intent?.action == "STOP") {
            stopVpn()
        }
        return START_STICKY
    }

    private fun startVpn() {
        if (_vpnStatus.value != VpnStatus.DISCONNECTED) return

        createNotificationChannel()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(NOTIFICATION_ID, getNotification("Menghubungkan ke server..."), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
        } else {
            startForeground(NOTIFICATION_ID, getNotification("Menghubungkan ke server..."))
        }

        startVpnInternal()
    }

    private fun establishVpn() {
        if (XrayCore.isShizukuModeActive) {
            val shizukuPrefs = getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE)
            val routingMode = shizukuPrefs.getString("routing_mode", "VpnService")
            if (routingMode == "Transparent Proxy") {
                Log.i(TAG, "Shizuku Transparent Proxy is active. Bypassing native tun establish.")
                _vpnStatus.value = VpnStatus.CONNECTED
                return
            }
        }

        val advPrefs = getSharedPreferences("sicepat_advanced_settings", Context.MODE_PRIVATE)
        val vpnMtuStr = advPrefs.getString("vpnMtu", "1500") ?: "1500"
        val vpnMtu = vpnMtuStr.toIntOrNull() ?: 1500
        val rawVpnAddress = advPrefs.getString("vpnInterfaceAddr", "10.0.0.2") ?: "10.0.0.2"
        val vpnAddress = if (rawVpnAddress.endsWith(".x", ignoreCase = true)) {
            rawVpnAddress.substring(0, rawVpnAddress.length - 2) + ".2"
        } else if (rawVpnAddress.contains('x', ignoreCase = true)) {
            rawVpnAddress.replace("x", "2", ignoreCase = true)
        } else {
            rawVpnAddress
        }
        val vpnDns = advPrefs.getString("vpnDns", "8.8.8.8") ?: "8.8.8.8"
        
        val builder = Builder()
        builder.setSession("SiCepat VPN")
            .setMtu(vpnMtu)
            .addAddress(vpnAddress, 32)
            .addRoute("0.0.0.0", 0)
            
        // Add multiple DNS servers if comma-separated
        vpnDns.split(",").map { it.trim() }.forEach {
            if (it.isNotBlank()) {
                builder.addDnsServer(it)
            }
        }

        // Retrieve and register split-tunneling application exclusions
        val splitPrefs = getSharedPreferences("vpn_split_tunnel_settings", Context.MODE_PRIVATE)
        val excludedPackages = splitPrefs.getStringSet("excluded_packages", emptySet()) ?: emptySet()
        for (pkg in excludedPackages) {
            try {
                builder.addDisallowedApplication(pkg)
                Log.d(TAG, "Successfully added split-tunnel exclusion for package: $pkg")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to exclude application from VPN split-tunneling: $pkg", e)
            }
        }

        val established = builder.establish() ?: throw java.lang.IllegalStateException("Tidak dapat membuat interface VPN (Builder returned null)")
        vpnInterface = established
        _vpnStatus.value = VpnStatus.CONNECTED
        Log.i(TAG, "VPN Interface established")

        val fd = established.fd
        if (fd != -1) {
            val configDir = File(filesDir, "config")
            if (!configDir.exists()) {
                configDir.mkdirs()
            }
            val configFile = File(configDir, "hev-socks5-tunnel.yaml")
            
            // Read saved Advanced settings from SharedPreferences
            val advPrefsSettings = getSharedPreferences("sicepat_advanced_settings", Context.MODE_PRIVATE)
            val socksPortStr = advPrefsSettings.getString("localProxyPort", "10808") ?: "10808"
            val socksPortInt = socksPortStr.toIntOrNull() ?: 10808
            val socksAddress = "127.0.0.1"
            
            Log.i(TAG, "Starting VPN Tunnel - Socks5 server: $socksAddress:$socksPortInt")
            
            val configContent = buildString {
                appendLine("tunnel:")
                appendLine("  mtu: $vpnMtu")
                appendLine("  ipv4: $vpnAddress")
                appendLine("socks5:")
                appendLine("  port: $socksPortInt")
                appendLine("  address: $socksAddress")
                appendLine("  udp: 'udp'")
                appendLine("misc:")
                appendLine("  tcp-read-write-timeout: 300000")
                appendLine("  udp-read-write-timeout: 60000")
                appendLine("  log-level: warn")
            }

            // Re-write the YAML file with current configured values
            configFile.writeText(configContent)

            serviceScope.launch(Dispatchers.IO) {
                try {
                    com.example.hevsocks5tunnel.TProxyStartService(configFile.absolutePath, fd)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start HevSocks5Tunnel: ${e.message}")
                }
            }
        }
    }

    private suspend fun runVpnLoop() {
        var wakeLock: android.os.PowerManager.WakeLock? = null
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as android.os.PowerManager
            wakeLock = powerManager.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "SiCepatVPN::AdaptiveWakelock")
            wakeLock.acquire(10 * 60 * 1000L /*10 minutes max*/)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire wakelock", e)
        }

        try {
            if (_connectionMode.value == ConnectionMode.VPN) {
                val isShizukuProxy = XrayCore.isShizukuModeActive && 
                        getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE).getString("routing_mode", "VpnService") == "Transparent Proxy"
                
                if (!isShizukuProxy && vpnInterface == null) {
                    return
                }
                
                var totalRx = 0L
                var totalTx = 0L
                var lastRx = 0L
                var lastTx = 0L
                
                var currentDelay = 1000L
                val minDelay = 1000L
                val maxDelay = 3000L
                var failedHealthChecks = 0
                
                while (vpnThread?.isActive == true) {
                    val stats = try { com.example.hevsocks5tunnel.TProxyGetStats() } catch (e: Exception) { null }
                    var hasTraffic = false
                    
                    if (stats != null && stats.size >= 4) {
                        val currentTx = stats[1]
                        val currentRx = stats[3]
                        
                        if (currentRx > lastRx || currentTx > lastTx) {
                            totalRx = currentRx
                            totalTx = currentTx
                            hasTraffic = true
                        }
                        lastRx = currentRx
                        lastTx = currentTx
                    } else {
                        // Fallback to XrayCore
                        val rxDelta = XrayCore.queryStats("proxy", "downlink")
                        val txDelta = XrayCore.queryStats("proxy", "uplink")
                        
                        if (rxDelta > 0 || txDelta > 0) {
                            totalRx += rxDelta
                            totalTx += txDelta
                            hasTraffic = true
                        }
                    }
                    
                    if (hasTraffic) {
                        currentDelay = maxOf(minDelay, currentDelay - 500L)
                        _byteStats.value = ByteStats(totalRx, totalTx)
                        updateStatsNotification(totalRx, totalTx)
                        failedHealthChecks = 0
                    } else {
                        currentDelay = minOf(maxDelay, currentDelay + 500L)
                        
                        // Crash Recovery Mechanism
                        if (!checkProxyHealth()) {
                            failedHealthChecks++
                            if (failedHealthChecks >= 3) {
                                Log.w(TAG, "Core unreachable, initiating crash recovery...")
                                performCrashRecovery()
                                failedHealthChecks = 0
                            }
                        } else {
                            failedHealthChecks = 0
                        }
                    }
                    
                    kotlinx.coroutines.delay(currentDelay)
                }
            } else {
                // Proxy only mode - wait with max delay to save battery
                while (vpnThread?.isActive == true) {
                    kotlinx.coroutines.delay(3000)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "VPN Loop error", e)
        } finally {
            try {
                if (wakeLock?.isHeld == true) {
                    wakeLock.release()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to release wakelock", e)
            }
        }
    }

    private suspend fun checkProxyHealth(): Boolean {
        val listenPort = if (XrayCore.isShizukuModeActive) {
            getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE).getString("listen_port", "10808")?.toIntOrNull() ?: 10808
        } else {
            10808
        }
        return kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val socket = java.net.Socket()
                socket.connect(java.net.InetSocketAddress("127.0.0.1", listenPort), 1000)
                socket.close()
                true
            } catch (e: Exception) {
                // Return false to trigger crash recovery mechanism
                false
            }
        }
    }

    private suspend fun performCrashRecovery() {
        Log.w(TAG, "Executing automatic crash recovery...")
        
        // Restart Core Engine
        XrayCore.stopCore()
        kotlinx.coroutines.delay(500)
        val configJson = XrayConfigGenerator.generateConfig(applicationContext)
        XrayCore.startCore(applicationContext, configJson)

        // Restart JNI Tunnel
        if (_connectionMode.value == ConnectionMode.VPN) {
            try {
                com.example.hevsocks5tunnel.TProxyStopService()
            } catch (e: Exception) {}

            kotlinx.coroutines.delay(500)

            val tunnel = vpnInterface
            if (tunnel != null) {
                val fd = tunnel.fd
                if (fd != -1) {
                    val configDir = java.io.File(filesDir, "config")
                    val configFile = java.io.File(configDir, "hev-socks5-tunnel.yaml")
                    serviceScope.launch(kotlinx.coroutines.Dispatchers.IO) {
                        try {
                            com.example.hevsocks5tunnel.TProxyStartService(configFile.absolutePath, fd)
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to start HevSocks5Tunnel during crash recovery: ${e.message}")
                        }
                    }
                }
            }
        }
        
        Log.i(TAG, "Crash recovery completed.")
    }

    private fun internalStopVpn() {
        _vpnStatus.value = VpnStatus.DISCONNECTED
        vpnThread?.cancel()
        vpnThread = null
        logRotationJob?.cancel()
        logRotationJob = null

        // Stop Xray Core
        XrayCore.stopCore()

        // Stop hev-socks5-tunnel JNI
        try {
            com.example.hevsocks5tunnel.TProxyStopService()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop hev-socks5-tunnel", e)
        }

        try {
            vpnInterface?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing interface", e)
        }
        vpnInterface = null
    }

    private fun rotateLogs() {
        val maxLogSize = 2 * 1024 * 1024L // 2MB
        val accessLog = File(filesDir, "access.log")
        val errorLog = File(filesDir, "error.log")

        if (accessLog.exists() && accessLog.length() > maxLogSize) {
            try {
                accessLog.writeText("")
            } catch (e: Exception) {}
        }
        if (errorLog.exists() && errorLog.length() > maxLogSize) {
            try {
                errorLog.writeText("")
            } catch (e: Exception) {}
        }
    }

    private fun stopVpn() {
        internalStopVpn()

        // Stop Foreground cleanly
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } else {
            @Suppress("DEPRECATION")
            stopForeground(true)
        }

        stopSelf()
    }

    override fun onDestroy() {
        stopVpn()
        super.onDestroy()
    }
}
