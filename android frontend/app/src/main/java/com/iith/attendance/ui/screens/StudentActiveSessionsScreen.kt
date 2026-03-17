package com.iith.attendance.ui.screens

import android.Manifest
import android.content.Context
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.common.InputImage
import com.iith.attendance.UserSession
import com.iith.attendance.ui.components.MainScaffold
import java.util.concurrent.Executors

@Composable
fun StudentActiveSessionsScreen(session: UserSession?, onLogout: () -> Unit, paddingValues: PaddingValues) {
    var step by remember { mutableStateOf(1) }
    var method by remember { mutableStateOf<String?>(null) }

    MainScaffold("Active Session", session, onLogout) { modifier ->
        Column(
            modifier = modifier
                .padding(paddingValues)
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Step $step / 3", style = MaterialTheme.typography.titleMedium)
            when (step) {
                1 -> SessionMethodStep(
                    onQR = { method = "QR"; step = 2 },
                    onBle = { method = "BLE"; step = 2 }
                )
                2 -> if (method == "QR") {
                    QRScanStep(onDone = { step = 3 })
                } else {
                    BLEStep(onDone = { step = 3 })
                }
                3 -> FaceCaptureStep(onDone = { step = 4 })
                else -> SuccessStep()
            }
        }
    }
}

@Composable
private fun SessionMethodStep(onQR: () -> Unit, onBle: () -> Unit) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Current active class: CS301 - Operating Systems") // TODO: replace with API call
            Button(onClick = onQR, modifier = Modifier.fillMaxWidth()) { Text("Mark via QR") }
            Button(onClick = onBle, modifier = Modifier.fillMaxWidth()) { Text("Mark via BLE") }
        }
    }
}

@Composable
private fun QRScanStep(onDone: () -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text("Step 1: Scan classroom QR")
        if (hasPermission) {
            QRPreview(onQRCodeDetected = onDone)
        } else {
            Text("Camera permission required")
        }
    }
}

@Composable
private fun BLEStep(onDone: () -> Unit) {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Step 1: BLE Detection")
            Text("Detecting professor beacon...") // TODO: replace with BLE scan implementation
            Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text("Beacon Detected (Mock)") }
        }
    }
}

@Composable
private fun FaceCaptureStep(onDone: () -> Unit) {
    var hasPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    LaunchedEffect(Unit) { permissionLauncher.launch(Manifest.permission.CAMERA) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text("Step 2: Face Capture Verification")
        if (hasPermission) {
            FaceCapturePreview(onDone)
        } else {
            Text("Camera permission required")
        }
    }
}

@Composable
private fun SuccessStep() {
    Card(shape = RoundedCornerShape(15.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(20.dp)) {
            Text("✅ Attendance Marked Successfully", style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable
private fun QRPreview(onQRCodeDetected: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val scanner = remember { BarcodeScanning.getClient() }
    val executor = remember { Executors.newSingleThreadExecutor() }

    AndroidView(
        modifier = Modifier
            .fillMaxWidth()
            .height(260.dp),
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }
                val analysis = ImageAnalysis.Builder().build().also { useCase ->
                    useCase.setAnalyzer(executor) { imageProxy ->
                        val mediaImage = imageProxy.image
                        if (mediaImage != null) {
                            val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
                            scanner.process(image)
                                .addOnSuccessListener { barcodes ->
                                    if (barcodes.isNotEmpty()) onQRCodeDetected()
                                }
                                .addOnFailureListener { Log.e("QR", "Scan fail", it) }
                                .addOnCompleteListener { imageProxy.close() }
                        } else imageProxy.close()
                    }
                }
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_BACK_CAMERA, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        }
    )
}

@Composable
private fun FaceCapturePreview(onCaptured: () -> Unit) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val imageCapture = remember { ImageCapture.Builder().build() }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            factory = { ctx ->
                val previewView = PreviewView(ctx)
                bindPreview(ctx, lifecycleOwner, previewView, imageCapture)
                previewView
            }
        )
        Button(onClick = onCaptured, modifier = Modifier.fillMaxWidth()) {
            Text("Capture Face (Mock)") // TODO: integrate backend face verification
        }
    }
}

private fun bindPreview(
    context: Context,
    lifecycleOwner: androidx.lifecycle.LifecycleOwner,
    previewView: PreviewView,
    imageCapture: ImageCapture
) {
    val providerFuture = ProcessCameraProvider.getInstance(context)
    providerFuture.addListener({
        val provider = providerFuture.get()
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = previewView.surfaceProvider
        }
        provider.unbindAll()
        provider.bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview, imageCapture)
    }, ContextCompat.getMainExecutor(context))
}
