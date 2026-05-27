package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.conversationIdFor
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ProviderReservationsViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ProviderReservationsFragment : Fragment(R.layout.fragment_provider_reservations) {

    private val viewModel: ProviderReservationsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val providerId = session.getUserId()
        if (providerId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val emptyState = view.findViewById<View>(R.id.layout_empty_provider_reservations)
        val emptyAction = view.findViewById<Button>(R.id.btn_empty_provider_reservations_action)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_provider_reservations)

        val adapter = ProviderReservationsAdapter(
            onChat = { item ->
                val conversationId = conversationIdFor(providerId, item.studentInternalId, item.listingId)
                val action = ProviderReservationsFragmentDirections.actionProviderReservationsFragmentToChatThreadFragment(
                    conversationId = conversationId,
                    listingId = item.listingId,
                    partnerId = item.studentInternalId
                )
                findNavController().navigate(action)
            },
            onViewReceipt = { item ->
                val action = ProviderReservationsFragmentDirections.actionProviderReservationsFragmentToReceiptDetailFragment(
                    referenceNumber = item.referenceNumber
                )
                findNavController().navigate(action)
            }
        )
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reservations.collectLatest { reservations ->
                adapter.submitList(reservations)
                val showEmpty = reservations.isEmpty()
                emptyState.visibility = if (showEmpty) View.VISIBLE else View.GONE
                recyclerView.visibility = if (showEmpty) View.GONE else View.VISIBLE
            }
        }

        emptyAction.setOnClickListener {
            findNavController().navigate(R.id.providerDashboardFragment)
        }

        viewModel.loadReservations(providerId)
    }
}
