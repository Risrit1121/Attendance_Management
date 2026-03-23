package com.iith.attendance.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.util.Size
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.iith.attendance.data.models.ActiveSession
import com.iith.attendance.data.models.AttendanceMode
import com.iith.attendance.ui.components.*
import com.iith.attendance.ui.theme.*

// ── Mock sessions ─────────────────────────────────────────────────────────────

private val mockSessions = listOf(
    ActiveSession("s1", "Signals & Systems",   "EE2030", "Room 201", "Dr. Sharma",   AttendanceMode.QR,  "11:50 AM"),
    ActiveSession("s2", "Digital Electronics", "EC2010", "Room 305", "Dr. Reddy",    AttendanceMode.BLE, "01:50 PM"),
)

// ── Active sessions list ──────────────────────────────────────────────────────

@Composable
fun ActiveSessionsScreen() {
    var flowSession by remember { mutableStateOf<ActiveSession?>(null) }

    if (flowSession != null) {
        MarkAttendanceFlow(session = flowSession!!, onDone = { flowSession = null })
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SurfaceGray)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SectionHeader("Active Sessions")

        if (mockSessions.isEmpty()) {
            Box(Modifier.fillMaxWidth().padding(top = 60.dp), contentAlignment = Alignment.Center) {
                Text("No active sessions right now.", color = Color.Gray)
            }
        }

        // TODO: Replace with LiveData / API
        mockSessions.forEach { session ->
            SessionCard(session) { flowSession = session }
        }
    }
}

@Composable
private fun SessionCard(session: ActiveSession, onMark: () -> Unit) {
    val modeColor = if (session.mode == AttendanceMode.QR) Color(0xFF34A853) else IITHBlue

    Card(
        modifier  = Modifier.fillMaxWidth().clickable(onClick = onMark),
        shape     = RoundedCornerShape(14.dp),
        elevation = CardDefaults.cardElevation(3.dp),
        colors    = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(shape = RoundedCornerShape(10.dp), color = modeColor.copy(alpha = 0.12f)) {
                Icon(
                    if (session.mode == AttendanceMode.QR) Icons.Default.QrCode else Icons.Default.Bluetooth,
                    null,
                    tint     = modeColor,
                    modifier = Modifier.padding(10.dp).size(24.dp)
                )
            }
            Spacer(Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(session.courseName, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("${session.courseCode} · ${session.room}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Text("Prof. ${session.professorName}", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ModeChip(session.mode.name)
                    Text("Ends ${session.endsAt}", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                }
            }
            Icon(Icons.Default.ChevronRight, null, tint = Color.LightGray)
        }
    }
}

// ── Mark Attendance Flow (Step 1: QR/BLE  →  Step 2: Face  →  Done) ──────────

@Composable
fun MarkAttendanceFlow(session: ActiveSession, onDone: () -> Unit) {
    var step by remember { mutableIntStateOf(0) }  // 0=scan, 1=face, 2=done

    when (step) {
        0 -> if (session.mode == AttendanceMode.QR)
                QRScanStep(onSuccess = { step = 1 }, onBack = onDone)
             else
                BLEScanStep(onSuccess = { step = 1 }, onBack = onDone)
        1 -> FaceCaptureStep(onSuccess = { step = 2 }, onBack = { step = 0 })
        2 -> SuccessStep(onDone = onDone)
    }
}

// ────────────────────────────────────────────────────────────────────────────
// STEP 1a: QR Scan
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun QRScanStep(onSuccess: () -> Unit, onBack: () -> Unit) {
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IITHBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!camPerm.status.isGranted) {
                PermissionRequest("Camera permission is needed to scan QR codes.") { camPerm.launchPermissionRequest() }
            } else {
                QRCameraPreview(onQRScanned = { onSuccess() })
                Box(
                    modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    StepDots(total = 2, current = 0)
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// STEP 1b: BLE Scan
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun BLEScanStep(onSuccess: () -> Unit, onBack: () -> Unit) {
    val context   = LocalContext.current
    var scanning  by remember { mutableStateOf(false) }
    var countdown by remember { mutableIntStateOf(3) }
    var result    by remember { mutableStateOf<String?>(null) }
    var rssiList  by remember { mutableStateOf(listOf<Int>()) }

    // Permissions needed: BLUETOOTH_SCAN + ACCESS_FINE_LOCATION
    val blePerm  = rememberPermissionState(Manifest.permission.BLUETOOTH_SCAN)
    val locPerm  = rememberPermissionState(Manifest.permission.ACCESS_FINE_LOCATION)

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("BLE Scan") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IITHBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            StepDots(total = 2, current = 0)

            Spacer(Modifier.height(16.dp))

            // Beacon indicator
            Surface(
                shape  = CircleShape,
                color  = if (scanning) IITHBlue else Color(0xFFEEEEEE),
                modifier = Modifier.size(120.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Bluetooth,
                        null,
                        tint     = if (scanning) Color.White else Color.Gray,
                        modifier = Modifier.size(56.dp)
                    )
                }
            }

            Text(
                if (scanning) "Scanning... ($countdown s)" else "Tap to scan for BLE beacon",
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyLarge,
                color = if (scanning) IITHBlue else Color.Gray
            )

            result?.let {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors   = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Beacon Detected", fontWeight = FontWeight.Bold, color = AttendanceGreen)
                        Text(it, style = MaterialTheme.typography.bodySmall)
                        if (rssiList.isNotEmpty()) {
                            Text(
                                "Mean RSSI: ${rssiList.average().toInt()} dBm",
                                style = MaterialTheme.typography.bodySmall,
                                color = Color.Gray
                            )
                        }
                    }
                }

                Button(
                    onClick  = { onSuccess() },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(12.dp)
                ) { Text("Continue to Face Verify") }
            }

            if (result == null) {
                if (!blePerm.status.isGranted || !locPerm.status.isGranted) {
                    PermissionRequest("Bluetooth & Location permissions are required for BLE scanning.") {
                        blePerm.launchPermissionRequest()
                        locPerm.launchPermissionRequest()
                    }
                } else {
                    Button(
                        onClick  = {
                            scanning  = true
                            countdown = 3
                            rssiList  = emptyList()
                            // TODO: Start BleScanner.scan(context) in a coroutine,
                            //       collect BeaconResult for 3 seconds,
                            //       compute mean RSSI, then POST to backend.
                            // Simulated result after countdown:
                        },
                        enabled  = !scanning,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp)
                    ) { Text("Scan Beacon") }
                }
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────────────
// STEP 2: Face Capture
// ────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FaceCaptureStep(onSuccess: () -> Unit, onBack: () -> Unit) {
    val camPerm = rememberPermissionState(Manifest.permission.CAMERA)

    Scaffold(
        topBar = {
            @OptIn(ExperimentalMaterial3Api::class)
            TopAppBar(
                title = { Text("Face Verification") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, null) } },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = IITHBlue, titleContentColor = Color.White, navigationIconContentColor = Color.White)
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (!camPerm.status.isGranted) {
                PermissionRequest("Camera permission is required for face verification.") { camPerm.launchPermissionRequest() }
            } else {
                FrontCameraPreview()
                Column(
                    modifier = Modifier.fillMaxSize().padding(24.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    StepDots(total = 2, current = 1)
                    Button(
                        onClick  = {
                            // TODO: Capture photo + POST to backend face verification endpoint
                            onSuccess()
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape    = RoundedCornerShape(12.dp),
                        colors   = ButtonDefaults.buttonColors(containerColor = IITHBlue)
                    ) {
                        Icon(Icons.Default.Camera, null)
                        Spacer(Modifier.width(8.dp))
                        Text("Capture & Verify")
                    }
                }
            }
        }
    }
}

