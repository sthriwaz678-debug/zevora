package com.example.ble

enum class BleConnectionState(val displayName: String) {
    NOT_CONNECTED("Not Connected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    CONNECTION_LOST("Connection lost"),
    RECONNECTING("Reconnecting...")
}
