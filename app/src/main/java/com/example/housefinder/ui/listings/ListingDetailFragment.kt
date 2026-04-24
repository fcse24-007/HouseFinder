package com.example.housefinder.ui.listings

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.core.os.bundleOf
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import kotlinx.coroutines.launch

class ListingDetailFragment : Fragment(R.layout.fragment_listing_detail) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val listingId = requireArguments().getInt("listingId")
        val title = view.findViewById<TextView>(R.id.txt_title)
        val description = view.findViewById<TextView>(R.id.txt_description)
        val location = view.findViewById<TextView>(R.id.txt_location)
        val price = view.findViewById<TextView>(R.id.txt_price)
        val type = view.findViewById<TextView>(R.id.txt_type)
        val availability = view.findViewById<TextView>(R.id.txt_availability)
        val reserveButton = view.findViewById<Button>(R.id.btn_reserve)

        lifecycleScope.launch {
            AppDatabase.getInstance(requireContext())
                .listingDao()
                .getById(listingId)
                .collect { listing ->
                    if (listing == null) return@collect
                    title.text = listing.title
                    description.text = listing.description
                    location.text = getString(R.string.location_value, listing.location)
                    price.text = getString(R.string.rent_deposit_value, listing.price.toInt(), listing.depositAmount)
                    type.text = getString(R.string.type_value, listing.type)
                    availability.text = getString(R.string.availability_from_value, listing.availabilityDate)
                }
        }

        reserveButton.setOnClickListener {
            findNavController().navigate(
                R.id.action_listingDetailFragment_to_paymentFragment,
                bundleOf("listingId" to listingId)
            )
        }
    }
}

