package com.example.housefinder.data.di

import android.content.Context
import com.example.housefinder.db.dao.*
import com.example.housefinder.db.entities.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideListingDao(database: AppDatabase): ListingDao = database.listingDao()

    @Provides
    fun provideListingImageDao(database: AppDatabase): ListingImageDao = database.listingImageDao()

    @Provides
    fun provideReservationDao(database: AppDatabase): ReservationDao = database.reservationDao()

    @Provides
    fun provideReceiptDao(database: AppDatabase): ReceiptDao = database.receiptDao()

    @Provides
    fun provideUserPreferenceDao(database: AppDatabase): UserPreferenceDao = database.userPreferenceDao()

    @Provides
    fun provideChatMessageDao(database: AppDatabase): ChatMessageDao = database.chatMessageDao()
}
