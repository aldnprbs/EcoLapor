package com.example.ecolapor.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [ReportEntity::class], version = 2, exportSchema = false)
abstract class EcoDatabase : RoomDatabase() {
    abstract fun reportDao(): ReportDao

    companion object {
        @Volatile
        private var INSTANCE: EcoDatabase? = null

        fun getDatabase(context: Context): EcoDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    EcoDatabase::class.java,
                    "eco_database"
                )
                    .fallbackToDestructiveMigration() // Hapus data lama jika schema berubah
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}