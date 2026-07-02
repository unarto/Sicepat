package com.example

import android.util.Log

object HevTunnelBridge {
    private const val TAG = "HevTunnelBridge"
    private var useLegacy = false

    init {
        // Pre-initialize both native loaders so their JNI_OnLoad runs and registers
        try {
            val dummyNew = hevsocks5tunnel::class.java
            Log.d(TAG, "hevsocks5tunnel class pre-initialized")
        } catch (e: Throwable) {
            Log.w(TAG, "hevsocks5tunnel pre-init failed: ${e.message}")
        }

        try {
            val dummyLegacy = com.example.service.HevSocks5Tunnel::class.java
            Log.d(TAG, "HevSocks5Tunnel legacy class pre-initialized")
        } catch (e: Throwable) {
            Log.w(TAG, "HevSocks5Tunnel legacy pre-init failed: ${e.message}")
        }
    }

    fun startService(configPath: String, fd: Int) {
        if (useLegacy) {
            try {
                com.example.service.HevSocks5Tunnel.TProxyStartService(configPath, fd)
                Log.i(TAG, "Started service using legacy JNI bridge")
                return
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Legacy start failed, falling back to new JNI bridge: ${e.message}")
            }
        }

        try {
            hevsocks5tunnel.TProxyStartService(configPath, fd)
            Log.i(TAG, "Started service using new JNI bridge")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "New start failed, falling back to legacy JNI bridge: ${e.message}")
            useLegacy = true
            try {
                com.example.service.HevSocks5Tunnel.TProxyStartService(configPath, fd)
                Log.i(TAG, "Started service using legacy JNI bridge as fallback")
            } catch (e2: UnsatisfiedLinkError) {
                Log.e(TAG, "Both JNI bridges failed to start service! ${e2.message}")
            }
        }
    }

    fun stopService() {
        if (useLegacy) {
            try {
                com.example.service.HevSocks5Tunnel.TProxyStopService()
                Log.i(TAG, "Stopped service using legacy JNI bridge")
                return
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Legacy stop failed, falling back to new JNI bridge: ${e.message}")
            }
        }

        try {
            hevsocks5tunnel.TProxyStopService()
            Log.i(TAG, "Stopped service using new JNI bridge")
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "New stop failed, falling back to legacy JNI bridge: ${e.message}")
            useLegacy = true
            try {
                com.example.service.HevSocks5Tunnel.TProxyStopService()
                Log.i(TAG, "Stopped service using legacy JNI bridge as fallback")
            } catch (e2: UnsatisfiedLinkError) {
                Log.e(TAG, "Both JNI bridges failed to stop service! ${e2.message}")
            }
        }
    }

    fun getStats(): LongArray? {
        if (useLegacy) {
            try {
                return com.example.service.HevSocks5Tunnel.TProxyGetStats()
            } catch (e: UnsatisfiedLinkError) {
                Log.w(TAG, "Legacy getStats failed, falling back to new JNI bridge: ${e.message}")
            }
        }

        return try {
            hevsocks5tunnel.TProxyGetStats()
        } catch (e: UnsatisfiedLinkError) {
            Log.w(TAG, "New getStats failed, falling back to legacy JNI bridge: ${e.message}")
            useLegacy = true
            try {
                com.example.service.HevSocks5Tunnel.TProxyGetStats()
            } catch (e2: UnsatisfiedLinkError) {
                Log.e(TAG, "Both JNI bridges failed to get stats! ${e2.message}")
                null
            }
        }
    }
}
