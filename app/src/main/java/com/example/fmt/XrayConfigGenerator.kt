package com.example.fmt

import com.example.ui.ProxyItem
import android.content.Context
import android.util.Base64
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder

object XrayConfigGenerator {
    private const val TAG = "XrayConfigGenerator"

    fun generateConfig(
        context: Context,
        overrideName: String? = null,
        overrideType: String? = null,
        overrideConfig: String? = null
    ): String {
        try {
            // 1. Get Selected Proxy details
            val activeProxyPrefs = context.getSharedPreferences("sicepat_active_proxy", Context.MODE_PRIVATE)
            val proxyName = overrideName ?: activeProxyPrefs.getString("name", "") ?: ""
            val proxyType = overrideType ?: activeProxyPrefs.getString("type", "") ?: ""
            val fullConfig = overrideConfig ?: activeProxyPrefs.getString("fullConfig", "") ?: ""

            // 2. Get Advanced Configuration details
            val advPrefs = context.getSharedPreferences("sicepat_advanced_settings", Context.MODE_PRIVATE)
            val logLevel = advPrefs.getString("logLevel", "debug") ?: "debug"
            val localProxyPortStr = advPrefs.getString("localProxyPort", "10808") ?: "10808"
            val localProxyPort = localProxyPortStr.toIntOrNull() ?: 10808
            val httpProxyPort = localProxyPort + 1

            // If the user pasted a complete custom Xray JSON config
            val trimmedFull = fullConfig.trim()
            if (trimmedFull.startsWith("{")) {
                try {
                    val root = JSONObject(trimmedFull)
                    if (root.has("outbounds") || root.has("inbounds")) {
                        // Make sure our local ports map correctly to prevent conflicts
                        if (root.has("inbounds")) {
                            val inbounds = root.getJSONArray("inbounds")
                            for (i in 0 until inbounds.length()) {
                                val inbound = inbounds.optJSONObject(i)
                                if (inbound != null) {
                                    val tag = inbound.optString("tag", "")
                                    val protocol = inbound.optString("protocol", "")
                                    if (tag == "socks" || protocol == "socks") {
                                        inbound.put("port", localProxyPort)
                                        inbound.put("listen", "127.0.0.1")
                                    } else if (tag == "http" || protocol == "http") {
                                        inbound.put("port", httpProxyPort)
                                        inbound.put("listen", "127.0.0.1")
                                    }
                                }
                            }
                        }
                        Log.d(TAG, "Using custom full Xray JSON configuration")
                        return root.toString(2)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Not a full JSON config or failed to parse, falling back to template generation", e)
                }
            }

            val enableLocalDns = advPrefs.getBoolean("enableLocalDns", true)
            val remoteDns = advPrefs.getString("remoteDns", "https://dns.adguard-dns.com/dns-query") ?: "https://dns.adguard-dns.com/dns-query"
            val domesticDns = advPrefs.getString("domesticDns", "94.140.15.15") ?: "94.140.15.15"
            val dnsHostsStr = advPrefs.getString("dnsHosts", "dns.adguard-dns.com:94.140.14.14") ?: ""
            val allowInsecure = advPrefs.getBoolean("allowInsecure", true)
            val enableSniffing = advPrefs.getBoolean("enableSniffing", true)
            val enableRouteOnly = advPrefs.getBoolean("enableRouteOnly", false)

            val enableMux = advPrefs.getBoolean("enableMux", false)
            val tcpConnections = advPrefs.getString("tcpConnections", "8") ?: "8"
            val xudpConnections = advPrefs.getString("xudpConnections", "8") ?: "8"
            val handlingQuicMux = advPrefs.getString("handlingQuicMux", "reject") ?: "reject"

            val enableFragment = advPrefs.getBoolean("enableFragment", false)
            val fragmentLength = advPrefs.getString("fragmentLength", "50-100") ?: "50-100"
            val fragmentInterval = advPrefs.getString("fragmentInterval", "10-20") ?: "10-20"
            val fragmentPackets = advPrefs.getString("fragmentPackets", "tlshello") ?: "tlshello"

            // Get Routing Configuration details
            val routingPrefs = context.getSharedPreferences("zivpn_routing_settings", Context.MODE_PRIVATE)
            val domainStrategy = routingPrefs.getString("domainStrategy", "AsIs") ?: "AsIs"

            // Log parameters
            Log.d(TAG, "Generating config for $proxyName [$proxyType]. Insecure: $allowInsecure")

            // Parse routing rules dynamically
            val routingRulesArr = JSONArray()
            val domainsArr = JSONArray()
            
            val rulesStr = routingPrefs.getString("routing_rules_json", null)
            val jsonRules = JSONArray()
            if (rulesStr != null) {
                try {
                    val arr = JSONArray(rulesStr)
                    for (i in 0 until arr.length()) {
                        jsonRules.put(arr.getJSONObject(i))
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing rules json", e)
                }
            }
            if (jsonRules.length() == 0) {
                val defaultRules = listOf(
                    JSONObject().apply {
                        put("name", "阻断udp443")
                        put("value", "443")
                        put("outbound", "block")
                        put("isEnabled", true)
                    },
                    JSONObject().apply {
                        put("name", "代理Google")
                        put("value", "[geosite:google]")
                        put("outbound", "proxy")
                        put("isEnabled", true)
                    },
                    JSONObject().apply {
                        put("name", "绕过局域网IP")
                        put("value", "[geoip:private]")
                        put("outbound", "direct")
                        put("isEnabled", true)
                    },
                    JSONObject().apply {
                        put("name", "绕过局域网域名")
                        put("value", "[geosite:private]")
                        put("outbound", "direct")
                        put("isEnabled", true)
                    },
                    JSONObject().apply {
                        put("name", "绕过中国公共DNSIP")
                        put("value", "[geoip:cn]")
                        put("outbound", "direct")
                        put("isEnabled", true)
                    }
                )
                for (dr in defaultRules) {
                    jsonRules.put(dr)
                }
            }

            for (i in 0 until jsonRules.length()) {
                val ruleObj = jsonRules.getJSONObject(i)
                val isEnabled = ruleObj.optBoolean("isEnabled", true)
                if (isEnabled) {
                    val name = ruleObj.optString("name", "")
                    val value = ruleObj.optString("value", "")
                    val domainVal = ruleObj.optString("domain", "")
                    val ipVal = ruleObj.optString("ip", "")
                    val portVal = ruleObj.optString("port", "")
                    val protocolVal = ruleObj.optString("protocol", "")
                    val networkVal = ruleObj.optString("network", "")
                    val outbound = ruleObj.optString("outbound", "proxy")
                    
                    val xrayRule = JSONObject().apply {
                        put("type", "field")
                        put("outboundTag", outbound)
                        
                        val ruleDomains = JSONArray()
                        if (value.isNotBlank()) {
                            val cleanVal = value.removeSurrounding("[", "]").trim()
                            if (cleanVal.startsWith("geosite:") || cleanVal.startsWith("domain:") || cleanVal.startsWith("keyword:") || cleanVal.contains(".")) {
                                ruleDomains.put(cleanVal)
                                domainsArr.put(cleanVal)
                            }
                        }
                        if (domainVal.isNotBlank()) {
                            val parts = domainVal.split(",")
                            for (p in parts) {
                                val tp = p.trim()
                                if (tp.isNotEmpty()) {
                                    ruleDomains.put(tp)
                                    domainsArr.put(tp)
                                }
                            }
                        }
                        if (ruleDomains.length() > 0) {
                            put("domain", ruleDomains)
                        }

                        val ruleIps = JSONArray()
                        if (value.isNotBlank()) {
                            val cleanVal = value.removeSurrounding("[", "]").trim()
                            if (cleanVal.startsWith("geoip:") || cleanVal.contains("/") || cleanVal.matches("\\d+\\.\\d+\\.\\d+\\.\\d+".toRegex())) {
                                ruleIps.put(cleanVal)
                            }
                        }
                        if (ipVal.isNotBlank()) {
                            val parts = ipVal.split(",")
                            for (p in parts) {
                                val tp = p.trim()
                                if (tp.isNotEmpty()) ruleIps.put(tp)
                            }
                        }
                        if (ruleIps.length() > 0) {
                            put("ip", ruleIps)
                        }

                        if (portVal.isNotBlank()) {
                            put("port", portVal)
                        }

                        if (networkVal.isNotBlank()) {
                            put("network", networkVal)
                        }

                        if (protocolVal.isNotBlank()) {
                            val protocolsArr = JSONArray()
                            val parts = protocolVal.split(",")
                            for (p in parts) {
                                val tp = p.trim()
                                if (tp.isNotEmpty()) protocolsArr.put(tp)
                            }
                            put("protocol", protocolsArr)
                        }
                    }
                    
                    if (xrayRule.has("domain") || xrayRule.has("ip") || xrayRule.has("port") || xrayRule.has("network") || xrayRule.has("protocol")) {
                        routingRulesArr.put(xrayRule)
                    }
                }
            }

            if (domainsArr.length() == 0) {
                val fallbackDoms = listOf(
                    "geosite:youtube", "geosite:category-games", "geosite:category-media",
                    "geosite:tiktok", "geosite:facebook", "geosite:instagram", "geosite:twitter",
                    "geosite:telegram", "geosite:netflix", "geosite:disney", "geosite:hbo",
                    "geosite:spotify", "geosite:twitch"
                )
                for (fd in fallbackDoms) {
                    domainsArr.put(fd)
                }
            }

            // 3. Build DNS section
            val dnsObj = JSONObject().apply {
                val hostsObj = JSONObject().apply {
                    put("domain:googleapis.cn", "googleapis.com")
                    
                    put("dns.alidns.com", JSONArray().apply {
                        put("223.5.5.5")
                        put("223.6.6.6")
                        put("2400:3200::1")
                        put("2400:3200:baba::1")
                    })
                    put("one.one.one.one", JSONArray().apply {
                        put("1.1.1.1")
                        put("1.0.0.1")
                        put("2606:4700:4700::1111")
                        put("2606:4700:4700::1001")
                    })
                    put("dns.cloudflare.com", JSONArray().apply {
                        put("104.16.132.229")
                        put("104.16.133.229")
                        put("2606:4700::6810:84e5")
                        put("2606:4700::6810:85e5")
                    })
                    put("cloudflare-dns.com", JSONArray().apply {
                        put("104.16.248.249")
                        put("104.16.249.249")
                        put("2606:4700::6810:f8f9")
                        put("2606:4700::6810:f9f9")
                    })
                    put("dot.pub", JSONArray().apply {
                        put("1.12.12.12")
                        put("120.53.53.53")
                    })
                    put("dns.google", JSONArray().apply {
                        put("8.8.8.8")
                        put("8.8.4.4")
                        put("2001:4860:4860::8888")
                        put("2001:4860:4860::8844")
                    })
                    put("dns.quad9.net", JSONArray().apply {
                        put("9.9.9.9")
                        put("149.112.112.112")
                        put("2620:fe::fe")
                        put("2620:fe::9")
                    })
                    put("common.dot.dns.yandex.net", JSONArray().apply {
                        put("77.88.8.8")
                        put("77.88.8.1")
                        put("2a02:6b8::feed:0ff")
                        put("2a02:6b8:0:1::feed:0ff")
                    })
                    
                    if (dnsHostsStr.isNotBlank()) {
                        val hostsParts = dnsHostsStr.split(",")
                        for (part in hostsParts) {
                            val kv = part.split(":")
                            if (kv.size == 2) {
                                put(kv[0].trim(), kv[1].trim())
                            }
                        }
                    }
                    
                    val serverAddress = parseServerAddress(fullConfig, proxyType, proxyName)
                    if (serverAddress.isNotBlank() && !has(serverAddress)) {
                        put(serverAddress, "162.159.130.11")
                    }
                    
                    try {
                        val dnsUri = java.net.URI(remoteDns)
                        val dnsHost = dnsUri.host
                        if (!dnsHost.isNullOrBlank() && !has(dnsHost)) {
                            put(dnsHost, "94.140.14.14")
                        }
                    } catch (e: Exception) {
                        put("dns.adguard-dns.com", "94.140.14.14")
                    }
                }
                
                put("hosts", hostsObj)
                
                put("servers", JSONArray().apply {
                    put(remoteDns)
                    put(JSONObject().apply {
                        put("address", remoteDns)
                        put("domains", domainsArr)
                    })
                    put(JSONObject().apply {
                        put("address", domesticDns)
                        put("domains", domainsArr)
                        put("skipFallback", true)
                        put("tag", "domestic-dns")
                    })
                })
                
                put("tag", "dns-module")
            }

            // 4. Build inbounds
            val inboundsArr = JSONArray()
            
            // Socks inbound
            val socksInbound = JSONObject().apply {
                put("listen", "127.0.0.1")
                put("port", localProxyPort)
                put("protocol", "socks")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("destOverride", JSONArray().apply { put("http"); put("tls") })
                    put("enabled", enableSniffing)
                    put("routeOnly", enableRouteOnly)
                })
                put("tag", "socks")
            }
            inboundsArr.put(socksInbound)

            // HTTP inbound
            val httpInbound = JSONObject().apply {
                put("listen", "127.0.0.1")
                put("port", httpProxyPort)
                put("protocol", "http")
                put("settings", JSONObject().apply {
                    put("auth", "noauth")
                    put("udp", true)
                    put("userLevel", 8)
                })
                put("sniffing", JSONObject().apply {
                    put("destOverride", JSONArray().apply { put("http"); put("tls") })
                    put("enabled", enableSniffing)
                    put("routeOnly", enableRouteOnly)
                })
                put("tag", "http")
            }
            inboundsArr.put(httpInbound)

            // 5. Build Log block
            val accessLogPath = java.io.File(context.filesDir, "access.log").absolutePath
            val errorLogPath = java.io.File(context.filesDir, "error.log").absolutePath
            
            // clear old logs on start
            try {
                java.io.File(accessLogPath).writeText("")
            } catch (e: Exception) {}
            
            val logObj = JSONObject().apply {
                put("loglevel", logLevel)
                put("access", accessLogPath)
                put("error", errorLogPath)
            }

            // 6. Build selected Proxy outbound
            val proxyOutbound = buildProxyOutbound(fullConfig, proxyType, proxyName, allowInsecure, enableMux, tcpConnections, xudpConnections, handlingQuicMux, enableFragment, fragmentLength, fragmentInterval, fragmentPackets)

            // Direct and block outbounds
            val directOutbound = JSONObject().apply {
                put("protocol", "freedom")
                put("settings", JSONObject().apply {
                    put("domainStrategy", "UseIP")
                })
                put("tag", "direct")
            }

            val blockOutbound = JSONObject().apply {
                put("protocol", "blackhole")
                put("settings", JSONObject().apply {
                    put("response", JSONObject().apply {
                        put("type", "http")
                    })
                })
                put("tag", "block")
            }

            val outboundsArr = JSONArray().apply {
                put(proxyOutbound)
                put(directOutbound)
                put(blockOutbound)
            }

            // 7. Policy
            val policyObj = JSONObject().apply {
                put("levels", JSONObject().apply {
                    put("8", JSONObject().apply {
                        put("connIdle", 300)
                        put("downlinkOnly", 1)
                        put("handshake", 4)
                        put("uplinkOnly", 1)
                    })
                })
                put("system", JSONObject().apply {
                    put("statsOutboundUplink", true)
                    put("statsOutboundDownlink", true)
                })
            }

            // 8. Routing
            val routingObj = JSONObject().apply {
                put("domainStrategy", domainStrategy)
                
                // Append standard DNS inbound rules
                routingRulesArr.put(JSONObject().apply {
                    put("inboundTag", JSONArray().apply { put("domestic-dns") })
                    put("outboundTag", "direct")
                    put("type", "field")
                })
                routingRulesArr.put(JSONObject().apply {
                    put("inboundTag", JSONArray().apply { put("dns-module") })
                    put("outboundTag", "proxy")
                    put("type", "field")
                })
                
                put("rules", routingRulesArr)
            }

            // 9. Put everything together
            val rootObj = JSONObject().apply {
                put("dns", dnsObj)
                put("inbounds", inboundsArr)
                put("log", logObj)
                put("outbounds", outboundsArr)
                put("policy", policyObj)
                put("remarks", if (proxyName.isNotBlank()) proxyName else "sicepat_xray")
                put("routing", routingObj)
                put("stats", JSONObject())
            }

            return rootObj.toString(2)
        } catch (e: Exception) {
            Log.e(TAG, "Error generating Xray config JSON", e)
            return ""
        }
    }

