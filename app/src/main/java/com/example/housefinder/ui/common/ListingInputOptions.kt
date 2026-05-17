package com.example.housefinder.ui.common

object ListingInputOptions {
    val gaboroneAreas = listOf(
        "Block 6",
        "Block 7",
        "Block 8",
        "Block 9",
        "Broadhurst",
        "Gaborone West",
        "The Village",
        "Tlokweng",
        "Mogoditshane"
    )

    private val roomTypeToStorage = linkedMapOf(
        "En Suite" to "EN_SUITE",
        "Shared" to "SHARED",
        "Studio" to "STUDIO",
        "Flat" to "FLAT",
        "House" to "HOUSE"
    )

    val roomTypeLabels: List<String> = roomTypeToStorage.keys.toList()
    val supportedRoomTypes: Set<String> = roomTypeToStorage.values.toSet()

    fun toStorageType(label: String): String? = roomTypeToStorage[label]

    fun toDisplayType(storageType: String?): String {
        if (storageType.isNullOrBlank()) return ""
        return roomTypeToStorage.entries.firstOrNull { it.value == storageType }?.key
            ?: storageType
    }
}
