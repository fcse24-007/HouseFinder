package com.example.housefinder.ui.preferences

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.UserPreference
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.ListingInputOptions
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.PreferencesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.Calendar

@AndroidEntryPoint
class PreferencesFragment : Fragment(R.layout.fragment_preferences) {

    private val viewModel: PreferencesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            val navOptions = NavOptions.Builder()
                .setPopUpTo(R.id.nav_graph, true)
                .build()
            findNavController().navigate(R.id.loginFragment, null, navOptions)
            return
        }

        val minPriceInput = view.findViewById<EditText>(R.id.edt_min_price)
        val maxPriceInput = view.findViewById<EditText>(R.id.edt_max_price)
        val locationInput = view.findViewById<AutoCompleteTextView>(R.id.edt_location)
        val typeInput = view.findViewById<AutoCompleteTextView>(R.id.edt_type)
        val availabilityInput = view.findViewById<AutoCompleteTextView>(R.id.edt_availability_date)
        val notificationsSwitch = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val saveButton = view.findViewById<Button>(R.id.btn_save_preferences)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        toolbar.navigationIcon = null

        locationInput.setAdapter(
            android.widget.ArrayAdapter(
                requireContext(),
                android.R.layout.simple_dropdown_item_1line,
                ListingInputOptions.gaboroneAreas
            )
        )
        locationInput.setRawInputType(InputType.TYPE_NULL)
        locationInput.setOnClickListener { locationInput.showDropDown() }

        typeInput.setAdapter(
            android.widget.ArrayAdapter(
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

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.preference.collectLatest { pref ->
                if (pref != null) {
                    minPriceInput.setText(pref.minPrice?.toString().orEmpty())
                    maxPriceInput.setText(pref.maxPrice?.toString().orEmpty())
                    locationInput.setText(pref.location.orEmpty(), false)
                    typeInput.setText(ListingInputOptions.toDisplayType(pref.type), false)
                    availabilityInput.setText(HouseDateFormatter.toDisplayDate(pref.availabilityDate))
                    notificationsSwitch.isChecked = pref.notificationsEnabled
                }
            }
        }

        viewModel.loadPreference(userId)

        saveButton.setOnClickListener {
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

            val typeLabel = typeInput.text.toString().trim()
            val typeStorage = if (typeLabel.isBlank()) null else ListingInputOptions.toStorageType(typeLabel)
            if (typeLabel.isNotBlank() && typeStorage == null) {
                Toast.makeText(requireContext(), R.string.listing_type_required, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val preference = UserPreference(
                userId = userId,
                minPrice = minPriceInput.text.toString().trim().toFloatOrNull(),
                maxPrice = maxPriceInput.text.toString().trim().toFloatOrNull(),
                location = locationInput.text.toString().trim().takeIf { it.isNotBlank() },
                type = typeStorage,
                availabilityDate = availabilityDate,
                notificationsEnabled = notificationsSwitch.isChecked
            )

            viewModel.savePreference(preference)
            Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
        }
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
