package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
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

        val emptyText = view.findViewById<TextView>(R.id.txt_empty_reservations)
        val recyclerView = view.findViewById<RecyclerView>(R.id.rv_my_reservations)
        val menuButton = view.findViewById<View>(R.id.btn_menu)

        menuButton.setOnClickListener {
            (activity as? com.example.housefinder.MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        val adapter = MyReservationsAdapter()
        recyclerView.layoutManager = LinearLayoutManager(requireContext())
        recyclerView.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.reservations.collectLatest { reservations ->
                adapter.submitList(reservations)
                val showEmpty = reservations.isEmpty()
                emptyText.visibility = if (showEmpty) View.VISIBLE else View.GONE
                recyclerView.visibility = if (showEmpty) View.GONE else View.VISIBLE
            }
        }

        viewModel.loadReservations(studentId)
    }
}
