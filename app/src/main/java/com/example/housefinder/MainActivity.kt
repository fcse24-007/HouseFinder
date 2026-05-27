package com.example.housefinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.NavigationUI
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.housefinder.data.repository.UserRepository
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.worker.MatchingListingWorker
import com.google.android.material.bottomnavigation.BottomNavigationView
import java.util.concurrent.TimeUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var bottomNav: BottomNavigationView
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bottomNav = findViewById(R.id.bottom_nav)

        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val sessionManager = SessionManager(this)

        fun isAuthDestination(destinationId: Int): Boolean {
            return destinationId == R.id.loginFragment || destinationId == R.id.registerFragment
        }

        fun navigateToLoginIfNeeded() {
            if (navController.currentDestination?.id != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment)
            }
        }

        fun resolveDestination(itemId: Int, role: String): Int? {
            return when (itemId) {
                R.id.nav_home -> if (role == "PROVIDER") {
                    R.id.providerDashboardFragment
                } else {
                    R.id.listingListFragment
                }

                R.id.nav_my_reservations -> if (role == "PROVIDER") {
                    R.id.providerReservationsFragment
                } else {
                    R.id.myReservationsFragment
                }

                R.id.nav_preferences -> if (role == "PROVIDER") null else R.id.preferencesFragment
                R.id.nav_chat -> R.id.chatListFragment
                R.id.nav_settings -> R.id.settingsFragment
                else -> null
            }
        }

        fun applyRoleMenuState() {
            lifecycleScope.launch {
                val userId = sessionManager.getUserId()
                val user = userId?.let { userRepository.getById(it) }
                if (userId != null && user == null) {
                    sessionManager.clearSession()
                    navigateToLoginIfNeeded()
                    return@launch
                }

                val isProvider = user?.role == "PROVIDER"
                val reservationsTitle = if (isProvider) {
                    getString(R.string.nav_provider_reservations)
                } else {
                    getString(R.string.nav_my_reservations)
                }

                bottomNav.menu.findItem(R.id.nav_my_reservations).apply {
                    isVisible = true
                    title = reservationsTitle
                }
                bottomNav.menu.findItem(R.id.nav_preferences).isVisible = !isProvider
            }
        }

        bottomNav.setOnItemSelectedListener { item ->
            lifecycleScope.launch {
                val userId = sessionManager.getUserId()
                val user = userId?.let { userRepository.getById(it) }
                if (userId == null || user == null) {
                    sessionManager.clearSession()
                    navigateToLoginIfNeeded()
                    return@launch
                }

                val targetDestination = resolveDestination(item.itemId, user.role) ?: return@launch
                if (navController.currentDestination?.id != targetDestination) {
                    navController.navigate(targetDestination)
                }
            }
            true
        }

        bottomNav.setOnItemReselectedListener { }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuth = isAuthDestination(destination.id)
            if (isAuth) {
                bottomNav.visibility = View.GONE
            } else {
                bottomNav.visibility = View.VISIBLE
                applyRoleMenuState()
            }

            val bottomNavItem = when (destination.id) {
                R.id.listingListFragment,
                R.id.providerDashboardFragment,
                R.id.listingDetailFragment,
                R.id.listingFiltersFragment,
                R.id.providerListingFormFragment,
                R.id.paymentFragment,
                R.id.reservationSuccessFragment -> R.id.nav_home

                R.id.myReservationsFragment,
                R.id.providerReservationsFragment,
                R.id.receiptDetailFragment -> R.id.nav_my_reservations

                R.id.preferencesFragment -> R.id.nav_preferences
                R.id.chatListFragment,
                R.id.chatThreadFragment -> R.id.nav_chat
                R.id.settingsFragment,
                R.id.helpFragment -> R.id.nav_settings
                else -> null
            }
            bottomNavItem?.let { bottomNav.menu.findItem(it)?.isChecked = true }
        }

        applyRoleMenuState()
        setupBackgroundWork()
        requestNotificationPermissionIfNeeded()
    }

    private fun setupBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .build()

        val workRequest = PeriodicWorkRequestBuilder<MatchingListingWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "MatchingListingWork",
            ExistingPeriodicWorkPolicy.KEEP,
            workRequest
        )
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        if (
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}
