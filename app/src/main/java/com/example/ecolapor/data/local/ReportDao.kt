package com.example.ecolapor.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReportDao {

    @Query("SELECT * FROM reports ORDER BY timestamp DESC")
    suspend fun getAllReports(): List<ReportEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reports: List<ReportEntity>)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(report: ReportEntity)

    @Query("DELETE FROM reports")
    suspend fun clearAll()
    
    @Query("DELETE FROM reports WHERE status != 'Tersimpan'")
    suspend fun clearSentReports()
    
    @Query("DELETE FROM reports WHERE id = :reportId")
    suspend fun deleteById(reportId: String)
    
    @Query("SELECT * FROM reports WHERE status = 'Tersimpan' ORDER BY timestamp DESC")
    suspend fun getDraftReports(): List<ReportEntity>
    
    @Query("SELECT * FROM reports WHERE status != 'Tersimpan' ORDER BY timestamp DESC")
    suspend fun getSentReports(): List<ReportEntity>
}