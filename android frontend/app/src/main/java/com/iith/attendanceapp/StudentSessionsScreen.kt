package com.iith.attendanceapp

import android.Manifest
import android.bluetooth.BluetoothManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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

// ── Sessions list ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudentSessionsScreen(userId: String, token: String) {
    var sessions       by remember { mutableStateOf<List<ActiveSessionItem>>(emptyList()) }
    var loading        by remember { mutableStateOf(true) }
    var errorMsg       by remember { mutableStateOf<String?>(null) }
    var markingSession by remember { mutableStateOf<ActiveSessionItem?>(null) }
    var refreshing     by remember { mutableStateOf(false) }

    fun load() {
        apiGetStudentSessions(userId, token) { result, err ->
            loading = false; refreshing = false
            if (result != null) sessions = result else errorMsg = err
        }
    }

    LaunchedEffect(userId) { load() }

    if (markingSession != null) {
        MarkAttendanceFlow(
            session = markingSession!!,
            userId  = userId,
            token   = token,
            onDone  = { markingSession = null; refreshing = true; load() }
        )
        return
    }

    PullToRefreshBox(isRefreshing = refreshing, onRefresh = { refreshing = true; load() }) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(BGGray)
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            when {
                loading -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = GBlue)
                }
                errorMsg != null -> Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text("Error: $errorMsg", color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center)
                }
                sessions.isEmpty() -> Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("No active sessions right now.\nPull down to refresh.", color = Color.Gray,
                        textAlign = TextAlign.Center)
                }
                else -> sessions.forEach { session ->
                    LiveSessionCard(session = session, onMark = { markingSession = session })
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun LiveSessionCard(session: ActiveSessionItem, onMark: () -> Unit) {
    val expiryMs    = session.startedAt + session.durationSeconds * 1000L
    var timeLeft by remember {
        mutableStateOf(((expiryMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0))
    }
    LaunchedEffect(session.sessionId) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft = ((expiryMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)
        }
    }

    BannerCard(title = session.courseName, bannerColor = GBlue, modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(Color.Red))
            Spacer(Modifier.width(6.dp))
            Text("Currently Active", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.Red)
        }
        Spacer(Modifier.height(6.dp))
        Text("${session.courseCode}  •  ${session.room}", fontSize = 14.sp, color = Color.Gray)
        Spacer(Modifier.height(4.dp))
        Text(
            "Mode: ${session.mode}  •  Expires in %d:%02d".format(timeLeft / 60, timeLeft % 60),
            fontSize = 12.sp,
            color = if (timeLeft > 30) GBlue else Color.Red,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(Modifier.height(10.dp))
        Divider()
        Spacer(Modifier.height(10.dp))
        Button(
            onClick = onMark,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = GBlue)
        ) { Text("Mark Attendance", fontWeight = FontWeight.Bold) }
    }
}

// ── Mark Attendance Flow ──────────────────────────────────────────────────────
@Composable
fun MarkAttendanceFlow(session: ActiveSessionItem, userId: String, token: String, onDone: () -> Unit) {
    var step  by remember { mutableStateOf(1) }
    val isBLE = session.mode == "BLE"

    Column(modifier = Modifier.fillMaxSize().background(BGGray), horizontalAlignment = Alignment.CenterHorizontally) {
        Spacer(Modifier.height(24.dp))
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 40.dp), verticalAlignment = Alignment.CenterVertically) {
            StepDot(1, if (isBLE) "BLE" else "QR", step >= 1)
            Divider(modifier = Modifier.weight(1f), color = if (step >= 2) GBlue else Color.LightGray, thickness = 2.dp)
            StepDot(2, "Face Verify", step >= 2)
        }
        Spacer(Modifier.height(32.dp))

        if (step == 1) {
            if (isBLE) StudentBLEScreen(session = session, onSuccess = { step = 2 })
            else QRCameraScreen(session = session!!, onSuccess = { step = 2 })
        } else {
            FaceCameraScreen(userId = userId, sessionId = session.sessionId, token = token, mode = session.mode, onSuccess = onDone)
        }
    }
}

// ── Step Dot ──────────────────────────────────────────────────────────────────
@Composable
fun StepDot(num: Int, label: String, active: Boolean) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(modifier = Modifier.size(32.dp).clip(CircleShape).background(if (active) GBlue else Color.LightGray),
            contentAlignment = Alignment.Center) {
            Text("$num", color = Color.White, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(4.dp))
        Text(label, fontSize = 11.sp, color = if (active) GBlue else Color.Gray)
    }
}

// ── Camera permission helper ──────────────────────────────────────────────────
@Composable
fun WithCameraPermission(content: @Composable () -> Unit) {
    val context = LocalContext.current
    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    if (granted) content()
    else {
        LaunchedEffect(Unit) { launcher.launch(Manifest.permission.CAMERA) }
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Camera permission required", color = Color.Gray)
        }
    }
}

