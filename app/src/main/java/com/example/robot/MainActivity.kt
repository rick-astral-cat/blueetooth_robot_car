package com.example.robot

import android.Manifest
import android.R
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import com.example.robot.ui.theme.PsBackground
import com.example.robot.ui.theme.RobotTheme
import java.io.IOException
import java.util.UUID

const val REQUEST_ENABLE_BT = 1
var m_bluetoothSocket: BluetoothSocket? = null
class MainActivity : ComponentActivity() {
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null
    var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var m_isConnected: Boolean = false
    @RequiresApi(Build.VERSION_CODES.S)
    override fun onCreate(savedInstanceState: Bundle?) {
        val someActivityResultlauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ){
                result ->
            if(result.resultCode == REQUEST_ENABLE_BT){
                Log.i("MainActivity", "REGISTERED ACTIVITY")
            }
        }
        val requestMultiplePermissions =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    Log.d("test006", "${it.key} = ${it.value}")
                }
            }
        mAddressDevices = ArrayAdapter(this, R.layout.simple_list_item_1)
        mNameDevices = ArrayAdapter(this, R.layout.simple_list_item_1)

        //Init bluetooth adapter
        mBtAdapter = (getSystemService(BLUETOOTH_SERVICE) as BluetoothManager).adapter
        //Check if bluetooth is available on device
        if(mBtAdapter == null){
            Toast.makeText(this, "Bluetooth not available on this device",  Toast.LENGTH_LONG).show()
        }
        else{
            Toast.makeText(this, "Bluetooth is available on this device",  Toast.LENGTH_LONG).show()
        }
        //Check if bluetooth is enabled
        if(mBtAdapter.isEnabled){
            Toast.makeText(this, "Bluetooth is ON",  Toast.LENGTH_LONG).show()
        }
        else{
            if(ActivityCompat.checkSelfPermission(
                    this,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) != PackageManager.PERMISSION_GRANTED)
            {
                Log.i("MainActivity", "ActivityCompat#requestPermissions")
            }
            requestMultiplePermissions.launch(arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_PRIVILEGED
            ))
            val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            someActivityResultlauncher.launch(enableBtIntent)
        }
        if(m_bluetoothSocket == null || !m_isConnected){
            mBtAdapter.cancelDiscovery()
            val device: BluetoothDevice = mBtAdapter.getRemoteDevice("58:56:00:00:83:25")
            m_bluetoothSocket = device.createInsecureRfcommSocketToServiceRecord(m_myUUID)
            m_bluetoothSocket!!.connect()
        }
        Toast.makeText(this, "SUCCESS CONECTION", Toast.LENGTH_LONG).show()
        super.onCreate(savedInstanceState)
        setContent {
            RobotTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = PsBackground) {
                    HomeScreen()
                }
            }
        }
    }
}

@Preview(
    showBackground = true,
    showSystemUi = true
)
@Composable
fun HomeScreenPreview() {
    RobotTheme {
        HomeScreen()
    }
}