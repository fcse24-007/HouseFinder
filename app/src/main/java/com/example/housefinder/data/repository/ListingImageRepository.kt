package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.ListingImageDao
import com.example.housefinder.db.entities.ListingImage
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class ListingImageRepository @Inject constructor(private val listingImageDao: ListingImageDao) {

    fun getForListing(listingId: Int): Flow<List<ListingImage>> = listingImageDao.getForListing(listingId)

    suspend fun getCoverImage(listingId: Int): ListingImage? = listingImageDao.getCoverImage(listingId)

    fun observeChangeToken(): Flow<Long> = listingImageDao.observeChangeToken()

    suspend fun replaceCoverImage(listingId: Int, imagePath: String) =
        listingImageDao.replaceCoverImage(listingId, imagePath)
}
