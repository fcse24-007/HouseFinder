package com.example.housefinder.db.entities

object ListingCatalog {
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

    val roomTypeLabels = listOf(
        "En Suite",
        "Shared",
        "Studio",
        "Flat",
        "House",
        "Loft",
        "Garden Flat",
        "Duplex",
        "Townhouse",
        "Bachelor Pad",
        "Single Room",
        "Double Room",
        "Family Suite",
        "Courtyard Room",
        "Patio Suite",
        "Penthouse",
        "Corner Room",
        "Pool View",
        "City View",
        "Courtyard View",
        "Executive Studio",
        "Executive Suite",
        "Premium Studio",
        "Premium Suite",
        "Budget Room",
        "Budget Studio",
        "Budget Flat",
        "Garden Suite",
        "Courtyard Flat",
        "Terrace Studio",
        "Balcony Room",
        "Balcony Suite",
        "Shared Kitchen",
        "Private Kitchen",
        "Ensuite Deluxe",
        "Ensuite Compact",
        "Shared Deluxe",
        "Shared Compact",
        "Studio Deluxe",
        "Studio Compact",
        "Flat Deluxe",
        "Flat Compact",
        "House Deluxe",
        "House Compact",
        "Loft Deluxe",
        "Loft Compact",
        "Twin Room",
        "Triple Room",
        "Quad Room",
        "Micro Studio"
    )

    private val roomTypeToStorage: Map<String, String> = roomTypeLabels.associateWith { label ->
        label.uppercase()
            .replace(Regex("[^A-Z0-9]+"), "_")
            .trim('_')
    }

    val supportedRoomTypes: List<String> = roomTypeLabels.map { roomTypeToStorage.getValue(it) }

    fun toStorageType(label: String): String? = roomTypeToStorage[label]

    fun toDisplayType(storageType: String?): String {
        if (storageType.isNullOrBlank()) return ""
        return roomTypeToStorage.entries.firstOrNull { it.value == storageType }?.key ?: storageType
    }
}
