package com.example.housefinder.ui.preferences

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.widget.SwitchCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.example.housefinder.R
import com.example.housefinder.db.entities.AppDatabase
import com.example.housefinder.db.entities.UserPreference
import com.example.housefinder.ui.common.SessionManager
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

class PreferencesFragment : Fragment(R.layout.fragment_preferences) {

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
        val notificationsSwitch = view.findViewById<SwitchCompat>(R.id.switch_notifications)
        val saveButton = view.findViewById<Button>(R.id.btn_save_preferences)

        lifecycleScope.launch {
            val pref = AppDatabase.getInstance(requireContext())
                .userPreferenceDao()
                .getForUser(userId)
                .firstOrNull()

            if (pref != null) {
                minPriceInput.setText(pref.minPrice?.toString().orEmpty())
                maxPriceInput.setText(pref.maxPrice?.toString().orEmpty())
                locationInput.setText(pref.location.orEmpty())
                typeInput.setText(pref.type.orEmpty())
                notificationsSwitch.isChecked = pref.notificationsEnabled
            }
        }

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                val preference = UserPreference(
                    userId = userId,
                    minPrice = minPriceInput.text.toString().trim().toFloatOrNull(),
                    maxPrice = maxPriceInput.text.toString().trim().toFloatOrNull(),
                    location = locationInput.text.toString().trim().takeIf { it.isNotBlank() },
                    type = typeInput.text.toString().trim().uppercase().takeIf { it.isNotBlank() },
                    notificationsEnabled = notificationsSwitch.isChecked
                )

                AppDatabase.getInstance(requireContext()).userPreferenceDao().upsert(preference)
                Toast.makeText(requireContext(), "Preferences saved", Toast.LENGTH_SHORT).show()
            }
        }
    }
}