    data class ParsedProfile(
        var configType: String = "",
        var remarks: String = "",
        var server: String = "",
        var serverPort: Int = 443,
        var passwordOrUuid: String = "",
        var method: String = "", 
        var network: String = "tcp", 
        var headerType: String = "none",
        var host: String = "",
        var path: String = "",
        var security: String = "none", 
        var sni: String = "",
        var alpn: String = "",
        var fingerPrint: String = "",
        var allowInsecure: Boolean = true,
        var publicKey: String = "", 
        var shortId: String = "", 
        var spiderX: String = "", 
        var flow: String = "", 
        var localAddress: String = "",
        var preSharedKey: String = "",
        var reserved: String = "",
        var mtu: Int = 1420,
        var obfs: String = "",
        var obfsPassword: String = "",
        var portHopping: String = "",
        var username: String = ""
    )

    private fun decodeBase64(input: String): String {
        val clean = input.replace("\\s".toRegex(), "").trim()
        if (clean.isEmpty()) return ""
        for (flag in listOf(Base64.DEFAULT, Base64.NO_PADDING, Base64.URL_SAFE, Base64.NO_WRAP)) {
            try {
                val bytes = Base64.decode(clean, flag)
                val decoded = String(bytes, Charsets.UTF_8)
                if (decoded.isNotEmpty()) return decoded
            } catch (e: Exception) {
                // Try next flag
            }
        }
        return ""
    }