// ── Live camera preview ───────────────────────────────────────────────────────
@Composable
fun CameraPreview(useFrontCamera: Boolean, modifier: Modifier = Modifier) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            ProcessCameraProvider.getInstance(ctx).addListener({
                val provider = ProcessCameraProvider.getInstance(ctx).get()
                val preview  = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
                val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

// ── QR Camera Screen — auto-scans using ML Kit barcode detection ──────────────
@androidx.annotation.OptIn(androidx.camera.core.ExperimentalGetImage::class)
@Composable
fun QRCameraScreen(session: ActiveSessionItem, onSuccess: () -> Unit) {
    var validated    by remember { mutableStateOf(false) }
    var validating   by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    val context      = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor     = remember { java.util.concurrent.Executors.newSingleThreadExecutor() }
    // AtomicBoolean so the analyzer thread sees the flag immediately without waiting for recomposition
    val scanLock     = remember { java.util.concurrent.atomic.AtomicBoolean(false) }

    // ML Kit barcode scanner
    val scanner = remember {
        com.google.mlkit.vision.barcode.BarcodeScanning.getClient(
            com.google.mlkit.vision.barcode.BarcodeScannerOptions.Builder()
                .setBarcodeFormats(com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE)
                .build()
        )
    }

    WithCameraPermission {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // Camera with ImageAnalysis for real-time QR detection
            AndroidView(
                factory = { ctx ->
                    val previewView = PreviewView(ctx)
                    val preview = androidx.camera.core.Preview.Builder().build()
                        .also { it.setSurfaceProvider(previewView.surfaceProvider) }

                    val analysis = androidx.camera.core.ImageAnalysis.Builder()
                        .setBackpressureStrategy(androidx.camera.core.ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                        .build()
                        .also { ia ->
                            ia.setAnalyzer(executor) { imageProxy ->
                                if (scanLock.get()) { imageProxy.close(); return@setAnalyzer }
                                val mediaImage = imageProxy.image
                                if (mediaImage != null) {
                                    val image = com.google.mlkit.vision.common.InputImage
                                        .fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                                    scanner.process(image)
                                        .addOnSuccessListener { barcodes ->
                                            val qr = barcodes.firstOrNull()?.rawValue
                                            if (qr != null && scanLock.compareAndSet(false, true)) {
                                                validating = true
                                                errorMsg   = null
                                                apiQrValidate(session.room, qr) { valid ->
                                                    validating = false
                                                    if (valid) validated = true
                                                    else { scanLock.set(false); errorMsg = "Invalid QR. Please try again." }
                                                }
                                            }
                                        }
                                        .addOnCompleteListener { imageProxy.close() }
                                } else {
                                    imageProxy.close()
                                }
                            }
                        }

                    ProcessCameraProvider.getInstance(ctx).addListener({
                        val provider = ProcessCameraProvider.getInstance(ctx).get()
                        provider.unbindAll()
                        provider.bindToLifecycle(
                            lifecycleOwner,
                            CameraSelector.DEFAULT_BACK_CAMERA,
                            preview, analysis
                        )
                    }, ContextCompat.getMainExecutor(ctx))
                    previewView
                },
                modifier = Modifier.fillMaxSize()
            )

            // Scanning frame overlay
            Box(
                modifier = Modifier
                    .size(240.dp)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Transparent)
            ) {
                // Corner markers
                val cornerColor = if (validated) GGreen else GBlue
                val cornerSize  = 32.dp
                val strokeWidth = 4.dp
                // Top-left
                Box(Modifier.size(cornerSize).align(Alignment.TopStart).background(Color.Transparent)) {
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.width(cornerSize))
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.height(cornerSize).width(strokeWidth))
                }
                // Top-right
                Box(Modifier.size(cornerSize).align(Alignment.TopEnd).background(Color.Transparent)) {
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.width(cornerSize).align(Alignment.TopEnd))
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.height(cornerSize).width(strokeWidth).align(Alignment.TopEnd))
                }
                // Bottom-left
                Box(Modifier.size(cornerSize).align(Alignment.BottomStart).background(Color.Transparent)) {
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.width(cornerSize).align(Alignment.BottomStart))
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.height(cornerSize).width(strokeWidth))
                }
                // Bottom-right
                Box(Modifier.size(cornerSize).align(Alignment.BottomEnd).background(Color.Transparent)) {
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.width(cornerSize).align(Alignment.BottomEnd))
                    Divider(color = cornerColor, thickness = strokeWidth, modifier = Modifier.height(cornerSize).width(strokeWidth).align(Alignment.BottomEnd))
                }
            }

            // Status text
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.55f))
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when {
                    validated -> {
                        Text("✓ QR Verified!", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = GGreen)
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSuccess,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = GGreen)
                        ) { Text("Proceed to Face Verify →", fontWeight = FontWeight.Bold) }
                    }
                    validating -> {
                        CircularProgressIndicator(color = GBlue, modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(8.dp))
                        Text("Validating QR...", fontSize = 14.sp, color = Color.White)
                    }
                    errorMsg != null -> {
                        Text(errorMsg!!, fontSize = 13.sp, color = Color.Red, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(8.dp))
                        Text("Point camera at the QR code", fontSize = 13.sp, color = Color.White.copy(alpha = 0.7f))
                    }
                    else -> {
                        Text("Point camera at the QR code shown by professor",
                            fontSize = 13.sp, color = Color.White, textAlign = TextAlign.Center)
                        Spacer(Modifier.height(4.dp))
                        Text("Class: ${session.room}",
                            fontSize = 11.sp, color = Color.White.copy(alpha = 0.5f))
                    }
                }
            }
        }
    }
}

