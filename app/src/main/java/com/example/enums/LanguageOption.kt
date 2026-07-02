package com.example.enums

enum class LanguageOption(val code: String, val displayName: String) {
    AUTO("auto", "System Default"),
    INDONESIAN("id", "Bahasa Indonesia"),
    ENGLISH("en", "English (US)");

    companion object {
        fun fromCode(code: String?): LanguageOption {
            return values().find { it.code.equals(code, ignoreCase = true) } ?: AUTO
        }

        fun fromDisplayName(name: String?): LanguageOption {
            return values().find { it.displayName.equals(name, ignoreCase = true) } ?: AUTO
        }
    }
}
