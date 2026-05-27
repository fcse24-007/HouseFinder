package com.example.housefinder.db.dao

import androidx.room.*
import com.example.housefinder.db.entities.Listing

import kotlinx.coroutines.flow.Flow

@Dao
interface ListingDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(listing: Listing): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(listings: List<Listing>)

    @Update
    suspend fun update(listing: Listing)

    @Query("DELETE FROM listings WHERE id = :listingId AND providerId = :providerId")
    suspend fun deleteById(listingId: Int, providerId: Int)

    @Query("SELECT * FROM listings WHERE status = 'AVAILABLE' ORDER BY createdAt DESC")
    fun getAllAvailable(): Flow<List<Listing>>

    @Transaction
    @Query("SELECT * FROM listings WHERE status = 'AVAILABLE' ORDER BY createdAt DESC")
    fun getAllAvailableWithImage(): Flow<List<com.example.housefinder.db.entities.ListingWithImage>>

    @Query("""
        SELECT * FROM listings 
        WHERE status = 'AVAILABLE'
        AND (:minPrice IS NULL OR price >= :minPrice)
        AND (:maxPrice IS NULL OR price <= :maxPrice)
        AND (:location IS NULL OR :location = '' OR location LIKE '%' || :location || '%')
        AND (:type IS NULL OR :type = '' OR type = :type)
        AND (:availabilityDate IS NULL OR :availabilityDate = '' OR availabilityDate >= :availabilityDate)
        AND (:keyword IS NULL OR :keyword = '' OR title LIKE '%' || :keyword || '%' OR location LIKE '%' || :keyword || '%')
        ORDER BY
            CASE WHEN :sortOrder = 'PRICE_ASC' THEN price END ASC,
            CASE WHEN :sortOrder = 'PRICE_DESC' THEN price END DESC,
            CASE WHEN :sortOrder = 'DISTANCE_ASC' THEN distanceToCampusKm END ASC,
            CASE WHEN :sortOrder = 'NEWEST' THEN createdAt END DESC,
            createdAt DESC
    """)
    fun filter(
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        type: String? = null,
        availabilityDate: String? = null,
        keyword: String? = null,
        sortOrder: String = "NEWEST"
    ): Flow<List<Listing>>

    @Transaction
    @Query("""
        SELECT * FROM listings 
        WHERE status = 'AVAILABLE'
        AND (:minPrice IS NULL OR price >= :minPrice)
        AND (:maxPrice IS NULL OR price <= :maxPrice)
        AND (:location IS NULL OR :location = '' OR location LIKE '%' || :location || '%')
        AND (:type IS NULL OR :type = '' OR type = :type)
        AND (:availabilityDate IS NULL OR :availabilityDate = '' OR availabilityDate >= :availabilityDate)
        AND (:keyword IS NULL OR :keyword = '' OR title LIKE '%' || :keyword || '%' OR location LIKE '%' || :keyword || '%')
        ORDER BY
            CASE WHEN :sortOrder = 'PRICE_ASC' THEN price END ASC,
            CASE WHEN :sortOrder = 'PRICE_DESC' THEN price END DESC,
            CASE WHEN :sortOrder = 'DISTANCE_ASC' THEN distanceToCampusKm END ASC,
            CASE WHEN :sortOrder = 'NEWEST' THEN createdAt END DESC,
            createdAt DESC
    """)
    fun filterWithImage(
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        type: String? = null,
        availabilityDate: String? = null,
        keyword: String? = null,
        sortOrder: String = "NEWEST"
    ): Flow<List<com.example.housefinder.db.entities.ListingWithImage>>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    fun getById(id: Int): Flow<Listing?>

    @Query("SELECT * FROM listings WHERE id = :id LIMIT 1")
    suspend fun getByIdOnce(id: Int): Listing?

    @Query("SELECT * FROM listings WHERE providerId = :providerId")
    fun getByProvider(providerId: Int): Flow<List<Listing>>

    @Query("UPDATE listings SET status = :status WHERE id = :listingId")
    suspend fun updateStatus(listingId: Int, status: String)

    @Query("UPDATE listings SET status = :newStatus WHERE id = :listingId AND status = :expectedStatus")
    suspend fun updateStatusIfCurrent(listingId: Int, expectedStatus: String, newStatus: String): Int

    @Query("""
        SELECT * FROM listings 
        WHERE status = 'AVAILABLE'
        AND createdAt > :sinceTimestamp
        AND (:minPrice IS NULL OR price >= :minPrice)
        AND (:maxPrice IS NULL OR price <= :maxPrice)
        AND (:location IS NULL OR :location = '' OR location LIKE '%' || :location || '%')
        AND (:type IS NULL OR :type = '' OR type = :type)
        AND (:availabilityDate IS NULL OR :availabilityDate = '' OR availabilityDate >= :availabilityDate)
    """)
    suspend fun findNewMatchingListings(
        sinceTimestamp: Long,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        type: String? = null,
        availabilityDate: String? = null
    ): List<Listing>

    @Query("SELECT COUNT(*) FROM listings")
    suspend fun count(): Int
}