// ── BLE Student Screen ────────────────────────────────────────────────────────
private const val TARGET_UUID = "49495448-2d41-5454-454e-44414e434520"

@Composable
fun StudentBLEScreen(session: ActiveSessionItem, onSuccess: () -> Unit) {
    val expiryMs = session.startedAt + session.durationSeconds * 1000L
    var timeLeft  by remember { mutableStateOf(((expiryMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0)) }
    var scanning  by remember { mutableStateOf(false) }
    var validating by remember { mutableStateOf(false) }
    var results   by remember { mutableStateOf<List<BleBeaconResult>>(emptyList()) }
    var errorMsg  by remember { mutableStateOf<String?>(null) }
    val context   = LocalContext.current

    val blePermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT, Manifest.permission.ACCESS_FINE_LOCATION)
    else arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

    var permissionsGranted by remember {
        mutableStateOf(blePermissions.all { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED })
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        grants -> permissionsGranted = grants.values.all { it }
    }

    LaunchedEffect(session.sessionId) {
        while (timeLeft > 0) { delay(1000); timeLeft = ((expiryMs - System.currentTimeMillis()) / 1000).toInt().coerceAtLeast(0) }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "ble")
    val alpha by infiniteTransition.animateFloat(0.3f, 1f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "alpha")

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        Text("BLE Scan", fontSize = 18.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(8.dp))
        Text("Stay in the classroom. Tap Scan to detect the BLE beacon.",
            fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
        Spacer(Modifier.height(32.dp))

        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            CircularProgressIndicator(progress = { timeLeft.toFloat() / session.durationSeconds },
                modifier = Modifier.size(120.dp), color = GPurple, trackColor = Color.LightGray, strokeWidth = 8.dp)
            Text("%d:%02d".format(timeLeft / 60, timeLeft % 60), fontSize = 24.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.height(24.dp))

        Box(modifier = Modifier.size(70.dp).clip(CircleShape).background(GPurple.copy(alpha = if (scanning) alpha else 0.3f)),
            contentAlignment = Alignment.Center) {
            Text("BLE", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(24.dp))

        Button(
            onClick = {
                if (!permissionsGranted) { permLauncher.launch(blePermissions); return@Button }
                val btManager = context.getSystemService(android.content.Context.BLUETOOTH_SERVICE) as BluetoothManager
                if (btManager.adapter?.isEnabled == false) { errorMsg = "Bluetooth is turned off."; return@Button }
                scanning = true; errorMsg = null; results = emptyList()
                startBleScan(context, TARGET_UUID) { found ->
                    scanning = false
                    if (found.isEmpty()) { errorMsg = "No beacon found with the target UUID."; return@startBleScan }
                    results = found
                    validating = true
                    apiBleValidate(classId = session.room, beacons = found) { validateResult ->
                        validating = false
                        if (!validateResult.valid) { errorMsg = "BLE validation failed: not in the correct classroom."; results = emptyList() }
                    }
                }
            },
            enabled  = !scanning && !validating,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(10.dp),
            colors   = ButtonDefaults.buttonColors(containerColor = GPurple)
        ) {
            when {
                scanning   -> { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Scanning for 3s...") }
                validating -> { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(10.dp)); Text("Validating...") }
                else       -> Text("Scan", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }

        if (errorMsg != null) { Spacer(Modifier.height(12.dp)); Text(errorMsg!!, color = Color.Red, fontSize = 13.sp, textAlign = TextAlign.Center) }

        if (results.isNotEmpty() && errorMsg == null) {
            Spacer(Modifier.height(20.dp))
            Text("Beacon Validated \u2713", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = GPurple)
            Spacer(Modifier.height(10.dp))
            results.forEach { r ->
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color.White).padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    BleRow("UUID", r.uuid); BleRow("Major", "${r.major}"); BleRow("Minor", "${r.minor}")
                    BleRow("Avg RSSI", "${"%.1f".format(r.avgRssi)} dBm")
                }
                Spacer(Modifier.height(8.dp))
            }
            Spacer(Modifier.height(16.dp))
            Button(onClick = onSuccess, modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = GPurple)) {
                Text("Proceed to Face Verify \u2192", fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun BleRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text("$label: ", fontSize = 13.sp, color = Color.Gray, fontWeight = FontWeight.SemiBold, modifier = Modifier.width(80.dp))
        Text(value, fontSize = 13.sp, color = Color.Black)
    }
}
