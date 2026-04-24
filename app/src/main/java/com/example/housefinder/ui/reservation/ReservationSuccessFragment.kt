package com.example.housefinder.ui.reservation

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R

class ReservationSuccessFragment : Fragment(R.layout.fragment_reservation_success) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val reference = requireArguments().getString("referenceNumber").orEmpty()
        view.findViewById<TextView>(R.id.txt_reference).text = getString(R.string.reservation_reference_value, reference)

        view.findViewById<Button>(R.id.btn_back_to_listings).setOnClickListener {
            findNavController().navigate(R.id.action_reservationSuccessFragment_to_listingListFragment)
        }
    }
}

