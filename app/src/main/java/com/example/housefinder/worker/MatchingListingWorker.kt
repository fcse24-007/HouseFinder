package com.example.housefinder.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.UserPreferenceRepository
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.ui.common.notification.NotificationHelper
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.firstOrNull

@HiltWorker
class MatchingListingWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val listingRepository: ListingRepository,
    private val preferenceRepository: UserPreferenceRepository
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        val sessionManager = SessionManager(applicationContext)
        val userId = sessionManager.getUserId() ?: return Result.success()

        val lastCheck = sessionManager.getLastAlertCheck(userId)
        val pref = try {
            preferenceRepository.getForUser(userId).firstOrNull()
        } catch (e: Exception) {
            null
        }

        if (pref == null || !pref.notificationsEnabled) return Result.success()

        val matches = listingRepository.findNewMatchingListings(
            sinceTimestamp = lastCheck,
            minPrice = pref.minPrice,
            maxPrice = pref.maxPrice,
            location = pref.location,
            type = pref.type
        )

        if (matches.isNotEmpty()) {
            val message = if (matches.size == 1) {
                "Found a new match: ${matches[0].title}"
            } else {
                "Found ${matches.size} new matches for your preferences!"
            }
            NotificationHelper.showNotification(applicationContext, "New House Found!", message)
            sessionManager.setLastAlertCheck(userId, System.currentTimeMillis())
        }

        return Result.success()
    }
}
