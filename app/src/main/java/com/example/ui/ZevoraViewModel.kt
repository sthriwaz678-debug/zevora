package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.ble.BleConnectionState
import com.example.ble.BleDeviceItem
import com.example.ble.BleSyncResult
import com.example.ble.ZevoraBleManager
import com.example.data.AppDatabase
import com.example.data.MedicineEntity
import com.example.data.MedicineRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class NextDoseInfo(
    val medicine: MedicineEntity,
    val formattedTime: String, // e.g. "08:00 PM"
    val countdownFormatted: String, // e.g. "01:25:43"
    val remainingMillis: Long
)

class ZevoraViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    private val repository = MedicineRepository(database.medicineDao())
    val bleManager = ZevoraBleManager(application)

    val medicines: StateFlow<List<MedicineEntity>> = repository.allMedicines
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val connectionState = bleManager.connectionState
    val connectedDevice = bleManager.connectedDevice
    val discoveredDevices = bleManager.discoveredDevices
    val isScanning = bleManager.isScanning
    val batteryLevel = bleManager.batteryLevel
    val lastSyncTime = bleManager.lastSyncTime
    val isSyncing = bleManager.isSyncing
    val isBluetoothEnabled = bleManager.isBluetoothEnabled
    val syncResultFlow = bleManager.syncResult

    // UI Feedback events
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // Real-time ticking ticker state for live countdowns
    private val _currentTimeMillis = MutableStateFlow(System.currentTimeMillis())
    val currentTimeMillis: StateFlow<Long> = _currentTimeMillis.asStateFlow()

    // Next upcoming dose computation combining current time and medicines list
    val nextDoseInfo: StateFlow<NextDoseInfo?> = combine(medicines, _currentTimeMillis) { list, now ->
        computeNextDose(list.filter { it.isEnabled }, now)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    init {
        // Ticker loop for live countdown timer
        viewModelScope.launch {
            while (true) {
                _currentTimeMillis.value = System.currentTimeMillis()
                delay(1000)
            }
        }

        // Initialize default medicines if empty
        viewModelScope.launch {
            // Check if db is initialized, if not insert default schedule
            delay(300)
            val current = database.medicineDao().getActiveMedicines()
            // db callback handles prepopulation, ensure we refresh Bluetooth status
            bleManager.checkBluetoothStatus()
        }
    }

    fun startScan() {
        bleManager.startScan()
    }

    fun stopScan() {
        bleManager.stopScan()
    }

    fun connectDevice(device: BleDeviceItem) {
        bleManager.connect(device)
    }

    fun disconnectDevice() {
        bleManager.disconnect()
    }

    fun triggerConnectionLostTest() {
        bleManager.triggerConnectionLostForTesting()
    }

    fun refreshBluetoothStatus() {
        bleManager.checkBluetoothStatus()
    }

    fun saveMedicineSlot(
        slot: Int,
        name: String,
        time: String,
        isEnabled: Boolean = true,
        dosageNotes: String = "1 Tablet"
    ) {
        viewModelScope.launch {
            val entity = MedicineEntity(
                slot = slot,
                name = name.trim().ifEmpty { "Medicine $slot" },
                time = time,
                isEnabled = isEnabled,
                dosageNotes = dosageNotes,
                colorTag = when (slot) {
                    1 -> 0xFF0284C7
                    2 -> 0xFF10B981
                    3 -> 0xFF8B5CF6
                    4 -> 0xFFF59E0B
                    5 -> 0xFFEC4899
                    else -> 0xFF0284C7
                }
            )
            repository.insertOrUpdate(entity)
            _userMessage.emit("Slot $slot schedule saved.")
        }
    }

    fun deleteSlot(slot: Int) {
        viewModelScope.launch {
            repository.deleteBySlot(slot)
            _userMessage.emit("Slot $slot removed.")
        }
    }

    fun setConfiguredSlotCount(targetCount: Int) {
        viewModelScope.launch {
            val clamped = targetCount.coerceIn(1, 5)
            val current = medicines.value
            if (current.size < clamped) {
                // Add slots up to clamped
                val defaults = listOf(
                    Triple("Paracetamol", "08:00", "500mg - 1 Tablet"),
                    Triple("Vitamin D", "13:00", "1000 IU - 1 Capsule"),
                    Triple("Amoxicillin", "20:00", "250mg - 1 Capsule"),
                    Triple("Omega 3", "09:00", "1 Softgel"),
                    Triple("Melatonin", "22:00", "3mg - 1 Tablet")
                )
                for (s in (current.size + 1)..clamped) {
                    val defaultData = defaults.getOrElse(s - 1) { Triple("Medicine $s", "08:00", "1 Tablet") }
                    saveMedicineSlot(
                        slot = s,
                        name = defaultData.first,
                        time = defaultData.second,
                        dosageNotes = defaultData.third
                    )
                }
            } else if (current.size > clamped) {
                repository.setSlotCount(clamped)
            }
        }
    }

    fun syncScheduleToDispenser() {
        if (connectionState.value != BleConnectionState.CONNECTED) {
            viewModelScope.launch {
                _userMessage.emit("Please connect your ZEVORA ESP32 device first.")
            }
            return
        }

        val jsonPayload = generateScheduleJson()
        bleManager.syncSchedule(jsonPayload)
    }

    fun generateScheduleJson(): String {
        val list = medicines.value.filter { it.isEnabled }
        val root = JSONObject()
        root.put("medicineCount", list.size)

        val array = JSONArray()
        list.forEach { med ->
            val obj = JSONObject()
            obj.put("slot", med.slot)
            obj.put("name", med.name)
            obj.put("time", med.time)
            array.put(obj)
        }
        root.put("medicines", array)
        return root.toString(2)
    }

    private fun computeNextDose(list: List<MedicineEntity>, nowMillis: Long): NextDoseInfo? {
        if (list.isEmpty()) return null

        val nowCalendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val nowHour = nowCalendar.get(Calendar.HOUR_OF_DAY)
        val nowMinute = nowCalendar.get(Calendar.MINUTE)
        val nowSecond = nowCalendar.get(Calendar.SECOND)
        val nowTotalSeconds = (nowHour * 3600) + (nowMinute * 60) + nowSecond

        var bestDiffSeconds = Long.MAX_VALUE
        var bestMedicine: MedicineEntity? = null
        var bestTargetCalendar: Calendar? = null

        for (med in list) {
            val parts = med.time.split(":")
            val hour = parts.getOrNull(0)?.toIntOrNull() ?: 8
            val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
            val medSeconds = (hour * 3600) + (minute * 60)

            val diffSeconds = if (medSeconds > nowTotalSeconds) {
                (medSeconds - nowTotalSeconds).toLong()
            } else {
                // Next day
                ((24 * 3600) - nowTotalSeconds + medSeconds).toLong()
            }

            if (diffSeconds < bestDiffSeconds) {
                bestDiffSeconds = diffSeconds
                bestMedicine = med
                val cal = Calendar.getInstance().apply {
                    timeInMillis = nowMillis
                    set(Calendar.HOUR_OF_DAY, hour)
                    set(Calendar.MINUTE, minute)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                    if (medSeconds <= nowTotalSeconds) {
                        add(Calendar.DAY_OF_YEAR, 1)
                    }
                }
                bestTargetCalendar = cal
            }
        }

        val chosenMed = bestMedicine ?: list.first()
        val remainingMillis = bestDiffSeconds * 1000

        val hours = bestDiffSeconds / 3600
        val minutes = (bestDiffSeconds % 3600) / 60
        val seconds = bestDiffSeconds % 60
        val countdownStr = String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)

        val formatted12h = formatTime12h(chosenMed.time)

        return NextDoseInfo(
            medicine = chosenMed,
            formattedTime = formatted12h,
            countdownFormatted = countdownStr,
            remainingMillis = remainingMillis
        )
    }

    fun formatTime12h(time24h: String): String {
        return try {
            val parts = time24h.split(":")
            val h = parts[0].toInt()
            val m = parts[1].toInt()
            val amPm = if (h >= 12) "PM" else "AM"
            val h12 = when {
                h == 0 -> 12
                h > 12 -> h - 12
                else -> h
            }
            String.format(Locale.getDefault(), "%02d:%02d %s", h12, m, amPm)
        } catch (_: Exception) {
            time24h
        }
    }

    fun getGreetingText(): String {
        val cal = Calendar.getInstance()
        return when (cal.get(Calendar.HOUR_OF_DAY)) {
            in 4..11 -> "GOOD MORNING 👋"
            in 12..16 -> "GOOD AFTERNOON 👋"
            else -> "GOOD EVENING 👋"
        }
    }

    fun getLastSyncText(timestamp: Long?): String {
        if (timestamp == null) return "Never"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 30_000 -> "Just now"
            diff < 60_000 -> "${diff / 1000}s ago"
            diff < 3600_000 -> "${diff / 60_000}m ago"
            else -> {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                sdf.format(Date(timestamp))
            }
        }
    }
}
