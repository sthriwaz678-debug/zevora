package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "medicines")
data class MedicineEntity(
    @PrimaryKey
    val slot: Int, // 1 to 5
    val name: String,
    val time: String, // "08:00", "13:00", "20:00" in 24h format
    val isEnabled: Boolean = true,
    val dosageNotes: String = "1 Tablet",
    val colorTag: Long = 0xFF0284C7,
    val lastTakenTimestamp: Long = 0L
)
