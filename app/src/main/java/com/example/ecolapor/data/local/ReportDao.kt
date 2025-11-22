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

    @Query("DELETE FROM reports")
    suspend fun clearAll()
}