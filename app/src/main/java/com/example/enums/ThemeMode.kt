package com.example.enums

enum class ThemeMode(val code: String, val displayName: String) {
    LIGHT("light", "Light"),
    DARK("dark", "Dark"),
    AUTO("auto", "Auto");

    companion object {
        fun fromCode(code: String?): ThemeMode {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: DARK
        }

        fun fromDisplayName(name: String?): ThemeMode {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: DARK
        }
    }
}
