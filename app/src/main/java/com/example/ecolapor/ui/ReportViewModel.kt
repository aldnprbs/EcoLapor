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

        uiState = ReportState.Loading

        viewModelScope.launch {
            var imageUrl = ""

            // Upload image if provided
            if (imageUri != null) {
                val uploadResult = repository.uploadImage(imageUri)

                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull() ?: ""
                } else {
                    uiState = ReportState.Error("Gagal upload foto: ${uploadResult.exceptionOrNull()?.message}")
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

            // Save as draft or send to server
            val result = if (isDraft) {
                repository.saveDraftReport(newReport)
            } else {
                repository.addReport(newReport)
            }

            if (result.isSuccess) {
                uiState = ReportState.Success
                onSuccess() // Callback untuk refresh
            } else {
                uiState = ReportState.Error(
                    if (isDraft) "Gagal menyimpan draft"
                    else "Gagal mengirim laporan"
                )
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