package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.db.entities.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderDashboardViewModel @Inject constructor(private val listingRepository: ListingRepository) : ViewModel() {

    private val _listings = MutableStateFlow<List<Listing>>(emptyList())
    val listings: StateFlow<List<Listing>> = _listings

    fun loadListings(providerId: Int) {
        viewModelScope.launch {
            listingRepository.getByProvider(providerId).collectLatest {
                _listings.value = it
            }
        }
    }

    fun deleteListing(listingId: Int, providerId: Int) {
        viewModelScope.launch {
            listingRepository.deleteById(listingId, providerId)
        }
    }
}
