package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.db.entities.Listing
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ListingDetailViewModel @Inject constructor(private val listingRepository: ListingRepository) : ViewModel() {

    private val _uiState = MutableStateFlow<UiState>(UiState.Loading)
    val uiState: StateFlow<UiState> = _uiState
    private var loadJob: Job? = null

    fun loadListing(id: Int) {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            listingRepository.getById(id).collectLatest {
                _uiState.value = if (it == null) UiState.Missing else UiState.Loaded(it)
            }
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(val listing: Listing) : UiState()
        object Missing : UiState()
    }
}
