package com.example.cleanciti

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

abstract class BaseActivity : AppCompatActivity() {
    protected val auth: FirebaseAuth by lazy { Firebase.auth }
    protected val db: FirebaseFirestore by lazy { Firebase.firestore }
    protected val currentUserId: String?
        get() = auth.currentUser?.uid

    protected val currentUserPhone: String?
        get() = auth.currentUser?.phoneNumber

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        FirebaseApp.initializeApp(this)
        val debugProvider = DebugAppCheckProviderFactory.getInstance()
        FirebaseAppCheck.getInstance().installAppCheckProviderFactory(debugProvider)
    }

    /**
     * Helper to check if a user is currently logged in.
     * Useful for splash screens or re-routing
     */
    protected fun isUserLoggedIn(): Boolean {
        return auth.currentUser != null
    }
}