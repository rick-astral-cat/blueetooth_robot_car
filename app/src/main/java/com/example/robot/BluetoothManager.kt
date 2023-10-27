package com.example.robot

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.IOException
import java.io.OutputStream
import java.util.*

class BluetoothManager {
    private var socket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null
    private var isConnected = false

    //Execute scope to bluetooth on other thread
    private val scope = CoroutineScope(Dispatchers.IO)
    private var onConnectionStateChangedListener: OnConnectionStateChangedListener? = null

    // Define una interfaz para notificar a otras partes de la aplicación sobre los cambios de estado de la conexión.
    interface OnConnectionStateChangedListener {
        fun onConnected()
        fun onDisconnected()
    }

    fun setOnConnectionStateChangedListener(listener: OnConnectionStateChangedListener) {
        onConnectionStateChangedListener = listener
    }

    private fun onConnected() {
        isConnected = true
        onConnectionStateChangedListener?.onConnected()
    }

    private fun onDisconnected() {
        isConnected = false
        onConnectionStateChangedListener?.onDisconnected()
    }

    fun connect(deviceAddress: String) {
        scope.launch {
            val btAdapter = BluetoothAdapter.getDefaultAdapter()
            val device: BluetoothDevice = btAdapter.getRemoteDevice(deviceAddress)

            try {
                socket = device.createRfcommSocketToServiceRecord(UUID.fromString("00001101-0000-1000-8000-00805F9B34FB"))
                socket?.connect()
                outputStream = socket?.outputStream
                isConnected = true
                onConnected()
                Log.i("Bluetooth", isConnected.toString())
            } catch (e: IOException) {
                onDisconnected()
                isConnected = false
                e.printStackTrace()
            }
        }
    }

    fun disconnect() {
        try {
            socket?.close()
            isConnected = false
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    fun sendCommand(command: Byte) {
        if (isConnected) {
            try {
                outputStream?.write(command.toInt())
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    fun isConnected(): Boolean {
        return isConnected
    }
}