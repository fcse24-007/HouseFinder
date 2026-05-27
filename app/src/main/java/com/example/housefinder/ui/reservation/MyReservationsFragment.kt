package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.Button
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.conversationIdFor
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.launch

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.MyReservationsViewModel
import kotlinx.coroutines.flow.collectLatest

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MyReservationsFragment : Fragment(R.layout.fragment_my_reservations) {

    private val viewModel: MyReservationsViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val studentId = session.getUserId()
        if (studentId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        val emptyState = view.findViewById<View>(R.id.layout_empty_reservations)
        val emptyAction = view.findViewById<Button>(R.id.btn_empty_reservations_action)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_my_reservations)

        val adapter = MyReservationsAdapter(
            onChat = { item ->
                val conversationId = conversationIdFor(studentId, item.providerId, item.listingId)
                val action = MyReservationsFragmentDirections.actionMyReservationsFragmentToChatThreadFragment(
                    conversationId = conversationId,
                    listingId = item.listingId,
                    partnerId = item.providerId
                )
                findNavController().navigate(action)
            },
            onViewReceipt = { item ->
                val action = MyReservationsFragmentDirections.actionMyReservationsFragmentToReceiptDetailFragment(
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
            findNavController().navigate(R.id.listingListFragment)
        }

        viewModel.loadReservations(studentId)
    }
}
