package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MedicineDao {
    @Query("SELECT * FROM medicines ORDER BY slot ASC")
    fun getAllMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE isEnabled = 1 ORDER BY slot ASC")
    fun getActiveMedicines(): Flow<List<MedicineEntity>>

    @Query("SELECT * FROM medicines WHERE slot = :slot LIMIT 1")
    suspend fun getMedicineBySlot(slot: Int): MedicineEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(medicine: MedicineEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(medicines: List<MedicineEntity>)

    @Query("DELETE FROM medicines WHERE slot = :slot")
    suspend fun deleteBySlot(slot: Int)

    @Query("DELETE FROM medicines WHERE slot > :maxSlot")
    suspend fun deleteSlotsGreaterThan(maxSlot: Int)

    @Query("DELETE FROM medicines")
    suspend fun clearAll()
}
