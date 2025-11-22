package com.example.ecolapor.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reports")
data class ReportEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val userName: String,
    val category: String,
    val description: String,
    val imageUrl: String,
    val status: String,
    val timestamp: Long
)