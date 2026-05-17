package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.ListingDao
import com.example.housefinder.db.entities.Listing
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

import javax.inject.Inject

class ListingRepository @Inject constructor(private val listingDao: ListingDao) {

    fun getAllAvailable(): Flow<List<Listing>> = listingDao.getAllAvailable()

    fun getAllAvailableWithImage(): Flow<List<com.example.housefinder.db.entities.ListingWithImage>> =
        listingDao.getAllAvailableWithImage()

    fun filterAvailableWithImage(
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        type: String? = null,
        availabilityDate: String? = null
    ): Flow<List<com.example.housefinder.db.entities.ListingWithImage>> =
        listingDao.filterWithImage(
            minPrice = minPrice,
            maxPrice = maxPrice,
            location = location,
            type = type,
            availabilityDate = availabilityDate
        )

    fun getById(id: Int): Flow<Listing?> = listingDao.getById(id)

    suspend fun getByIdOnce(id: Int): Listing? = listingDao.getByIdOnce(id)

    fun getByProvider(providerId: Int): Flow<List<Listing>> = listingDao.getByProvider(providerId)

    suspend fun insert(listing: Listing): Long = listingDao.insert(listing)

    suspend fun update(listing: Listing) = listingDao.update(listing)

    suspend fun deleteById(listingId: Int, providerId: Int) = listingDao.deleteById(listingId, providerId)

    suspend fun findNewMatchingListings(
        sinceTimestamp: Long,
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        type: String? = null,
        availabilityDate: String? = null
    ): List<Listing> = listingDao.findNewMatchingListings(
        sinceTimestamp = sinceTimestamp,
        minPrice = minPrice,
        maxPrice = maxPrice,
        location = location,
        type = type,
        availabilityDate = availabilityDate
    )

    fun observeChangeToken(): Flow<Long> = flowOf(System.currentTimeMillis())
}
