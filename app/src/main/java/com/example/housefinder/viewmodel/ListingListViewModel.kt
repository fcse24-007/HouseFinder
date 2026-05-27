package com.example.housefinder.viewmodel

import androidx.lifecycle.SavedStateHandle
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
    private val imageRepository: ListingImageRepository,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val activeFilter = MutableStateFlow(loadSavedFilter())
    val currentFilter: StateFlow<ListingFilter> = activeFilter

    val listings: StateFlow<List<ListingWithImage>> = activeFilter
        .flatMapLatest { filter ->
            listingRepository.filterAvailableWithImage(
                minPrice = filter.minPrice,
                maxPrice = filter.maxPrice,
                location = filter.location,
                type = filter.type,
                availabilityDate = filter.availabilityDate,
                keyword = filter.keyword,
                sortOrder = filter.sortOrder.value
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
        type: String? = null,
        availabilityDate: String? = null
    ) {
        activeFilter.update { current ->
            current.copy(
                minPrice = minPrice,
                maxPrice = maxPrice,
                location = location,
                type = type,
                availabilityDate = availabilityDate
            )
        }
        persistFilter(activeFilter.value)
    }

    fun clearFilters() {
        activeFilter.update { current ->
            current.copy(
                minPrice = null,
                maxPrice = null,
                location = null,
                type = null,
                availabilityDate = null
            )
        }
        persistFilter(activeFilter.value)
    }

    fun updateSearchQuery(keyword: String) {
        activeFilter.update { current ->
            current.copy(keyword = keyword.trim().ifBlank { null })
        }
        persistFilter(activeFilter.value)
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        activeFilter.update { current -> current.copy(sortOrder = sortOrder) }
        persistFilter(activeFilter.value)
    }

    fun updateFilter(transform: (ListingFilter) -> ListingFilter) {
        activeFilter.update(transform)
        persistFilter(activeFilter.value)
    }

    fun resetAllFilters() {
        activeFilter.value = ListingFilter()
        persistFilter(activeFilter.value)
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
        val type: String? = null,
        val availabilityDate: String? = null,
        val keyword: String? = null,
        val sortOrder: SortOrder = SortOrder.NEWEST
    )

    enum class SortOrder(val value: String) {
        NEWEST("NEWEST"),
        PRICE_ASC("PRICE_ASC"),
        PRICE_DESC("PRICE_DESC"),
        DISTANCE_ASC("DISTANCE_ASC")
    }

    private fun loadSavedFilter(): ListingFilter {
        val sortName = savedStateHandle.get<String>(KEY_SORT)
        val sortOrder = SortOrder.values().firstOrNull { it.name == sortName } ?: SortOrder.NEWEST
        return ListingFilter(
            minPrice = savedStateHandle.get<Float>(KEY_MIN_PRICE),
            maxPrice = savedStateHandle.get<Float>(KEY_MAX_PRICE),
            location = savedStateHandle.get<String>(KEY_LOCATION),
            type = savedStateHandle.get<String>(KEY_TYPE),
            availabilityDate = savedStateHandle.get<String>(KEY_AVAILABILITY),
            keyword = savedStateHandle.get<String>(KEY_KEYWORD),
            sortOrder = sortOrder
        )
    }

    private fun persistFilter(filter: ListingFilter) {
        savedStateHandle[KEY_MIN_PRICE] = filter.minPrice
        savedStateHandle[KEY_MAX_PRICE] = filter.maxPrice
        savedStateHandle[KEY_LOCATION] = filter.location
        savedStateHandle[KEY_TYPE] = filter.type
        savedStateHandle[KEY_AVAILABILITY] = filter.availabilityDate
        savedStateHandle[KEY_KEYWORD] = filter.keyword
        savedStateHandle[KEY_SORT] = filter.sortOrder.name
    }

    companion object {
        private const val KEY_MIN_PRICE = "filters_min_price"
        private const val KEY_MAX_PRICE = "filters_max_price"
        private const val KEY_LOCATION = "filters_location"
        private const val KEY_TYPE = "filters_type"
        private const val KEY_AVAILABILITY = "filters_availability"
        private const val KEY_KEYWORD = "filters_keyword"
        private const val KEY_SORT = "filters_sort"
    }
}
