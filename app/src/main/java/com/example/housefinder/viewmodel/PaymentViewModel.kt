package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.ReceiptRepository
import com.example.housefinder.data.repository.ReservationRepository
import com.example.housefinder.db.entities.Listing
import com.example.housefinder.db.entities.Receipt
import com.example.housefinder.db.entities.Reservation
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
    private val receiptRepository: ReceiptRepository
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

    fun processPayment(userId: Int, listingId: Int, cardNumber: String) {
        viewModelScope.launch {
            try {
                val listing = listingRepository.getByIdOnce(listingId)
                if (listing == null) {
                    _paymentResult.emit(PaymentResult.Error("Listing no longer available"))
                    return@launch
                }

                if (listing.status != "AVAILABLE") {
                    _paymentResult.emit(PaymentResult.Error("Listing already reserved"))
                    return@launch
                }

                if (reservationRepository.countActiveForStudent(userId) > 0) {
                    _paymentResult.emit(PaymentResult.Error("You already have an active reservation"))
                    return@launch
                }

                if (reservationRepository.countActiveForListing(listingId) > 0) {
                    _paymentResult.emit(PaymentResult.Error("Room already reserved by another student"))
                    return@launch
                }

                val referenceNumber = "RSV-${System.currentTimeMillis().toString().takeLast(8)}"
                val reservationId = reservationRepository.insert(
                    Reservation(
                        referenceNumber = referenceNumber,
                        studentId = userId,
                        listingId = listingId,
                        status = "ACTIVE"
                    )
                ).toInt()

                receiptRepository.insert(
                    Receipt(
                        reservationId = reservationId,
                        amountPaid = listing.depositAmount.toFloat(),
                        paymentMethod = "Card •••• ${cardNumber.takeLast(4)}"
                    )
                )

                listingRepository.update(listing.copy(status = "RESERVED"))
                _paymentResult.emit(PaymentResult.Success(referenceNumber))

            } catch (e: Exception) {
                _paymentResult.emit(PaymentResult.Error(e.message ?: "Payment failed"))
            }
        }
    }

    sealed class PaymentResult {
        data class Success(val referenceNumber: String) : PaymentResult()
        data class Error(val message: String) : PaymentResult()
    }
}
