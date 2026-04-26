package com.iith.attendanceapp

import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
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
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

private fun imageProxyToBase64(image: ImageProxy): String {
    val bitmap = image.toBitmap()
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
    image.close()
    return Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
}

// Reads frame as base64 WITHOUT closing the proxy (caller is responsible for closing)
private fun imageProxyToBase64Safe(image: ImageProxy): String? {
    return try {
        val bitmap = image.toBitmap()
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, stream)
        Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
    } catch (e: Exception) { null }
}

// ── Camera with ImageCapture ──────────────────────────────────────────────────
@Composable
fun CameraWithCapture(useFrontCamera: Boolean, modifier: Modifier = Modifier, onCaptureBound: (ImageCapture) -> Unit) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    AndroidView(
        factory = { ctx ->
            val previewView  = PreviewView(ctx)
            val imageCapture = ImageCapture.Builder().setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY).build()
            val preview      = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val selector     = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            ProcessCameraProvider.getInstance(ctx).addListener({
                val provider = ProcessCameraProvider.getInstance(ctx).get()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                onCaptureBound(imageCapture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

// ── Camera with ImageAnalysis (liveness) ─────────────────────────────────────
@Composable
fun CameraWithAnalysis(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onFrameCaptured: ((String) -> Unit)? = null,
    onAnalyzer: (ImageProxy) -> Unit
) {
    val context        = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val executor       = remember { Executors.newSingleThreadExecutor() }
    AndroidView(
        factory = { ctx ->
            val previewView = PreviewView(ctx)
            val preview     = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
            val analysis    = ImageAnalysis.Builder().setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST).build()
                .also { it.setAnalyzer(executor) { imageProxy ->
                    if (onFrameCaptured != null) {
                        val b64 = imageProxyToBase64Safe(imageProxy)
                        if (b64 != null) onFrameCaptured(b64)
                    }
                    onAnalyzer(imageProxy)
                } }
            val selector = if (useFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
            ProcessCameraProvider.getInstance(ctx).addListener({
                val provider = ProcessCameraProvider.getInstance(ctx).get()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

// ── FACE VERIFY SCREEN ────────────────────────────────────────────────────────
// Uses LivenessDetector + FaceAnalyzer from liveness folder.
// On liveness success → sends frames to /api/student/faceVerify → markAttendance
// If not enrolled → prompts user to upload photo via profile icon
@Composable
fun FaceCameraScreen(userId: String, sessionId: String, token: String, mode: String = "BLE", onSuccess: () -> Unit) {
    var promptText   by remember { mutableStateOf("Position your face in the oval") }
    var stepText     by remember { mutableStateOf("") }
    var progressFrac by remember { mutableStateOf(0f) }
    var loading      by remember { mutableStateOf(false) }
    var verifyStatus by remember { mutableStateOf<String?>(null) }
    var markingDone  by remember { mutableStateOf(false) }
    var livenessFail by remember { mutableStateOf<String?>(null) }

    val livenessEvent = remember { mutableStateOf<LivenessEvent>(LivenessEvent.Idle) }

    // Captured frames buffer — filled during liveness, sent to backend on success
    val capturedFrames = remember { mutableListOf<String>() }

    val detector = remember {
        LivenessDetector(
            onChallengeChanged = { challenge, index, total ->
                livenessEvent.value = LivenessEvent.PromptChanged(challenge.instruction, index, total)
            },
            onComplete = { result ->
                if (result.isLive) {
                    livenessEvent.value = LivenessEvent.Success(
                        capturedFrames.toList(),
                        result.completedChallenges.map { it.instruction }
                    )
                } else {
                    livenessEvent.value = LivenessEvent.Failed(result.failureReason ?: "Liveness check failed.")
                }
            },
            challengeCount    = 2,
            challengeTimeoutMs = 6000L
        )
    }

    val analyzer = remember {
        FaceAnalyzer { face ->
            detector.processFace(face)
        }
    }

    LaunchedEffect(Unit) { detector.start() }

    LaunchedEffect(livenessEvent.value) {
        when (val event = livenessEvent.value) {
            is LivenessEvent.PromptChanged -> {
                promptText   = event.text
                stepText     = "Step ${event.index} of ${event.total}"
                progressFrac = (event.index - 1f) / event.total
            }
            is LivenessEvent.Failed -> { livenessFail = event.reason }
            is LivenessEvent.Success -> {
                promptText = "Liveness verified! Verifying identity..."
                loading    = true
                val frames = event.frames.ifEmpty { listOf("") }
                apiFaceVerify(userId, frames, emptyList(), token) { result ->
                    loading      = false
                    verifyStatus = result.status
                    if (result.status == "verified") {
                        loading = true
                        apiMarkAttendance(userId, sessionId, mode, token) { markResult ->
                            loading     = false
                            markingDone = markResult.success
                        }
                    }
                }
            }
            is LivenessEvent.Idle -> {}
        }
    }

    WithCameraPermission {
        Column(modifier = Modifier.fillMaxSize().background(BGGray).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(16.dp))
            Text("Face Verification", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            // Result card
            if (verifyStatus != null) {
                val accepted  = verifyStatus == "verified"
                val cardColor = if (accepted) GGreen else Color.Red
                val label = when {
                    accepted                          -> if (markingDone) "Attendance Marked!" else "Verified! Marking attendance..."
                    verifyStatus == "no_face"         -> "No face detected."
                    verifyStatus == "user_not_found"  -> "Not enrolled.\nTap the profile icon (top right) to upload your photo."
                    verifyStatus == "failed"          -> "Verification Failed. Try again."
                    else                              -> "Error: $verifyStatus"
                }
                ResultCard(if (accepted) "✓" else "✗", label, cardColor, loading)
                Spacer(Modifier.height(24.dp))
                if (accepted && markingDone) {
                    Button(onClick = onSuccess, modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = GGreen)) {
                        Text("Done", fontWeight = FontWeight.Bold)
                    }
                } else if (!accepted) {
                    Button(onClick = {
                        verifyStatus = null; livenessFail = null; markingDone = false
                        capturedFrames.clear(); detector.start(); livenessEvent.value = LivenessEvent.Idle
                    }, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                        Text("Try Again", fontWeight = FontWeight.Bold)
                    }
                }
                return@WithCameraPermission
            }

            // Liveness failed
            if (livenessFail != null) {
                StatusCard(livenessFail!!, Color.Red)
                Spacer(Modifier.height(16.dp))
                Button(onClick = { livenessFail = null; capturedFrames.clear(); detector.start(); livenessEvent.value = LivenessEvent.Idle },
                    modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
                    Text("Retry", fontWeight = FontWeight.Bold)
                }
                return@WithCameraPermission
            }

            // Progress
            if (stepText.isNotBlank()) {
                Text(stepText, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(progress = { progressFrac },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = GBlue, trackColor = Color.LightGray)
                Spacer(Modifier.height(8.dp))
            }

            // Prompt
            Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                .background(GBlue.copy(alpha = 0.10f)).padding(12.dp), contentAlignment = Alignment.Center) {
                Text(promptText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = GBlue, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))

            // Camera
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)) {
                CameraWithAnalysis(
                    useFrontCamera = true,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp)),
                    onFrameCaptured = { b64 ->
                        if (capturedFrames.size < 3) capturedFrames.add(b64)
                    }
                ) { imageProxy ->
                    analyzer.analyze(imageProxy)
                }
            }

            if (loading) {
                Spacer(Modifier.height(16.dp))
                CircularProgressIndicator(color = GBlue)
                Spacer(Modifier.height(8.dp))
                Text("Processing...", fontSize = 13.sp, color = Color.Gray)
            }
        }
    }
}

// ── Liveness event ────────────────────────────────────────────────────────────
sealed class LivenessEvent {
    object Idle : LivenessEvent()
    data class PromptChanged(val text: String, val index: Int, val total: Int) : LivenessEvent()
    data class Success(val frames: List<String>, val challenges: List<String>) : LivenessEvent()
    data class Failed(val reason: String) : LivenessEvent()
}

// ── Shared UI helpers ─────────────────────────────────────────────────────────
@Composable
fun CaptureButton(loading: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp), colors = ButtonDefaults.buttonColors(containerColor = GBlue)) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp)); Text("Processing...", fontWeight = FontWeight.Bold)
        } else Text("Capture", fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun StatusCard(message: String, color: Color) {
    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp))
        .background(color.copy(alpha = 0.1f)).border(1.dp, color, RoundedCornerShape(12.dp)).padding(16.dp)) {
        Text(message, color = color, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
    }
}

