package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.cleanciti.databinding.ActivityAuthBinding
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class AuthActivity : BaseActivity() {

    private lateinit var binding: ActivityAuthBinding
    private var isLogin: Boolean = false // Track if returning user

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityAuthBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Session check: skip if already logged in
        if (isUserLoggedIn()) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        setupClickListeners()
    }

    private fun setupClickListeners() {
        binding.fieldWorker.setOnClickListener {
            startActivity(Intent(this, LoginActivityWorker::class.java))
        }

        // Integrated Anonymous Login from old LoginActivity
        binding.reportAnonymously.setOnClickListener {
            loginUserAnonymously()
        }

        binding.signupButton.setOnClickListener {
            var phone = binding.signUpPhone.text.toString().trim()
            if (phone.startsWith("0")) phone = phone.substring(1)

            if (phone.length in 9..10) {
                val fullNumber = "+233$phone"
                checkUserAndProceed(fullNumber)
            } else {
                Toast.makeText(this, "Enter a valid phone number", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkUserAndProceed(phone: String) {
        binding.signupButton.isEnabled = false
        binding.signupButton.text = getString(R.string.verifying)

        db.collection("users")
            .whereEqualTo("phoneNumber", phone)
            .get()
            .addOnSuccessListener { documents ->
                isLogin = !documents.isEmpty // True if user exists
                sendVerificationCode(phone)
            }
            .addOnFailureListener { e ->
                binding.signupButton.isEnabled = true
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun sendVerificationCode(phoneNumber: String) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks)
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            binding.signupButton.isEnabled = true
        }

        override fun onVerificationFailed(e: FirebaseException) {
            binding.signupButton.isEnabled = true
            binding.signupButton.text = getString(R.string.get_verification_code)
            Toast.makeText(this@AuthActivity, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
        }

        override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
            val intent = Intent(this@AuthActivity, VerifyOtpActivity::class.java)
            intent.putExtra("VERIFICATION_ID", verificationId)

            val phoneInput = binding.signUpPhone.text.toString().trim().let {
                if (it.startsWith("0")) it.substring(1) else it
            }

            intent.putExtra("PHONE_NUMBER", "+233${phoneInput}")

            intent.putExtra("RESEND_TOKEN", token)
            intent.putExtra("IS_LOGIN", isLogin) // Pass the flag
            startActivity(intent)

            binding.signupButton.isEnabled = true
            binding.signupButton.text = getString(R.string.get_verification_code)
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