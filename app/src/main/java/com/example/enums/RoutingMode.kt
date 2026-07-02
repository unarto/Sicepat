package com.example.enums

enum class RoutingMode(val value: String) {
    VPN_SERVICE("VpnService"),
    TRANSPARENT_PROXY("Transparent Proxy");

    companion object {
        fun fromValue(value: String?): RoutingMode {
            return values().find { it.value.equals(value, ignoreCase = true) } ?: VPN_SERVICE
        }
    }
}
