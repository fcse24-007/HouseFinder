package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import com.example.housefinder.R
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ReceiptDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ReceiptDetailFragment : Fragment(R.layout.fragment_receipt_detail) {

    private val args: ReceiptDetailFragmentArgs by navArgs()
    private val viewModel: ReceiptDetailViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val referenceText = view.findViewById<TextView>(R.id.txt_receipt_reference)
        val listingText = view.findViewById<TextView>(R.id.txt_receipt_listing)
        val amountText = view.findViewById<TextView>(R.id.txt_receipt_amount)
        val methodText = view.findViewById<TextView>(R.id.txt_receipt_method)
        val paidAtText = view.findViewById<TextView>(R.id.txt_receipt_paid_at)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }

        val userId = SessionManager(requireContext()).getUserId()
        if (userId == null) {
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
            return
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.detail.collectLatest { detail ->
                if (detail == null) return@collectLatest
                referenceText.text = getString(R.string.receipt_reference_value, detail.referenceNumber)
                listingText.text = getString(R.string.receipt_listing_value, detail.listingTitle)
                amountText.text = getString(R.string.receipt_amount_value, detail.amountPaid.toInt())
                methodText.text = getString(R.string.receipt_method_value, detail.paymentMethod)
                paidAtText.text = getString(
                    R.string.receipt_paid_at_value,
                    SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault()).format(Date(detail.paidAt))
                )
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { message ->
                Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
                findNavController().popBackStack()
            }
        }

        viewModel.load(args.referenceNumber, userId)
    }
}
