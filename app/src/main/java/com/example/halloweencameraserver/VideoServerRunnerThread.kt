package com.example.halloweencameraserver

import android.bluetooth.BluetoothSocket
import android.graphics.Color.argb
import android.os.Bundle
import android.os.Handler
import android.util.Log
import androidx.annotation.ColorInt
import androidx.core.graphics.toColor
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

class VideoServerRunnerThread {
    enum class MessageTypes( val type: Int ) {
        MESSAGE_READ(0),
        MESSAGE_WRITE(1),
        MESSAGE_ERROR(2),
        MESSAGE_PIXMAP( 3 )
    }

    private val PIXMAP_WIDTH = 320
    private val PIXMAP_HEIGHT = 240

    private val TAG = "JBBTRUN"

    lateinit private var connectThread : ConnectedThread
    private lateinit var handler : Handler

    //private var rcvdPixmap : UByteArray = UByteArray( PIXMAP_HEIGHT * PIXMAP_WIDTH )
    @kotlin.ExperimentalUnsignedTypes
//    private var rcvdPixmap : Array<UByteArray> = Array( PIXMAP_HEIGHT) { i -> UByteArray(PIXMAP_WIDTH) }

    private var rcvdPixmap : IntArray = IntArray(PIXMAP_WIDTH * PIXMAP_HEIGHT ) { i -> argb(255,128,128,128) }

    fun connect(socket : BluetoothSocket, handler : Handler) {
        connectThread = ConnectedThread( socket, handler )
        this.handler = handler
        connectThread.start()
    }

    fun disconnect( ) {
        connectThread.cancel()
    }

    fun write(bytes: ByteArray) {
        connectThread.write( bytes )
    }

    private inner class ConnectedThread(private val mmSocket: BluetoothSocket, private val handler : Handler) : Thread() {

        private val mmInStream: InputStream = mmSocket.inputStream
        private val mmOutStream: OutputStream = mmSocket.outputStream
        private val mmBuffer: UByteArray = UByteArray(1024) // mmBuffer store for the stream

        override fun run() {
            var numBytes: Int // bytes returned from read()

            // Keep listening to the InputStream until an exception occurs.
            while (true) {
                // Read from the InputStream.
                numBytes = 4
                for (i in 0..3) {
                    @kotlin.ExperimentalUnsignedTypes
                    mmBuffer[i] = (10 + i).toUByte()
                }
                for (y in 0 until PIXMAP_HEIGHT) {
                    for (x in 0 until PIXMAP_WIDTH) {
                        rcvdPixmap[y * PIXMAP_WIDTH + x] = argb(255, 200, 20, 200)
                    }
                }
                Thread.sleep(500)
//                numBytes = try {
//                    mmInStream.read(mmBuffer)
//
//                } catch (e: IOException) {
//                    Log.d(TAG, "Input stream was disconnected", e)
//                    break
//                }

                // Send the obtained bytes to the UI activity.
                handler.obtainMessage(
                    MessageTypes.MESSAGE_PIXMAP.type, rcvdPixmap)?.apply{ sendToTarget() }
            }
        }

        // Call this from the main activity to send data to the remote device.
        fun write(bytes: ByteArray) {
            Log.d(TAG, "Output stream sendng data $bytes" )

            try {
                mmOutStream.write(bytes)
            } catch (e: IOException) {
                Log.e( TAG, "Error occurred when sending data", e)

                // Send a failure message back to the activity.
                val writeErrorMsg = handler.obtainMessage( MessageTypes.MESSAGE_ERROR.type )
                val bundle = Bundle().apply {
                    putString("msg", "Couldn't send data to the other device")
                }
                writeErrorMsg.data = bundle
                handler.sendMessage(writeErrorMsg)
                return
            }

            // Share the sent message with the UI activity.
            val writtenMsg = handler.obtainMessage(
                MessageTypes.MESSAGE_PIXMAP.type, -1, -1, mmBuffer)
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
