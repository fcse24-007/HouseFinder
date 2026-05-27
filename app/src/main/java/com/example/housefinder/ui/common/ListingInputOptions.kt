package com.example.housefinder.ui.common

import com.example.housefinder.db.entities.ListingCatalog

object ListingInputOptions {
    val gaboroneAreas: List<String> = ListingCatalog.gaboroneAreas
    val roomTypeLabels: List<String> = ListingCatalog.roomTypeLabels
    val supportedRoomTypes: Set<String> = ListingCatalog.supportedRoomTypes.toSet()

    fun toStorageType(label: String): String? = ListingCatalog.toStorageType(label)

    fun toDisplayType(storageType: String?): String = ListingCatalog.toDisplayType(storageType)
}
