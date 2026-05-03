package com.example.housefinder

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.drawerlayout.widget.DrawerLayout
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.NavigationUI
import com.example.housefinder.ui.common.SessionManager
import com.example.housefinder.worker.MatchingListingWorker
import com.google.android.material.navigation.NavigationView
import java.util.concurrent.TimeUnit

import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var drawerLayout: DrawerLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        drawerLayout = findViewById(R.id.drawer_layout)
        val navView = findViewById<NavigationView>(R.id.nav_view)
        
        val navHostFragment =
            supportFragmentManager.findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        NavigationUI.setupWithNavController(navView, navController)

        fun isAuthDestination(destinationId: Int): Boolean {
            return destinationId == R.id.loginFragment || destinationId == R.id.registerFragment
        }

        fun applyRoleMenuState() {
            val role = SessionManager(this).getRole()
            val name = SessionManager(this).getDisplayName()
            val isProvider = role == "PROVIDER"
            
            navView.menu.findItem(R.id.nav_my_reservations).isVisible = !isProvider
            navView.menu.findItem(R.id.nav_preferences).isVisible = !isProvider

            val headerView = navView.getHeaderView(0)
            headerView.findViewById<android.widget.TextView>(R.id.nav_header_name).text = name ?: "House Finder"
            headerView.findViewById<android.widget.TextView>(R.id.nav_header_subtitle).text = role ?: ""
        }

        navView.setNavigationItemSelectedListener { item ->
            val targetDestination = when (item.itemId) {
                R.id.nav_home -> {
                    if (SessionManager(this).getRole() == "PROVIDER") R.id.providerDashboardFragment
                    else R.id.listingListFragment
                }

                R.id.nav_my_reservations -> R.id.myReservationsFragment
                R.id.nav_preferences -> R.id.preferencesFragment
                R.id.nav_chat -> R.id.chatListFragment
                R.id.nav_help -> R.id.helpFragment
                R.id.nav_settings -> R.id.settingsFragment
                else -> return@setNavigationItemSelectedListener false
            }

            if (navController.currentDestination?.id != targetDestination) {
                navController.navigate(targetDestination)
            }
            drawerLayout.closeDrawers()
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
    }

    private fun setupBackgroundWork() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
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
}
