package com.example.ecolapor.ui

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.data.repository.ReportRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.GeoPoint
import kotlinx.coroutines.launch

class ReportViewModel : ViewModel() {
    private val repository = ReportRepository()

    var uiState by mutableStateOf<ReportState>(ReportState.Idle)
        private set

    fun submitReport(
        description: String,
        category: String,
        imageUri: Uri?,
        location: GeoPoint?
    ) {
        // PERUBAHAN 1: Hapus validasi "imageUri == null"
        // Sekarang hanya Deskripsi dan Kategori yang wajib
        if (description.isBlank() || category.isBlank()) {
            uiState = ReportState.Error("Kategori dan Deskripsi wajib diisi!")
            return
        }

        uiState = ReportState.Loading

        viewModelScope.launch {
            var imageUrl = ""

            // PERUBAHAN 2: Cek dulu, apakah user melampirkan foto?
            if (imageUri != null) {
                // Kalau ada foto, upload dulu
                val uploadResult = repository.uploadImage(imageUri)

                if (uploadResult.isSuccess) {
                    imageUrl = uploadResult.getOrNull() ?: ""
                } else {
                    // Jika upload foto gagal, batalkan pengiriman laporan
                    uiState = ReportState.Error("Gagal upload foto: ${uploadResult.exceptionOrNull()?.message}")
                    return@launch
                }
            }
            // Jika imageUri == null, lewati proses upload, imageUrl tetap string kosong ""

            // 3. Buat data laporan
            val newReport = Report(
                category = category,
                description = description,
                imageUrl = imageUrl, // Bisa berisi URL foto, atau kosong jika tidak ada foto
                location = location ?: GeoPoint(0.0, 0.0),
                timestamp = Timestamp.now(),
                status = "Terkirim"
            )

            // 4. Kirim ke Firestore
            val reportResult = repository.addReport(newReport)
            if (reportResult.isSuccess) {
                uiState = ReportState.Success
            } else {
                uiState = ReportState.Error("Gagal mengirim laporan")
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