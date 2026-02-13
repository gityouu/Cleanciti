package com.example.cleanciti

import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.firestore

abstract class BaseActivity : AppCompatActivity() {
    protected val auth: FirebaseAuth by  lazy { Firebase.auth }
    protected val db: FirebaseFirestore by  lazy { Firebase.firestore }

    protected val currentUserId: String?
        get() = auth.currentUser?.uid
}