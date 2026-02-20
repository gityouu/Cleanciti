package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cleanciti.databinding.ActivitySignupReporterBinding

class SignupActivityReporter : BaseActivity() {

    private lateinit var binding: ActivitySignupReporterBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySignupReporterBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Link to Worker Login
        binding.fieldWorker.setOnClickListener {
            val intent = Intent(this, LoginActivityWorker::class.java)
            startActivity(intent)
        }

        // Link to Reporter Login
        binding.haveAnAccount.setOnClickListener {
            val intent = Intent(this, LoginActivityReporter::class.java)
            startActivity(intent)
            finish()
        }

        // Signup Button
        binding.signupButton.setOnClickListener {
            val email = binding.signUpEmail.text.toString().trim()
            val password = binding.signUpPassword.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // Disable button to prevent double submission
                binding.signupButton.isEnabled = false
                registerUser(email, password)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun registerUser(email: String, password: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // currentUserId is inherited from BaseActivity
                    saveUserToFirestore(currentUserId, email)
                } else {
                    binding.signupButton.isEnabled = true
                    Toast.makeText(this, "Registration failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun saveUserToFirestore(uid: String?, email: String) {
        if (uid == null) return

        val user = hashMapOf(
            "email" to email,
            "role" to "reporter",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(uid)
            .set(user)
            .addOnSuccessListener {
                Toast.makeText(this, "Registration successful", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }
            .addOnFailureListener { e ->
                binding.signupButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}