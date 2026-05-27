package com.example.housefinder.ui.listings

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.ui.common.notification.NotificationHelper
import com.example.housefinder.viewmodel.ListingListViewModel
import com.example.housefinder.db.entities.ListingWithImage
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class ListingListFragment : Fragment(R.layout.fragment_listing_list) {

    private val viewModel: ListingListViewModel by activityViewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        if (userId == null) {
            findNavController().navigate(R.id.action_listingListFragment_to_loginFragment)
            return
        }

        val recycler = view.findViewById<RecyclerView>(R.id.rv_listings)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        val chipGroup = view.findViewById<ChipGroup>(R.id.chip_group_filters)
        val emptyState = view.findViewById<View>(R.id.layout_empty_listings)
        val emptyTitle = view.findViewById<TextView>(R.id.txt_empty_listings_title)
        val emptySubtitle = view.findViewById<TextView>(R.id.txt_empty_listings_subtitle)
        val emptyAction = view.findViewById<Button>(R.id.btn_empty_listings_action)

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_filters -> {
                    findNavController().navigate(R.id.action_listingListFragment_to_listingFiltersFragment)
                    true
                }
                R.id.action_search -> {
                    findNavController().navigate(R.id.action_listingListFragment_to_listingSearchSortFragment)
                    true
                }
                else -> false
            }
        }

        val adapter = ListingAdapter { item ->
            val action =
                ListingListFragmentDirections.actionListingListFragmentToListingDetailFragment(item.listing.id)
            findNavController().navigate(action)
        }

        recycler.layoutManager = LinearLayoutManager(requireContext())
        recycler.adapter = adapter

        var latestFilter = viewModel.currentFilter.value
        var latestListings: List<ListingWithImage> = emptyList()

        fun hasActiveFilters(filter: ListingListViewModel.ListingFilter): Boolean {
            return filter.minPrice != null ||
                filter.maxPrice != null ||
                !filter.location.isNullOrBlank() ||
                !filter.type.isNullOrBlank() ||
                !filter.availabilityDate.isNullOrBlank() ||
                !filter.keyword.isNullOrBlank()
        }

        fun renderEmptyState() {
            val isEmpty = latestListings.isEmpty()
            emptyState.visibility = if (isEmpty) View.VISIBLE else View.GONE
            recycler.visibility = if (isEmpty) View.GONE else View.VISIBLE
            if (!isEmpty) {
                return
            }

            if (hasActiveFilters(latestFilter)) {
                emptyTitle.setText(R.string.empty_listings_title_filtered)
                emptySubtitle.setText(R.string.empty_listings_subtitle_filtered)
                emptyAction.text = getString(R.string.filter_clear)
                emptyAction.setOnClickListener {
                    viewModel.resetAllFilters()
                }
            } else {
                emptyTitle.setText(R.string.empty_listings_title)
                emptySubtitle.setText(R.string.empty_listings_subtitle)
                emptyAction.text = getString(R.string.start_searching)
                emptyAction.setOnClickListener {
                    findNavController().navigate(R.id.action_listingListFragment_to_listingFiltersFragment)
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listings.collectLatest { listings ->
                latestListings = listings
                adapter.submitList(listings)
                renderEmptyState()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.imageChangeToken.collectLatest {
                adapter.notifyDataSetChanged()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.alertMatches.collectLatest { matches ->
                session.setLastAlertCheck(userId, System.currentTimeMillis())
                val notificationMessage = if (matches.size == 1) {
                    "Found a new match: ${matches[0].title}"
                } else {
                    "Found ${matches.size} new matches for your preferences!"
                }
                NotificationHelper.showNotification(
                    requireContext(),
                    "New House Matches",
                    notificationMessage
                )
                showMatchedListingsDialog(matches)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.error.collectLatest { msg ->
                Toast.makeText(requireContext(), msg, Toast.LENGTH_SHORT).show()
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.currentFilter.collectLatest { filter ->
                latestFilter = filter
                renderFilterChips(
                    chipGroup = chipGroup,
                    filter = filter,
                    onClearAll = {
                        viewModel.resetAllFilters()
                    }
                )
                renderEmptyState()
            }
        }

        val lastCheck = session.getLastAlertCheck(userId)
        viewModel.checkAlerts(userId, lastCheck)
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

    private fun renderFilterChips(
        chipGroup: ChipGroup,
        filter: ListingListViewModel.ListingFilter,
        onClearAll: () -> Unit
    ) {
        chipGroup.removeAllViews()

        val chips = mutableListOf<Chip>()
        val context = requireContext()

        filter.minPrice?.let {
            chips += buildChip(context, "Min: P${formatPrice(it)}") {
                viewModel.updateFilter { current -> current.copy(minPrice = null) }
            }
        }
        filter.maxPrice?.let {
            chips += buildChip(context, "Max: P${formatPrice(it)}") {
                viewModel.updateFilter { current -> current.copy(maxPrice = null) }
            }
        }
        filter.location?.takeIf { it.isNotBlank() }?.let { location ->
            chips += buildChip(context, "Location: $location") {
                viewModel.updateFilter { current -> current.copy(location = null) }
            }
        }
        filter.type?.takeIf { it.isNotBlank() }?.let { type ->
            val label = ListingInputOptions.toDisplayType(type)
            chips += buildChip(context, "Type: $label") {
                viewModel.updateFilter { current -> current.copy(type = null) }
            }
        }
        filter.availabilityDate?.takeIf { it.isNotBlank() }?.let { date ->
            val displayDate = HouseDateFormatter.toDisplayDate(date)
            chips += buildChip(context, "Available: $displayDate") {
                viewModel.updateFilter { current -> current.copy(availabilityDate = null) }
            }
        }
        filter.keyword?.takeIf { it.isNotBlank() }?.let { keyword ->
            chips += buildChip(context, "Search: $keyword") {
                viewModel.updateSearchQuery("")
            }
        }

        chips.forEach { chipGroup.addView(it) }

        if (chips.isNotEmpty()) {
            val clearChip = buildChip(context, getString(R.string.clear_all_filters)) { onClearAll() }
            chipGroup.addView(clearChip)
        }

        chipGroup.visibility = if (chips.isEmpty()) View.GONE else View.VISIBLE
    }

    private fun buildChip(
        context: android.content.Context,
        text: String,
        onClose: () -> Unit
    ): Chip {
        return Chip(context).apply {
            this.text = text
            isCloseIconVisible = true
            setOnCloseIconClickListener { onClose() }
        }
    }

    private fun formatPrice(value: Float): String {
        return if (value % 1f == 0f) value.toInt().toString() else value.toString()
    }

    private data class SortOptionItem(val label: String, val value: ListingListViewModel.SortOrder)
}
