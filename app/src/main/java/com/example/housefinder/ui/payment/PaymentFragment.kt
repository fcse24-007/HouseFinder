package com.example.housefinder.ui.payment

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.Receipt
import com.example.housefinder.db.entities.Reservation
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class PaymentFragment : Fragment(R.layout.fragment_payment) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingId = requireArguments().getInt("listingId")
        val depositAmountText = view.findViewById<TextView>(R.id.txt_deposit_amount)
        val paymentMethodSpinner = view.findViewById<Spinner>(R.id.spinner_payment_method)
        val payButton = view.findViewById<Button>(R.id.btn_pay_deposit)

        val methods = listOf("Sandbox Card", "Mock Mobile Money", "Simulated Bank Transfer")
        paymentMethodSpinner.adapter = ArrayAdapter(requireContext(), android.R.layout.simple_spinner_dropdown_item, methods)

        lifecycleScope.launch {
            val listing = AppDatabase.getInstance(requireContext()).listingDao().getById(listingId).first()
            if (listing != null) {
                depositAmountText.text = getString(R.string.deposit_to_pay_value, listing.depositAmount)
            }
        }

        payButton.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val listing = db.listingDao().getById(listingId).first()
                val userId = SessionManager(requireContext()).getUserId()

                if (listing == null || userId == null) {
                    Toast.makeText(requireContext(), "Could not complete payment", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                if (db.reservationDao().countActiveForListing(listingId) > 0) {
                    Toast.makeText(requireContext(), "Room already reserved", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val referenceNumber = "RSV-${System.currentTimeMillis().toString().takeLast(8)}"
                val reservationId = db.reservationDao().insert(
                    Reservation(
                        referenceNumber = referenceNumber,
                        studentId = userId,
                        listingId = listingId,
                        status = "ACTIVE"
                    )
                ).toInt()

                db.receiptDao().insert(
                    Receipt(
                        reservationId = reservationId,
                        amountPaid = listing.depositAmount.toFloat(),
                        paymentMethod = paymentMethodSpinner.selectedItem.toString()
                    )
                )

                db.listingDao().updateStatus(listingId, "RESERVED")

                findNavController().navigate(
                    R.id.action_paymentFragment_to_reservationSuccessFragment,
                    bundleOf("referenceNumber" to referenceNumber)
                )
            }
        }
    }
}

