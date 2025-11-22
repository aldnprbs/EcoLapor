package com.example.ecolapor.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint

data class Report(
    val id: String = "",
    val userId: String = "",
    val userName: String = "",
    val category: String = "",
    val description: String = "",
    val imageUrl: String = "",
    val status: String = "Terkirim",
    val timestamp: Timestamp? = null,
    val location: GeoPoint? = null
)