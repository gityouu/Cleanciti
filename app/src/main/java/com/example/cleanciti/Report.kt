package com.example.cleanciti

import com.google.firebase.Timestamp

data class Report (
    val reportNumber: String = "",
    val category: String = "",
    val status: String = "New",
    val photoURL: String? = null,
    val reporterId: String = "",
    val locationName: String = "",
    val description: String = "",
    val assignedTeamMunicipality: String = "",
    val createdAt: Timestamp? = null
)