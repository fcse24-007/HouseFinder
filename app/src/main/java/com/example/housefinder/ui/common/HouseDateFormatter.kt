package com.example.housefinder.ui.common

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object HouseDateFormatter {
    private const val STORAGE_PATTERN = "yyyy-MM-dd"
    private const val DISPLAY_PATTERN = "dd-MM-yyyy"

    private fun formatter(pattern: String): SimpleDateFormat =
        SimpleDateFormat(pattern, Locale.getDefault()).apply { isLenient = false }

    fun toDisplayDate(storageDate: String?): String {
        if (storageDate.isNullOrBlank()) return ""
        return parse(storageDate, STORAGE_PATTERN)?.let { formatter(DISPLAY_PATTERN).format(it) }
            ?: parse(storageDate, DISPLAY_PATTERN)?.let { formatter(DISPLAY_PATTERN).format(it) }
            ?: storageDate
    }

    fun toStorageDate(userInput: String): String? {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) return null
        return parse(trimmed, DISPLAY_PATTERN)?.let { formatter(STORAGE_PATTERN).format(it) }
            ?: parse(trimmed, STORAGE_PATTERN)?.let { formatter(STORAGE_PATTERN).format(it) }
    }

    private fun parse(value: String, pattern: String): Date? {
        return runCatching { formatter(pattern).parse(value) }.getOrNull()
    }
}
