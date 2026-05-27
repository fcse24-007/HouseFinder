package com.example.housefinder.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavOptions
import androidx.navigation.fragment.findNavController
import com.example.housefinder.R
import com.example.housefinder.db.entities.UserPreference
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.viewmodel.PreferencesViewModel
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.switchmaterial.SwitchMaterial
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SettingsFragment : Fragment() {
    private val preferenceViewModel: PreferencesViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_settings, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val sessionManager = SessionManager(requireContext())
        val userId = sessionManager.getUserId()
        if (userId == null) {
            navigateToLogin()
            return
        }

        val toolbar = view.findViewById<MaterialToolbar>(R.id.toolbar)
        toolbar.navigationIcon = null

        toolbar.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_logout -> {
                    sessionManager.clearSession()
                    navigateToLogin()
                    true
                }
                else -> false
            }
        }

        view.findViewById<View>(R.id.btn_help_faq).setOnClickListener {
            findNavController().navigate(R.id.helpFragment)
        }

        val notificationsSwitch = view.findViewById<SwitchMaterial>(R.id.switch_notifications)
        var applyingPreference = false

        notificationsSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (applyingPreference) return@setOnCheckedChangeListener
            val currentPreference = preferenceViewModel.preference.value
            val updatedPreference = (currentPreference ?: UserPreference(userId = userId))
                .copy(notificationsEnabled = isChecked)
            preferenceViewModel.savePreference(updatedPreference)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            preferenceViewModel.preference.collectLatest { preference ->
                applyingPreference = true
                notificationsSwitch.isChecked = preference?.notificationsEnabled ?: true
                applyingPreference = false
            }
        }
        preferenceViewModel.loadPreference(userId)
    }

    private fun navigateToLogin() {
        val navOptions = NavOptions.Builder()
            .setPopUpTo(R.id.nav_graph, true)
            .build()
        findNavController().navigate(R.id.loginFragment, null, navOptions)
    }
}
