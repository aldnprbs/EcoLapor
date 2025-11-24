package com.example.ecolapor.ui

import android.app.Application
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.data.repository.ReportRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class ReportViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = ReportRepository(application.applicationContext)

    var uiState by mutableStateOf<ReportState>(ReportState.Idle)
        private set

    fun submitReport(
        description: String,
        category: String,
        imageUri: Uri?,
        location: GeoPoint?,
        isDraft: Boolean = false,
        onSuccess: () -> Unit = {}
    ) {
        if (description.isBlank() || category.isBlank()) {
            uiState = ReportState.Error("Kategori dan Deskripsi wajib diisi!")
            return
        }

        android.util.Log.d("ReportViewModel", "Starting submitReport - isDraft: $isDraft")
        uiState = ReportState.Loading

        viewModelScope.launch {
            try {
                var imageUrl = ""

                // Upload image if provided
                if (imageUri != null) {
                    android.util.Log.d("ReportViewModel", "Uploading image...")
                    val uploadResult = repository.uploadImage(imageUri)

                    if (uploadResult.isSuccess) {
                        imageUrl = uploadResult.getOrNull() ?: ""
                        android.util.Log.d("ReportViewModel", "Image uploaded successfully: $imageUrl")
                    } else {
                        val errorMsg = uploadResult.exceptionOrNull()?.message ?: "Unknown error"
                        android.util.Log.e("ReportViewModel", "Failed to upload image: $errorMsg")
                        uiState = ReportState.Error("Gagal upload foto: $errorMsg")
                        return@launch
                    }
                }

                val newReport = Report(
                    category = category,
                    description = description,
                    imageUrl = imageUrl,
                    location = location ?: GeoPoint(0.0, 0.0),
                    timestamp = Timestamp.now(),
                    status = if (isDraft) "Tersimpan" else "Terkirim"
                )

                android.util.Log.d("ReportViewModel", "Saving report - isDraft: $isDraft")

                // Save as draft or send to server
                val result = if (isDraft) {
                    repository.saveDraftReport(newReport)
                } else {
                    repository.addReport(newReport)
                }

                android.util.Log.d("ReportViewModel", "Result: isSuccess=${result.isSuccess}")

                if (result.isSuccess) {
                    android.util.Log.d("ReportViewModel", "Report saved successfully!")
                    uiState = ReportState.Success
                    
                    // PENTING: Trigger callback SEBELUM navigate
                    // Ini akan memaksa HomeViewModel untuk refresh
                    onSuccess()
                    
                    android.util.Log.d("ReportViewModel", "onSuccess callback triggered")
                } else {
                    val errorMsg = result.exceptionOrNull()?.message ?: "Unknown error"
                    android.util.Log.e("ReportViewModel", "Failed to save report: $errorMsg")
                    uiState = ReportState.Error(
                        if (isDraft) "Gagal menyimpan draft: $errorMsg"
                        else "Gagal mengirim laporan: $errorMsg"
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("ReportViewModel", "Exception in submitReport: ${e.message}", e)
                uiState = ReportState.Error("Terjadi kesalahan: ${e.message}")
            }
        }
    }

    fun resetState() {
        uiState = ReportState.Idle
    }
}

sealed class ReportState {
    object Idle : ReportState()
    object Loading : ReportState()
    object Success : ReportState()
    data class Error(val message: String) : ReportState()
}