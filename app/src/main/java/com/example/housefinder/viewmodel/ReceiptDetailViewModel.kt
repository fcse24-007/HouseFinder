package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ListingRepository
import com.example.housefinder.data.repository.ReceiptRepository
import com.example.housefinder.data.repository.ReservationRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class ReceiptDetailViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val receiptRepository: ReceiptRepository,
    private val listingRepository: ListingRepository
) : ViewModel() {

    private val _detail = MutableStateFlow<ReceiptDetail?>(null)
    val detail: StateFlow<ReceiptDetail?> = _detail

    private val _error = MutableSharedFlow<String>()
    val error: SharedFlow<String> = _error.asSharedFlow()

    fun load(referenceNumber: String, userId: Int) {
        viewModelScope.launch {
            val reservation = reservationRepository.getByReference(referenceNumber)
            if (reservation == null) {
                _error.emit("Receipt not found")
                return@launch
            }

            val listing = listingRepository.getByIdOnce(reservation.listingId)
            if (listing == null) {
                _error.emit("Receipt not found")
                return@launch
            }
            val isAuthorized = reservation.studentId == userId || listing.providerId == userId
            if (!isAuthorized) {
                _error.emit("You are not allowed to view this receipt")
                return@launch
            }

            val receipt = receiptRepository.getForReservation(reservation.id)
            if (receipt == null) {
                _error.emit("Receipt not found")
                return@launch
            }

            val listingTitle = listing.title

            _detail.value = ReceiptDetail(
                referenceNumber = reservation.referenceNumber,
                listingTitle = listingTitle,
                amountPaid = receipt.amountPaid,
                paymentMethod = receipt.paymentMethod,
                paidAt = receipt.paidAt
            )
        }
    }

    data class ReceiptDetail(
        val referenceNumber: String,
        val listingTitle: String,
        val amountPaid: Float,
        val paymentMethod: String,
        val paidAt: Long
    )
}
