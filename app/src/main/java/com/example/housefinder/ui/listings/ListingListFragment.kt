package com.example.housefinder.ui.listings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.ui.common.SessionManager
import com.google.android.material.snackbar.Snackbar
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.ListingListViewModel
import kotlinx.coroutines.flow.collectLatest

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListingListFragment : Fragment(R.layout.fragment_listing_list) {

    private val viewModel: ListingListViewModel by viewModels()
    private var imageRefreshJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        if (userId == null) {
            findNavController().navigate(R.id.action_listingListFragment_to_loginFragment)
            return
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rv_listings)
        val menuButton = view.findViewById<View>(R.id.btn_menu)
        val alertsButton = view.findViewById<Button>(R.id.btn_check_alerts)

        menuButton.setOnClickListener {
            (activity as? com.example.housefinder.MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        val adapter = ListingAdapter { item ->
            val action = ListingListFragmentDirections.actionListingListFragmentToListingDetailFragment(item.listing.id)
            findNavController().navigate(action)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listings.collectLatest { adapter.submitList(it) }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.imageChangeToken.collectLatest { 
                adapter.notifyDataSetChanged() 
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.alertMatches.collectLatest { matches ->
                session.setLastAlertCheck(userId, System.currentTimeMillis())
                showMatchedListingsDialog(matches)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        alertsButton.setOnClickListener {
            val lastCheck = session.getLastAlertCheck(userId)
            viewModel.checkAlerts(userId, lastCheck)
        }
    }

    private fun showMatchedListingsDialog(matches: List<com.example.housefinder.db.entities.Listing>) {
        val listingTitles = matches.map { "${it.title}, P${it.price.toInt()}/month, ${it.location}" }.toTypedArray()
        
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Smart Alert: ${matches.size} New Listing${if (matches.size > 1) "s" else ""}")
            .setItems(listingTitles) { _, which ->
                val selectedListing = matches[which]
                val action = ListingListFragmentDirections.actionListingListFragmentToListingDetailFragment(selectedListing.id)
                findNavController().navigate(action)
            }
            .setNegativeButton("Dismiss") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}

