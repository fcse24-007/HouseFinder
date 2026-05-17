package com.example.housefinder.ui.payment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.housefinder.R
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.PaymentViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PaymentFragment : Fragment(R.layout.fragment_payment) {

    private val viewModel: PaymentViewModel by viewModels()
    private val args: PaymentFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingId = args.listingId
        val listingNameText = view.findViewById<TextView>(R.id.txt_summary_listing)
        val monthlyRentText = view.findViewById<TextView>(R.id.txt_summary_monthly_rent)
        val depositAmountText = view.findViewById<TextView>(R.id.txt_summary_deposit)
        val totalAmountText = view.findViewById<TextView>(R.id.txt_summary_total)
        val cardholderInput = view.findViewById<EditText>(R.id.edt_cardholder_name)
        val paymentAliasInput = view.findViewById<EditText>(R.id.edt_card_number)
        val closeButton = view.findViewById<View>(R.id.btn_close_payment)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_payment)
        val payButton = view.findViewById<Button>(R.id.btn_confirm_payment)

        closeButton.setOnClickListener { findNavController().popBackStack() }
        cancelButton.setOnClickListener { findNavController().popBackStack() }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listing.collectLatest { listing ->
                if (listing != null) {
                    listingNameText.text = listing.title
                    monthlyRentText.text = getString(R.string.currency_p_value, listing.price.toInt())
                    depositAmountText.text = getString(R.string.currency_p_value, listing.depositAmount)
                    totalAmountText.text = getString(R.string.currency_p_value, listing.depositAmount)
                    payButton.text = getString(R.string.btn_confirm_payment_amount, listing.depositAmount)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.paymentResult.collectLatest { result ->
                when (result) {
                    is PaymentViewModel.PaymentResult.Success -> {
                        val action = PaymentFragmentDirections.actionPaymentFragmentToReservationSuccessFragment(result.referenceNumber)
                        findNavController().navigate(action)
                    }
                    is PaymentViewModel.PaymentResult.Error -> {
                        showErrorDialog(result.message)
                    }
                }
            }
        }

        viewModel.loadListing(listingId)

        payButton.setOnClickListener {
            val userId = SessionManager(requireContext()).getUserId()
            if (userId == null) {
                showErrorDialog("Please log in again")
                return@setOnClickListener
            }

            val cardholderName = cardholderInput.text.toString().trim()
            val paymentAlias = paymentAliasInput.text.toString().trim().uppercase()

            if (cardholderName.isBlank() || paymentAlias.isBlank()) {
                Toast.makeText(requireContext(), R.string.error_payment_fields_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!SUPPORTED_PAYMENT_ALIASES.contains(paymentAlias)) {
                Toast.makeText(requireContext(), R.string.error_payment_alias_invalid, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            viewModel.processPayment(userId, listingId, paymentAlias)
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
            .show()
    }

    companion object {
        private val SUPPORTED_PAYMENT_ALIASES = setOf(
            "TEST_VISA_01",
            "TEST_MASTERCARD_01",
            "TEST_BW_MOBILE_01"
        )
    }
}
