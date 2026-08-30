package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(entities = [MedicineEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun medicineDao(): MedicineDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zevora_database.db"
                )
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            super.onCreate(db)
                            // Prepopulate default 3 scheduled medicines
                            INSTANCE?.let { database ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    database.medicineDao().insertAll(
                                        listOf(
                                            MedicineEntity(
                                                slot = 1,
                                                name = "Paracetamol",
                                                time = "08:00",
                                                isEnabled = true,
                                                dosageNotes = "500mg - 1 Tablet",
                                                colorTag = 0xFF0284C7
                                            ),
                                            MedicineEntity(
                                                slot = 2,
                                                name = "Vitamin D",
                                                time = "13:00",
                                                isEnabled = true,
                                                dosageNotes = "1000 IU - 1 Capsule",
                                                colorTag = 0xFF10B981
                                            ),
                                            MedicineEntity(
                                                slot = 3,
                                                name = "Amoxicillin",
                                                time = "20:00",
                                                isEnabled = true,
                                                dosageNotes = "250mg - 1 Capsule",
                                                colorTag = 0xFF8B5CF6
                                            )
                                        )
                                    )
                                }
                            }
                        }
                    })
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
