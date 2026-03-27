package com.example.cleanciti

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.TextWatcher
import android.view.KeyEvent
import android.widget.EditText
import android.widget.Toast
import com.example.cleanciti.databinding.ActivityVerifyOtpBinding
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit
import androidx.core.graphics.toColorInt
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class VerifyOtpActivity : BaseActivity() {

    private lateinit var binding: ActivityVerifyOtpBinding
    private var verificationId: String? = null
    private var phoneNumber: String? = null
    private var isLogin: Boolean = false
    private var countDownTimer: CountDownTimer? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null // Passed from previous activity
    private val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
            val smsCode = credential.smsCode
            if (!smsCode.isNullOrEmpty()) {
                fillOtpBoxes(smsCode) // Helper to fill the UI
            }
            signInWithPhoneAuthCredential(credential)
        }

        override fun onVerificationFailed(e: com.google.firebase.FirebaseException) {
            Toast.makeText(this@VerifyOtpActivity, e.message, Toast.LENGTH_SHORT).show()
        }

        override fun onCodeSent(vId: String, token: PhoneAuthProvider.ForceResendingToken) {
            verificationId = vId
            resendToken = token
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityVerifyOtpBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // 1. Retrieve data from Intent
        verificationId = intent.getStringExtra("VERIFICATION_ID")
        phoneNumber = intent.getStringExtra("PHONE_NUMBER")
        isLogin = intent.getBooleanExtra("IS_LOGIN", false)

        binding.otpDescription.text =
            getString(R.string.enter_the_6_digit_code_sent_to, phoneNumber)

        setupOtpInputs()

        binding.verifyButton.setOnClickListener {
            val code = getOtpCode()
            if (code.length == 6) {
                verifyCode(code)
            } else {
                Toast.makeText(this, "Please enter the full 6-digit code", Toast.LENGTH_SHORT).show()
            }
        }

        resendToken = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra("RESEND_TOKEN", PhoneAuthProvider.ForceResendingToken::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra("RESEND_TOKEN")
        }
        startResendTimer()

        binding.resendCode.setOnClickListener {
            if (binding.resendCode.isEnabled) {
                resendVerificationCode()
            }
        }
    }

    private fun setupOtpInputs() {
        val inputs = arrayOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4, binding.otp5, binding.otp6)

        for (i in inputs.indices) {
            inputs[i].addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    // Auto-move to next box
                    if (s?.length == 1 && i < inputs.size - 1) {
                        inputs[i + 1].requestFocus()
                    }
                }
                override fun afterTextChanged(s: Editable?) {}
            })

            inputs[i].setOnFocusChangeListener { view, hasFocus ->
                val editText = view as EditText
                if (hasFocus) {
                    // Clear the hint when the box is clicked/focused
                    editText.hint = ""
                } else {
                    // Restore the "0" hint if the user leaves the box empty
                    if (editText.text.isEmpty()) {
                        editText.hint = getString(R.string._0)
                    }
                }
            }

            // Handle backspace to move to previous box
            inputs[i].setOnKeyListener { _, keyCode, event ->
                if (keyCode == KeyEvent.KEYCODE_DEL && event.action == KeyEvent.ACTION_DOWN) {
                    if (inputs[i].text.isEmpty() && i > 0) {
                        inputs[i - 1].requestFocus()
                    }
                }
                false
            }
        }
    }

    private fun getOtpCode(): String {
        return binding.otp1.text.toString() + binding.otp2.text.toString() +
                binding.otp3.text.toString() + binding.otp4.text.toString() +
                binding.otp5.text.toString() + binding.otp6.text.toString()
    }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhoneAuthCredential(credential)
    }

    private fun fillOtpBoxes(code: String) {
        val inputs = arrayOf(binding.otp1, binding.otp2, binding.otp3, binding.otp4, binding.otp5, binding.otp6)
        for (i in code.indices) {
            if (i < inputs.size) {
                inputs[i].setText(code[i].toString())
            }
        }
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential) {
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    if (isLogin) {
                        // For login, just go to main
                        navigateToMain()
                    } else {
                        // For signup, create the user record
                        saveUserToFirestore()
                    }
                } else {
                    Toast.makeText(this, "Verification Failed: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startResendTimer() {
        binding.resendCode.isEnabled = false
        countDownTimer = object : CountDownTimer(60000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val seconds = millisUntilFinished / 1000
                binding.resendCode.text = getString(R.string.resend_code_in_s, seconds)
                binding.resendCode.setTextColor("#94a3b8".toColorInt())
            }

            override fun onFinish() {
                binding.resendCode.isEnabled = true
                binding.resendCode.text = getString(R.string.didn_t_receive_a_code_resend)
                binding.resendCode.setTextColor("#13ec49".toColorInt())
            }
        }.start()
    }

    private fun resendVerificationCode() {
        val builder = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber!!)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(callbacks) // Use the same callbacks as signup/login

        resendToken?.let {
            builder.setForceResendingToken(it)
        }

        val options = builder.build()
        PhoneAuthProvider.verifyPhoneNumber(options)
        startResendTimer()
        Toast.makeText(this, "Resending code...", Toast.LENGTH_SHORT).show()
    }

    private fun saveUserToFirestore() {
        val userMap = hashMapOf(
            "uid" to currentUserId,
            "phoneNumber" to phoneNumber,
            "role" to "Reporter",
            "createdAt" to com.google.firebase.Timestamp.now()
        )

        db.collection("users").document(currentUserId!!)
            .set(userMap)
            .addOnSuccessListener { navigateToMain() }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Error saving profile: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToMain() {
        val intent = Intent(this, MainActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}