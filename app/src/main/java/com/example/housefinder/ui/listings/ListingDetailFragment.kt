package com.example.housefinder.ui.listings

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.conversationIdFor
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingImageLoader
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.ListingDetailViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@AndroidEntryPoint
class ListingDetailFragment : Fragment(R.layout.fragment_listing_detail) {

    private val viewModel: ListingDetailViewModel by viewModels()
    private val args: ListingDetailFragmentArgs by navArgs()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingId = args.listingId
        val title = view.findViewById<TextView>(R.id.txt_detail_title)
        val description = view.findViewById<TextView>(R.id.txt_detail_description)
        val location = view.findViewById<TextView>(R.id.txt_detail_location)
        val price = view.findViewById<TextView>(R.id.txt_detail_rent_deposit)
        val type = view.findViewById<TextView>(R.id.txt_detail_type)
        val amenities = view.findViewById<TextView>(R.id.txt_detail_amenities)
        val availability = view.findViewById<TextView>(R.id.txt_detail_availability)
        val coverImage = view.findViewById<ImageView>(R.id.img_detail_cover)
        val imageCarousel = view.findViewById<RecyclerView>(R.id.rv_detail_image_carousel)
        val reserveButton = view.findViewById<Button>(R.id.btn_reserve)
        val chatButton = view.findViewById<Button>(R.id.btn_chat_landlord)
        val actionButtonsLayout = view.findViewById<View>(R.id.layout_action_buttons)
        val toolbar = view.findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.toolbar)
        toolbar.setNavigationOnClickListener { findNavController().popBackStack() }
        val sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUserId()
        if (currentUserId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        var isProvider = false
        var providerIdForListing: Int? = null
        var listingMissingHandled = false
        val carouselAdapter = ListingImageCarouselAdapter()

        imageCarousel.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        imageCarousel.adapter = carouselAdapter

        ListingImageLoader.bind(coverImage, null)
        viewLifecycleOwner.lifecycleScope.launch {
            AppDatabase.getInstance(requireContext())
                .listingImageDao()
                .getForListing(listingId)
                .collectLatest { images ->
                    val coverPath = images.firstOrNull()?.imagePath
                    ListingImageLoader.bind(coverImage, coverPath)
                    val carouselItems = if (images.size > 1) images.drop(1) else emptyList()
                    carouselAdapter.submitList(carouselItems)
                    imageCarousel.visibility = if (carouselItems.isEmpty()) View.GONE else View.VISIBLE
                }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            val sessionUser = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext())
                    .userDao()
                    .getById(currentUserId)
            }
            isProvider = sessionUser?.role == "PROVIDER"
            actionButtonsLayout.visibility = if (isProvider) View.GONE else View.VISIBLE
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.uiState.collectLatest { state ->
                when (state) {
                    is ListingDetailViewModel.UiState.Loading -> Unit
                    is ListingDetailViewModel.UiState.Missing -> {
                        if (!listingMissingHandled) {
                            listingMissingHandled = true
                            showErrorDialog(getString(R.string.error_listing_not_found))
                        }
                    }
                    is ListingDetailViewModel.UiState.Loaded -> {
                        val listing = state.listing
                        title.text = listing.title
                        description.text = listing.description
                        location.text = getString(R.string.location_value, listing.location)
                        price.text = getString(R.string.rent_deposit_value, listing.price.toInt(), listing.depositAmount)
                        type.text = getString(
                            R.string.type_value,
                            ListingInputOptions.toDisplayType(listing.type)
                        )
                        amenities.text = getString(R.string.amenities_value, listing.amenities)
                        availability.text = getString(
                            R.string.availability_from_value,
                            HouseDateFormatter.toDisplayDate(listing.availabilityDate)
                        )
                        providerIdForListing = listing.providerId

                        // Update reserve button state based on listing availability
                        val isAvailable = listing.status == "AVAILABLE"
                        reserveButton.isEnabled = isAvailable
                        reserveButton.text = if (isAvailable) {
                            getString(R.string.btn_reserve)
                        } else {
                            getString(R.string.listing_reserved_button)
                        }
                    }
                }
            }
        }

        viewModel.loadListing(listingId)

        reserveButton.setOnClickListener {
            if (isProvider) {
                Toast.makeText(requireContext(), "Providers cannot reserve listings", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (!reserveButton.isEnabled) {
                AlertDialog.Builder(requireContext())
                    .setTitle("Listing Unavailable")
                    .setMessage("This listing has been reserved")
                    .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                    .show()
                return@setOnClickListener
            }

            findNavController().navigate(
                ListingDetailFragmentDirections.actionListingDetailFragmentToPaymentFragment(listingId)
            )
        }

        chatButton.setOnClickListener {
            if (isProvider) return@setOnClickListener
            val userId = currentUserId
            val providerId = providerIdForListing
            if (providerId == null) {
                Toast.makeText(requireContext(), R.string.chat_open_error, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            findNavController().navigate(
                ListingDetailFragmentDirections.actionListingDetailFragmentToChatThreadFragment(
                    conversationId = conversationIdFor(userId, providerId, listingId),
                    listingId = listingId,
                    partnerId = providerId
                )
            )
        }
    }

    private fun showErrorDialog(message: String) {
        AlertDialog.Builder(requireContext())
            .setTitle("Error")
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
                findNavController().popBackStack()
            }
            .setCancelable(false)
            .show()
    }
}
