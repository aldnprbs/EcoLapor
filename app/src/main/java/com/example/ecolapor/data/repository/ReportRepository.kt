package com.example.ecolapor.data.repository

import android.content.Context
import android.net.Uri
import com.example.ecolapor.data.local.EcoDatabase
import com.example.ecolapor.data.local.ReportEntity
import com.example.ecolapor.data.model.Report
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class ReportRepository(context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val reportDao = context?.let { EcoDatabase.getDatabase(it).reportDao() }

    suspend fun uploadImage(imageUri: Uri): Result<String> {
        return try {
            val filename = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference.child("report_images/$filename")
            ref.putFile(imageUri).await()
            val downloadUrl = ref.downloadUrl.await()
            Result.success(downloadUrl.toString())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun addReport(report: Report): Result<Boolean> {
        return try {
            val user = auth.currentUser ?: return Result.failure(Exception("Belum login"))
            val reportWithUser = report.copy(
                userId = user.uid,
                userName = user.displayName ?: "Warga"
            )
            firestore.collection("reports").add(reportWithUser).await()
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getReportsRealtime(onDataChanged: (List<Report>) -> Unit, onError: (Exception) -> Unit) {

        if (reportDao != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val cachedData = reportDao.getAllReports().map { entity ->
                    Report(
                        id = entity.id,
                        userId = entity.userId,
                        userName = entity.userName,
                        category = entity.category,
                        description = entity.description,
                        imageUrl = entity.imageUrl,
                        status = entity.status,
                        timestamp = Timestamp(Date(entity.timestamp))
                    )
                }
                // Kirim data offline ke UI
                if (cachedData.isNotEmpty()) {
                    CoroutineScope(Dispatchers.Main).launch { onDataChanged(cachedData) }
                }
            }
        }

        firestore.collection("reports")
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    onError(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    val reports = snapshot.toObjects(Report::class.java)

                    onDataChanged(reports)

                    if (reportDao != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            val entities = reports.map { report ->
                                ReportEntity(
                                    id = report.id, // Pastikan ID tersimpan (Firestore mungkin butuh DocumentID)
                                    userId = report.userId,
                                    userName = report.userName,
                                    category = report.category,
                                    description = report.description,
                                    imageUrl = report.imageUrl,
                                    status = report.status,
                                    timestamp = report.timestamp?.toDate()?.time ?: System.currentTimeMillis()
                                )
                            }
                            reportDao.clearAll() // Hapus cache lama
                            reportDao.insertAll(entities) // Simpan cache baru
                        }
                    }
                }
            }
    }
}