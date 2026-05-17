package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.housefinder.R
import com.example.housefinder.viewmodel.ReservationSuccessViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReservationSuccessFragment : Fragment(R.layout.fragment_reservation_success) {

    private val args: ReservationSuccessFragmentArgs by navArgs()
    private val viewModel: ReservationSuccessViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reference = args.referenceNumber
        val amountText = view.findViewById<TextView>(R.id.txt_receipt_amount)
        val methodText = view.findViewById<TextView>(R.id.txt_receipt_method)
        val paidAtText = view.findViewById<TextView>(R.id.txt_receipt_paid_at)

        view.findViewById<TextView>(R.id.txt_reference).text =
            getString(R.string.reservation_reference_value, reference)

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.receiptSummary.collectLatest { receipt ->
                if (receipt != null) {
                    amountText.text = getString(
                        R.string.receipt_amount_value,
                        receipt.amountPaid.toInt()
                    )
                    methodText.text = getString(R.string.receipt_method_value, receipt.paymentMethod)
                    paidAtText.text = getString(
                        R.string.receipt_paid_at_value,
                        SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(receipt.paidAt))
                    )
                }
            }
        }
        viewModel.loadReceipt(reference)

        view.findViewById<Button>(R.id.btn_back_to_listings).setOnClickListener {
            findNavController().navigate(R.id.action_reservationSuccessFragment_to_listingListFragment)
        }

        view.findViewById<Button>(R.id.btn_go_to_my_reservations).setOnClickListener {
            findNavController().navigate(R.id.myReservationsFragment)
        }
    }
}
