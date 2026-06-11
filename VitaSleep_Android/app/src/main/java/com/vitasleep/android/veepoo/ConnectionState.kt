package com.vitasleep.android.veepoo

sealed class ConnectionState {
    class Disconnected : ConnectionState()
    class Connecting : ConnectionState()
    data class Connected(val mac: String) : ConnectionState()
    data class Error(val message: String) : ConnectionState()

    companion object {
        val Disconnected = Disconnected()
        val Connecting = Connecting()
    }
}