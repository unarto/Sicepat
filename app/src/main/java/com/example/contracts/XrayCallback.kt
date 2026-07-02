package com.example.contracts

import libv2ray.CoreCallbackHandler

class XrayCallback : CoreCallbackHandler {
    override fun onEmitStatus(p0: Long, p1: String?): Long {
        android.util.Log.i("XrayCallback", "Status: $p1")
        return 0
    }

    override fun shutdown(): Long {
        android.util.Log.i("XrayCallback", "Shutdown")
        return 0
    }

    override fun startup(): Long {
        android.util.Log.i("XrayCallback", "Startup")
        return 0
    }
}
