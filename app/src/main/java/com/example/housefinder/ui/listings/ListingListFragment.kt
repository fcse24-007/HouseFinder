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
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class ListingListFragment : Fragment(R.layout.fragment_listing_list) {

    private var listJob: Job? = null

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        if (userId == null) {
            findNavController().navigate(R.id.action_listingListFragment_to_loginFragment)
            return
        }

        val welcome = view.findViewById<TextView>(R.id.txt_welcome)
        val recycler = view.findViewById<RecyclerView>(R.id.recycler_listings)
        val minPriceInput = view.findViewById<EditText>(R.id.edt_min_price)
        val maxPriceInput = view.findViewById<EditText>(R.id.edt_max_price)
        val locationInput = view.findViewById<EditText>(R.id.edt_location)
        val typeInput = view.findViewById<EditText>(R.id.edt_type)
        val dateInput = view.findViewById<EditText>(R.id.edt_availability_date)
        val applyButton = view.findViewById<Button>(R.id.btn_apply_filters)
        val clearButton = view.findViewById<Button>(R.id.btn_clear_filters)
        val preferencesButton = view.findViewById<Button>(R.id.btn_preferences)
        val alertsButton = view.findViewById<Button>(R.id.btn_check_alerts)
        val logoutButton = view.findViewById<Button>(R.id.btn_logout)

        welcome.text = getString(R.string.welcome_user, session.getDisplayName().orEmpty())

        val adapter = ListingAdapter { listing ->
            findNavController().navigate(
                R.id.action_listingListFragment_to_listingDetailFragment,
                bundleOf("listingId" to listing.id)
            )
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        fun observeFilters() {
            listJob?.cancel()
            listJob = lifecycleScope.launch {
                val min = minPriceInput.text.toString().trim().toFloatOrNull()
                val max = maxPriceInput.text.toString().trim().toFloatOrNull()
                val location = locationInput.text.toString().trim().takeIf { it.isNotBlank() }
                val type = typeInput.text.toString().trim().uppercase().takeIf { it.isNotBlank() }
                val availabilityDate = dateInput.text.toString().trim().takeIf { it.isNotBlank() }

                AppDatabase.getInstance(requireContext())
                    .listingDao()
                    .filter(min, max, location, type, availabilityDate)
                    .collect { adapter.submitList(it) }
            }
        }

        applyButton.setOnClickListener { observeFilters() }

        clearButton.setOnClickListener {
            minPriceInput.text.clear()
            maxPriceInput.text.clear()
            locationInput.text.clear()
            typeInput.text.clear()
            dateInput.text.clear()
            observeFilters()
        }

        preferencesButton.setOnClickListener {
            findNavController().navigate(R.id.action_listingListFragment_to_preferencesFragment)
        }

        alertsButton.setOnClickListener {
            lifecycleScope.launch {
                val db = AppDatabase.getInstance(requireContext())
                val pref = db.userPreferenceDao().getForUserOnce(userId)
                if (pref == null || !pref.notificationsEnabled) {
                    Toast.makeText(requireContext(), "No active preferences/alerts", Toast.LENGTH_SHORT).show()
                    return@launch
                }

                val lastCheck = session.getLastAlertCheck(userId)
                val matches = db.listingDao().findNewMatchingListings(
                    sinceTimestamp = lastCheck,
                    minPrice = pref.minPrice,
                    maxPrice = pref.maxPrice,
                    location = pref.location,
                    type = pref.type,
                    availabilityDate = null
                )

                session.setLastAlertCheck(userId, System.currentTimeMillis())
                val message = if (matches.isEmpty()) {
                    "No new listings matched your preferences"
                } else {
                    "Smart alert: ${matches.size} new matching listing(s)"
                }
                Snackbar.make(view, message, Snackbar.LENGTH_LONG).show()
            }
        }

        logoutButton.setOnClickListener {
            session.clearSession()
            findNavController().navigate(R.id.action_listingListFragment_to_loginFragment)
        }

        observeFilters()
        lifecycleScope.launch { AppDatabase.getInstance(requireContext()).userPreferenceDao().getForUser(userId).firstOrNull() }
    }
}

