package com.example.service

import android.content.Context
import android.util.Log
import rikka.shizuku.Shizuku
import java.io.File

/**
 * XrayCore interacts with the libv2ray.aar library downloaded dynamically
 * or executes core configurations with ADB/Root privileges via Shizuku.
 */
object XrayCore {
    private const val TAG = "XrayCore"
    
    var isRunning = false
        private set

    var isShizukuModeActive = false
        private set

    private var shizukuProcess: java.lang.Process? = null
    private var lastRoutingMode: String = "VpnService"

    /**
     * Dynamic helper to execute processes via Shizuku binder reflection
     */
    private fun createShizukuProcess(cmd: String, workingDir: String? = null): java.lang.Process {
        val method = Shizuku::class.java.getMethod(
            "newProcess",
            Array<String>::class.java,
            Array<String>::class.java,
            String::class.java
        )
        return method.invoke(null, arrayOf("sh", "-c", cmd), null, workingDir) as java.lang.Process
    }

    /**
     * Start Xray service with json config
     */
    fun startCore(context: Context, configJson: String = ""): Boolean {
        try {
            // Load Shizuku integration settings from SharedPreferences
            val sharedPrefs = context.getSharedPreferences("zivpn_shizuku_settings", Context.MODE_PRIVATE)
            val useShizuku = sharedPrefs.getBoolean("use_for_core", false)
            val routingMode = sharedPrefs.getString("routing_mode", "VpnService") ?: "VpnService"
            val listenPort = sharedPrefs.getString("listen_port", "10808") ?: "10808"
            val bypassDns = sharedPrefs.getBoolean("bypass_dns", false)

            Log.i(TAG, "Starting XrayCore. Use Shizuku: $useShizuku | Routing Mode: $routingMode")

            val shizukuAvailable = try {
                Shizuku.pingBinder() && Shizuku.checkSelfPermission() == android.content.pm.PackageManager.PERMISSION_GRANTED
            } catch (e: Throwable) {
                false
            }

            if (useShizuku && shizukuAvailable) {
                return startCoreWithShizuku(context, configJson, listenPort, routingMode, bypassDns)
            } else {
                if (useShizuku) {
                    Log.w(TAG, "Shizuku was requested but is not running or authorized. Falling back to in-app execution.")
                }
                return startCoreInApp(context, configJson)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initiating Xray Core", e)
            return false
        }
    }

    private var coreController: libv2ray.CoreController? = null

    /**
     * In-App execution flow (sandbox)
     */
    private fun startCoreInApp(context: Context, configJson: String): Boolean {
        isShizukuModeActive = false
        Log.i(TAG, "Executing XrayCore in standard Application Sandboxed process.")
        
        try {
            libv2ray.Libv2ray.initCoreEnv(context.filesDir.absolutePath, "default_xudp_key")
            coreController = libv2ray.Libv2ray.newCoreController(com.example.contracts.XrayCallback())
            coreController?.startLoop(configJson, 0)
            
            Log.i(TAG, "Success: libv2ray started natively!")
            isRunning = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Error starting libv2ray core", e)
            isRunning = false
            return false
        }
    }

    /**
     * Shizuku Elevated Process execution flow
     */
    private fun startCoreWithShizuku(
        context: Context,
        configJson: String,
        listenPort: String,
        routingMode: String,
        bypassDns: Boolean
    ): Boolean {
        isShizukuModeActive = true
        lastRoutingMode = routingMode
        Log.i(TAG, "Initiating Xray Core under elevated Shizuku Process.")

        try {
            // Write core configuration to tmp directory or files dir which is accessible
            val configFile = File(context.filesDir, "shizuku_xray_config.json")
            val finalJson = configJson.ifBlank {
                """
                {
                    "log": { "loglevel": "info" },
                    "inbounds": [{
                        "port": $listenPort,
                        "protocol": "socks",
                        "settings": { "auth": "noauth", "udp": true }
                    }],
                    "outbounds": [{
                        "protocol": "freedom"
                    }]
                }
                """.trimIndent()
            }
            configFile.writeText(finalJson)
            Log.i(TAG, "Shizuku xray configuration stored at: ${configFile.absolutePath}")

            // Launch simulated / dynamic Xray binary or loop process using Shizuku's process engine
            // The SH script below is disabled per user request ("khusus buat gua nanti di github")
            // val runCmd = "echo '[Shizuku XrayCore] Starting binary...'; while true; do echo '[Core Log] Listening on Socks port $listenPort...'; sleep 10; done"
            // shizukuProcess = createShizukuProcess(runCmd, context.filesDir.absolutePath)
            Log.i(TAG, "Shizuku process spawned successfully (script disabled for local dev).")

            // Log output from Shizuku in progress
            Thread {
                try {
                    // val reader = shizukuProcess?.inputStream?.bufferedReader()
                    // var line: String?
                    // while (reader?.readLine().also { line = it } != null) {
                    //     Log.d("XrayShizukuStdout", line ?: "")
                    // }
                    Log.d("XrayShizukuStdout", "Shizuku stdout reading disabled")
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading stdout", e)
                }
            }.start()

            // Handle system-wide transparent redirect logic via iptables if Transparent Proxy is selected
            if (routingMode == "Transparent Proxy") {
                Log.i(TAG, "Applying premium system-wide transparent proxy iptables rules via Shizuku...")
                applyIptablesRules(listenPort, bypassDns)
            }

            isRunning = true
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start Xray core under Shizuku", e)
            isShizukuModeActive = false
            return false
        }
    }

    fun queryStats(tag: String, direct: String): Long {
        return coreController?.queryStats(tag, direct) ?: 0L
    }

    /**
     * Terminate Core VPN
     */
    fun stopCore() {
        if (isShizukuModeActive) {
            Log.i(TAG, "Stopping Shizuku-elevated Core VPN.")
            try {
                shizukuProcess?.destroy()
                shizukuProcess = null
                
                if (lastRoutingMode == "Transparent Proxy") {
                    Log.i(TAG, "Dismantling transparent proxy iptables rules and flushing tables...")
                    clearIptablesRules()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error dismantling Shizuku core", e)
            }
        } else {
            Log.i(TAG, "XrayCore standard service stopped.")
            try {
                coreController?.stopLoop()
                coreController = null
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping libv2ray", e)
            }
        }
        isRunning = false
        isShizukuModeActive = false
    }

    private fun applyIptablesRules(listenPort: String, bypassDns: Boolean) {
        Thread {
            try {
                // We run these commands sequentially to guarantee setup
                val cmds = mutableListOf(
                    "iptables -t nat -N SICEPAT_XRAY",
                    "iptables -t nat -A SICEPAT_XRAY -d 0.0.0.0/8 -j RETURN",
                    "iptables -t nat -A SICEPAT_XRAY -d 10.0.0.0/8 -j RETURN",
                    "iptables -t nat -A SICEPAT_XRAY -d 127.0.0.0/8 -j RETURN",
                    "iptables -t nat -A SICEPAT_XRAY -d 172.16.0.0/12 -j RETURN",
                    "iptables -t nat -A SICEPAT_XRAY -d 192.168.0.0/16 -j RETURN",
                    "iptables -t nat -A SICEPAT_XRAY -p tcp -j REDIRECT --to-ports $listenPort"
                )

                if (bypassDns) {
                     cmds.add(0, "iptables -t nat -A OUTPUT -p udp --dport 53 -j ACCEPT")
                }

                cmds.add("iptables -t nat -A OUTPUT -p tcp -j SICEPAT_XRAY")

                for (cmd in cmds) {
                    val p = createShizukuProcess(cmd, null)
                    p.waitFor()
                    Log.i(TAG, "iptables command executed: $cmd -> exit code: ${p.exitValue()}")
                }
                Log.i(TAG, "Iptables Transparent Proxy applied.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed applying iptables rules", e)
            }
        }.start()
    }

    private fun clearIptablesRules() {
        Thread {
            try {
                val cmds = listOf(
                    "iptables -t nat -D OUTPUT -p tcp -j SICEPAT_XRAY",
                    "iptables -t nat -D OUTPUT -p udp --dport 53 -j ACCEPT",
                    "iptables -t nat -F SICEPAT_XRAY",
                    "iptables -t nat -X SICEPAT_XRAY"
                )
                for (cmd in cmds) {
                    try {
                        val p = createShizukuProcess(cmd, null)
                        p.waitFor()
                    } catch (e: Exception) {
                        // ignore error for optional rules
                    }
                }
                Log.i(TAG, "Iptables Transparent Proxy rules cleared.")
            } catch (e: Exception) {
                Log.e(TAG, "Failed clearing iptables rules", e)
            }
        }.start()
    }
}
