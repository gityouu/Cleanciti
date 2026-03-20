package com.example.cleanciti

import com.google.firebase.Timestamp

data class Report (
    val reportNumber: String = "",
    val category: String = "",
    val status: String = "New",
    val photoURL: String? = null,
    val createdAt: Timestamp? = null
)