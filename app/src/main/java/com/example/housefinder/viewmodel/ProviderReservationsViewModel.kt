package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ReservationRepository
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.ProviderReservationDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ProviderReservationsViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _reservations = MutableStateFlow<List<ProviderReservationDetails>>(emptyList())
    val reservations: StateFlow<List<ProviderReservationDetails>> = _reservations

    fun loadReservations(providerId: Int) {
        viewModelScope.launch {
            val sessionUser = userRepository.getById(providerId)
            if (sessionUser?.role != "PROVIDER") {
                _reservations.value = emptyList()
                return@launch
            }

            reservationRepository.getProviderReservationDetails(providerId).collectLatest {
                _reservations.value = it
            }
        }
    }
}
