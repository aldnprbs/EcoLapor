package com.example.ecolapor.data.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
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
import kotlinx.coroutines.withTimeout
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Date
import java.util.UUID

class ReportRepository(private val context: Context? = null) {
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val reportDao = context?.let { EcoDatabase.getDatabase(it).reportDao() }

    /**
     * Copy URI to a temporary file in app cache, then upload
     * This avoids all URI permission issues
     */
    private suspend fun copyUriToFile(uri: Uri): Result<File> {
        return try {
            if (context == null) {
                return Result.failure(Exception("Context is null"))
            }

            // Open input stream from URI
            val inputStream = context.contentResolver.openInputStream(uri)
                ?: return Result.failure(Exception("Cannot open image"))

            // Create temporary file in cache
            val tempFile = File.createTempFile(
                "upload_${System.currentTimeMillis()}",
                ".jpg",
                context.cacheDir
            )

            // Copy data to temporary file
            FileOutputStream(tempFile).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            inputStream.close()

            if (!tempFile.exists() || tempFile.length() == 0L) {
                return Result.failure(Exception("Failed to copy image data"))
            }

            Result.success(tempFile)
        } catch (e: Exception) {
            Result.failure(Exception("Error copying image: ${e.message}"))
        }
    }

    /**
     * Compress and fix image orientation
     */
    private fun compressImage(file: File): ByteArray? {
        return try {
            // Decode image
            val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null

            // Fix orientation based on EXIF data
            val exif = ExifInterface(file.absolutePath)
            val orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateBitmap(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateBitmap(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateBitmap(bitmap, 270f)
                else -> bitmap
            }

            // Compress to reduce size
            val outputStream = ByteArrayOutputStream()
            rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)

            if (rotatedBitmap != bitmap) {
                rotatedBitmap.recycle()
            }
            bitmap.recycle()

            outputStream.toByteArray()
        } catch (e: Exception) {
            null
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Float): Bitmap {
        val matrix = Matrix()
        matrix.postRotate(degrees)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    suspend fun uploadImage(imageUri: Uri): Result<String> {
        var tempFile: File? = null
        return try {
            if (context == null) {
                return Result.failure(Exception("Context is null"))
            }

            // Step 1: Copy URI to temporary file
            val fileResult = copyUriToFile(imageUri)
            if (fileResult.isFailure) {
                val errorMsg = fileResult.exceptionOrNull()?.message ?: "Unknown error"
                return Result.failure(Exception("Copy failed: $errorMsg"))
            }

            tempFile = fileResult.getOrNull()
            if (tempFile == null || !tempFile.exists()) {
                return Result.failure(Exception("Temp file not created"))
            }

            // Step 2: Compress image
            val compressedData = compressImage(tempFile)
            if (compressedData == null || compressedData.isEmpty()) {
                return Result.failure(Exception("Image compression failed - file may be corrupted"))
            }

            // Step 3: Try Firebase Storage first, fallback to Base64 if Storage not enabled
            try {
                val filename = "${UUID.randomUUID()}.jpg"
                val ref = storage.reference.child("report_images/$filename")

                android.util.Log.d("ReportRepository", "Attempting Firebase Storage upload...")
                val uploadTask = ref.putBytes(compressedData).await()
                val downloadUrl = ref.downloadUrl.await()

                android.util.Log.d("ReportRepository", "Firebase Storage upload successful!")
                return Result.success(downloadUrl.toString())

            } catch (storageError: Exception) {
                android.util.Log.w("ReportRepository", "Firebase Storage failed: ${storageError.message}")
                android.util.Log.d("ReportRepository", "Falling back to Base64 encoding...")

                // Fallback: Convert to Base64 string (works without Firebase Storage)
                val base64String = android.util.Base64.encodeToString(
                    compressedData,
                    android.util.Base64.DEFAULT
                )

                // Return as data URL (can be used directly in Image components)
                val dataUrl = "data:image/jpeg;base64,$base64String"

                android.util.Log.d("ReportRepository", "Base64 encoding successful (${base64String.length} chars)")
                return Result.success(dataUrl)
            }

        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Upload failed: ${e.message}", e)
            Result.failure(Exception("Upload failed: ${e.message}"))
        } finally {
            // Clean up temporary file
            try {
                tempFile?.delete()
            } catch (e: Exception) {
                android.util.Log.w("ReportRepository", "Failed to delete temp file: ${e.message}")
            }
        }
    }

    suspend fun addReport(report: Report): Result<Boolean> {
        return try {
            android.util.Log.d("ReportRepository", "addReport called")
            
            val user = auth.currentUser
            if (user == null) {
                android.util.Log.e("ReportRepository", "User not logged in")
                return Result.failure(Exception("Belum login"))
            }
            
            android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            android.util.Log.d("ReportRepository", "📤 SENDING REPORT")
            android.util.Log.d("ReportRepository", "User ID: ${user.uid}")
            android.util.Log.d("ReportRepository", "User Email: ${user.email}")
            android.util.Log.d("ReportRepository", "User Name: ${user.displayName}")
            android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
            
            val reportWithUser = report.copy(
                userId = user.uid,
                userName = user.displayName ?: "Warga"
            )
            
            android.util.Log.d("ReportRepository", "Adding report to Firestore...")
            android.util.Log.d("ReportRepository", "Report data: userId=${reportWithUser.userId}, category=${reportWithUser.category}, status=${reportWithUser.status}")
            
            // Add timeout to prevent infinite loading
            val docRef = withTimeout(30000L) { // 30 seconds timeout
                firestore.collection("reports").add(reportWithUser).await()
            }
            
            val reportId = docRef.id
            android.util.Log.d("ReportRepository", "✅ Report added to Firestore with ID: $reportId")
            
            // PERBAIKAN: Langsung simpan ke local cache untuk instant UI update
            if (reportDao != null) {
                val entity = ReportEntity(
                    id = reportId,
                    userId = reportWithUser.userId,
                    userName = reportWithUser.userName,
                    category = reportWithUser.category,
                    description = reportWithUser.description,
                    imageUrl = reportWithUser.imageUrl,
                    status = reportWithUser.status,
                    timestamp = reportWithUser.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                    latitude = reportWithUser.location?.latitude ?: 0.0,
                    longitude = reportWithUser.location?.longitude ?: 0.0
                )
                reportDao.insert(entity)
                android.util.Log.d("ReportRepository", "✅ Report cached locally for instant UI update")
            }
            
            Result.success(true)
        } catch (e: kotlinx.coroutines.TimeoutCancellationException) {
            android.util.Log.e("ReportRepository", "❌ Timeout: Request took too long (>30s)")
            Result.failure(Exception("Koneksi timeout. Periksa internet Anda dan coba lagi."))
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "❌ Failed to add report: ${e.message}", e)
            android.util.Log.e("ReportRepository", "Exception type: ${e.javaClass.simpleName}")
            Result.failure(e)
        }
    }

    suspend fun saveDraftReport(report: Report): Result<Boolean> {
        return try {
            if (context == null) {
                return Result.failure(Exception("Context is null"))
            }

            val user = auth.currentUser ?: return Result.failure(Exception("Belum login"))
            val draftId = UUID.randomUUID().toString()

            val draftEntity = ReportEntity(
                id = draftId,
                userId = user.uid,
                userName = user.displayName ?: "Warga",
                category = report.category,
                description = report.description,
                imageUrl = report.imageUrl,
                status = "Tersimpan",
                timestamp = System.currentTimeMillis(),
                latitude = report.location?.latitude ?: 0.0,
                longitude = report.location?.longitude ?: 0.0
            )

            reportDao?.insert(draftEntity)
            android.util.Log.d("ReportRepository", "Draft saved to local DB: $draftId")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to save draft: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun sendDraftReport(reportEntity: ReportEntity): Result<Boolean> {
        return try {
            // Upload to Firestore
            val report = Report(
                id = "",
                userId = reportEntity.userId,
                userName = reportEntity.userName,
                category = reportEntity.category,
                description = reportEntity.description,
                imageUrl = reportEntity.imageUrl,
                status = "Terkirim",
                timestamp = com.google.firebase.Timestamp(Date(reportEntity.timestamp)),
                location = com.google.firebase.firestore.GeoPoint(reportEntity.latitude, reportEntity.longitude)
            )

            firestore.collection("reports").add(report).await()
            android.util.Log.d("ReportRepository", "Draft sent to Firestore successfully")

            // Delete from local after successful upload
            reportDao?.deleteById(reportEntity.id)
            android.util.Log.d("ReportRepository", "Draft deleted from local DB after sending")

            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to send draft: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteDraftReport(reportId: String): Result<Boolean> {
        return try {
            reportDao?.deleteById(reportId)
            android.util.Log.d("ReportRepository", "Successfully deleted draft from local DB: $reportId")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to delete draft: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun deleteSentReport(reportId: String): Result<Boolean> {
        return try {
            // PERBAIKAN 1: Hapus dari Firestore
            firestore.collection("reports")
                .document(reportId)
                .delete()
                .await()

            android.util.Log.d("ReportRepository", "Successfully deleted report from Firestore: $reportId")

            // PERBAIKAN 2: Hapus juga dari local cache
            reportDao?.deleteById(reportId)
            android.util.Log.d("ReportRepository", "Successfully deleted report from local cache: $reportId")

            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to delete sent report: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun clearAllLocalData(): Result<Boolean> {
        return try {
            reportDao?.clearAll()
            android.util.Log.d("ReportRepository", "Successfully cleared all local data")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "Failed to clear local data: ${e.message}", e)
            Result.failure(e)
        }
    }

    // PERBAIKAN: Clear only sent reports cache (keep drafts) untuk sync antar device
    suspend fun clearSentReportsCache(): Result<Boolean> {
        return try {
            reportDao?.clearSentReports()
            android.util.Log.d("ReportRepository", "✅ Successfully cleared sent reports cache (drafts preserved)")
            Result.success(true)
        } catch (e: Exception) {
            android.util.Log.e("ReportRepository", "❌ Failed to clear sent reports cache: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getAllReportsIncludingDrafts(): List<Report> {
        val reports = mutableListOf<Report>()
        val currentUser = auth.currentUser

        if (currentUser == null) {
            android.util.Log.w("ReportRepository", "User not logged in")
            return reports
        }

        val currentUserId = currentUser.uid

        // Get reports from local database for current user only
        if (reportDao != null) {
            val allLocalReports = reportDao.getAllReports()
                .filter { it.userId == currentUserId } // Filter by current user
                .map { entity ->
                    Report(
                        id = entity.id,
                        userId = entity.userId,
                        userName = entity.userName,
                        category = entity.category,
                        description = entity.description,
                        imageUrl = entity.imageUrl,
                        status = entity.status,
                        timestamp = Timestamp(Date(entity.timestamp)),
                        location = com.google.firebase.firestore.GeoPoint(entity.latitude, entity.longitude)
                    )
                }
            reports.addAll(allLocalReports)
            android.util.Log.d("ReportRepository", "Loaded ${allLocalReports.size} reports for user $currentUserId from local DB")
        }

        return reports
    }

    fun getReportsRealtime(onDataChanged: (List<Report>) -> Unit, onError: (Exception) -> Unit) {
        val currentUser = auth.currentUser
        if (currentUser == null) {
            android.util.Log.w("ReportRepository", "User not logged in, cannot fetch reports")
            onError(Exception("User not logged in"))
            return
        }

        val currentUserId = currentUser.uid
        android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
        android.util.Log.d("ReportRepository", "📥 FETCHING REPORTS")
        android.util.Log.d("ReportRepository", "User ID: $currentUserId")
        android.util.Log.d("ReportRepository", "User Email: ${currentUser.email}")
        android.util.Log.d("ReportRepository", "User Name: ${currentUser.displayName}")
        android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")

        if (reportDao != null) {
            CoroutineScope(Dispatchers.IO).launch {
                val cachedData = reportDao.getAllReports()
                    .filter { it.userId == currentUserId } // Filter by current user
                    .map { entity ->
                        Report(
                            id = entity.id,
                            userId = entity.userId,
                            userName = entity.userName,
                            category = entity.category,
                            description = entity.description,
                            imageUrl = entity.imageUrl,
                            status = entity.status,
                            timestamp = Timestamp(Date(entity.timestamp)),
                            location = com.google.firebase.firestore.GeoPoint(entity.latitude, entity.longitude)
                        )
                    }
                // PERBAIKAN: SELALU kirim data cache (bahkan kalau kosong) untuk init UI
                CoroutineScope(Dispatchers.Main).launch {
                    onDataChanged(cachedData)
                    if (cachedData.isNotEmpty()) {
                        android.util.Log.d("ReportRepository", "📦 Sent cached data to UI: ${cachedData.size} reports")
                    } else {
                        android.util.Log.d("ReportRepository", "📦 No cached data, sent empty list. Waiting for Firestore...")
                    }
                }
            }
        } else {
            // PERBAIKAN: Jika tidak ada DAO, kirim empty list dulu
            android.util.Log.d("ReportRepository", "⚠️ No local database, waiting for Firestore...")
            onDataChanged(emptyList())
        }

        android.util.Log.d("ReportRepository", "🔍 Firestore Query: collection('reports').whereEqualTo('userId', '$currentUserId')")
        android.util.Log.d("ReportRepository", "🎧 Attaching Firestore snapshot listener...")
        
        firestore.collection("reports")
            .whereEqualTo("userId", currentUserId) // Filter by current user ID
            // PERBAIKAN: Hapus orderBy untuk menghindari composite index requirement
            // Sort akan dilakukan di client side
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("ReportRepository", "❌ Firestore listener error: ${e.message}", e)
                    onError(e)
                    return@addSnapshotListener
                }
                if (snapshot != null) {
                    android.util.Log.d("ReportRepository", "🔔 Firestore snapshot received! (listener triggered)")
                    android.util.Log.d("ReportRepository", "📊 Snapshot metadata: hasPendingWrites=${snapshot.metadata.hasPendingWrites()}, isFromCache=${snapshot.metadata.isFromCache}")
                    android.util.Log.d("ReportRepository", "📊 Total documents in snapshot: ${snapshot.documents.size}")
                    
                    val reports = snapshot.documents.mapNotNull { doc ->
                        try {
                            val report = doc.toObject(Report::class.java)
                            if (report == null) {
                                android.util.Log.w("ReportRepository", "⚠️ Failed to parse document: ${doc.id}")
                            }
                            report?.copy(id = doc.id)
                        } catch (e: Exception) {
                            android.util.Log.e("ReportRepository", "❌ Error parsing document ${doc.id}: ${e.message}", e)
                            null
                        }
                    }
                    // PERBAIKAN: Sort di client side (descending by timestamp)
                    .sortedByDescending { it.timestamp?.toDate()?.time ?: 0 }

                    android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    android.util.Log.d("ReportRepository", "✅ Firestore data received: ${reports.size} reports (after parsing)")
                    reports.forEachIndexed { index, report ->
                        android.util.Log.d("ReportRepository", "Report #${index + 1}: id=${report.id}, userId=${report.userId}, category=${report.category}")
                    }
                    android.util.Log.d("ReportRepository", "Current User ID: $currentUserId")
                    android.util.Log.d("ReportRepository", "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                    onDataChanged(reports)

                    if (reportDao != null) {
                        CoroutineScope(Dispatchers.IO).launch {
                            try {
                                // PERBAIKAN: Simpan draft dulu sebelum clear
                                val existingDrafts = reportDao.getDraftReports()
                                android.util.Log.d("ReportRepository", "💾 Preserving ${existingDrafts.size} drafts before cache update")
                                
                                val entities = reports.map { report ->
                                    ReportEntity(
                                        id = report.id,
                                        userId = report.userId,
                                        userName = report.userName,
                                        category = report.category,
                                        description = report.description,
                                        imageUrl = report.imageUrl,
                                        status = report.status,
                                        timestamp = report.timestamp?.toDate()?.time ?: System.currentTimeMillis(),
                                        latitude = report.location?.latitude ?: 0.0,
                                        longitude = report.location?.longitude ?: 0.0
                                    )
                                }
                                
                                // Clear ONLY sent reports (drafts tetap aman)
                                reportDao.clearSentReports()
                                android.util.Log.d("ReportRepository", "🗑️ Cleared sent reports cache")
                                
                                // Insert sent reports dari Firestore
                                reportDao.insertAll(entities)
                                android.util.Log.d("ReportRepository", "✅ Inserted ${entities.size} sent reports from Firestore")
                                
                                // Re-insert drafts (untuk memastikan tidak terhapus)
                                if (existingDrafts.isNotEmpty()) {
                                    reportDao.insertAll(existingDrafts)
                                    android.util.Log.d("ReportRepository", "✅ Re-inserted ${existingDrafts.size} drafts")
                                }
                                
                                android.util.Log.d("ReportRepository", "🔄 Cache sync completed for user $currentUserId")
                            } catch (e: Exception) {
                                android.util.Log.e("ReportRepository", "❌ Error updating cache: ${e.message}", e)
                            }
                        }
                    }
                }
            }
    }
}