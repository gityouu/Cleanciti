package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class SignupActivityReporter : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_signup_reporter)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val emailField = findViewById<EditText>(R.id.signUpEmail)
        val passwordField = findViewById<EditText>(R.id.signUpPassword)
        val signupButton = findViewById<Button>(R.id.signupButton)
        val fieldWorkerLink = findViewById<TextView>(R.id.fieldWorker)
        val haveAnAccount = findViewById<TextView>(R.id.haveAnAccount)

        fieldWorkerLink.setOnClickListener {
            val intent = Intent(this, LoginActivityWorker::class.java)
            startActivity(intent)
        }

        haveAnAccount.setOnClickListener {
            val intent = Intent(this, LoginActivityReporter::class.java)
            startActivity(intent)
        }

        signupButton.setOnClickListener {
            val email = emailField.text.toString().trim()
            val password = passwordField.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                if (password.length >= 6) {
                    signupButton.isEnabled = false
                    registerUser(email, password)
                } else {
                    Toast.makeText(this, "Password must be at least 6 characters long", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }

    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = currentUserId
                    saveUserToFirestore(userId, email)
                } else {
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }

            }
    }

    private fun saveUserToFirestore(uid: String?, email: String) {
        if (uid == null) return

        val user = hashMapOf(
            "email" to email,
            "role" to "reporter",
            "createdAt" to System.currentTimeMillis()
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Registration failed: ${e.message}", Toast.LENGTH_SHORT).show()
            }

    }
}