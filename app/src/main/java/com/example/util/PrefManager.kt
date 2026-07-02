package com.example.util

import android.content.Context
import android.content.SharedPreferences
import com.example.enums.LanguageOption
import com.example.enums.RoutingMode
import com.example.enums.ThemeMode

object PrefManager {
    private const val PREF_SHIZUKU = "zivpn_shizuku_settings"
    private const val PREF_ADVANCED = "sicepat_advanced_settings"
    private const val PREF_SPLIT_TUNNEL = "vpn_split_tunnel_settings"
    private const val PREF_ROUTING = "zivpn_routing_settings"
    private const val PREF_HYSTERIA = "zivpn_hysteria_settings"
    private const val PREF_THEME = "zivpn_theme_settings"
    private const val PREF_BACKUP = "zivpn_backup_settings"
    private const val PREF_ACTIVE_PROXY = "sicepat_active_proxy"
    private const val PREF_LANGUAGE = "zivpn_language_settings"

    private fun getPrefs(context: Context, name: String): SharedPreferences {
        return context.getSharedPreferences(name, Context.MODE_PRIVATE)
    }

    //region Language
    fun getLanguage(context: Context): LanguageOption {
        val code = getPrefs(context, PREF_LANGUAGE).getString("lang_code", LanguageOption.AUTO.code)
        return LanguageOption.fromCode(code)
    }

    fun setLanguage(context: Context, lang: LanguageOption) {
        getPrefs(context, PREF_LANGUAGE).edit().putString("lang_code", lang.code).apply()
    }
    //endregion

    //region Theme
    fun getThemeMode(context: Context): ThemeMode {
        val mode = getPrefs(context, PREF_THEME).getString("theme_mode", ThemeMode.DARK.displayName)
        return ThemeMode.fromDisplayName(mode)
    }

    fun setThemeMode(context: Context, mode: ThemeMode) {
        getPrefs(context, PREF_THEME).edit().putString("theme_mode", mode.displayName).apply()
    }
    //endregion

    //region Shizuku
    fun getShizukuUseForCore(context: Context): Boolean {
        return getPrefs(context, PREF_SHIZUKU).getBoolean("use_for_core", false)
    }

    fun setShizukuUseForCore(context: Context, value: Boolean) {
        getPrefs(context, PREF_SHIZUKU).edit().putBoolean("use_for_core", value).apply()
    }

    fun getShizukuRoutingMode(context: Context): RoutingMode {
        val mode = getPrefs(context, PREF_SHIZUKU).getString("routing_mode", RoutingMode.VPN_SERVICE.value)
        return RoutingMode.fromValue(mode)
    }

    fun setShizukuRoutingMode(context: Context, mode: RoutingMode) {
        getPrefs(context, PREF_SHIZUKU).edit().putString("routing_mode", mode.value).apply()
    }

    fun getShizukuBypassDns(context: Context): Boolean {
        return getPrefs(context, PREF_SHIZUKU).getBoolean("bypass_dns", false)
    }

    fun setShizukuBypassDns(context: Context, value: Boolean) {
        getPrefs(context, PREF_SHIZUKU).edit().putBoolean("bypass_dns", value).apply()
    }

    fun getShizukuListenPort(context: Context): String {
        return getPrefs(context, PREF_SHIZUKU).getString("listen_port", "10808") ?: "10808"
    }

    fun setShizukuListenPort(context: Context, port: String) {
        getPrefs(context, PREF_SHIZUKU).edit().putString("listen_port", port).apply()
    }
    //endregion

    //region Advanced Settings
    fun getVpnMtu(context: Context): Int {
        val mtuStr = getPrefs(context, PREF_ADVANCED).getString("vpnMtu", "1500") ?: "1500"
        return mtuStr.toIntOrNull() ?: 1500
    }

    fun setVpnMtu(context: Context, mtu: Int) {
        getPrefs(context, PREF_ADVANCED).edit().putString("vpnMtu", mtu.toString()).apply()
    }

    fun getVpnInterfaceAddress(context: Context): String {
        return getPrefs(context, PREF_ADVANCED).getString("vpnInterfaceAddr", "10.0.0.2") ?: "10.0.0.2"
    }

    fun setVpnInterfaceAddress(context: Context, address: String) {
        getPrefs(context, PREF_ADVANCED).edit().putString("vpnInterfaceAddr", address).apply()
    }

    fun getVpnDns(context: Context): String {
        return getPrefs(context, PREF_ADVANCED).getString("vpnDns", "8.8.8.8") ?: "8.8.8.8"
    }

    fun setVpnDns(context: Context, dns: String) {
        getPrefs(context, PREF_ADVANCED).edit().putString("vpnDns", dns).apply()
    }

    fun getLocalProxyPort(context: Context): Int {
        val portStr = getPrefs(context, PREF_ADVANCED).getString("localProxyPort", "10808") ?: "10808"
        return portStr.toIntOrNull() ?: 10808
    }

    fun setLocalProxyPort(context: Context, port: Int) {
        getPrefs(context, PREF_ADVANCED).edit().putString("localProxyPort", port.toString()).apply()
    }
    //endregion

    //region Split Tunnel Settings
    fun getExcludedPackages(context: Context): Set<String> {
        return getPrefs(context, PREF_SPLIT_TUNNEL).getStringSet("excluded_packages", emptySet()) ?: emptySet()
    }

    fun setExcludedPackages(context: Context, packages: Set<String>) {
        getPrefs(context, PREF_SPLIT_TUNNEL).edit().putStringSet("excluded_packages", packages).apply()
    }
    //endregion

    //region Routing Settings
    fun getGlobalOutboundMode(context: Context): String {
        return getPrefs(context, PREF_ROUTING).getString("global_outbound_mode", "Proxy") ?: "Proxy"
    }

    fun setGlobalOutboundMode(context: Context, mode: String) {
        getPrefs(context, PREF_ROUTING).edit().putString("global_outbound_mode", mode).apply()
    }

    fun getDomainStrategy(context: Context): String {
        return getPrefs(context, PREF_ROUTING).getString("domainStrategy", "AsIs") ?: "AsIs"
    }

    fun setDomainStrategy(context: Context, strategy: String) {
        getPrefs(context, PREF_ROUTING).edit().putString("domainStrategy", strategy).apply()
    }

    fun getRoutingRulesJson(context: Context): String? {
        return getPrefs(context, PREF_ROUTING).getString("routing_rules_json", null)
    }

    fun setRoutingRulesJson(context: Context, json: String) {
        getPrefs(context, PREF_ROUTING).edit().putString("routing_rules_json", json).apply()
    }
    //endregion

    //region Active Proxy
    fun getActiveProxyName(context: Context): String {
        return getPrefs(context, PREF_ACTIVE_PROXY).getString("name", "") ?: ""
    }

    fun getActiveProxyProfileName(context: Context): String {
        return getPrefs(context, PREF_ACTIVE_PROXY).getString("profileName", "") ?: ""
    }

    fun setActiveProxy(context: Context, name: String, profileName: String) {
        getPrefs(context, PREF_ACTIVE_PROXY).edit()
            .putString("name", name)
            .putString("profileName", profileName)
            .apply()
    }
    //endregion
}
