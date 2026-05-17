package com.example.housefinder

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.drawerlayout.widget.DrawerLayout
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
import com.google.android.material.navigation.NavigationView
import java.util.concurrent.TimeUnit
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var userRepository: UserRepository

    private lateinit var drawerLayout: DrawerLayout
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController
        val sessionManager = SessionManager(this)

        NavigationUI.setupWithNavController(navView, navController)

        fun isAuthDestination(destinationId: Int): Boolean {
            return destinationId == R.id.loginFragment || destinationId == R.id.registerFragment
        }

        fun navigateToLoginIfNeeded() {
            if (navController.currentDestination?.id != R.id.loginFragment) {
                navController.navigate(R.id.loginFragment)
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
                navView.menu.findItem(R.id.nav_my_reservations).isVisible = !isProvider
                navView.menu.findItem(R.id.nav_preferences).isVisible = !isProvider

                val headerView = navView.getHeaderView(0)
                headerView.findViewById<android.widget.TextView>(R.id.nav_header_name).text =
                    user?.name ?: getString(R.string.app_brand_name)
                headerView.findViewById<android.widget.TextView>(R.id.nav_header_subtitle).text =
                    user?.role.orEmpty()
            }
        }

        navView.setNavigationItemSelectedListener { item ->
            lifecycleScope.launch {
                val userId = sessionManager.getUserId()
                val user = userId?.let { userRepository.getById(it) }
                if (userId == null || user == null) {
                    sessionManager.clearSession()
                    navigateToLoginIfNeeded()
                    drawerLayout.closeDrawers()
                    return@launch
                }

                val targetDestination = when (item.itemId) {
                    R.id.nav_home -> {
                        if (user.role == "PROVIDER") R.id.providerDashboardFragment
                        else R.id.listingListFragment
                    }

                    R.id.nav_my_reservations -> R.id.myReservationsFragment
                    R.id.nav_preferences -> R.id.preferencesFragment
                    R.id.nav_chat -> R.id.chatListFragment
                    R.id.nav_help -> R.id.helpFragment
                    R.id.nav_settings -> R.id.settingsFragment
                    else -> return@launch
                }

                if (navController.currentDestination?.id != targetDestination) {
                    navController.navigate(targetDestination)
                }
                drawerLayout.closeDrawers()
            }
            true
        }

        navController.addOnDestinationChangedListener { _, destination, _ ->
            val isAuth = isAuthDestination(destination.id)
            if (isAuth) {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED)
            } else {
                drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED)
                applyRoleMenuState()
            }
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
