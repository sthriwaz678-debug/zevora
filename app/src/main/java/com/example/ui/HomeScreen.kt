package com.example.ui

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.Medication
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.ble.BleConnectionState
import com.example.ble.BleSyncResult
import com.example.ui.components.BleConnectionDialog
import com.example.ui.components.BluetoothConnectionCard
import com.example.ui.components.MyScheduleDialog
import com.example.ui.components.NextMedicineCard
import com.example.ui.components.ScheduleEditorDialog
import com.example.ui.components.SyncSuccessDialog
import com.example.ui.components.TodayProgressCard
import com.example.ui.components.QuickActionsCard
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: ZevoraViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val connectionState by viewModel.connectionState.collectAsStateWithLifecycle()
    val connectedDevice by viewModel.connectedDevice.collectAsStateWithLifecycle()
    val discoveredDevices by viewModel.discoveredDevices.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()
    val batteryLevel by viewModel.batteryLevel.collectAsStateWithLifecycle()
    val lastSyncTime by viewModel.lastSyncTime.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isSyncing.collectAsStateWithLifecycle()
    val isBluetoothEnabled by viewModel.isBluetoothEnabled.collectAsStateWithLifecycle()
    val medicines by viewModel.medicines.collectAsStateWithLifecycle()
    val nextDose by viewModel.nextDoseInfo.collectAsStateWithLifecycle()

    var showBleDialog by remember { mutableStateOf(false) }
    var showScheduleEditor by remember { mutableStateOf(false) }
    var showMySchedule by remember { mutableStateOf(false) }
    var showSyncSuccessDialog by remember { mutableStateOf(false) }
    var hasPermissions by remember { mutableStateOf(viewModel.bleManager.hasPermissions()) }

    // Bluetooth permission launcher
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissionsMap ->
        val allGranted = permissionsMap.values.all { it }
        hasPermissions = allGranted
        if (allGranted) {
            viewModel.startScan()
        } else {
            coroutineScope.launch {
                snackbarHostState.showSnackbar("ZEVORA uses Bluetooth to securely connect with your medicine dispenser.")
            }
        }
    }

    // Enable Bluetooth Intent Launcher
    val enableBtLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.refreshBluetoothStatus()
    }

    // Handle ViewModel messages & sync results
    LaunchedEffect(Unit) {
        viewModel.userMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncResultFlow.collectLatest { result ->
            when (result) {
                is BleSyncResult.Success -> {
                    showSyncSuccessDialog = true
                }
                is BleSyncResult.Error -> {
                    snackbarHostState.showSnackbar(result.errorMessage)
                }
            }
        }
    }

    fun requestRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.BLUETOOTH_SCAN,
                    Manifest.permission.BLUETOOTH_CONNECT
                )
            )
        } else {
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    fun openEnableBluetooth() {
        try {
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            enableBtLauncher.launch(enableBtIntent)
        } catch (_: Exception) {
            val intent = Intent(Settings.ACTION_BLUETOOTH_SETTINGS)
            context.startActivity(intent)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0)
    ) { innerPadding ->
        val statusBarPadding = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
        val navBarPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 600.dp)
                    .align(Alignment.TopCenter)
                    .testTag("home_screen_scroll"),
                contentPadding = PaddingValues(
                    start = 18.dp,
                    end = 18.dp,
                    top = statusBarPadding + 12.dp,
                    bottom = navBarPadding + 24.dp
                ),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // ====================================
                // TOP HEADER: ZEVORA LOGO, GREETING & TAGLINE
                // ====================================
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.Default.Medication,
                                            contentDescription = "ZEVORA Logo",
                                            tint = Color.White,
                                            modifier = Modifier.size(26.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = "ZEVORA",
                                        style = MaterialTheme.typography.titleLarge,
                                        fontWeight = FontWeight.Black,
                                        letterSpacing = 1.sp,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        text = "\"Never Miss What Matters.\"",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Greeting Badge
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f)
                            ) {
                                Text(
                                    text = viewModel.getGreetingText(),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                // ====================================
                // SECTION 1: BLUETOOTH DEVICE CONNECTION CARD
                // ====================================
                item {
                    BluetoothConnectionCard(
                        connectionState = connectionState,
                        connectedDevice = connectedDevice,
                        batteryLevel = batteryLevel,
                        lastSyncTime = lastSyncTime,
                        lastSyncText = viewModel.getLastSyncText(lastSyncTime),
                        isSyncing = isSyncing,
                        onConnectClick = {
                            hasPermissions = viewModel.bleManager.hasPermissions()
                            viewModel.refreshBluetoothStatus()
                            showBleDialog = true
                        },
                        onDisconnectClick = {
                            viewModel.disconnectDevice()
                        },
                        onSyncClick = {
                            viewModel.syncScheduleToDispenser()
                        }
                    )
                }

                // ====================================
                // SECTION 2: NEXT MEDICINE CARD
                // ====================================
                item {
                    NextMedicineCard(
                        nextDose = nextDose,
                        onSetTimeClick = { showScheduleEditor = true }
                    )
                }

                // ====================================
                // SECTION 3: TODAY'S PROGRESS CARD
                // ====================================
                item {
                    TodayProgressCard(
                        medicines = medicines,
                        onManageScheduleClick = { showScheduleEditor = true }
                    )
                }

                // ====================================
                // SECTION 4: QUICK ACTIONS CARD
                // ====================================
                item {
                    QuickActionsCard(
                        isSyncing = isSyncing,
                        onSetMedicineTimeClick = { showScheduleEditor = true },
                        onMyScheduleClick = { showMySchedule = true },
                        onSyncScheduleClick = {
                            viewModel.syncScheduleToDispenser()
                        }
                    )
                }
            }
        }
    }

    // Bluetooth Connection Dialog
    if (showBleDialog) {
        BleConnectionDialog(
            isScanning = isScanning,
            discoveredDevices = discoveredDevices,
            connectionState = connectionState,
            isBluetoothEnabled = isBluetoothEnabled,
            hasPermissions = hasPermissions,
            onStartScan = {
                if (hasPermissions && isBluetoothEnabled) {
                    viewModel.startScan()
                }
            },
            onStopScan = { viewModel.stopScan() },
            onConnectDevice = { device ->
                viewModel.connectDevice(device)
                showBleDialog = false
            },
            onRequestPermissions = { requestRequiredPermissions() },
            onEnableBluetooth = { openEnableBluetooth() },
            onDismiss = { showBleDialog = false }
        )
    }

    // Medicine Schedule Editor Dialog
    if (showScheduleEditor) {
        ScheduleEditorDialog(
            initialMedicines = medicines,
            onSaveSchedule = { updatedSlots ->
                viewModel.setConfiguredSlotCount(updatedSlots.size)
                updatedSlots.forEach { slot ->
                    viewModel.saveMedicineSlot(
                        slot = slot.slot,
                        name = slot.name,
                        time = slot.time,
                        isEnabled = slot.isEnabled,
                        dosageNotes = slot.dosageNotes
                    )
                }
            },
            onSaveAndSync = { updatedSlots ->
                viewModel.setConfiguredSlotCount(updatedSlots.size)
                updatedSlots.forEach { slot ->
                    viewModel.saveMedicineSlot(
                        slot = slot.slot,
                        name = slot.name,
                        time = slot.time,
                        isEnabled = slot.isEnabled,
                        dosageNotes = slot.dosageNotes
                    )
                }
                showScheduleEditor = false
                viewModel.syncScheduleToDispenser()
            },
            onDismiss = { showScheduleEditor = false }
        )
    }

    // My Schedule Viewer Dialog
    if (showMySchedule) {
        MyScheduleDialog(
            medicines = medicines,
            jsonPayload = viewModel.generateScheduleJson(),
            onEditClick = {
                showMySchedule = false
                showScheduleEditor = true
            },
            onSyncClick = {
                showMySchedule = false
                viewModel.syncScheduleToDispenser()
            },
            onDismiss = { showMySchedule = false }
        )
    }

    // Sync Success Notification Dialog
    if (showSyncSuccessDialog) {
        SyncSuccessDialog(
            syncedMedicinesCount = medicines.count { it.isEnabled },
            onDismiss = { showSyncSuccessDialog = false }
        )
    }
}