// ── Success screen ────────────────────────────────────────────────────────────

@Composable
fun SuccessStep(onDone: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().background(SurfaceGray).padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.CheckCircle,
            null,
            tint     = AttendanceGreen,
            modifier = Modifier.size(96.dp)
        )
        Spacer(Modifier.height(24.dp))
        Text("Attendance Marked!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Text("Your attendance has been recorded successfully.", textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(32.dp))
        Button(
            onClick  = onDone,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape    = RoundedCornerShape(12.dp)
        ) { Text("Done") }
    }
}

// ── Camera helpers ────────────────────────────────────────────────────────────

@Composable
fun QRCameraPreview(onQRScanned: (String) -> Unit) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var scanned       by remember { mutableStateOf(false) }

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val provider = cameraProviderFuture.get()
                val preview  = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                val imageAnalysis = ImageAnalysis.Builder()
                    .setTargetResolution(Size(1280, 720))
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .build()

                val scanner = BarcodeScanning.getClient()
                imageAnalysis.setAnalyzer(ContextCompat.getMainExecutor(ctx)) { proxy ->
                    if (scanned) { proxy.close(); return@setAnalyzer }
                    @SuppressLint("UnsafeOptInUsageError")
                    val mediaImage = proxy.image
                    if (mediaImage != null) {
                        val image = InputImage.fromMediaImage(mediaImage, proxy.imageInfo.rotationDegrees)
                        scanner.process(image)
                            .addOnSuccessListener { barcodes ->
                                barcodes.firstOrNull()?.rawValue?.let { value ->
                                    if (!scanned) { scanned = true; onQRScanned(value) }
                                }
                            }
                            .addOnCompleteListener { proxy.close() }
                    } else proxy.close()
                }

                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_BACK_CAMERA,
                        preview, imageAnalysis
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

@Composable
fun FrontCameraPreview() {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val future      = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                val preview  = Preview.Builder().build().also {
                    it.setSurfaceProvider(previewView.surfaceProvider)
                }
                try {
                    provider.unbindAll()
                    provider.bindToLifecycle(
                        lifecycleOwner,
                        CameraSelector.DEFAULT_FRONT_CAMERA,
                        preview
                    )
                } catch (e: Exception) { e.printStackTrace() }
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = Modifier.fillMaxSize()
    )
}

// ── Permission helper ─────────────────────────────────────────────────────────

@Composable
fun PermissionRequest(rationale: String, onRequest: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Warning, null, tint = AttendanceOrange, modifier = Modifier.size(64.dp))
        Spacer(Modifier.height(16.dp))
        Text(rationale, textAlign = TextAlign.Center, color = Color.Gray)
        Spacer(Modifier.height(24.dp))
        Button(onClick = onRequest) { Text("Grant Permission") }
    }
}
