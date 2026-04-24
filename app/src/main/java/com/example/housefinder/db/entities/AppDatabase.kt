package com.example.housefinder.db.entities

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.housefinder.db.dao.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.mindrot.jbcrypt.BCrypt

// 1. Helper data class for University variations in Gaborone
data class UniversityConfig(
    val name: String,
    val idPrefix: String,
    val emailDomain: String
)

val GABORONE_UNIVERSITIES = listOf(
    UniversityConfig("University of Botswana", "fcse", "student.ub.bw"),
    UniversityConfig("Botho University", "cse", "bothocollege.ac.bw"),
    UniversityConfig("Botswana Accountancy College", "bac", "bac.ac.bw"),
    UniversityConfig("ISBS", "isb", "isbs.ac.bw"),
    UniversityConfig("Boitekanelo College", "btc", "boitekanelo.ac.bw")
)


@Database(
    entities = [
        User::class,
        Listing::class,
        ListingImage::class,
        Reservation::class,
        Receipt::class,
        UserPreference::class,
        ChatMessage::class
    ],
    version = 4,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun listingDao(): ListingDao
    abstract fun listingImageDao(): ListingImageDao
    abstract fun reservationDao(): ReservationDao
    abstract fun receiptDao(): ReceiptDao
    abstract fun userPreferenceDao(): UserPreferenceDao
    abstract fun chatMessageDao(): ChatMessageDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase = INSTANCE ?: synchronized(this) {
            INSTANCE ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "student_nest_finder_db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            val database = getInstance(context)
                            seedUsers(database)
                            seedListings(database)
                            seedChatMessages(database)
                        }
                    }
                })
                // Intentional destructive migration: seeded/demo data is regenerated and
                // password hashes are re-seeded with bcrypt for this app.
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}

/**
 * --- Utility and Seeding Functions ---
 */

fun hashPassword(input: String): String = BCrypt.hashpw(input, BCrypt.gensalt())

suspend fun seedUsers(db: AppDatabase) {
    // List of Gaborone Institutions
    val universities = GABORONE_UNIVERSITIES

    val names = listOf(
        "Kagiso Sithole", "Mpho Mokoena", "Tebogo Dlamini", "Naledi Kgosi",
        "Oratile Sebego", "Goabaone Tladi", "Kefilwe Molefe", "Thabo Nkwe",
        "Lesedi Gabaake", "Boipelo Marumo", "Ditiro Segwabe", "Onkutlwe Motlhale",
        "Sethunya Ramasedi", "Phenyo Keatlaretse", "Gofaone Mogopodi",
        "Refilwe Modise", "Tuelo Moseki", "Khumoetsile Lekwape", "Ipeleng Mosweu",
        "Baleseng Ntshole", "Modiri Chiepe", "Gaone Ramothwa", "Lorato Sadie",
        "Tlotlo Seretse", "Kenanao Gaboeletswe", "Mompati Mothibi", "Otsile Moroke",
        "Laone Garekwe", "Kefiloe Mosalakae", "Tshepiso Dingalo", "Kago Segaetsho",
        "Bontleng Molefi", "Lerato Kabelo", "Tshepo Rapetswa", "Sethabo Sengwe",
        "Ntombi Garekwe", "Motlapele Digang", "Pako Pule", "Kelebogile Mathe",
        "Gape Mogami", "Itumeleng Baaitse", "Maipelo Tlhomedi", "Kagiso Ramorobi",
        "Neo Gaetsewe", "Thatayaone Sello", "Gaositwe Kenosi", "Mmoloki Tau",
        "Thuli Dineo", "Rudo Matshediso", "Bokang Motshwari"
    )

    val maxStudentSequenceNumber = 900
    val yearCodeCycleSize = 30

    // Map names to User objects, cycling through the university list
    val users = names.mapIndexed { i, name ->
        val config = universities[i % universities.size]
        val idx = ((i % maxStudentSequenceNumber) + 1).toString().padStart(3, '0')
        val yearCode = ((i % yearCodeCycleSize) + 1).toString().padStart(2, '0')
        val firstName = name.split(" ")[0].lowercase()

        User(
            studentId = "${config.idPrefix}${yearCode}-${idx}",
            name = name,
            email = "$firstName$idx@${config.emailDomain}",
            passwordHash = hashPassword("password$idx"),
            role = "STUDENT",
            university = config.name
        )
    }

    // Landlord / Provider Accounts
    val providers = listOf(
        User(
            studentId = "PRV001",
            name = "Modise Properties",
            email = "modise@provider.bw",
            passwordHash = hashPassword("provider123"),
            role = "PROVIDER",
            university = "N/A"
        ),
        User(
            studentId = "PRV002",
            name = "Gaborone Student Homes",
            email = "gsh@provider.bw",
            passwordHash = hashPassword("provider456"),
            role = "PROVIDER",
            university = "N/A"
        )
    )

    db.userDao().insertAll(users + providers)
}

