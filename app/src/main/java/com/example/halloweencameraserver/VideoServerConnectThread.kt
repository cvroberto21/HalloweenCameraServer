package com.example.halloweencameraserver

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.util.*

class VideoServerConnectThread( device: BluetoothDevice, private val handler: Handler ) {
    private val TAG = "JBVidCon"
    //private val SPP_UUID = "00001101-0000-1000-8000-00805f9b34fb"
    private val VIDEO_SERVER_UUID = "00001101-0000-1000-8000-00805f9b34ff"

    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var connectThread : ConnectThread? = null

    init {
        connectThread = ConnectThread( device, handler )
        connectThread?.start()
    }

    public var videoServerRunnerThread : VideoServerRunnerThread? = null

    fun getRunner() : VideoServerRunnerThread? {
        return videoServerRunnerThread
    }

    fun cancel() {
        connectThread?.cancel()
    }

    private inner class ConnectThread(private val device: BluetoothDevice, var handler : Handler) : Thread() {

        private var mmServerSocket: BluetoothServerSocket? = null


        override fun run() {
            // Keep listening until exception occurs or a socket is returned.
            mmServerSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("JBVIDEO", UUID.fromString(VIDEO_SERVER_UUID) )

            var shouldLoop = true
            while (shouldLoop) {
                val socket: BluetoothSocket? = try {
                    mmServerSocket?.accept()
                } catch (e: IOException) {
                    Log.e(TAG, "Socket's accept() method failed", e)
                    shouldLoop = false
                    null
                }
                socket?.also {
                videoServerRunnerThread = VideoServerRunnerThread()

                // The connection attempt succeeded. Perform work associated with
                // the connection in a separate thread.
                videoServerRunnerThread?.connect(it, handler)
                    mmServerSocket?.close()
                    shouldLoop = false
                }
            }
        }

        // Closes the connect socket and causes the thread to finish.
        fun cancel() {
            videoServerRunnerThread?.disconnect()
            videoServerRunnerThread = null
            try {
                mmServerSocket?.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}