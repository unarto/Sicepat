package com.example.util

import com.example.dto.DiagnosticLog
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DiagnosticLogDao {
    @Query("SELECT * FROM diagnostic_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DiagnosticLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DiagnosticLog)

    @Query("DELETE FROM diagnostic_logs WHERE id = :id")
    suspend fun deleteLogById(id: Int)

    @Query("DELETE FROM diagnostic_logs")
    suspend fun clearAllLogs()
}
