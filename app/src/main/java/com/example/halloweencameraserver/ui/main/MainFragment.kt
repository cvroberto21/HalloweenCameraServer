package com.example.halloweencameraserver.ui.main

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.graphics.Bitmap
import android.os.*
import androidx.lifecycle.ViewModelProviders
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.Spinner
import androidx.core.graphics.set
import androidx.core.graphics.toColorInt
import com.example.halloweencameraserver.R
import com.example.halloweencameraserver.VideoServerConnectThread
import com.example.halloweencameraserver.VideoServerRunnerThread
import kotlinx.android.synthetic.main.main_fragment.*

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
        peerSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                stopBluetoothThread()
                connectBluetooth()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                stopBluetoothThread()
            }
        }
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
                    val peerName = peerSpinner.selectedItem as String

                    if (device.name == peerName ) {
                        peer = device
                        break
                    }
                }
            }
            Log.d( TAG, "Starting bluetooth thread $peer" )
            logView.append( "Starting bluetooth thread $peer")
            if (peer != null ) {
                startBluetoothThread()
                videoServer = VideoServerConnectThread( peer, bluetoothHandler!! )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        logView.append( "onResume\n")
        // When the screen is turned off and turned back on, the SurfaceTexture is already
        // available, and "onSurfaceTextureAvailable" will not be called. In that case, we can open
        // a camera and start preview from here (otherwise, we wait until the surface is ready in
        // the SurfaceTextureListener).
    }

    override fun onPause() {
        logView.append( "onPause\n" )
        stopBluetoothThread()
        videoServer?.cancel()
        super.onPause()
    }

    private var bluetoothThread : HandlerThread? = null
    private var bluetoothHandler: Handler? = null

    private fun startBluetoothThread() {
        logView.append( "start bluetooth thread\n" )
        bluetoothThread = HandlerThread("Bluetooth")
        bluetoothThread?.start()
        bluetoothHandler = object: Handler( Looper.getMainLooper() ) {
            override fun handleMessage(inputMessage: Message) {
                // Gets the image task from the incoming Message object.
                Log.d( TAG, "Received message what $inputMessage.what" )
                if ( inputMessage.what == VideoServerRunnerThread.MessageTypes.MESSAGE_PIXMAP.type ) {
                    val length = inputMessage.arg1
                    val pixmap = inputMessage.obj as IntArray
                    Log.d(TAG, "Received pixmap of length ${pixmap.size}")
                    logView.text = ""
                    logView.append( "Received pixmap of length ${pixmap.size}" )
//                    val bitmap : Bitmap = Bitmap.createBitmap( 320, 240, Bitmap.Config.ARGB_8888)
//                    bitmap.setPixels( pixmap, 0, 320, 0, 0, 320, 240  )
//                    for(i in 0..240-1) {
//                        for( j in 0..320-1) {
//                            bitmap.set(j,i, 0xFF808080.toColorInt() )
//                        }
//                    }
//                    remoteView.drawBitmap( bitmap )
                }
            }
        }

    }

    private fun stopBluetoothThread( ) {
        logView.append( "stop bluetooth thread\n" )
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
