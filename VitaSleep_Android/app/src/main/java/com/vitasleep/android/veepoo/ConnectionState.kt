package com.vitasleep.android.veepoo

sealed class ConnectionState {
    data object Disconnected : ConnectionState()
    data object Connecting : ConnectionState()
    data object Confirming : ConnectionState()
    data class Connected(val mac: String, val name: String = "") : ConnectionState()
    data class Error(val message: String) : ConnectionState()
}
