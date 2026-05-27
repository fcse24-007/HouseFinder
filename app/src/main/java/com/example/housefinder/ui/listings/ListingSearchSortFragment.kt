package com.example.housefinder.ui.listings

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.activityViewModels
import com.example.housefinder.R
import com.example.housefinder.viewmodel.ListingListViewModel
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ListingSearchSortFragment : BottomSheetDialogFragment(R.layout.fragment_listing_search_sort) {

    private val viewModel: ListingListViewModel by activityViewModels()

    override fun getTheme(): Int = R.style.ThemeOverlay_HouseFinder_BottomSheet

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val searchInput = view.findViewById<EditText>(R.id.edt_search_listings)
        val sortInput = view.findViewById<AutoCompleteTextView>(R.id.edt_sort_listings)
        val applyButton = view.findViewById<Button>(R.id.btn_apply_search)

        val sortOptions = listOf(
            SortOptionItem(getString(R.string.sort_newest), ListingListViewModel.SortOrder.NEWEST),
            SortOptionItem(getString(R.string.sort_price_low_high), ListingListViewModel.SortOrder.PRICE_ASC),
            SortOptionItem(getString(R.string.sort_price_high_low), ListingListViewModel.SortOrder.PRICE_DESC),
            SortOptionItem(getString(R.string.sort_distance), ListingListViewModel.SortOrder.DISTANCE_ASC)
        )
        sortInput.setAdapter(
            ArrayAdapter(requireContext(), android.R.layout.simple_dropdown_item_1line, sortOptions.map { it.label })
        )
        sortInput.setRawInputType(InputType.TYPE_NULL)
        sortInput.setOnClickListener { sortInput.showDropDown() }

        val currentFilter = viewModel.currentFilter.value
        searchInput.setText(currentFilter.keyword.orEmpty())
        sortOptions.firstOrNull { it.value == currentFilter.sortOrder }?.let { option ->
            sortInput.setText(option.label, false)
        }

        applyButton.setOnClickListener {
            val keyword = searchInput.text.toString().trim()
            val selectedSortLabel = sortInput.text.toString()
            val sortOrder = sortOptions.firstOrNull { it.label == selectedSortLabel }?.value
                ?: ListingListViewModel.SortOrder.NEWEST

            viewModel.updateSearchQuery(keyword)
            viewModel.updateSortOrder(sortOrder)
            dismiss()
        }
    }

    private data class SortOptionItem(val label: String, val value: ListingListViewModel.SortOrder)
}
