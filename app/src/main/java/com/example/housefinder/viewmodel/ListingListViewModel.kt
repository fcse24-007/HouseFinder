package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.db.entities.Listing
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

import com.example.housefinder.data.repository.ListingImageRepository
import com.example.housefinder.data.repository.UserPreferenceRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.update

import com.example.housefinder.db.entities.ListingWithImage

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class ListingListViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val preferenceRepository: UserPreferenceRepository,
    private val imageRepository: ListingImageRepository
) : ViewModel() {

    private val activeFilter = MutableStateFlow(ListingFilter())

    val listings: StateFlow<List<ListingWithImage>> = activeFilter
        .flatMapLatest { filter ->
            listingRepository.filterAvailableWithImage(
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                location = filter.location,
                availabilityDate = filter.availabilityDate
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val imageChangeToken: StateFlow<Long> = imageRepository.observeChangeToken()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0L
        )

    private val _alertMatches = MutableSharedFlow<List<Listing>>()
    val alertMatches: SharedFlow<List<Listing>> = _alertMatches

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error

    fun applyFilters(
        minPrice: Float? = null,
        maxPrice: Float? = null,
        location: String? = null,
        availabilityDate: String? = null
    ) {
        activeFilter.update {
            ListingFilter(
                minPrice = minPrice,
                maxPrice = maxPrice,
                location = location,
                availabilityDate = availabilityDate
            )
        }
    }

    fun clearFilters() {
        activeFilter.value = ListingFilter()
    }

    fun checkAlerts(userId: Int, lastCheck: Long) {
        viewModelScope.launch {
            try {
                val pref = preferenceRepository.getForUser(userId).first()
                if (pref == null || !pref.notificationsEnabled) {
                    _error.emit("No active preferences/alerts")
                    return@launch
                }

                val matches = listingRepository.findNewMatchingListings(
                    sinceTimestamp = lastCheck,
                    minPrice = pref.minPrice,
                    maxPrice = pref.maxPrice,
                    location = pref.location,
                    type = pref.type,
                    availabilityDate = pref.availabilityDate
                )

                if (matches.isEmpty()) {
                    _error.emit("No new listings matched your preferences")
                } else {
                    _alertMatches.emit(matches)
                }
            } catch (e: Exception) {
                _error.emit("Error checking alerts: ${e.message}")
            }
        }
    }

    data class ListingFilter(
        val minPrice: Float? = null,
        val maxPrice: Float? = null,
        val location: String? = null,
        val availabilityDate: String? = null
    )
}
