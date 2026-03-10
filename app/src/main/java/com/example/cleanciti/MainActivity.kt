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

        // 1. Check if we arrived here from the Worker Login
        val role = intent.getStringExtra("USER_ROLE")

        if (role == "worker") {
            // User is a Worker, load Worker View
            setupNavigation(R.navigation.nav_graph_worker, R.menu.bottom_nav_menu_worker)
        } else {
            // 2. Fallback: Check Firebase Auth for Reporters
            val user = auth.currentUser
            if (user != null) {
                setupNavigation(R.navigation.nav_graph, R.menu.bottom_nav_menu)
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, 0)
            insets
        }
    }

    private fun setupNavigation(graphId: Int, menuId: Int) {
        val navHostFragment = supportFragmentManager
            .findFragmentById(R.id.nav_host_fragment) as NavHostFragment
        val navController = navHostFragment.navController

        // Swap the graph and menu dynamically
        navController.setGraph(graphId)
        binding.bottomNavigation.menu.clear()
        binding.bottomNavigation.inflateMenu(menuId)
        binding.bottomNavigation.setupWithNavController(navController)
        binding.bottomNavigation.visibility = View.VISIBLE
    }
}