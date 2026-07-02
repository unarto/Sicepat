package com.example.dto

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "diagnostic_logs")
data class DiagnosticLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val command: String,
    val timestamp: Long = System.currentTimeMillis(),
    val output: String,
    val isSuccess: Boolean
)
