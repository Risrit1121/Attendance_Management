package com.iith.attendanceapp

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

data class ActiveSession(val title: String, val subtitle: String, val mode: String)

// ── Sessions list ─────────────────────────────────────────────────────────────
@Composable
fun StudentSessionsScreen(userId: String) {
    val sessions = listOf(
        ActiveSession("Swift App Dev",    "Prof. Smith  •  Room 304", "QR Scan"),
        ActiveSession("Machine Learning", "Prof. Rao  •  Room 101",   "BLE Scan"),
    )
    var markingSession by remember { mutableStateOf<ActiveSession?>(null) }

    if (markingSession != null) {
        MarkAttendanceFlow(
            session = markingSession!!,
            userId  = userId,
            onDone  = { markingSession = null }
        )
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BGGray)
            .verticalScroll(rememberScrollState())
            .padding(vertical = 16.dp)
    ) {
        sessions.forEach { session ->
            SessionCard(session = session, onMark = { markingSession = session })
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
fun SessionCard(session: ActiveSession, onMark: () -> Unit) {
    BannerCard(title = session.title, bannerColor = GBlue, modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
            Spacer(Modifier.width(6.dp))
            Text("Currently Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        Spacer(Modifier.height(6.dp))
        Text(session.subtitle, fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text("Mode: ${session.mode}", fontSize = 12.sp, color = GBlue, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onMark,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GBlue)
        ) { Text("Mark Attendance", fontWeight = FontWeight.Bold) }
    }
}

// ── Mark Attendance Flow ──────────────────────────────────────────────────────
@Composable
fun MarkAttendanceFlow(session: ActiveSession, userId: String, onDone: () -> Unit) {
    var step by remember { mutableStateOf(1) }
    val isBLE = session.mode == "BLE Scan"

    Column(
        modifier = Modifier.fillMaxSize().background(BGGray),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepDot(num = 1, label = session.mode, active = step >= 1)
            Divider(
                modifier = Modifier.weight(1f),
                color = if (step >= 2) GBlue else Color.LightGray,
                thickness = 2.dp
            )
            StepDot(num = 2, label = "Face Verify", active = step >= 2)
        }

        Spacer(Modifier.height(32.dp))

        if (step == 1) {
            if (isBLE) {
                StudentBLEScreen(onSuccess = { step = 2 })
            } else {
                QRCameraScreen(onSuccess = { step = 2 })
            }
        } else {
            // FaceCameraScreen is defined in FaceScreens.kt
            FaceCameraScreen(userId = userId, onSuccess = onDone)
        }
    }
}

// ── Step Dot ──────────────────────────────────────────────────────────────────
@Composable
fun StepDot(num: Int, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape)
                .background(if (active) GBlue else Color.LightGray),
            contentAlignment = Alignment.Center
        ) { Text("$num", color = Color.White, fontWeight = FontWeight.Bold) }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (active) GBlue else Color.Gray)
    }
}

// ── Camera permission helper ──────────────────────────────────────────────────
@Composable
fun WithCameraPermission(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }

    if (granted) {
        content()
    } else {
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required", color = Color.Gray)
        }
    }
}

// ── Live camera preview (preview only, no capture) ───────────────────────────
@Composable
fun CameraPreview(useFrontCamera: Boolean, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val selector = if (useFrontCamera)
                    CameraSelector.DEFAULT_FRONT_CAMERA
                else
                    CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

// ── QR Camera Screen (rear camera) ───────────────────────────────────────────
@Composable
fun QRCameraScreen(onSuccess: () -> Unit) {
    WithCameraPermission {
        Column(
            modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Scan QR Code", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text("Point camera at the QR shown by professor", fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            Spacer(Modifier.height(16.dp))

            CameraPreview(
                useFrontCamera = false,
                modifier = Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp))
            )

            Spacer(Modifier.height(24.dp))
            // TODO: replace with actual QR scan + backend verification
            Button(
                onClick = onSuccess,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GBlue)
            ) { Text("QR Verified \u2713  \u2192  Face Verify", fontWeight = FontWeight.Bold) }
        }
    }
}

// ── BLE Student Screen ────────────────────────────────────────────────────────
private const val TARGET_UUID = "49495448-2d41-5454-454e-44414e434520"

@Composable
fun StudentBLEScreen(onSuccess: () -> Unit) {
    var timeLeft  by remember { mutableStateOf(120) }
    var scanning  by remember { mutableStateOf(false) }
    var results   by remember { mutableStateOf<List<BleBeaconResult>>(emptyList()) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    val context   = LocalContext.current

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
    } else {
        arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
    }

    var permissionsGranted by remember {
        mutableStateOf(
            blePermissions.all {
                ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED
            }
        )
    }
    val permLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants -> permissionsGranted = grants.values.all { it } }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) { delay(1000); timeLeft-- }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ble")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f, targetValue = 1f, label = "alpha",
        animationSpec = infiniteRepeatable(tween(800), RepeatMode.Reverse)
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("BLE Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text(
            "Stay in the classroom. Tap Scan to detect the BLE beacon.",
            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(
                progress = { timeLeft / 120f },
                modifier = Modifier.size(120.dp),
                color = GPurple,
                trackColor = Color.LightGray,
                strokeWidth = 8.dp
            )
            Text("%d:%02d".format(timeLeft / 60, timeLeft % 60), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(24.dp))

        Box(
            modifier = Modifier.size(70.dp).clip(CircleShape)
                .background(GPurple.copy(alpha = if (scanning) alpha else 0.3f)),
            contentAlignment = Alignment.Center
        ) { Text("BLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp) }

        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!permissionsGranted) {
                    permLauncher.launch(blePermissions)
                } else {
                    val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
                    if (btManager.adapter?.isEnabled == false) {
                        errorMsg = "Bluetooth is turned off. Please enable it."
                    } else {
                        scanning = true
                        errorMsg = null
                        results  = emptyList()
                        startBleScan(context, TARGET_UUID) { found ->
                            scanning = false
                            results  = found
                            if (found.isEmpty()) errorMsg = "No beacon found with the target UUID."
                        }
                    }
                }
            },
            enabled = !scanning,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GPurple)
        ) {
            if (scanning) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                Spacer(Modifier.width(10.dp))
                Text("Scanning for 3s...", fontWeight = FontWeight.Bold)
            } else {
                Text("Scan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (errorMsg != null) {
            Spacer(Modifier.height(12.dp))
            Text(errorMsg!!, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
        }

        if (results.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Text("Beacon Detected", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GPurple)
            Spacer(Modifier.height(10.dp))
            results.forEach { r ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    BleResultRow("UUID",     r.uuid)
                    BleResultRow("Major",    "${r.major}")
                    BleResultRow("Minor",    "${r.minor}")
                    BleResultRow("Avg RSSI", "${"%.1f".format(r.avgRssi)} dBm")
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onSuccess,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = GPurple)
            ) { Text("BLE Verified \u2713  \u2192  Face Verify", fontWeight = FontWeight.Bold) }
        }

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BleResultRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 13.sp, color = Color.Black)
    }
}
