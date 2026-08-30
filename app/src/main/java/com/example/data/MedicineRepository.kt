package com.example.data

import kotlinx.coroutines.flow.Flow

class MedicineRepository(private val medicineDao: MedicineDao) {
    val allMedicines: Flow<List<MedicineEntity>> = medicineDao.getAllMedicines()
    val activeMedicines: Flow<List<MedicineEntity>> = medicineDao.getActiveMedicines()

    suspend fun insertOrUpdate(medicine: MedicineEntity) = medicineDao.insertOrUpdate(medicine)

    suspend fun insertAll(medicines: List<MedicineEntity>) = medicineDao.insertAll(medicines)

    suspend fun deleteBySlot(slot: Int) = medicineDao.deleteBySlot(slot)

    suspend fun setSlotCount(count: Int) {
        val clamped = count.coerceIn(1, 5)
        medicineDao.deleteSlotsGreaterThan(clamped)
    }

    suspend fun clearAll() = medicineDao.clearAll()
}
