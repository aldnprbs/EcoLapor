package com.example.ecolapor.ui

import android.app.Application
import androidx.compose.runtime.mutableStateListOf
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.data.repository.AuthRepository
import com.example.ecolapor.data.repository.ReportRepository
import kotlinx.coroutines.launch

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
        // Initial load of drafts
        loadAllReports()
        
        // Listen to Firestore for sent reports
        reportRepository.getReportsRealtime(
            onDataChanged = { newReports ->
                loadAllReports()
            },
            onError = { e ->
                e.printStackTrace()
            }
        )
    }
    
    private fun loadAllReports() {
        viewModelScope.launch {
            val drafts = reportRepository.getAllReportsIncludingDrafts()
            _reports.clear()
            _reports.addAll(drafts)
            
            // Sort by timestamp descending
            _reports.sortByDescending { it.timestamp?.toDate()?.time ?: 0 }
        }
    }
    
    fun refreshReports() {
        loadAllReports()
    }
    
    fun sendDraftReport(report: Report) {
        viewModelScope.launch {
            val entity = com.example.ecolapor.data.local.ReportEntity(
                id = report.id,
                userId = report.userId,
                userName = report.userName,
                category = report.category,
                description = report.description,
                imageUrl = report.imageUrl,
                status = report.status,
                timestamp = report.timestamp?.toDate()?.time ?: System.currentTimeMillis()
            )
            
            val result = reportRepository.sendDraftReport(entity)
            if (result.isSuccess) {
                refreshReports()
            }
        }
    }
    
    fun deleteDraftReport(reportId: String) {
        viewModelScope.launch {
            reportRepository.deleteDraftReport(reportId)
            refreshReports()
        }
    }
    
    fun deleteSentReport(reportId: String) {
        viewModelScope.launch {
            reportRepository.deleteSentReport(reportId)
            refreshReports()
        }
    }

    fun logout() {
        authRepository.logout()
    }
}