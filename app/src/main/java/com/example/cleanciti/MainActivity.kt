package com.example.cleanciti

import android.app.NotificationManager
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.ui.setupWithNavController
import com.example.cleanciti.databinding.ActivityMainBinding

class MainActivity : BaseActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        createNotificationChannel()

        // Check if we arrived here from the Worker Login
        val role = intent.getStringExtra("USER_ROLE")
        Log.d("MainActivity", "User role: $role")

        if (role == "worker") {
            // User is a Worker, load Worker View
            configureNavigation(R.navigation.nav_graph_worker, R.menu
                .bottom_nav_menu_worker)
        } else {
            // Fallback: Check Firebase Auth for Reporters
            val user = auth.currentUser
            if (user != null) {
                configureNavigation(R.navigation.nav_graph, R.menu.bottom_nav_menu)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun configureNavigation(graphId: Int, menuId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        val shouldSetGraph = try {
            navController.graph.id != graphId
        } catch (_: IllegalStateException) {
            true
        }

        if (shouldSetGraph) {
            if (graphId == R.navigation.nav_graph_worker) {
                val bundle = Bundle().apply {
                    // Retrieve from intent and put into navigation bundle
                    val tId = intent.getStringExtra("TEAM_ID")
                    val mId = intent.getStringExtra("MUNICIPALITY_ID")

                    putString("TEAM_ID", tId)
                    putString("MUNICIPALITY_ID", mId)
                }
                navController.setGraph(graphId, bundle)
            } else {
                navController.setGraph(graphId)
            }
        }

        binding.bottomNavigation.apply {
            menu.clear()
            inflateMenu(menuId)
            setupWithNavController(navController)
            visibility = View.VISIBLE
        }
    }

    private fun createNotificationChannel() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val name = "Waste Reports"
            val descriptionText = "Alerts for new waste reports in your area"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = android.app.NotificationChannel("CLEANCITI_NOTIFS", name, importance)
                .apply {
                description = descriptionText
            }
            val notificationManager = getSystemService(NOTIFICATION_SERVICE)
                    as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}