@Composable
fun ResultCard(icon: String, label: String, color: Color, loading: Boolean) {
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp))
        .background(color.copy(alpha = 0.1f)).border(2.dp, color, RoundedCornerShape(16.dp)).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally) {
        if (loading) CircularProgressIndicator(color = color)
        else {
            Text(icon, fontSize = 48.sp, color = color)
            Spacer(Modifier.height(8.dp))
            Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = color, textAlign = TextAlign.Center)
        }
    }
}

// ── Liveness Test Screen (debug only — no backend calls) ─────────────────────
@Composable
fun LivenessTestScreen(onBack: () -> Unit) {
    var promptText   by remember { mutableStateOf("Position your face in the oval") }
    var stepText     by remember { mutableStateOf("") }
    var progressFrac by remember { mutableStateOf(0f) }
    var livenessFail by remember { mutableStateOf<String?>(null) }
    var livenessOk   by remember { mutableStateOf(false) }

    val livenessEvent = remember { mutableStateOf<LivenessEvent>(LivenessEvent.Idle) }

    val detector = remember {
        LivenessDetector(
            onChallengeChanged = { challenge, index, total ->
                livenessEvent.value = LivenessEvent.PromptChanged(challenge.instruction, index, total)
            },
            onComplete = { result ->
                if (result.isLive)
                    livenessEvent.value = LivenessEvent.Success(emptyList(), emptyList())
                else
                    livenessEvent.value = LivenessEvent.Failed(result.failureReason ?: "Liveness check failed.")
            },
            challengeCount     = 2,
            challengeTimeoutMs = 6000L
        )
    }

    val analyzer = remember { FaceAnalyzer { face -> detector.processFace(face) } }

    LaunchedEffect(Unit) { detector.start() }

    LaunchedEffect(livenessEvent.value) {
        when (val event = livenessEvent.value) {
            is LivenessEvent.PromptChanged -> {
                promptText   = event.text
                stepText     = "Step ${event.index} of ${event.total}"
                progressFrac = (event.index - 1f) / event.total
            }
            is LivenessEvent.Success -> { livenessOk = true }
            is LivenessEvent.Failed  -> { livenessFail = event.reason }
            is LivenessEvent.Idle    -> {}
        }
    }

    WithCameraPermission {
        Column(
            modifier = Modifier.fillMaxSize().background(BGGray).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onBack) { Text("← Back", color = GBlue) }
                Text("Liveness Test", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(8.dp))

            // Success
            if (livenessOk) {
                Spacer(Modifier.height(32.dp))
                ResultCard("✓", "Liveness Successful!", GGreen, false)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = {
                        livenessOk = false; livenessFail = null
                        livenessEvent.value = LivenessEvent.Idle; detector.start()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GBlue)
                ) { Text("Test Again", fontWeight = FontWeight.Bold) }
                return@WithCameraPermission
            }

            // Failed
            if (livenessFail != null) {
                Spacer(Modifier.height(32.dp))
                StatusCard(livenessFail!!, Color.Red)
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        livenessFail = null; livenessEvent.value = LivenessEvent.Idle; detector.start()
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = GBlue)
                ) { Text("Retry", fontWeight = FontWeight.Bold) }
                return@WithCameraPermission
            }

            // Progress
            if (stepText.isNotBlank()) {
                Text(stepText, fontSize = 12.sp, color = Color.Gray)
                Spacer(Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { progressFrac },
                    modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp)),
                    color = GBlue, trackColor = Color.LightGray
                )
                Spacer(Modifier.height(8.dp))
            }

            // Prompt
            Box(
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp))
                    .background(GBlue.copy(alpha = 0.10f)).padding(12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(promptText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold,
                    color = GBlue, textAlign = TextAlign.Center)
            }
            Spacer(Modifier.height(12.dp))

            // Camera
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(0.85f)) {
                CameraWithAnalysis(
                    useFrontCamera = true,
                    modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(16.dp))
                ) { imageProxy -> analyzer.analyze(imageProxy) }
            }
        }
    }
}
