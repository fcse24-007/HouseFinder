package com.example.housefinder.ui.common

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object HouseDateFormatter {
    private val storageFormatter: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
    private val displayFormatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("dd-MM-uuuu", Locale.getDefault())

    fun toDisplayDate(storageDate: String?): String {
        if (storageDate.isNullOrBlank()) return ""
        return runCatching { LocalDate.parse(storageDate, storageFormatter).format(displayFormatter) }
            .recoverCatching { LocalDate.parse(storageDate, displayFormatter).format(displayFormatter) }
            .getOrElse { storageDate }
    }

    fun toStorageDate(userInput: String): String? {
        val trimmed = userInput.trim()
        if (trimmed.isEmpty()) return null
        return runCatching { LocalDate.parse(trimmed, displayFormatter).format(storageFormatter) }
            .recoverCatching { LocalDate.parse(trimmed, storageFormatter).format(storageFormatter) }
            .getOrNull()
    }
}
