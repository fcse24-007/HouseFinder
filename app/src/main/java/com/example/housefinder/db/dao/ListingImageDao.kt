package com.example.housefinder.db.dao

import androidx.room.*
import com.example.housefinder.db.entities.ListingImage

import kotlinx.coroutines.flow.Flow

@Dao
interface ListingImageDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(images: List<ListingImage>)

    @Query("DELETE FROM listing_images WHERE listingId = :listingId")
    suspend fun deleteForListing(listingId: Int)

    @Query("SELECT * FROM listing_images WHERE listingId = :listingId ORDER BY sortOrder ASC")
    fun getForListing(listingId: Int): Flow<List<ListingImage>>

    @Query("SELECT * FROM listing_images WHERE listingId = :listingId ORDER BY sortOrder ASC LIMIT 1")
    suspend fun getCoverImage(listingId: Int): ListingImage?

    @Query("SELECT IFNULL(SUM(id), 0) FROM listing_images")
    fun observeChangeToken(): Flow<Long>

    /**
     * Atomically replaces a listing's cover image by deleting existing image rows
     * for the listing and inserting exactly one new cover image entry.
     */
    @Transaction
    suspend fun replaceCoverImage(listingId: Int, imagePath: String) {
        deleteForListing(listingId)
        insertAll(
            listOf(
                ListingImage(
                    listingId = listingId,
                    imagePath = imagePath,
                    sortOrder = 0
                )
            )
        )
    }
}
