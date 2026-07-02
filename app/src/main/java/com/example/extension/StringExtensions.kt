package com.example.extension

fun String?.isNotNullEmpty(): Boolean {
    return this != null && this.isNotEmpty()
}

fun String?.nullIfBlank(): String? {
    return if (this.isNullOrBlank()) null else this
}

fun String?.toIntOrDefault(default: Int): Int {
    if (this == null) return default
    return this.toIntOrNull() ?: default
}
