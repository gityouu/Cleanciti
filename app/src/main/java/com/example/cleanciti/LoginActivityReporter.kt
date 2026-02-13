package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivityReporter : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_reporter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailField = findViewById<EditText>(R.id.loginEmail)
        val passwordField = findViewById<EditText>(R.id.loginPassword)
        val loginButton = findViewById<Button>(R.id.reporterLoginButton)
        val anonymousText = findViewById<Button>(R.id.reportAnonymously)
        val forgotPassword = findViewById<Button>(R.id.forgotPassword)

        forgotPassword.setOnClickListener {
            val email = emailField.text.toString().trim()
            if (email.isEmpty()) {
                Toast.makeText(this, "Please enter your email first", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            auth.sendPasswordResetEmail(email)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Toast.makeText(this, "Reset link sent to $email", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Error: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
        }

        loginButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                performLogin(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

        anonymousText.setOnClickListener {
            loginUserAnonymously()
        }
    }

    private fun performLogin(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = currentUserId
                    checkUserRole(userId)
                } else {
                    Toast.makeText(this, "Login failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
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