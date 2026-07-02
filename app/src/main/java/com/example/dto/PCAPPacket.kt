package com.example.dto

data class PCAPPacket(
    val id: Int,
    val timestamp: String,
    val sourceIp: String,
    val destIp: String,
    val sourcePort: Int,
    val destPort: Int,
    val protocol: String,
    val size: Int,
    val flags: String,
    val latencyMs: Int,
    val threatLevel: String, // "CLEAN", "WARNING", "CRITICAL"
    val payloadHex: String,
    val payloadAscii: String,
    val appName: String? = null
)