    private fun getQueryParams(query: String?): Map<String, String> {
        val map = HashMap<String, String>()
        if (query.isNullOrBlank()) return map
        val pairs = query.split("&")
        for (pair in pairs) {
            val idx = pair.indexOf("=")
            if (idx != -1) {
                try {
                    val key = URLDecoder.decode(pair.substring(0, idx), "UTF-8").lowercase()
                    val value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                    map[key] = value
                } catch (e: Exception) {
                    // Ignore decoding errors
                }
            }
        }
        return map
    }

    fun parseUri(uriStr: String, defaultName: String = "Proxy"): ParsedProfile {
        val profile = ParsedProfile()
        val trimmed = uriStr.trim()
        if (trimmed.isEmpty()) return profile

        var working = trimmed
        var fragment = ""
        val hashIdx = working.indexOf("#")
        if (hashIdx != -1) {
            fragment = working.substring(hashIdx + 1)
            working = working.substring(0, hashIdx)
        }
        try {
            profile.remarks = URLDecoder.decode(fragment, "UTF-8")
        } catch (e: Exception) {
            profile.remarks = fragment
        }
        if (profile.remarks.isBlank()) {
            profile.remarks = defaultName
        }

        if (working.startsWith("vmess://", ignoreCase = true)) {
            profile.configType = "vmess"
            val body = working.substring(8).trim()
            if (body.contains("?") && body.contains("&")) {
                parseStandardUri(working, profile)
            } else {
                val decoded = decodeBase64(body)
                if (decoded.isNotEmpty()) {
                    try {
                        val json = JSONObject(decoded)
                        profile.remarks = json.optString("ps", profile.remarks)
                        profile.server = json.optString("add", "")
                        profile.serverPort = json.optInt("port", 443)
                        profile.passwordOrUuid = json.optString("id", "")
                        profile.method = json.optString("scy", "auto")
                        profile.network = json.optString("net", "tcp")
                        profile.headerType = json.optString("type", "none")
                        profile.host = json.optString("host", "")
                        profile.path = json.optString("path", "")
                        profile.security = json.optString("tls", "none")
                        profile.sni = json.optString("sni", "")
                        profile.fingerPrint = json.optString("fp", "")
                        profile.alpn = json.optString("alpn", "")
                        val ins = json.optString("insecure", "")
                        profile.allowInsecure = (ins == "1" || ins.lowercase() == "true")
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing base64 VMess JSON", e)
                    }
                }
            }
        } else if (working.startsWith("vless://", ignoreCase = true)) {
            profile.configType = "vless"
            parseStandardUri(working, profile)
        } else if (working.startsWith("trojan://", ignoreCase = true)) {
            profile.configType = "trojan"
            parseStandardUri(working, profile)
        } else if (working.startsWith("ss://", ignoreCase = true)) {
            profile.configType = "shadowsocks"
            val body = working.substring(5).trim()
            if (body.contains("@")) {
                val userInfo = body.substringBefore("@")
                val remaining = body.substringAfter("@")
                val decoded = decodeBase64(userInfo)
                val parts = decoded.split(":")
                if (parts.size >= 2) {
                    profile.method = parts[0]
                    profile.passwordOrUuid = parts[1]
                } else {
                    profile.passwordOrUuid = decoded
                }
                
                val qIdx = remaining.indexOf("?")
                val serverPart = if (qIdx != -1) remaining.substring(0, qIdx) else remaining
                val queryPart = if (qIdx != -1) remaining.substring(qIdx + 1) else ""
                
                if (serverPart.contains(":")) {
                    profile.server = serverPart.substringBeforeLast(":")
                    profile.serverPort = serverPart.substringAfterLast(":").toIntOrNull() ?: 8388
                } else {
                    profile.server = serverPart
                    profile.serverPort = 8388
                }
                
                if (queryPart.isNotEmpty()) {
                    val params = getQueryParams(queryPart)
                    if (params.containsKey("plugin") && params["plugin"]?.contains("obfs=http") == true) {
                        profile.network = "tcp"
                        profile.headerType = "http"
                        val pluginVal = params["plugin"] ?: ""
                        for (pair in pluginVal.split(";")) {
                            val kv = pair.split("=")
                            if (kv.size == 2) {
                                if (kv[0] == "obfs-host") profile.host = kv[1]
                                if (kv[0] == "path") profile.path = kv[1]
                            }
                        }
                    }
                }
            } else {
                val decoded = decodeBase64(body)
                if (decoded.contains("@")) {
                    val legacyPattern = "^(.+?):(.*)@(.+?):(\\d+?)/?$".toRegex()
                    val match = legacyPattern.matchEntire(decoded)
                    if (match != null) {
                        profile.method = match.groupValues[1].lowercase()
                        profile.passwordOrUuid = match.groupValues[2]
                        profile.server = match.groupValues[3].removeSurrounding("[", "]")
                        profile.serverPort = match.groupValues[4].toIntOrNull() ?: 8388
                    }
                }
            }
        } else if (working.startsWith("hysteria2://", ignoreCase = true) || working.startsWith("hysteria://", ignoreCase = true)) {
            profile.configType = "hysteria2"
            parseStandardUri(working, profile)
        } else if (working.startsWith("wireguard://", ignoreCase = true) || working.startsWith("wg://", ignoreCase = true)) {
            profile.configType = "wireguard"
            parseStandardUri(working, profile)
        } else if (working.startsWith("socks5://", ignoreCase = true) || working.startsWith("socks://", ignoreCase = true)) {
            profile.configType = "socks"
            parseStandardUri(working, profile)
        } else {
            profile.configType = "freedom"
        }

        return profile
    }

