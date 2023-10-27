package com.example.robot

import PermissionManager
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.robot.ui.theme.PsBackground
import com.example.robot.ui.theme.RobotTheme

const val REQUEST_ENABLE_BT = 1
var m_bluetoothSocket: BluetoothSocket? = null
class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    var someActivityResultlauncher = registerForActivityResult(
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
    
    
    /*
    lateinit var mBtAdapter: BluetoothAdapter
    var mAddressDevices: ArrayAdapter<String>? = null
    var mNameDevices: ArrayAdapter<String>? = null
    var m_myUUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")
    var m_isConnected: Boolean = false
    @RequiresApi(Build.VERSION_CODES.S)
    */
    override fun onCreate(savedInstanceState: Bundle?) {
        
        /*
        
        
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
         */

        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)
        setContent {
            RobotTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = PsBackground) {
                    MainUI()
                }
            }
        }
    }


    // Método para solicitar permisos
    private fun requestBluetoothPermission() {
        //permissionManager.requestPermission(android.Manifest.permission.BLUETOOTH, YOUR_PERMISSION_REQUEST_CODE)
        requestMultiplePermissions.launch(arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADVERTISE,
            Manifest.permission.BLUETOOTH_PRIVILEGED,
        ))
        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
        someActivityResultlauncher.launch(enableBtIntent)
    }

    // Verificar si el permiso está concedido
    private fun checkBluetoothPermission() {
        if (permissionManager.isPermissionGranted(android.Manifest.permission.BLUETOOTH)) {
            // El permiso está concedido, puedes usar Bluetooth aquí
            Log.i("Bluetoothpermission", "Permission already granted")
        } else {
            // El permiso no está concedido, solicita permiso
            requestBluetoothPermission()
        }
    }

    @Composable
    fun MainUI() {
        Column {
            BluetoothUI()
        }
    }

    @Composable
    fun BluetoothUI() {
        val bluetoothManager = remember { BluetoothManager() }
        var isConnected by remember { mutableStateOf(false)}
        bluetoothManager.setOnConnectionStateChangedListener(object : BluetoothManager.OnConnectionStateChangedListener{
            override fun onConnected() {
                Log.i("Bluetooth", "Evento de conexión escuchado")
                isConnected = true
            }

            override fun onDisconnected() {
                // Actualizar la interfaz de usuario cuando la conexión se pierda
                Log.i("Bluetooth", "Evento de desconexión escuchado")
                isConnected = false
            }
        })
        Button(onClick = { checkBluetoothPermission() }) {
            Text("request Permission")
        }

        if (isConnected) {
            Text("Bluetooth Connected")
        } else {
            Text("Bluetooth Disconnected")
        }

        if (isConnected and permissionManager.isPermissionGranted(android.Manifest.permission.BLUETOOTH)) {
            // Botones u otros elementos de la interfaz para enviar comandos
            Button(onClick = { bluetoothManager.sendCommand(100) }) {
                Text("Enviar Comando")
            }
        } else {
            // Botón para conectarse
            Button(onClick = { bluetoothManager.connect("58:56:00:00:83:25") }) {
                Text("Conectar a Bluetooth")
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