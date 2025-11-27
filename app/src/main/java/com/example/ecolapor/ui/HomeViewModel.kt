package com.example.ecolapor.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.data.repository.AuthRepository
import com.example.ecolapor.data.repository.ReportRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val reportRepository = ReportRepository(application.applicationContext)
    private val authRepository = AuthRepository()

    // StateFlow untuk observasi yang lebih baik
    private val _reports = MutableStateFlow<List<Report>>(emptyList())
    val reports: StateFlow<List<Report>> get() = _reports

    init {
        android.util.Log.d("HomeViewModel", "🚀 HomeViewModel initialized")
        
        // PERBAIKAN: Setup draft change listener
        reportRepository.setDraftChangeListener {
            android.util.Log.d("HomeViewModel", "📝 Draft changed, refreshing UI...")
            viewModelScope.launch {
                refreshReportsData()
            }
        }
        
        // Fetch reports
        fetchReports()
    }

    // PERBAIKAN: Fungsi helper untuk refresh data (digunakan saat draft berubah)
    private suspend fun refreshReportsData() {
        try {
            android.util.Log.d("HomeViewModel", "🔄 Refreshing reports data...")
            
            // Get current Firestore reports (non-draft)
            val currentReports = _reports.value.filter { it.status != "Tersimpan" }
            android.util.Log.d("HomeViewModel", "📊 Current non-draft reports: ${currentReports.size}")
            
            // Get fresh drafts from local DB
            val localDrafts = reportRepository.getDraftsFromLocal()
            android.util.Log.d("HomeViewModel", "📦 Fresh local drafts: ${localDrafts.size}")
            
            // Combine and update
            val allReports = (currentReports + localDrafts).distinctBy { it.id }
            val sortedReports = allReports.sortedByDescending { it.timestamp?.toDate()?.time ?: 0 }
            
            _reports.value = sortedReports
            android.util.Log.d("HomeViewModel", "✅ UI refreshed with ${sortedReports.size} total reports")
        } catch (e: Exception) {
            android.util.Log.e("HomeViewModel", "❌ Error refreshing reports: ${e.message}", e)
            e.printStackTrace()
        }
    }

    private fun fetchReports() {
        android.util.Log.d("HomeViewModel", "Setting up report fetching with realtime listener")
        
        reportRepository.getReportsRealtime(
            onDataChanged = { firestoreReports ->
                android.util.Log.d("HomeViewModel", "🔔 Firestore data changed: ${firestoreReports.size} reports")
                // PERBAIKAN: Langsung set data dari Firestore, jangan load ulang dari getAllReportsIncludingDrafts
                // karena bisa overwrite dengan data cache yang kosong
                viewModelScope.launch {
                    try {
                        // Get drafts dari local DB
                        val localDrafts = reportRepository.getDraftsFromLocal()
                        android.util.Log.d("HomeViewModel", "📦 Local drafts: ${localDrafts.size}")
                        
                        // Combine Firestore reports + local drafts
                        val allReports = (firestoreReports + localDrafts).distinctBy { it.id }
                        
                        // Sort by timestamp
                        val sortedReports = allReports.sortedByDescending { it.timestamp?.toDate()?.time ?: 0 }
                        
                        // Update UI
                        _reports.value = sortedReports
                        android.util.Log.d("HomeViewModel", "✅ Updated UI with ${sortedReports.size} reports (${firestoreReports.size} from Firestore + ${localDrafts.size} drafts)")
                    } catch (e: Exception) {
                        android.util.Log.e("HomeViewModel", "❌ Error combining reports: ${e.message}", e)
                    }
                }
            },
            onError = { e ->
                android.util.Log.e("HomeViewModel", "❌ Firestore error: ${e.message}", e)
                e.printStackTrace()
            }
        )
    }

    private fun loadAllReports() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "📥 Loading all reports from repository...")
                val drafts = reportRepository.getAllReportsIncludingDrafts()
                android.util.Log.d("HomeViewModel", "📦 Got ${drafts.size} reports from repository")
                
                // Sort by timestamp descending
                val sortedReports = drafts.sortedByDescending { it.timestamp?.toDate()?.time ?: 0 }
                
                // Update StateFlow
                _reports.value = sortedReports
                
                android.util.Log.d("HomeViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                android.util.Log.d("HomeViewModel", "✅ Loaded ${sortedReports.size} reports")
                sortedReports.forEachIndexed { index, report ->
                    android.util.Log.d("HomeViewModel", "  Report #${index + 1}: ${report.category} - ${report.status}")
                }
                android.util.Log.d("HomeViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error loading reports: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    fun refreshReports() {
        android.util.Log.d("HomeViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("HomeViewModel", "🔄 MANUAL REFRESH TRIGGERED (for drafts)")
        android.util.Log.d("HomeViewModel", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        viewModelScope.launch {
            try {
                // PERBAIKAN: Langsung refresh data untuk draft
                refreshReportsData()
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Error during refresh: ${e.message}", e)
            }
        }
    }

    fun sendDraftReport(report: Report) {
        viewModelScope.launch {
            try {
                // Convert Report to ReportEntity
                val entity = com.example.ecolapor.data.local.ReportEntity(
                    id = report.id,
                    userId = report.userId,
                    userName = report.userName,
                    category = report.category,
                    description = report.description,
                    imageUrl = report.imageUrl,
                    status = "Tersimpan", // Tetap sebagai draft dulu
                    timestamp = report.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                    latitude = report.location?.latitude ?: 0.0,
                    longitude = report.location?.longitude ?: 0.0
                )

                android.util.Log.d("HomeViewModel", "Sending draft report: ${report.id}")
                val result = reportRepository.sendDraftReport(entity)

                if (result.isSuccess) {
                    android.util.Log.d("HomeViewModel", "Draft sent successfully")
                    refreshReports()
                } else {
                    android.util.Log.e("HomeViewModel", "Failed to send draft: ${result.exceptionOrNull()?.message}")
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error sending draft: ${e.message}", e)
                e.printStackTrace()
            }
        }
    }

    // FUNGSI BARU: Universal delete function - DIPERBAIKI
    fun deleteReport(reportId: String) {
        viewModelScope.launch {
            try {
                // Cari report untuk menentukan jenisnya
                val report = _reports.value.find { it.id == reportId }

                if (report == null) {
                    android.util.Log.e("HomeViewModel", "Report not found: $reportId")
                    return@launch
                }

                android.util.Log.d("HomeViewModel", "Deleting report: $reportId, status: ${report.status}")

                // PERBAIKAN 1: Update UI dulu (Optimistic Update)
                // Langsung hapus dari StateFlow untuk instant UI feedback
                _reports.value = _reports.value.filter { it.id != reportId }
                android.util.Log.d("HomeViewModel", "UI updated, report removed from list")

                // PERBAIKAN 2: Hapus dari database berdasarkan status
                val result = if (report.status == "Tersimpan") {
                    // Hapus draft dari local database
                    android.util.Log.d("HomeViewModel", "Deleting DRAFT from local DB")
                    reportRepository.deleteDraftReport(reportId)
                } else {
                    // Hapus sent report dari Firestore DAN local cache
                    android.util.Log.d("HomeViewModel", "Deleting SENT REPORT from Firestore + cache")
                    reportRepository.deleteSentReport(reportId)
                }

                if (result.isSuccess) {
                    android.util.Log.d("HomeViewModel", "✅ Delete successful from database")
                    // JANGAN refresh disini, biar realtime listener yang handle
                    // Tapi kalau mau lebih pasti, bisa uncomment line berikut:
                    // refreshReports()
                } else {
                    android.util.Log.e("HomeViewModel", "❌ Delete failed: ${result.exceptionOrNull()?.message}")
                    // Kalau gagal, rollback UI (restore data)
                    loadAllReports()
                }
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "❌ Delete error: ${e.message}", e)
                // Rollback UI kalau ada error
                loadAllReports()
            }
        }
    }

    // Tetap pertahankan fungsi lama untuk backward compatibility
    fun deleteDraftReport(reportId: String) {
        android.util.Log.d("HomeViewModel", "deleteDraftReport called, forwarding to deleteReport()")
        deleteReport(reportId)
    }

    fun deleteSentReport(reportId: String) {
        android.util.Log.d("HomeViewModel", "deleteSentReport called, forwarding to deleteReport()")
        deleteReport(reportId)
    }

    fun logout() {
        viewModelScope.launch {
            try {
                android.util.Log.d("HomeViewModel", "Logout triggered - clearing local data")
                
                // Bersihkan semua data lokal sebelum logout
                reportRepository.clearAllLocalData()
                
                // Clear StateFlow
                _reports.value = emptyList()
                
                // Logout dari Firebase
                authRepository.logout()
                
                android.util.Log.d("HomeViewModel", "Logout completed successfully")
            } catch (e: Exception) {
                android.util.Log.e("HomeViewModel", "Error during logout: ${e.message}", e)
                // Tetap logout meskipun ada error clearing data
                authRepository.logout()
            }
        }
    }
}