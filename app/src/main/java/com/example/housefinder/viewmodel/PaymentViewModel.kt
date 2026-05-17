package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.BuildConfig
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.ReservationRepository
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.db.entities.Listing
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class PaymentViewModel @Inject constructor(
    private val listingRepository: ListingRepository,
    private val reservationRepository: ReservationRepository,
    private val userRepository: UserRepository
) : ViewModel() {

    private val _listing = MutableStateFlow<Listing?>(null)
    val listing: StateFlow<Listing?> = _listing

    private val _paymentResult = MutableSharedFlow<PaymentResult>()
    val paymentResult: SharedFlow<PaymentResult> = _paymentResult

    fun loadListing(listingId: Int) {
        viewModelScope.launch {
            _listing.value = listingRepository.getByIdOnce(listingId)
        }
    }

    fun processPayment(userId: Int, listingId: Int, paymentAlias: String) {
        viewModelScope.launch {
            if (!BuildConfig.SIMULATED_PAYMENTS) {
                _paymentResult.emit(PaymentResult.Error("Simulated payments are disabled in this build"))
                return@launch
            }

            if (!SUPPORTED_PAYMENT_ALIASES.contains(paymentAlias.uppercase())) {
                _paymentResult.emit(PaymentResult.Error("Unsupported test payment alias"))
                return@launch
            }

            val sessionUser = userRepository.getById(userId)
            if (sessionUser?.role != "STUDENT") {
                _paymentResult.emit(PaymentResult.Error("Only students can reserve listings"))
                return@launch
            }

            when (
                val result = reservationRepository.createSimulatedReservation(
                    studentId = userId,
                    listingId = listingId,
                    paymentAlias = paymentAlias
                )
            ) {
                is ReservationRepository.BookingResult.Success ->
                    _paymentResult.emit(PaymentResult.Success(result.referenceNumber))

                is ReservationRepository.BookingResult.ListingUnavailable ->
                    _paymentResult.emit(PaymentResult.Error("Room already reserved by another student"))

                is ReservationRepository.BookingResult.StudentAlreadyReserved ->
                    _paymentResult.emit(PaymentResult.Error("You already have an active reservation"))

                is ReservationRepository.BookingResult.Error ->
                    _paymentResult.emit(PaymentResult.Error(result.message))
            }
        }
    }

    sealed class PaymentResult {
        data class Success(val referenceNumber: String) : PaymentResult()
        data class Error(val message: String) : PaymentResult()
    }

    companion object {
        private val SUPPORTED_PAYMENT_ALIASES = setOf(
            "TEST_VISA_01",
            "TEST_MASTERCARD_01",
            "TEST_BW_MOBILE_01"
        )
    }
}
