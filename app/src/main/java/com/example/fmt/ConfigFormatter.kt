package com.example.fmt

import java.util.Locale

object ConfigFormatter {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(Locale.US, "%.2f %s", bytes / Math.pow(1024.0, digitGroups.toDouble()), units[digitGroups])
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        return "${formatBytes(bytesPerSecond)}/s"
    }

    fun parsePortRange(range: String?): Pair<Int, Int>? {
        if (range.isNullOrBlank()) return null
        val parts = range.split("-")
        return try {
            if (parts.size == 1) {
                val p = parts[0].trim().toInt()
                Pair(p, p)
            } else if (parts.size == 2) {
                Pair(parts[0].trim().toInt(), parts[1].trim().toInt())
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
}
