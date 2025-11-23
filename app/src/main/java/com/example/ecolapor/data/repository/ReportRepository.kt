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