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
                            DatabaseInitializer.seedUsers(database)
                            DatabaseInitializer.seedListings(database)
                            DatabaseInitializer.seedChatMessages(database)
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
        }
    }
}
