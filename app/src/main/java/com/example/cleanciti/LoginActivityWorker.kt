package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cleanciti.databinding.ActivityLoginWorkerBinding

class LoginActivityWorker : BaseActivity() {

    private lateinit var binding: ActivityLoginWorkerBinding

    private val teamPasswords = mapOf(
        "Team A" to "1011",
        "Team B" to "1022",
        "Team C" to "1033",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginWorkerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {

        binding.workerLoginButton.setOnClickListener {
            val inputId = binding.teamID.text.toString().trim().uppercase()
            val inputPassword = binding.teamPassword.text.toString().trim()

            if (inputId.isNotEmpty() && inputPassword.isNotEmpty()) {
                val formattedId = if (inputId.startsWith("CC-")) inputId.substring(3) else inputId

                binding.workerLoginButton.isEnabled = false
                binding.workerLoginButton.text = "Logging in..."

                performWorkerLogin(formattedId, inputPassword)
            } else {
                Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun performWorkerLogin(id: String, inputPass: String) {
        db.collection("team").whereEqualTo("assignedTeamId", "CC-$id")
            .get()
            .addOnSuccessListener { documents ->
                if (documents.isEmpty) {
                    resetLoginButton()
                    Toast.makeText(this, "Invalid Team ID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val team = documents.documents[0]
                val teamName = team.getString("assignedTeam")
                val expectedPass = teamPasswords[teamName]

                if (inputPass == expectedPass) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                } else {
                    resetLoginButton()
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                resetLoginButton()
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun resetLoginButton() {
        binding.workerLoginButton.isEnabled = true
        binding.workerLoginButton.text = "Login"
    }
}