suspend fun seedListings(db: AppDatabase) {
    val locations = listOf(
        "Block 6", "Block 7", "Block 8", "Block 9", "Broadhurst",
        "Gaborone West", "The Village", "Tlokweng", "Mogoditshane"
    )
    val types = listOf("EN_SUITE", "SHARED", "STUDIO", "FLAT")
    val amenitySets = listOf(
        """["Free WiFi","Study Desk","24/7 Security","Laundry"]""",
        """["Free WiFi","En-suite Bathroom","Study Desk"]""",
        """["24/7 Security","Shared Kitchen","Laundry"]"""
    )
    val availabilityDates = listOf("2024-08-01", "2024-08-15", "2024-09-01")
    val prices = listOf(950f, 1200f, 1500f, 1800f, 2200f)

    // Create 50 listings distributed between the two providers (IDs 51 and 52)
    val listings = (1..50).map { i ->
        val price = prices[i % prices.size]
        Listing(
            providerId = if (i <= 25) 51 else 52,
            title = "${locations[i % locations.size]} ${types[i % types.size]}",
            description = "Quality student accommodation in Gaborone. Close to transport and shops.",
            price = price,
            location = locations[i % locations.size],
            type = types[i % types.size],
            amenities = amenitySets[i % amenitySets.size],
            depositAmount = price.toInt(),
            availabilityDate = availabilityDates[i % availabilityDates.size],
            status = "AVAILABLE",
            distanceToCampusKm = (0.5 + (i % 5)).toFloat()
        )
    }

    db.listingDao().insertAll(listings)

    // Seed one image per listing
    val imageDrawables = listOf("img_room_1", "img_room_2", "img_room_3")
    val images = (1..50).map { i ->
        ListingImage(
            listingId = i,
            imagePath = imageDrawables[i % imageDrawables.size],
            sortOrder = 0
        )
    }
    db.listingImageDao().insertAll(images)
}

private suspend fun seedChatMessages(db: AppDatabase) {
    val initialMessages = listOf(
        ChatMessage(
            id = "seed-msg-1",
            conversationId = conversationIdFor(1, 51, 1),
            senderId = 1,
            receiverId = 51,
            listingId = 1,
            message = "Hi, is this room still available?",
            isRead = true,
            timestamp = System.currentTimeMillis() - 172_800_000L
        ),
        ChatMessage(
            id = "seed-msg-2",
            conversationId = conversationIdFor(1, 51, 1),
            senderId = 51,
            receiverId = 1,
            listingId = 1,
            message = "Yes, it is available from next month.",
            isRead = false,
            timestamp = System.currentTimeMillis() - 172_700_000L
        ),
        ChatMessage(
            id = "seed-msg-3",
            conversationId = conversationIdFor(2, 52, 26),
            senderId = 2,
            receiverId = 52,
            listingId = 26,
            message = "Can I schedule a viewing this weekend?",
            isRead = true,
            timestamp = System.currentTimeMillis() - 86_400_000L
        )
    )
    db.chatMessageDao().insertAll(initialMessages)
}
