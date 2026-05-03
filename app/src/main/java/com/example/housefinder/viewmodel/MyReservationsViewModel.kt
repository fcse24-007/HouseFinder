package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ReservationRepository
import com.example.housefinder.db.entities.StudentReservationDetails
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class MyReservationsViewModel @Inject constructor(private val reservationRepository: ReservationRepository) : ViewModel() {

    private val _reservations = MutableStateFlow<List<StudentReservationDetails>>(emptyList())
    val reservations: StateFlow<List<StudentReservationDetails>> = _reservations

    fun loadReservations(studentId: Int) {
        viewModelScope.launch {
            reservationRepository.getStudentReservationDetails(studentId).collectLatest {
                _reservations.value = it
            }
        }
    }
}