    private fun parseStandardUri(uriStr: String, profile: ParsedProfile) {
        try {
            val cleaned = uriStr.replace(" ", "%20")
            val uri = java.net.URI(cleaned)
            profile.server = uri.host ?: ""
            profile.serverPort = if (uri.port != -1) uri.port else 443
            
            val userInfo = uri.userInfo
            if (!userInfo.isNullOrBlank()) {
                if (profile.configType == "socks" && userInfo.contains(":")) {
                    val parts = userInfo.split(":", limit = 2)
                    profile.username = parts[0]
                    profile.passwordOrUuid = parts[1]
                } else if (profile.configType == "socks") {
                    val decoded = decodeBase64(userInfo)
                    if (decoded.contains(":")) {
                        val parts = decoded.split(":", limit = 2)
                        profile.username = parts[0]
                        profile.passwordOrUuid = parts[1]
                    } else {
                        profile.username = decoded
                    }
                } else {
                    profile.passwordOrUuid = userInfo
                }
            }

            val query = uri.query
            val params = getQueryParams(query)
            
            if (params.containsKey("security")) profile.security = params["security"] ?: "none"
            if (params.containsKey("tls")) profile.security = params["tls"] ?: "none"
            if (params.containsKey("type")) profile.network = params["type"] ?: "tcp"
            if (params.containsKey("net")) profile.network = params["net"] ?: "tcp"
            if (params.containsKey("path")) profile.path = params["path"] ?: ""
            if (params.containsKey("host")) profile.host = params["host"] ?: ""
            if (params.containsKey("sni")) profile.sni = params["sni"] ?: ""
            if (params.containsKey("fp")) profile.fingerPrint = params["fp"] ?: ""
            if (params.containsKey("alpn")) profile.alpn = params["alpn"] ?: ""
            if (params.containsKey("flow")) profile.flow = params["flow"] ?: ""
            if (params.containsKey("encryption")) profile.method = params["encryption"] ?: ""
            if (params.containsKey("pbk")) profile.publicKey = params["pbk"] ?: ""
            if (params.containsKey("sid")) profile.shortId = params["sid"] ?: ""
            if (params.containsKey("spx")) profile.spiderX = params["spx"] ?: ""

            if (params.containsKey("insecure")) {
                val ins = params["insecure"] ?: ""
                profile.allowInsecure = (ins == "1" || ins.lowercase() == "true")
            }
            if (params.containsKey("allowinsecure")) {
                val ins = params["allowinsecure"] ?: ""
                profile.allowInsecure = (ins == "1" || ins.lowercase() == "true")
            }

            if (params.containsKey("address")) profile.localAddress = params["address"] ?: ""
            if (params.containsKey("publickey")) profile.publicKey = params["publickey"] ?: ""
            if (params.containsKey("presharedkey")) profile.preSharedKey = params["presharedkey"] ?: ""
            if (params.containsKey("reserved")) profile.reserved = params["reserved"] ?: ""
            if (params.containsKey("mtu")) profile.mtu = params["mtu"]?.toIntOrNull() ?: 1420

            if (params.containsKey("obfs")) profile.obfs = params["obfs"] ?: ""
            if (params.containsKey("obfs-password")) profile.obfsPassword = params["obfs-password"] ?: ""
            if (params.containsKey("mport")) profile.portHopping = params["mport"] ?: ""

        } catch (e: Exception) {
            Log.e(TAG, "Standard URI parsing failed: $uriStr", e)
            try {
                var working = uriStr.substringAfter("://").substringBefore("#")
                if (working.contains("@")) {
                    val ui = working.substringBefore("@")
                    if (profile.configType == "socks" && ui.contains(":")) {
                        val parts = ui.split(":", limit = 2)
                        profile.username = parts[0]
                        profile.passwordOrUuid = parts[1]
                    } else {
                        profile.passwordOrUuid = ui
                    }
                    working = working.substringAfter("@")
                }
                var queryStr = ""
                if (working.contains("?")) {
                    queryStr = working.substringAfter("?")
                    working = working.substringBefore("?")
                }
                if (working.contains(":")) {
                    profile.server = working.substringBefore(":")
                    profile.serverPort = working.substringAfter(":").toIntOrNull() ?: profile.serverPort
                } else {
                    profile.server = working
                }

                if (queryStr.isNotEmpty()) {
                    val params = getQueryParams(queryStr)
                    if (params.containsKey("security")) profile.security = params["security"] ?: "none"
                    if (params.containsKey("tls")) profile.security = params["tls"] ?: "none"
                    if (params.containsKey("type")) profile.network = params["type"] ?: "tcp"
                    if (params.containsKey("net")) profile.network = params["net"] ?: "tcp"
                    if (params.containsKey("path")) profile.path = params["path"] ?: ""
                    if (params.containsKey("host")) profile.host = params["host"] ?: ""
                    if (params.containsKey("sni")) profile.sni = params["sni"] ?: ""
                    if (params.containsKey("fp")) profile.fingerPrint = params["fp"] ?: ""
                    if (params.containsKey("alpn")) profile.alpn = params["alpn"] ?: ""
                    if (params.containsKey("flow")) profile.flow = params["flow"] ?: ""
                    if (params.containsKey("encryption")) profile.method = params["encryption"] ?: ""
                    if (params.containsKey("pbk")) profile.publicKey = params["pbk"] ?: ""
                    if (params.containsKey("sid")) profile.shortId = params["sid"] ?: ""
                    if (params.containsKey("spx")) profile.spiderX = params["spx"] ?: ""
                    if (params.containsKey("address")) profile.localAddress = params["address"] ?: ""
                    if (params.containsKey("publickey")) profile.publicKey = params["publickey"] ?: ""
                    if (params.containsKey("presharedkey")) profile.preSharedKey = params["presharedkey"] ?: ""
                    if (params.containsKey("reserved")) profile.reserved = params["reserved"] ?: ""
                    if (params.containsKey("mtu")) profile.mtu = params["mtu"]?.toIntOrNull() ?: 1420
                    if (params.containsKey("obfs")) profile.obfs = params["obfs"] ?: ""
                    if (params.containsKey("obfs-password")) profile.obfsPassword = params["obfs-password"] ?: ""
                    if (params.containsKey("mport")) profile.portHopping = params["mport"] ?: ""
                }
            } catch (ex: Exception) {
                Log.e(TAG, "RegEx standard URI parsing failed too", ex)
            }
        }
    }

