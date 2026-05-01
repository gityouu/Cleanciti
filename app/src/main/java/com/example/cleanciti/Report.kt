package com.example.cleanciti

import com.google.firebase.Timestamp
import com.google.firebase.firestore.Exclude

data class Report(
    // We use @Exclude because the ID is the Document name, not a field inside it
    @get:Exclude var id: String = "",

    val reportNumber: String = "",
    val category: String = "",
    val description: String = "",
    val status: String = "New",

    // Team Routing Fields
    val assignedTeamId: String = "",
    val assignedTeamMunicipality: String = "",

    // User & Location Info
    val reporterId: String = "",
    val locationName: String = "",
    val photoURL: String? = null,

    // Timing
    val createdAt: Timestamp? = null
)