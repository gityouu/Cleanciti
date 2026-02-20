package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cleanciti.databinding.ActivityLoginReporterBinding

class LoginActivityReporter : BaseActivity() {

    private lateinit var binding: ActivityLoginReporterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginReporterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Forgot Password
        binding.forgotPassword.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
            } else {
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent to your email", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // Login Button
        binding.reporterLoginButton.setOnClickListener {
            val email = binding.loginEmail.text.toString().trim()
            val password = binding.loginPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                auth.signInWithEmailAndPassword(email, password)
                    .addOnCompleteListener(this) { task ->
                        if (task.isSuccessful) {
                            checkUserRole(currentUserId)
                        } else {
                            Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        // Navigate to Worker Login
        binding.fieldWorker.setOnClickListener {
            startActivity(Intent(this, LoginActivityWorker::class.java))
            finish()
        }

        // Anonymous Login
        binding.reportAnonymously.setOnClickListener {
            loginUserAnonymously()
        }
    }

    private fun checkUserRole(uid: String?) {
        if (uid == null) return

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                if (document != null && document.exists()) {
                    val role = document.getString("role")
                    if (role == "reporter") {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    } else {
                        auth.signOut()
                        Toast.makeText(this, "Access denied", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error, Try again: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun loginUserAnonymously() {
        auth.signInAnonymously().addOnCompleteListener(this) { task ->
            if (task.isSuccessful) {
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
        }
    }
}