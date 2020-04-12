package com.example.halloweencameraserver

import android.bluetooth.BluetoothSocket
import android.graphics.Color.argb
import android.os.Bundle
import android.os.Handler
import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream


class VideoServerRunnerThread {
    val TAG = "JBBTRUN"

    enum class MessageTypes(val type: Int) {
        MESSAGE_READ(0),
        MESSAGE_WRITE(1),
        MESSAGE_TOAST(2)
    }

    lateinit private var connectThread: ConnectedThread
    lateinit private var handler: Handler
    lateinit private var framebuffer: FrameBuffer

    fun connect(socket: BluetoothSocket, handler: Handler) {
        connectThread = ConnectedThread(socket, handler)
        this.handler = handler
        framebuffer = FrameBuffer(320, 240, FrameBuffer.Type.ARGB8888)
        connectThread.start()
    }

    fun disconnect() {
        connectThread.cancel()
    }

    fun write(bytes: ByteArray) {
        connectThread.write(bytes)
    }

    private inner class ConnectedThread(
        private val mmSocket: BluetoothSocket,
        private val handler: Handler
    ) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: ByteArray = ByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            var r : Int = 0

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = try {
                    mmInStream.read(mmBuffer)

                } catch (e: IOException) {
                    Log.d(TAG, "Input stream was disconnected", e)
                    break
                }
                
                Log.d(TAG, "Input stream bytes received $numBytes")
                for (y in 0 until framebuffer.height) {
                    for (x in 0 until framebuffer.width) {
                        framebuffer.setPixel(x, y, argb(255, r, 20, 200))
                    }
                }
                Thread.sleep(500)

                r = r + 1
                if (r < 0) {
                    r = 0
                }



//                // Send the obtained bytes to the UI activity.
//                handler.obtainMessage(
//                    MessageTypes.MESSAGE_WRITE.type, framebuffer
//                )?.apply { sendToTarget() }
            }
        }
//        while (true) {
//                // Read from the InputStream.
//                numBytes = try {
//                    mmInStream.read(mmBuffer)
//                } catch (e: IOException) {
//                    Log.d(TAG, "Input stream was disconnected", e)
//                    break
//                }

//                // Send the obtained bytes to the UI activity.
//                val readMsg = handler.obtainMessage(
//                    MessageTypes.MESSAGE_WRITE.type, numBytes, -1,
//                    mmBuffer)
//                readMsg.sendToTarget()

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            Log.d(TAG, "Output stream sending data $bytes")

            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e(TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage( MessageTypes.MESSAGE_TOAST.type )
                val bundle = Bundle().apply {
                    putString("toast", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MessageTypes.MESSAGE_WRITE.type, -1, -1, mmBuffer
            )
            writtenMsg.sendToTarget()
        }

        // Call this method from the main activity to shut down the connection.
        fun cancel() {
            try {
                mmSocket.close()
            } catch (e: IOException) {
                Log.e(TAG, "Could not close the connect socket", e)
            }
        }
    }
}