package com.example.util

import com.example.dto.DiagnosticLog
import kotlinx.coroutines.flow.Flow

class DiagnosticLogRepository(private val diagnosticLogDao: DiagnosticLogDao) {
    val allLogs: Flow<List<DiagnosticLog>> = diagnosticLogDao.getAllLogs()

    suspend fun insert(log: DiagnosticLog) {
        diagnosticLogDao.insertLog(log)
    }

    suspend fun deleteById(id: Int) {
        diagnosticLogDao.deleteLogById(id)
    }

    suspend fun clearAll() {
        diagnosticLogDao.clearAllLogs()
    }
}