    private fun parseServerAddress(configUri: String, type: String, name: String): String {
        try {
            if (configUri.isBlank()) return name.substringBefore(":")
            val profile = parseUri(configUri, name)
            if (profile.server.isNotBlank()) {
                return profile.server
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing server address", e)
        }
        return name.substringBefore(":")
    }

    private fun buildProxyOutbound(
        configUri: String,
        type: String,
        name: String,
        allowInsecure: Boolean,
        enableMux: Boolean,
        tcpConnections: String,
        xudpConnections: String,
        handlingQuicMux: String,
        enableFragment: Boolean,
        fragmentLength: String,
        fragmentInterval: String,
        fragmentPackets: String
    ): JSONObject {
        val trimmed = configUri.trim()
        if (trimmed.startsWith("{")) {
            try {
                val json = JSONObject(trimmed)
                if (json.has("protocol") || json.has("settings")) {
                    json.put("tag", "proxy")
                    return json
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse raw JSON outbound, fallback to standard parsing", e)
            }
        }

        val profile = parseUri(configUri, name)
        
        val outbound = JSONObject()
        val settings = JSONObject()
        val streamSettings = JSONObject()
        
        val protocol = if (profile.configType.isNotBlank()) profile.configType else type.lowercase()
        outbound.put("tag", "proxy")
        outbound.put("protocol", protocol)

        when (protocol) {
            "vmess" -> {
                settings.put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", profile.server)
                        put("port", profile.serverPort)
                        put("users", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", profile.passwordOrUuid)
                                put("alterId", 0)
                                put("security", if (profile.method.isNotBlank()) profile.method else "auto")
                                put("level", 8)
                            })
                        })
                    })
                })
            }
            "vless" -> {
                settings.put("vnext", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", profile.server)
                        put("port", profile.serverPort)
                        put("users", JSONArray().apply {
                            put(JSONObject().apply {
                                put("id", profile.passwordOrUuid)
                                put("encryption", if (profile.method.isNotBlank()) profile.method else "none")
                                if (profile.flow.isNotBlank()) {
                                    put("flow", profile.flow)
                                }
                                put("level", 8)
                            })
                        })
                    })
                })
            }
            "trojan" -> {
                settings.put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", profile.server)
                        put("port", profile.serverPort)
                        put("password", profile.passwordOrUuid)
                        put("level", 8)
                    })
                })
            }
            "shadowsocks" -> {
                settings.put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", profile.server)
                        put("port", profile.serverPort)
                        put("method", if (profile.method.isNotBlank()) profile.method else "aes-128-gcm")
                        put("password", profile.passwordOrUuid)
                        put("level", 8)
                    })
                })
            }
            "socks" -> {
                settings.put("servers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("address", profile.server)
                        put("port", profile.serverPort)
                        if (profile.username.isNotBlank()) {
                            put("users", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("user", profile.username)
                                    put("pass", profile.passwordOrUuid)
                                    put("level", 8)
                                })
                            })
                        }
                    })
                })
            }
            "hysteria2" -> {
                settings.put("address", profile.server)
                settings.put("port", profile.serverPort)
                settings.put("version", 2)
                if (profile.passwordOrUuid.isNotBlank()) {
                    settings.put("auth", profile.passwordOrUuid)
                }
            }
            "wireguard" -> {
                settings.put("secretKey", profile.passwordOrUuid)
                val localAddressList = JSONArray()
                if (profile.localAddress.isNotBlank()) {
                    for (addr in profile.localAddress.split(",")) {
                        localAddressList.put(addr.trim())
                    }
                } else {
                    localAddressList.put("10.0.0.2/32")
                }
                settings.put("address", localAddressList)
                
                settings.put("peers", JSONArray().apply {
                    put(JSONObject().apply {
                        put("publicKey", profile.publicKey)
                        if (profile.preSharedKey.isNotBlank()) {
                            put("preSharedKey", profile.preSharedKey)
                        }
                        put("endpoint", "${profile.server}:${profile.serverPort}")
                    })
                })
                settings.put("mtu", profile.mtu)
                if (profile.reserved.isNotBlank()) {
                    val reservedList = JSONArray()
                    for (r in profile.reserved.split(",")) {
                        val parsedR = r.trim().toIntOrNull()
                        if (parsedR != null) {
                            reservedList.put(parsedR)
                        }
                    }
                    if (reservedList.length() > 0) {
                        settings.put("reserved", reservedList)
                    }
                }
            }
            else -> {
                outbound.put("protocol", "freedom")
                settings.put("domainStrategy", "UseIP")
            }
        }
        
        outbound.put("settings", settings)
        
        val outboundProtocol = outbound.optString("protocol", "freedom")
        if (outboundProtocol != "freedom") {
            val net = if (profile.network.isNotBlank()) profile.network else "tcp"
            streamSettings.put("network", net)
            
            val sec = if (profile.security.isNotBlank()) profile.security else "none"
            streamSettings.put("security", sec)
            
            val sockopt = JSONObject().apply {
                put("domainStrategy", "UseIP")
                put("happyEyeballs", JSONObject().apply {
                    put("interleave", 2)
                    put("maxConcurrentTry", 4)
                    put("prioritizeIPv6", false)
                    put("tryDelayMs", 250)
                })
            }
            
            if (enableFragment) {
                var packets = fragmentPackets
                val secLower = sec.lowercase()
                if (secLower == "reality" && packets == "tlshello") {
                    packets = "1-3"
                } else if ((secLower == "tls" || secLower == "xtls") && packets != "tlshello") {
                    packets = "tlshello"
                }
                val fragmentObj = JSONObject().apply {
                    put("packets", packets)
                    put("length", fragmentLength)
                    put("interval", fragmentInterval)
                }
                sockopt.put("fragment", fragmentObj)
            }
            streamSettings.put("sockopt", sockopt)
            
            if (sec == "tls" || sec == "xtls") {
                val tlsSettings = JSONObject().apply {
                    put("allowInsecure", allowInsecure || profile.allowInsecure)
                    put("serverName", if (profile.sni.isNotBlank()) profile.sni else (if (profile.host.isNotBlank()) profile.host else profile.server))
                    if (profile.alpn.isNotBlank()) {
                        put("alpn", JSONArray().apply {
                            for (a in profile.alpn.split(",")) {
                                put(a.trim())
                            }
                        })
                    }
                    put("show", false)
                }
                streamSettings.put("${sec}Settings", tlsSettings)
            } else if (sec == "reality") {
                val realitySettings = JSONObject().apply {
                    put("serverName", if (profile.sni.isNotBlank()) profile.sni else (if (profile.host.isNotBlank()) profile.host else profile.server))
                    put("publicKey", profile.publicKey)
                    put("shortId", profile.shortId)
                    if (profile.spiderX.isNotBlank()) {
                        put("spiderX", profile.spiderX)
                    }
                    put("show", false)
                }
                streamSettings.put("realitySettings", realitySettings)
            }
            
            when (net) {
                "ws" -> {
                    val wsSettings = JSONObject().apply {
                        val pathVal = if (profile.path.isNotBlank()) profile.path else "/ws"
                        put("path", if (pathVal.startsWith("/")) pathVal else "/$pathVal")
                        put("headers", JSONObject().apply {
                            put("Host", if (profile.host.isNotBlank()) profile.host else profile.server)
                        })
                    }
                    streamSettings.put("wsSettings", wsSettings)
                }
                "grpc" -> {
                    val grpcSettings = JSONObject().apply {
                        put("serviceName", if (profile.path.isNotBlank()) profile.path else "grpc")
                        put("multiMode", false)
                    }
                    streamSettings.put("grpcSettings", grpcSettings)
                }
            }
            
            if (protocol == "hysteria2") {
                streamSettings.put("network", "udp")
                streamSettings.put("security", "tls")
                
                val hysteriaSettings = JSONObject().apply {
                    put("version", 2)
                    put("auth", profile.passwordOrUuid)
                }
                streamSettings.put("hysteriaSettings", hysteriaSettings)
                
                val tlsSettings = JSONObject().apply {
                    put("allowInsecure", allowInsecure || profile.allowInsecure)
                    put("serverName", if (profile.sni.isNotBlank()) profile.sni else (if (profile.host.isNotBlank()) profile.host else profile.server))
                    put("alpn", JSONArray().apply { put("h3") })
                    put("show", false)
                }
                streamSettings.put("tlsSettings", tlsSettings)
                
                if (profile.obfsPassword.isNotBlank()) {
                    val udpmasks = JSONArray().apply {
                        put(JSONObject().apply {
                            put("type", "salamander")
                            put("settings", JSONObject().apply {
                                put("password", profile.obfsPassword)
                            })
                        })
                    }
                    streamSettings.put("udpmasks", udpmasks)
                }
            }
            
            outbound.put("streamSettings", streamSettings)
        }
        
        val muxObj = JSONObject().apply {
            put("enabled", enableMux)
            put("concurrency", if (enableMux) (tcpConnections.toIntOrNull() ?: 8) else -1)
            if (enableMux) {
                put("xudpConcurrency", xudpConnections.toIntOrNull() ?: 8)
                put("xudpProxyUDP443", handlingQuicMux)
            }
        }
        outbound.put("mux", muxObj)
        
        return outbound
    }

    fun generateProxyUri(proxy: ProxyItem): String {
        if (proxy.fullConfig.isNotBlank()) {
            return proxy.fullConfig
        }
        
        val name = proxy.name
        val type = proxy.type.lowercase()
        
        var address = name.substringBefore(":")
        var port = 443
        val portMatch = ":(\\d+)".toRegex().find(name)
        if (portMatch != null) {
            port = portMatch.groupValues[1].toIntOrNull() ?: port
        }
        
        val uuidOrPassword = "26a62d0f-47ea-4e36-9f17-d69eb15cf01e"
        val net = "ws"
        val security = "tls"
        val path = "/trojan-ws"
        val host = "investors.spotify.com.ah.hamxtun.web.id"
        val method = "aes-128-gcm"
        
        val encName = try {
            java.net.URLEncoder.encode(name, "UTF-8")
        } catch (e: Exception) {
            name
        }
        
        return when (type) {
            "vmess" -> {
                val json = JSONObject().apply {
                    put("v", "2")
                    put("ps", name)
                    put("add", address)
                    put("port", port)
                    put("id", uuidOrPassword)
                    put("aid", "0")
                    put("net", net)
                    put("type", "none")
                    put("host", host)
                    put("path", path)
                    put("tls", security)
                    put("sni", host)
                }
                val b64 = Base64.encodeToString(json.toString().toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "vmess://$b64"
            }
            "vless" -> {
                val encPath = try { java.net.URLEncoder.encode(path, "UTF-8") } catch (e: Exception) { path }
                val encHost = try { java.net.URLEncoder.encode(host, "UTF-8") } catch (e: Exception) { host }
                "vless://$uuidOrPassword@$address:$port?type=$net&security=$security&path=$encPath&host=$encHost&sni=$encHost#$encName"
            }
            "trojan" -> {
                val encPath = try { java.net.URLEncoder.encode(path, "UTF-8") } catch (e: Exception) { path }
                val encHost = try { java.net.URLEncoder.encode(host, "UTF-8") } catch (e: Exception) { host }
                "trojan://$uuidOrPassword@$address:$port?type=$net&security=$security&path=$encPath&host=$encHost&sni=$encHost#$encName"
            }
            "shadowsocks", "ss" -> {
                val credential = "$method:$uuidOrPassword"
                val b64Credential = Base64.encodeToString(credential.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
                "ss://$b64Credential@$address:$port#$encName"
            }
            else -> {
                "socks5://$address:$port#$encName"
            }
        }
    }
}
