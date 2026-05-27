package com.example.housefinder.ui.payment

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.housefinder.R
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.PaymentViewModel
import com.google.android.material.textfield.TextInputLayout
import androidx.core.widget.doAfterTextChanged
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
        val cardholderLayout = view.findViewById<TextInputLayout>(R.id.input_cardholder_name)
        val cardNumberLayout = view.findViewById<TextInputLayout>(R.id.input_card_number)
        val cvvLayout = view.findViewById<TextInputLayout>(R.id.input_card_cvv)
        val cardholderInput = view.findViewById<EditText>(R.id.edt_cardholder_name)
        val cardNumberInput = view.findViewById<EditText>(R.id.edt_card_number)
        val cvvInput = view.findViewById<EditText>(R.id.edt_card_cvv)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val cancelButton = view.findViewById<Button>(R.id.btn_cancel_payment)
        val payButton = view.findViewById<Button>(R.id.btn_confirm_payment)

        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        cancelButton.setOnClickListener { findNavController().popBackStack() }

        cardholderInput.doAfterTextChanged { cardholderLayout.error = null }
        cardNumberInput.doAfterTextChanged { cardNumberLayout.error = null }
        cvvInput.doAfterTextChanged { cvvLayout.error = null }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listing.collectLatest { listing ->
                if (listing == null) {
                    payButton.isEnabled = false
                    return@collectLatest
                }
                listingNameText.text = listing.title
                monthlyRentText.text = getString(R.string.currency_p_value, listing.price.toInt())
                depositAmountText.text = getString(R.string.currency_p_value, listing.depositAmount)
                totalAmountText.text = getString(R.string.currency_p_value, listing.depositAmount)
                payButton.text = getString(R.string.btn_confirm_payment_amount, listing.depositAmount)
                payButton.isEnabled = true
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
                    PaymentViewModel.PaymentResult.ListingMissing -> {
                        showErrorDialog(getString(R.string.error_listing_not_found)) {
                            findNavController().popBackStack()
                        }
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

            clearPaymentErrors(cardholderLayout, cardNumberLayout, cvvLayout)

            val cardholderName = cardholderInput.text.toString().trim()
            val cardNumber = cardNumberInput.text.toString().trim()
            val cvv = cvvInput.text.toString().trim()

            if (cardholderName.isBlank()) {
                cardholderLayout.error = getString(R.string.error_cardholder_required)
            }
            if (cardNumber.isBlank()) {
                cardNumberLayout.error = getString(R.string.error_card_number_invalid)
            }
            if (cvv.isBlank()) {
                cvvLayout.error = getString(R.string.error_cvv_invalid)
            }
            if (cardholderName.isBlank() || cardNumber.isBlank() || cvv.isBlank()) {
                return@setOnClickListener
            }

            if (cardNumber.length != 16 || !cardNumber.all { it.isDigit() }) {
                cardNumberLayout.error = getString(R.string.error_card_number_invalid)
                return@setOnClickListener
            }

            if (cvv.length !in 3..4 || !cvv.all { it.isDigit() }) {
                cvvLayout.error = getString(R.string.error_cvv_invalid)
                return@setOnClickListener
            }

            viewModel.processPayment(userId, listingId, cardNumber)
        }
    }

    private fun showErrorDialog(message: String, onDismiss: (() -> Unit)? = null) {
        AlertDialog.Builder(requireContext())
            .setTitle("Payment Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                onDismiss?.invoke()
            }
            .show()
    }

    private fun clearPaymentErrors(
        cardholderLayout: TextInputLayout,
        cardNumberLayout: TextInputLayout,
        cvvLayout: TextInputLayout
    ) {
        cardholderLayout.error = null
        cardNumberLayout.error = null
        cvvLayout.error = null
    }

}
