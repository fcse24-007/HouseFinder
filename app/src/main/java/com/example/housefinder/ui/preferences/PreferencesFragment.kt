package com.example.housefinder.ui.preferences

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.example.housefinder.R
import com.example.housefinder.db.entities.UserPreference
import com.example.housefinder.ui.common.HouseDateFormatter
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.PreferencesViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class PreferencesFragment : Fragment(R.layout.fragment_preferences) {

    private val viewModel: PreferencesViewModel by viewModels()

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val session = SessionManager(requireContext())
        val userId = session.getUserId()
        if (userId == null) {
            Toast.makeText(requireContext(), "Login required", Toast.LENGTH_SHORT).show()
            return
        }

        val minPriceInput = view.findViewById<EditText>(R.id.edt_min_price)
        val maxPriceInput = view.findViewById<EditText>(R.id.edt_max_price)
        val locationInput = view.findViewById<EditText>(R.id.edt_location)
        val typeInput = view.findViewById<EditText>(R.id.edt_type)
        val availabilityInput = view.findViewById<EditText>(R.id.edt_availability_date)
        val notificationsSwitch = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val saveButton = view.findViewById<Button>(R.id.btn_save_preferences)
        val toolbar = view.findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            (activity as? com.example.housefinder.MainActivity)
                ?.findViewById<androidx.drawerlayout.widget.DrawerLayout>(R.id.drawer_layout)
                ?.openDrawer(androidx.core.view.GravityCompat.START)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewModel.preference.collectLatest { pref ->
                if (pref != null) {
                    minPriceInput.setText(pref.minPrice?.toString().orEmpty())
                    maxPriceInput.setText(pref.maxPrice?.toString().orEmpty())
                    locationInput.setText(pref.location.orEmpty())
                    typeInput.setText(pref.type.orEmpty())
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

            val preference = UserPreference(
                userId = userId,
                minPrice = minPriceInput.text.toString().trim().toFloatOrNull(),
                maxPrice = maxPriceInput.text.toString().trim().toFloatOrNull(),
                location = locationInput.text.toString().trim().takeIf { it.isNotBlank() },
                type = typeInput.text.toString().trim().uppercase().takeIf { it.isNotBlank() },
                availabilityDate = availabilityDate,
                notificationsEnabled = notificationsSwitch.isChecked
            )

            viewModel.savePreference(preference)
            Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
        }
    }
}
