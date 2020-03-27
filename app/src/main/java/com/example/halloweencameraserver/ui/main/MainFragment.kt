package com.example.halloweencameraserver.ui.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import androidx.lifecycle.ViewModelProviders
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.example.halloweencameraserver.R
import com.example.halloweencameraserver.VideoServerConnectThread

class MainFragment : Fragment() {
    private val TAG = "JBBTSERVER"

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.main_fragment, container, false)
    }

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        // TODO: Use the ViewModel
    }

    private var videoServer: VideoServerConnectThread? = null

    private fun connectBluetooth( ) {
        val REQUEST_ENABLE_BT = 1459

        val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

        if  ( bluetoothAdapter != null ) {
            if (bluetoothAdapter.isEnabled == false) {
                val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT)
            }

            val pairedDevices: Set<BluetoothDevice>? = bluetoothAdapter.bondedDevices

            var peer : BluetoothDevice? = null

            if ( pairedDevices != null ) {
                for (device in pairedDevices) {
                    val deviceName = device.name
                    //val deviceHardwareAddress = device.address // MAC address
                    //val PEER_NAME = "JB_Canary"
                    //val PEER_NAME = "JB_Flash"
                    val PEER_NAME = "JB_Robin"

                    if (device.name == PEER_NAME) {
                        peer = device
                        break
                    }
                }
            }
            if (peer != null ) {
                startBluetoothThread()
                videoServer = VideoServerConnectThread( peer, bluetoothHandler!! )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        connectBluetooth()

        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
    }

    override fun onPause() {
        stopBluetoothThread()
        videoServer?.cancel()

        super.onPause()
    }

    private var bluetoothThread : HandlerThread? = null
    private var bluetoothHandler: Handler? = null

    private fun startBluetoothThread() {
        bluetoothThread = HandlerThread("Bluetooth")
        bluetoothThread?.start()
        bluetoothHandler = Handler( bluetoothThread!!.looper)
    }

    private fun stopBluetoothThread( ) {
        bluetoothThread?.quitSafely()
        try {
            bluetoothThread?.join()
            bluetoothThread = null
            bluetoothHandler = null
        } catch( e: InterruptedException ) {
            Log.e( TAG,e.toString() )
        }
    }
}
