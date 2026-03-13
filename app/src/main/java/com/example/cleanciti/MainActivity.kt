package com.example.cleanciti

import android.os.Bundle
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

        // Check if we arrived here from the Worker Login
        val role = intent.getStringExtra("USER_ROLE")

        if (role == "worker") {
            // User is a Worker, load Worker View
            configureNavigation(R.navigation.nav_graph_worker, R.menu.bottom_nav_menu_worker)
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

    // MainActivity.kt
    private fun configureNavigation(graphId: Int, menuId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Capture the login ID passed from LoginActivityWorker
        val teamId = intent.getStringExtra("TEAM_ID")

        if (graphId == R.navigation.nav_graph_worker && teamId != null) {
            // Create a bundle to pass the TEAM_ID as an argument to the start destination
            val bundle = Bundle().apply {
                putString("TEAM_ID", teamId)
            }
            // Set the graph with the arguments included
            navController.setGraph(graphId, bundle)
        } else {
            navController.setGraph(graphId)
        }

        binding.bottomNavigation.apply {
            menu.clear()
            inflateMenu(menuId)
            setupWithNavController(navController)
            visibility = View.VISIBLE
        }
    }
}