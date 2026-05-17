package com.example.housefinder.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.housefinder.data.repository.ReceiptRepository
import com.example.housefinder.data.repository.ReservationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class ReservationSuccessViewModel @Inject constructor(
    private val reservationRepository: ReservationRepository,
    private val receiptRepository: ReceiptRepository
) : ViewModel() {

    private val _receiptSummary = MutableStateFlow<ReceiptSummary?>(null)
    val receiptSummary: StateFlow<ReceiptSummary?> = _receiptSummary

    fun loadReceipt(referenceNumber: String) {
        viewModelScope.launch {
            val reservation = reservationRepository.getByReference(referenceNumber) ?: return@launch
            val receipt = receiptRepository.getForReservation(reservation.id) ?: return@launch
            _receiptSummary.value = ReceiptSummary(
                amountPaid = receipt.amountPaid,
                paymentMethod = receipt.paymentMethod,
                paidAt = receipt.paidAt
            )
        }
    }

    data class ReceiptSummary(
        val amountPaid: Float,
        val paymentMethod: String,
        val paidAt: Long
    )
}
