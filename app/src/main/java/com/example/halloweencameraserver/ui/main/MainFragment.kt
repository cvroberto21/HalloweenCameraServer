package com.example.halloweencameraserver.ui.main

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker.checkSelfPermission
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.example.halloweencameraserver.DeviceListActivity
import com.example.halloweencameraserver.R
import com.example.halloweencameraserver.VideoServerConnectThread
import kotlinx.android.synthetic.main.main_fragment.*

class MainFragment : Fragment() {
    private val TAG = "JBBTSERVER"

    // Intent request codes
    private val REQUEST_CONNECT_DEVICE_SECURE = 1
    private val REQUEST_CONNECT_DEVICE_INSECURE = 2
    private val REQUEST_ENABLE_BT = 3

    /**
     * Local Bluetooth adapter
     */
    private var mBluetoothAdapter: BluetoothAdapter? = null

    companion object {
        fun newInstance() = MainFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate( R.layout.main_fragment, container, false)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        val fact = activity as FragmentActivity

        if (mBluetoothAdapter == null && activity != null) {
            Toast.makeText( fact, "Bluetooth is not available", Toast.LENGTH_LONG).show()
            fact.finish()
        }
    }


    //    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
//        super.onViewCreated(view, savedInstanceState)
//    }
//
    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
//        val peers = arrayOf( "JB_Robin", "JB_Canary", "JB_Flash", "JB_Hawk" )
//        val adapter = ArrayAdapter<String>( activity!!, android.R.layout.simple_spinner_item, peers )
//        adapter.setDropDownViewResource( android.R.layout.simple_spinner_dropdown_item )
//        // Apply the adapter to the spinner
//        spinnerBluetoothPeer.adapter = adapter
//        Log.d(TAG, "Setting adapter for spinnerBluetoothPeer")
//        spinnerBluetoothPeer?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener{
//            override fun onNothingSelected(parent: AdapterView<*>?) {
//                Log.d( TAG, "Bluetooth peer unselected")
//            }
//
//            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
//                val peer = parent!!.getItemAtPosition(position).toString()
//                Log.d( TAG, "Bluetooth peer $peer selected")
//                videoServer?.cancel()
//                stopUiHandlerThread()
//                stopBluetoothThread()
//
//                //connectBluetooth()
//            }
//        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            REQUEST_CONNECT_DEVICE_SECURE ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //connectDevice(data, true)
                }
            REQUEST_CONNECT_DEVICE_INSECURE ->                 // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    //connectDevice(data, false)
                }
            REQUEST_ENABLE_BT ->                 // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    //setupChat()
                } else {
                    // User did not enable Bluetooth or an error occurred
                    Log.d(TAG, "BT not enabled")
                    val fact = activity as FragmentActivity
                    if (activity != null) {
                        Toast.makeText(
                            fact, R.string.bt_not_enabled_leaving,
                            Toast.LENGTH_SHORT
                        ).show()
                        fact.finish()
                    }
                }
        }
    }

    override fun onStart() {
        super.onStart()
        if (mBluetoothAdapter == null) {
            return
        }
        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (! mBluetoothAdapter!!.isEnabled ) {
            val enableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT)
            // Otherwise, setup the chat session
        }
        val permission1 = checkSelfPermission( activity!!.applicationContext, Manifest.permission.BLUETOOTH)
        val permission2 = checkSelfPermission(activity!!.applicationContext, Manifest.permission.BLUETOOTH_ADMIN)
        val permission3 = checkSelfPermission( activity!!.applicationContext, Manifest.permission.ACCESS_COARSE_LOCATION)
        val permission4 = checkSelfPermission( activity!!.applicationContext, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permission1 != PackageManager.PERMISSION_GRANTED
            || permission2 != PackageManager.PERMISSION_GRANTED
            || permission3 != PackageManager.PERMISSION_GRANTED
            || permission4 != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity as FragmentActivity,
                arrayOf(
                    Manifest.permission.BLUETOOTH,
                    Manifest.permission.BLUETOOTH_ADMIN,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION),
                642)
        } else {
            Log.d("DISCOVERING-PERMISSIONS", "Permissions Granted")
        }

//        else if (mChatService == null) {
//            setupChat()
//        }
    }
    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate( R.menu.main_fragment_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.getItemId()) {
            R.id.secure_connect_scan -> {

                // Launch the DeviceListActivity to see devices and do scan
                val serverIntent = Intent(activity, DeviceListActivity::class.java )
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_SECURE)
                return true
            }
            R.id.insecure_connect_scan -> {

                // Launch the DeviceListActivity to see devices and do scan
                val serverIntent = Intent(activity, DeviceListActivity::class.java)
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE)
                return true
            }
            R.id.discoverable -> {
                // Ensure this device is discoverable by others
                ensureDiscoverable()
                return true
            }
        }
        return false
    }

    /**
     * Makes this device discoverable for 300 seconds (5 minutes).
     */
    private fun ensureDiscoverable() {
        if (mBluetoothAdapter?.getScanMode() !==
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE
        ) {
            val discoverableIntent = Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300)
            startActivity(discoverableIntent)
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

//                Log.d( TAG, "Looking for peer $peerName")
//                for (device in pairedDevices) {
//                    val deviceName = device.name
//                    //val deviceHardwareAddress = device.address // MAC address
//                    //val PEER_NAME = "JB_Canary"
//                    //val PEER_NAME = "JB_Flash"
//                    //val PEER_NAME = "JB_Robin"
//
//                    if (device.name == peerName) {
//                        peer = device
//                        break
//                    }
//                }
            }

            if (peer != null ) {
                Log.d( TAG, "Found peer ${peer.name}")
                startBluetoothThread()
                if (videoServer != null) {
                    videoServer?.cancel()
                }
                videoServer = VideoServerConnectThread( peer, bluetoothHandler!!, uiHandler!! )
            } else {
                Log.d( TAG, "Did not find peer")
            }
        }
    }

    override fun onResume() {
        super.onResume()
        //startUiHandlerThread()
        //remoteView.uiHandler = uiHandler

        //connectBluetooth()
    }

    override fun onPause() {
        videoServer?.cancel()
        stopUiHandlerThread()
        stopBluetoothThread()
        super.onPause()
    }

    private var bluetoothThread : HandlerThread? = null
    private var bluetoothHandler: Handler? = null

    private fun startBluetoothThread() {
        Log.d( TAG, "Starting bluetooth thread")

        bluetoothThread = HandlerThread("Bluetooth")
        bluetoothThread?.start()
        bluetoothHandler = Handler( bluetoothThread!!.looper)
    }

    private fun stopBluetoothThread( ) {
        Log.d( TAG, "Stopping bluetooth thread")
        bluetoothThread?.quitSafely()
        try {
            bluetoothThread?.join()
            bluetoothThread = null
            bluetoothHandler = null
        } catch( e: InterruptedException ) {
            Log.e( TAG,e.toString() )
        }
    }

    private var uiHandlerThread: HandlerThread? = null
    var uiHandler : Handler? = null

    private fun startUiHandlerThread() {
        Log.d( TAG, "Starting UI handler thread")

        uiHandlerThread = HandlerThread( "UiHandler")
        uiHandlerThread?.start()
        uiHandler = Handler( uiHandlerThread!!.looper, remoteView )
    }

    private fun stopUiHandlerThread( ) {
        Log.d( TAG, "Stopping UI handler thread")

        uiHandlerThread?.quitSafely()
        try {
            uiHandlerThread?.join()
            uiHandlerThread = null
        } catch( e: InterruptedException ) {
            Log.e( TAG, e.toString() )
        }
    }
}
