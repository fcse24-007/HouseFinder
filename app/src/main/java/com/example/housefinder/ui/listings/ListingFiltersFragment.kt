package com.example.housefinder.ui.listings

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputLayout
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.viewmodel.ListingListViewModel
import dagger.hilt.android.AndroidEntryPoint
import java.util.Calendar

@AndroidEntryPoint
class ListingFiltersFragment : BottomSheetDialogFragment(R.layout.fragment_listing_filters) {

    private val viewModel: ListingListViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.ThemeOverlay_HouseFinder_BottomSheet

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val closeButton = view.findViewById<View>(R.id.btn_close_filters)
        val applyFiltersButton = view.findViewById<Button>(R.id.btn_apply_filters)
        val clearFiltersButton = view.findViewById<Button>(R.id.btn_clear_filters)
        val minPriceLayout = view.findViewById<TextInputLayout>(R.id.layout_filter_min_price)
        val maxPriceLayout = view.findViewById<TextInputLayout>(R.id.layout_filter_max_price)
        val availabilityLayout = view.findViewById<TextInputLayout>(R.id.layout_filter_availability)
        val minPriceInput = view.findViewById<EditText>(R.id.edt_filter_min_price)
        val maxPriceInput = view.findViewById<EditText>(R.id.edt_filter_max_price)
        val locationInput = view.findViewById<AutoCompleteTextView>(R.id.edt_filter_location)
        val typeInput = view.findViewById<AutoCompleteTextView>(R.id.edt_filter_type)
        val availabilityInput = view.findViewById<EditText>(R.id.edt_filter_availability)

        closeButton.setOnClickListener { findNavController().popBackStack() }

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
                availabilityInput.setText(selectedDate)
            }
        }

        val currentFilter = viewModel.currentFilter.value
        minPriceInput.setText(currentFilter.minPrice?.let { formatPrice(it) }.orEmpty())
        maxPriceInput.setText(currentFilter.maxPrice?.let { formatPrice(it) }.orEmpty())
        locationInput.setText(currentFilter.location.orEmpty(), false)
        val typeLabel = currentFilter.type?.let { ListingInputOptions.toDisplayType(it) }
        if (!typeLabel.isNullOrBlank()) {
            typeInput.setText(typeLabel, false)
        }
        availabilityInput.setText(HouseDateFormatter.toDisplayDate(currentFilter.availabilityDate))

        applyFiltersButton.setOnClickListener {
            minPriceLayout.error = null
            maxPriceLayout.error = null
            availabilityLayout.error = null

            val minPrice = parseOptionalFloat(minPriceInput.text.toString())
            val maxPrice = parseOptionalFloat(maxPriceInput.text.toString())
            if (minPriceInput.text.toString().isNotBlank() && minPrice == null) {
                minPriceLayout.error = getString(R.string.filter_invalid_price_min)
                return@setOnClickListener
            }
            if (maxPriceInput.text.toString().isNotBlank() && maxPrice == null) {
                maxPriceLayout.error = getString(R.string.filter_invalid_price_max)
                return@setOnClickListener
            }
            if (minPrice != null && minPrice < 0f) {
                minPriceLayout.error = getString(R.string.filter_invalid_price_min)
                return@setOnClickListener
            }
            if (maxPrice != null && maxPrice < 0f) {
                maxPriceLayout.error = getString(R.string.filter_invalid_price_max)
                return@setOnClickListener
            }
            if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
                minPriceLayout.error = getString(R.string.filter_invalid_price_range)
                maxPriceLayout.error = getString(R.string.filter_invalid_price_range)
                return@setOnClickListener
            }

            val availabilityText = availabilityInput.text.toString().trim()
            val availabilityDate = if (availabilityText.isBlank()) {
                null
            } else {
                HouseDateFormatter.toStorageDate(availabilityText)
            }
            if (availabilityText.isNotBlank() && availabilityDate == null) {
                availabilityLayout.error = getString(R.string.filter_invalid_date)
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
            findNavController().popBackStack()
        }

        clearFiltersButton.setOnClickListener {
            minPriceInput.text?.clear()
            maxPriceInput.text?.clear()
            locationInput.text?.clear()
            typeInput.text?.clear()
            availabilityInput.text?.clear()
            minPriceLayout.error = null
            maxPriceLayout.error = null
            availabilityLayout.error = null
            viewModel.clearFilters()
            findNavController().popBackStack()
        }
    }

    private fun parseOptionalFloat(value: String): Float? {
        val trimmed = value.trim()
        return if (trimmed.isBlank()) null else trimmed.toFloatOrNull()
    }

    private fun formatPrice(value: Float): String {
        return if (value % 1f == 0f) value.toInt().toString() else value.toString()
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
}
