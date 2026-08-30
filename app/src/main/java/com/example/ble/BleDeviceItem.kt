package com.example.ble

import android.bluetooth.BluetoothDevice

data class BleDeviceItem(
    val name: String,
    val address: String,
    val rssi: Int = -60,
    val isZevoraDispenser: Boolean = false,
    val isVirtual: Boolean = false,
    val rawDevice: BluetoothDevice? = null
)
