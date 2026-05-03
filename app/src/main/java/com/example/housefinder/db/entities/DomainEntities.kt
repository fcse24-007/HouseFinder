package com.example.housefinder.db.entities

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(
    tableName = "users",
    indices = [
        Index(value = ["studentId"], unique = true),
        Index(value = ["email"], unique = true)
    ]
)
data class User(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val studentId: String,
    val name: String,
    val email: String,
    val passwordHash: String,
    val role: String,
    val university: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "listings",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("providerId"),
        Index("status"),
        Index("createdAt")
    ]
)
data class Listing(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val providerId: Int,
    val title: String,
    val description: String,
    val price: Float,
    val location: String,
    val type: String,
    val amenities: String,
    val depositAmount: Int,
    val availabilityDate: String,
    val status: String = "AVAILABLE",
    val distanceToCampusKm: Float,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "listing_images",
    foreignKeys = [
        ForeignKey(
            entity = Listing::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("listingId")
    ]
)
data class ListingImage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val listingId: Int,
    val imagePath: String,
    val sortOrder: Int = 0
)

@Entity(
    tableName = "reservations",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["studentId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = Listing::class,
            parentColumns = ["id"],
            childColumns = ["listingId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["referenceNumber"], unique = true),
        Index("studentId"),
        Index("listingId"),
        Index("status")
    ]
)
data class Reservation(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val referenceNumber: String,
    val studentId: Int,
    val listingId: Int,
    val status: String = "PENDING",
    val reservedAt: Long = System.currentTimeMillis(),
    val studentNotified: Boolean = false,
    val providerNotified: Boolean = false
)

@Entity(
    tableName = "receipts",
    foreignKeys = [
        ForeignKey(
            entity = Reservation::class,
            parentColumns = ["id"],
            childColumns = ["reservationId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["reservationId"], unique = true)
    ]
)
data class Receipt(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val reservationId: Int,
    val amountPaid: Float,
    val paymentMethod: String,
    val paidAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "user_preferences",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["userId"], unique = true)
    ]
)
data class UserPreference(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val minPrice: Float? = null,
    val maxPrice: Float? = null,
    val location: String? = null,
    val type: String? = null,
    val notificationsEnabled: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
)

data class StudentReservationDetails(
    val reservationId: Int,
    val referenceNumber: String,
    val status: String,
    val reservedAt: Long,
    val listingId: Int,
    val listingTitle: String,
    val location: String,
    val monthlyRent: Float,
    val providerId: Int,
    val providerName: String
)

data class ProviderReservationDetails(
    val reservationId: Int,
    val referenceNumber: String,
    val status: String,
    val reservedAt: Long,
    val listingId: Int,
    val listingTitle: String,
    val studentInternalId: Int,
    val studentIdentifier: String,
    val studentName: String
)

data class ListingWithImage(
    @Embedded val listing: Listing,
    @Relation(
        parentColumn = "id",
        entityColumn = "listingId"
    )
    val images: List<ListingImage>
) {
    val coverImagePath: String? get() = images.sortedBy { it.sortOrder }.firstOrNull()?.imagePath
}

