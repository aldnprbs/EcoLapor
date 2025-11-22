package com.example.ecolapor.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.data.repository.AuthRepository
import com.example.ecolapor.data.repository.ReportRepository

// Ubah dari ViewModel biasa menjadi AndroidViewModel agar bisa akses 'Application Context'
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    // Kirim context ke Repository agar Room bisa dibuat
    private val reportRepository = ReportRepository(application.applicationContext)
    private val authRepository = AuthRepository()

    private val _reports = mutableStateListOf<Report>()
    val reports: List<Report> get() = _reports

    init {
        fetchReports()
    }

    private fun fetchReports() {
        reportRepository.getReportsRealtime(
            onDataChanged = { newReports ->
                _reports.clear()
                _reports.addAll(newReports)
            },
            onError = { e ->
                e.printStackTrace()
            }
        )
    }

    fun logout() {
        authRepository.logout()
    }
}