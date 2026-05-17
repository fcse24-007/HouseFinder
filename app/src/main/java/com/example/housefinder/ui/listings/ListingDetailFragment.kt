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
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.conversationIdFor
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingImageLoader
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
        val reserveButton = view.findViewById<Button>(R.id.btn_reserve)
        val chatButton = view.findViewById<Button>(R.id.btn_chat_landlord)
        val menuButton = view.findViewById<View>(R.id.btn_menu)
        val sessionManager = SessionManager(requireContext())
        val currentUserId = sessionManager.getUserId()
        if (currentUserId == null) {
            findNavController().navigate(R.id.loginFragment)
            return
        }

        var isProvider = false
        var providerIdForListing: Int? = null

        ListingImageLoader.bind(coverImage, null)
        viewLifecycleOwner.lifecycleScope.launch {
            val sessionUser = withContext(Dispatchers.IO) {
                AppDatabase.getInstance(requireContext())
                    .userDao()
                    .getById(currentUserId)
            }
            isProvider = sessionUser?.role == "PROVIDER"
            chatButton.visibility = if (isProvider) View.GONE else View.VISIBLE
            reserveButton.visibility = if (isProvider) View.GONE else View.VISIBLE
        }

        menuButton.setOnClickListener {
            (activity as? com.example.housefinder.MainActivity)?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.listing.collectLatest { listing ->
                if (listing == null) return@collectLatest

                title.text = listing.title
                description.text = listing.description
                location.text = getString(R.string.location_value, listing.location)
                price.text = getString(R.string.rent_deposit_value, listing.price.toInt(), listing.depositAmount)
                type.text = getString(R.string.type_value, listing.type)
                amenities.text = getString(R.string.amenities_value, listing.amenities)
                availability.text = getString(
                    R.string.availability_from_value,
                    HouseDateFormatter.toDisplayDate(listing.availabilityDate)
                )
                providerIdForListing = listing.providerId
                
                val imagePath = withContext(Dispatchers.IO) {
                    AppDatabase.getInstance(requireContext())
                        .listingImageDao()
                        .getCoverImage(listing.id)
                        ?.imagePath
                }
                ListingImageLoader.bind(coverImage, imagePath)

                // Update reserve button state based on listing availability
                reserveButton.isEnabled = listing.status == "AVAILABLE"
                if (listing.status != "AVAILABLE") {
                    reserveButton.text = "Already Reserved"
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
