package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class LoginActivityWorker : BaseActivity() {

    private val teamPasswords = mapOf(
        "Team A" to "1011",
        "Team B" to "1022",
        "Team C" to "1033",
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login_worker)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val idField = findViewById<EditText>(R.id.teamID)
        val passwordField = findViewById<EditText>(R.id.teamPassword)
        val loginButton = findViewById<Button>(R.id.workerLoginButton)

        loginButton.setOnClickListener {
            val inputId = idField.text.toString().trim().uppercase()
            val inputPassword = passwordField.text.toString().trim()

            if (inputId.isNotEmpty() && inputPassword.isNotEmpty()) {
                val formattedId = if (inputId.startsWith("CC-")) inputId.substring(3) else inputId
                loginButton.isEnabled = false
                loginButton.text = "Logging in..."
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
                    Toast.makeText(this, "Invalid Team ID", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val team = documents.documents[0]
                val teamName = team.getString("assignedTeam")
                val expectedPass = teamPasswords[teamName]

                if (inputPass == expectedPass) {
                    Toast.makeText(this, "Login successful", Toast.LENGTH_SHORT).show()
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, "Invalid password", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }
}