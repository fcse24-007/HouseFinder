package com.example.housefinder.data.repository

import com.example.housefinder.db.dao.UserPreferenceDao
import com.example.housefinder.db.entities.UserPreference
import kotlinx.coroutines.flow.Flow

import javax.inject.Inject

class UserPreferenceRepository @Inject constructor(private val userPreferenceDao: UserPreferenceDao) {

    fun getForUser(userId: Int): Flow<UserPreference?> = userPreferenceDao.getForUser(userId)

    suspend fun upsert(preference: UserPreference) = userPreferenceDao.upsert(preference)
}
