package com.example.robot

import PermissionManager
import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.example.robot.shapes.DrawShape
import com.example.robot.ui.theme.PsBackground
import com.example.robot.ui.theme.RobotTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

const val REQUEST_ENABLE_BT = 1

class MainActivity : ComponentActivity() {

    private lateinit var permissionManager: PermissionManager

    var someActivityResultlauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == REQUEST_ENABLE_BT) {
            Log.i("MainActivity", "REGISTERED ACTIVITY")
        }
    }

    val requestMultiplePermissions =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            permissions.entries.forEach {
                Log.d("test006", "${it.key} = ${it.value}")
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        permissionManager = PermissionManager(this)
        if (ContextCompat.checkSelfPermission(
                this,
                android.Manifest.permission.BLUETOOTH
            ) != PermissionChecker.PERMISSION_GRANTED
        ) {
            checkBluetoothPermission()
        }
        setContent {
            RobotTheme {
                // A surface container using the 'background' color from the theme
                Surface(modifier = Modifier.fillMaxSize(), color = PsBackground) {
                    CurexControl()
                }
            }
        }
    }


    // Método para solicitar permisos
    private fun requestBluetoothPermission() {
        //permissionManager.requestPermission(android.Manifest.permission.BLUETOOTH, YOUR_PERMISSION_REQUEST_CODE)
        requestMultiplePermissions.launch(
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADVERTISE,
                Manifest.permission.BLUETOOTH_PRIVILEGED,
            )
        )
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

    fun btDiconectedMessage(context: Context){
        Toast.makeText(context, "Curex disconnected, please reconnect if you want use it",  Toast.LENGTH_LONG).show()
    }

    // COMPOSABLE FUNCTIONS

    @Composable
    fun CurexControl() {
        //Local context
        val context = LocalContext.current

        //Bluetooth variables and listeners
        val bluetoothManager = remember { BluetoothManager() }
        var isConnected by remember { mutableStateOf(false) }
        bluetoothManager.setOnConnectionStateChangedListener(object :
            BluetoothManager.OnConnectionStateChangedListener {
            override fun onConnected() {
                Log.i("Bluetooth", "Evento de conexión escuchado")
                isConnected = true
                MainScope().launch { Toast.makeText(context, "Connection to Curex successfully",  Toast.LENGTH_LONG).show() }
            }

            override fun onDisconnected() {
                // Actualizar la interfaz de usuario cuando la conexión se pierda
                Log.i("Bluetooth", "Evento de desconexión escuchado")
                isConnected = false
                MainScope().launch { btDiconectedMessage(context) }
            }

            override fun failureConnection() {
                MainScope().launch { Toast.makeText(context, "Connection to Curex failure, maybe is not in the area?",  Toast.LENGTH_LONG).show() }
            }
        })

        //Logs from commands and bluetooth buffer
        val bLog = remember { mutableStateListOf("Curex 1 logs!") }
        var commandSent by remember { mutableStateOf(false) }
        var currentDirection by remember { mutableStateOf("") }
        val scrollState = rememberScrollState()
        LaunchedEffect(bLog.size) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
        // Support RTL
        val layoutDirection = LocalLayoutDirection.current
        val directionFactor = if (layoutDirection == LayoutDirection.Rtl) -1 else 1

        val scope = rememberCoroutineScope()
        val buttonSize = 90.dp  //Size of joystick


        // Swipe size in px
        val buttonSizePx = with(LocalDensity.current) { buttonSize.toPx() }
        val dragSizePx = buttonSizePx * 1.5f

        // Drag offset
        val offsetX = remember { Animatable(0f) }
        val offsetY = remember { Animatable(0f) }

        var isDragging by remember { mutableStateOf(false) }

        var currentPosition by remember { mutableStateOf<Position?>(null) }

        LaunchedEffect(offsetX.value, offsetY.value) {

            val newPosition = getPosition(
                offset = Offset(offsetX.value, offsetY.value),
                buttonSizePx = buttonSizePx - 100
            )

            currentPosition = newPosition
            if (offsetX.value.toDouble() == 0.0 && offsetY.value.toDouble() == 0.0) {
                if (isConnected) {
                    bLog.add("Stopping Car...")
                    bluetoothManager.sendCommand(180)
                }
                else{
                    btDiconectedMessage(context)
                }
            }
        }
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            //TOP BAR
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .drawBehind {
                        drawLine(
                            color = Color.White,
                            start = Offset(0F, size.height),
                            end = Offset(size.width, size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                //Logo
                Row(
                    modifier = Modifier.weight(1F),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(modifier = Modifier.width(20.dp))
                    Image(
                        painter = painterResource(id = R.drawable.tank),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(20.dp))
                    Text(
                        color = Color.White,
                        text = "CUREX 1",
                        fontSize = 20.sp
                    )
                }
                //Bluetooth status
                Row(
                    modifier = Modifier.weight(1F),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Canvas(
                        modifier = Modifier
                            .size(size = 30.dp)
                    ) {
                        if (isConnected)
                            drawCircle(
                                color = Color.Green,
                                radius = 10.dp.toPx()
                            )
                        else
                            drawCircle(
                                color = Color.Red,
                                radius = 10.dp.toPx()
                            )
                    }
                    if (isConnected)
                        Text(
                            text = "Connected",
                            color = Color.White
                        )
                    else
                        Text(
                            text = "Disconnected",
                            color = Color.White
                        )
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            //Main content (joystick and buttons)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1F),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1F),
                    contentAlignment = Alignment.Center,
                ) {

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {

                        //Spacer(modifier = Modifier.height(40.dp))

                        //Joystick button
                        Box(
                            modifier = Modifier
                                .size(buttonSize * 3)
                                .background(Color.White, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {

                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            x = (offsetX.value).roundToInt(),
                                            y = (offsetY.value).roundToInt()
                                        )
                                    }
                                    .width(buttonSize)
                                    .height(buttonSize)
                                    .alpha(0.8f)
                                    .background(Color.DarkGray, CircleShape)
                                    .pointerInput(Unit) {
                                        detectDragGestures(
                                            onDragStart = {
                                                isDragging = true
                                                commandSent = false
                                            },
                                            onDragEnd = {
                                                scope.launch {
                                                    offsetX.animateTo(0f)
                                                }
                                                scope.launch {
                                                    offsetY.animateTo(0f)
                                                }
                                                isDragging = false
                                                commandSent = false
                                                currentDirection = ""
                                            },
                                            onDragCancel = {
                                                scope.launch {
                                                    offsetX.animateTo(0f)
                                                }
                                                scope.launch {
                                                    offsetY.animateTo(0f)
                                                }
                                                isDragging = false
                                                commandSent = false
                                                currentDirection = ""
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()

                                                scope.launch {
                                                    val newOffsetX =
                                                        offsetX.value + dragAmount.x * directionFactor
                                                    val newOffsetY = offsetY.value + dragAmount.y

                                                    if (
                                                        sqrt(newOffsetX.pow(2) + newOffsetY.pow(2)) < dragSizePx
                                                    ) {
                                                        offsetX.snapTo(newOffsetX)
                                                        offsetY.snapTo(newOffsetY)
                                                    } else if (
                                                        sqrt(offsetX.value.pow(2) + newOffsetY.pow(2)) < dragSizePx
                                                    ) {
                                                        offsetY.snapTo(newOffsetY)
                                                    } else if (
                                                        sqrt(newOffsetX.pow(2) + offsetY.value.pow(2)) < dragSizePx
                                                    ) {
                                                        offsetX.snapTo(newOffsetX)
                                                    }
                                                }

                                            }
                                        )
                                    }

                            )

                            val buttonAlpha = remember {
                                Animatable(0f)
                            }

                            LaunchedEffect(key1 = isDragging) {
                                if (isDragging) {
                                    buttonAlpha.animateTo(1f)
                                } else {
                                    buttonAlpha.animateTo(0f)
                                }
                            }

                            Position.values().forEach { position ->
                                val offset = position.getOffset(buttonSizePx - 50)
                                MyButton(
                                    modifier = Modifier
                                        .offset {
                                            IntOffset(
                                                x = offset.x.roundToInt(),
                                                y = offset.y.roundToInt()
                                            )
                                        }
                                        .graphicsLayer {
                                            alpha = buttonAlpha.value
                                            scaleX = buttonAlpha.value
                                            scaleY = buttonAlpha.value
                                        }
                                        .size(buttonSize)
                                        .padding(8.dp),
                                    isSelected = position == currentPosition,
                                    position = position
                                )
                            }

                        }
                    }


                }
                //Right side (logs, buttons and connections)
                Column(
                    modifier = Modifier
                        .weight(1F)
                        .fillMaxSize()
                ) {
                    Row {
                        //Logs and connection buttons
                        Column(
                            modifier = Modifier.weight(1F),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            //Preview of the pressed joystick button
                            Spacer(modifier = Modifier.size(20.dp))
                            Box(modifier = Modifier.size(buttonSize * 1)) {
                                currentPosition?.let { position ->
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .fillMaxSize()
                                            .background(Color.DarkGray.copy(alpha = 0.5f))
                                            .padding(20.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        DrawShape(
                                            position,
                                            isSelected = true
                                        )
                                    }
                                    if (position.name == "Top" && currentDirection != position.name && isConnected) {
                                        bLog.add("Moving Forward")
                                        currentDirection = "Top"
                                        commandSent = true
                                        bluetoothManager.sendCommand(102)
                                    } else if (position.name == "Bottom" && currentDirection != position.name && isConnected) {
                                        bLog.add("Moving back")
                                        currentDirection = "Bottom"
                                        commandSent = true
                                        bluetoothManager.sendCommand(98)
                                    } else if (position.name == "Left" && currentDirection != position.name && isConnected) {
                                        bLog.add("Turning Left")
                                        currentDirection = "Left"
                                        commandSent = true
                                        bluetoothManager.sendCommand(108)
                                    } else if (position.name == "Right" && currentDirection != position.name && isConnected) {
                                        bLog.add("Turning Right")
                                        currentDirection = "Right"
                                        commandSent = true
                                        bluetoothManager.sendCommand(114)
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.size(20.dp))
                            Text(
                                text = "Bluetooth settings",
                                color = Color.White
                            )
                            if (isConnected) {
                                OutlinedButton(onClick = { bluetoothManager.disconnect() }) {
                                    Text(
                                        text = "Disconnect from Curex 1"
                                    )
                                }
                            } else {
                                OutlinedButton(onClick = { bluetoothManager.connect("58:56:00:00:83:25") }) {
                                    Text(
                                        text = "Connect to Curex 1"
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(5.dp))
                            Card(
                                modifier = Modifier
                                    .verticalScroll(scrollState)
                                    .fillMaxWidth()
                                    .weight(1F)
                            ) {
                                bLog.forEach {
                                    Text(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .padding(0.dp),
                                        text = it
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.size(10.dp))
                        }

                        //Buttons for servo motor
                        Column(
                            modifier = Modifier
                                .width(120.dp)
                                .fillMaxHeight()
                                .padding(10.dp),
                            verticalArrangement = Arrangement.SpaceAround,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "Gripper", color = Color.White)
                            OutlinedButton(
                                onClick = {
                                    if (isConnected) {
                                        bLog.add("Moving servo up")
                                        bluetoothManager.sendCommand(119)
                                    }
                                    else{
                                        btDiconectedMessage(context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                Text(text = "Up")
                            }
                            OutlinedButton(
                                onClick = {
                                    if (isConnected) {
                                        bLog.add("Moving servo center")
                                        bluetoothManager.sendCommand(115)
                                    }
                                    else{
                                        btDiconectedMessage(context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                Text(text = "Center")
                            }
                            OutlinedButton(
                                onClick = {
                                    if (isConnected) {
                                        bLog.add("Moving servo down")
                                        bluetoothManager.sendCommand(120)
                                    }
                                    else{
                                        btDiconectedMessage(context)
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(60.dp)
                            ) {
                                Text(text = "Down")
                            }
                        }
                    }
                }
            }
        }

    }
}