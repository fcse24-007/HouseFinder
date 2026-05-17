package com.example.housefinder.ui.listings

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ListingListViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class ListingListFragment : Fragment(R.layout.fragment_listing_list) {

    private val viewModel: ListingListViewModel by viewModels()

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
        val applyFiltersButton = view.findViewById<Button>(R.id.btn_apply_filters)
        val clearFiltersButton = view.findViewById<Button>(R.id.btn_clear_filters)
        val minPriceInput = view.findViewById<EditText>(R.id.edt_filter_min_price)
        val maxPriceInput = view.findViewById<EditText>(R.id.edt_filter_max_price)
        val locationInput = view.findViewById<AutoCompleteTextView>(R.id.edt_filter_location)
        val typeInput = view.findViewById<AutoCompleteTextView>(R.id.edt_filter_type)
        val availabilityInput = view.findViewById<AutoCompleteTextView>(R.id.edt_filter_availability)

        locationInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ListingInputOptions.gaboroneAreas
            )
        )
        locationInput.setRawInputType(InputType.TYPE_NULL)
        locationInput.setOnClickListener { locationInput.showDropDown() }
        typeInput.setAdapter(
            ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ListingInputOptions.roomTypeLabels
            )
        )
        typeInput.setRawInputType(InputType.TYPE_NULL)
        typeInput.setOnClickListener { typeInput.showDropDown() }
        availabilityInput.setRawInputType(InputType.TYPE_NULL)
        availabilityInput.setOnClickListener {
            showDatePicker { selectedDate ->
                availabilityInput.setText(selectedDate, false)
            }
        }

        menuButton.setOnClickListener {
            (activity as? com.example.housefinder.MainActivity)
                ?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
                ?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        val adapter = ListingAdapter { item ->
            val action =
                ListingListFragmentDirections.actionListingListFragmentToListingDetailFragment(item.listing.id)
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

        applyFiltersButton.setOnClickListener {
            val minPrice = parseOptionalFloat(minPriceInput.text.toString())
            val maxPrice = parseOptionalFloat(maxPriceInput.text.toString())
            if (minPriceInput.text.toString().isNotBlank() && minPrice == null) {
                Toast.makeText(requireContext(), R.string.filter_invalid_price_min, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (maxPriceInput.text.toString().isNotBlank() && maxPrice == null) {
                Toast.makeText(requireContext(), R.string.filter_invalid_price_max, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (minPrice != null && minPrice < 0f) {
                Toast.makeText(requireContext(), R.string.filter_invalid_price_min, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (maxPrice != null && maxPrice < 0f) {
                Toast.makeText(requireContext(), R.string.filter_invalid_price_max, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                Toast.makeText(requireContext(), R.string.filter_invalid_price_range, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val availabilityText = availabilityInput.text.toString().trim()
            val availabilityDate = if (availabilityText.isBlank()) {
                null
            } else {
                HouseDateFormatter.toStorageDate(availabilityText)
            }
            if (availabilityText.isNotBlank() && availabilityDate == null) {
                Toast.makeText(requireContext(), R.string.filter_invalid_date, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val selectedTypeLabel = typeInput.text.toString().trim().takeIf { it.isNotBlank() }
            viewModel.applyFilters(
                minPrice = minPrice,
                maxPrice = maxPrice,
                location = locationInput.text.toString().trim().takeIf { it.isNotBlank() },
                type = selectedTypeLabel?.let { ListingInputOptions.toStorageType(it) },
                availabilityDate = availabilityDate
            )
        }

        clearFiltersButton.setOnClickListener {
            minPriceInput.text?.clear()
            maxPriceInput.text?.clear()
            locationInput.text?.clear()
            typeInput.text?.clear()
            availabilityInput.text?.clear()
            viewModel.clearFilters()
        }

        alertsButton.setOnClickListener {
            val lastCheck = session.getLastAlertCheck(userId)
            viewModel.checkAlerts(userId, lastCheck)
        }
    }

    private fun parseOptionalFloat(value: String): Float? {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) null else trimmed.toFloatOrNull()
    }

    private fun showDatePicker(onDateSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            requireContext(),
            { _, year, month, dayOfMonth ->
                val selected = String.format("%02d-%02d-%04d", dayOfMonth, month + 1, year)
                onDateSelected(selected)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun showMatchedListingsDialog(matches: List<com.example.housefinder.db.entities.Listing>) {
        val listingTitles = matches.map { "${it.title}, P${it.price.toInt()}/month, ${it.location}" }.toTypedArray()

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("Smart Alert: ${matches.size} New Listing${if (matches.size > 1) "s" else ""}")
            .setItems(listingTitles) { _, which ->
                val selectedListing = matches[which]
                val action =
                    ListingListFragmentDirections.actionListingListFragmentToListingDetailFragment(selectedListing.id)
                findNavController().navigate(action)
            }
            .setNegativeButton("Dismiss") { dialog, _ -> dialog.dismiss() }
            .show()
    }
}
