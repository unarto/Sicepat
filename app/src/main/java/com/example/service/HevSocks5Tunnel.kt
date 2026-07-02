package com.example.service

import android.util.Log

class HevSocks5Tunnel {
    companion object {
        private const val TAG = "HevSocks5Tunnel"

        init {
            try {
                System.loadLibrary("hevsocks5tunnel")
                Log.d(TAG, "Loaded hevsocks5tunnel in legacy class")
            } catch (e: UnsatisfiedLinkError) {
                try {
                    System.loadLibrary("hev-socks5-tunnel")
                    Log.d(TAG, "Loaded legacy hev-socks5-tunnel in legacy class")
                } catch (e2: UnsatisfiedLinkError) {
                    Log.e(TAG, "Failed to load libraries in legacy class: ${e2.message}")
                }
            }
        }

        @JvmStatic
        external fun TProxyStartService(configPath: String, fd: Int)

        @JvmStatic
        external fun TProxyStopService()

        @JvmStatic
        external fun TProxyGetStats(): LongArray
    }
}
