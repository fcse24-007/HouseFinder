package com.example.housefinder.ui.provider

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.Listing
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

import androidx.fragment.app.viewModels
import com.example.housefinder.viewmodel.ProviderDashboardViewModel
import kotlinx.coroutines.flow.collectLatest

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class ProviderDashboardFragment : Fragment(R.layout.fragment_provider_dashboard) {

    private val viewModel: ProviderDashboardViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val providerId = session.getUserId()

        if (providerId == null) {
            findNavController().navigate(R.id.action_providerDashboardFragment_to_loginFragment)
            return
        }

        val statsText = view.findViewById<TextView>(R.id.txt_provider_stats)
        val addListingButton = view.findViewById<View>(R.id.fab_add_listing)
        val menuButton = view.findViewById<View>(R.id.btn_menu)
        val listingsRecycler = view.findViewById<RecyclerView>(R.id.rv_provider_listings)

        menuButton.setOnClickListener {
            (activity as? com.example.housefinder.MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        val listingAdapter = ProviderListingAdapter { listing ->
            showListingOptionsDialog(listing, providerId)
        }

        listingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        listingsRecycler.adapter = listingAdapter

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listings.collectLatest { listings ->
                listingAdapter.submitList(listings)
                val activeCount = listings.count { it.status == "AVAILABLE" }
                val reservedCount = listings.count { it.status == "RESERVED" }
                statsText.text = "Total: ${listings.size} | Active: $activeCount | Reserved: $reservedCount"
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val sessionUser = AppDatabase.getInstance(requireContext()).userDao().getById(providerId)
            if (sessionUser?.role != "PROVIDER") {
                Toast.makeText(requireContext(), "Provider access required", Toast.LENGTH_SHORT).show()
                findNavController().navigate(R.id.listingListFragment)
                return@launch
            }
            viewModel.loadListings(providerId)
        }

        addListingButton.setOnClickListener {
            val action = ProviderDashboardFragmentDirections.actionProviderDashboardFragmentToProviderListingFormFragment(listingId = -1)
            findNavController().navigate(action)
        }
    }

    private fun showListingOptionsDialog(listing: Listing, providerId: Int) {
        val options = arrayOf("Edit", "Delete", "Cancel")

        AlertDialog.Builder(requireContext())
            .setTitle(listing.title)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> {
                        val action = ProviderDashboardFragmentDirections.actionProviderDashboardFragmentToProviderListingFormFragment(listingId = listing.id)
                        findNavController().navigate(action)
                    }
                    1 -> {
                        viewModel.deleteListing(listing.id, providerId)
                        Toast.makeText(requireContext(), "Listing deleted", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .show()
    }
}

private class ProviderListingAdapter(
    private val onClick: (Listing) -> Unit
) : androidx.recyclerview.widget.ListAdapter<Listing, ProviderListingAdapter.ListingViewHolder>(ProviderListingDiffCallback()) {

    override fun onCreateViewHolder(parent: android.view.ViewGroup, viewType: Int): ListingViewHolder {
        val view = android.view.LayoutInflater.from(parent.context)
            .inflate(R.layout.item_provider_listing, parent, false)
        return ListingViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ListingViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ListingViewHolder(
        itemView: View,
        private val onClick: (Listing) -> Unit
    ) : RecyclerView.ViewHolder(itemView) {
        private val title: TextView = itemView.findViewById(R.id.txt_listing_title)
        private val price: TextView = itemView.findViewById(R.id.txt_listing_price)
        private val status: TextView = itemView.findViewById(R.id.txt_listing_status)
        private var currentItem: Listing? = null

        init {
            itemView.setOnClickListener {
                currentItem?.let { onClick(it) }
            }
        }

        fun bind(item: Listing) {
            currentItem = item
            title.text = item.title
            price.text = "BWP ${item.price.toInt()}/month"
            status.text = item.status
            status.setTextColor(
                ContextCompat.getColor(
                    itemView.context,
                    if (item.status == "AVAILABLE") R.color.success else R.color.warning
                )
            )
        }
    }

    private class ProviderListingDiffCallback : androidx.recyclerview.widget.DiffUtil.ItemCallback<Listing>() {
        override fun areItemsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: Listing, newItem: Listing): Boolean {
            return oldItem == newItem
        }
    }
}
