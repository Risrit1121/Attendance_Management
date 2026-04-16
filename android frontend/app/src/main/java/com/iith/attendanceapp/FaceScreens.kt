package com.iith.attendanceapp

import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.CameraSelector
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

// ── Converts ImageProxy → base64 JPEG string ─────────────────────────────────
private fun imageProxyToBase64(image: ImageProxy): String {
    val bitmap = image.toBitmap()
    val stream = ByteArrayOutputStream()
    bitmap.compress(Bitmap.CompressFormat.JPEG, 85, stream)
    val bytes = stream.toByteArray()
    image.close()
    return Base64.encodeToString(bytes, Base64.NO_WRAP)
}

// ── Camera preview + capture composable ──────────────────────────────────────
@Composable
fun CameraWithCapture(
    useFrontCamera: Boolean,
    modifier: Modifier = Modifier,
    onCaptureBound: (ImageCapture) -> Unit
) {
    val context       = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    AndroidView(
        factory = { ctx ->
            val previewView   = PreviewView(ctx)
            val imageCapture  = ImageCapture.Builder()
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .build()
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }
            val selector = if (useFrontCamera)
                CameraSelector.DEFAULT_FRONT_CAMERA
            else
                CameraSelector.DEFAULT_BACK_CAMERA

            val future = ProcessCameraProvider.getInstance(ctx)
            future.addListener({
                val provider = future.get()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, imageCapture)
                onCaptureBound(imageCapture)
            }, ContextCompat.getMainExecutor(ctx))
            previewView
        },
        modifier = modifier
    )
}

// ── ENROLL SCREEN ─────────────────────────────────────────────────────────────
// Takes one straight-face photo and sends to /enroll
@Composable
fun FaceEnrollScreen(userId: String, onDone: () -> Unit, onBack: () -> Unit) {
    var imageCapture  by remember { mutableStateOf<ImageCapture?>(null) }
    var status        by remember { mutableStateOf<String?>(null) }   // null = idle
    var loading       by remember { mutableStateOf(false) }
    val executor      = remember { Executors.newSingleThreadExecutor() }
    val context       = LocalContext.current

    WithCameraPermission {
        Column(
            modifier = Modifier.fillMaxSize().background(BGGray).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) {
                Text("← Back", color = GBlue)
            }
            Text("Face Enrollment", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                "Look straight at the camera and tap Capture.",
                fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))

            CameraWithCapture(
                useFrontCamera = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(16.dp)),
                onCaptureBound = { imageCapture = it }
            )

            Spacer(Modifier.height(24.dp))

            when {
                status == "enrolled" -> {
                    StatusCard("✓ Enrolled successfully!", GGreen)
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GBlue)
                    ) { Text("Continue to App", fontWeight = FontWeight.Bold) }
                }
                status == "no_face" -> {
                    StatusCard("No face detected. Please try again.", Color.Red)
                    Spacer(Modifier.height(16.dp))
                    CaptureButton(loading) {
                        captureAndEnroll(imageCapture, executor, userId) { s ->
                            loading = false; status = s
                        }
                        loading = true; status = null
                    }
                }
                status?.startsWith("error") == true -> {
                    StatusCard("Error: $status\n\nCheck BACKEND_BASE_URL in FaceApiClient.kt", Color.Red)
                    Spacer(Modifier.height(16.dp))
                    CaptureButton(loading) {
                        captureAndEnroll(imageCapture, executor, userId) { s ->
                            loading = false; status = s
                        }
                        loading = true; status = null
                    }
                }
                else -> CaptureButton(loading) {
                    captureAndEnroll(imageCapture, executor, userId) { s ->
                        loading = false; status = s
                    }
                    loading = true; status = null
                }
            }
        }
    }
}

private fun captureAndEnroll(
    imageCapture: ImageCapture?,
    executor: java.util.concurrent.Executor,
    userId: String,
    onStatus: (String) -> Unit
) {
    imageCapture ?: run { onStatus("error: camera not ready"); return }
    imageCapture.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
        override fun onCaptureSuccess(image: ImageProxy) {
            val b64 = imageProxyToBase64(image)
            enrollFace(userId, listOf(b64)) { resp -> onStatus(resp.status) }
        }
        override fun onError(exc: ImageCaptureException) { onStatus("error: ${exc.message}") }
    })
}

// ── FACE VERIFY SCREEN ────────────────────────────────────────────────────────
// Step 1: capture straight face
// Step 2: capture turned face (randomly left or right)
// Then send both to /verify and show result
@Composable
fun FaceCameraScreen(userId: String, onSuccess: () -> Unit) {
    var imageCapture  by remember { mutableStateOf<ImageCapture?>(null) }
    var captureStep   by remember { mutableStateOf(1) }   // 1 = straight, 2 = turned
    var straightB64   by remember { mutableStateOf<String?>(null) }
    var loading       by remember { mutableStateOf(false) }
    var verifyResult  by remember { mutableStateOf<VerifyResponse?>(null) }
    val executor      = remember { Executors.newSingleThreadExecutor() }
    // Randomly pick left or right once per verification session
    val turnDirection = remember { if ((0..1).random() == 0) "LEFT" else "RIGHT" }

    WithCameraPermission {
        Column(
            modifier = Modifier.fillMaxSize().background(BGGray).padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.height(16.dp))

            Text("Face Verification", fontSize = 20.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))

            // Instruction changes per step
            val instruction = when {
                verifyResult != null -> ""
                captureStep == 1     -> "Look straight at the camera and tap Capture."
                else                 -> "Now slowly turn your face to the $turnDirection and tap Capture."
            }
            if (instruction.isNotEmpty()) {
                Text(instruction, fontSize = 13.sp, color = Color.Gray, textAlign = TextAlign.Center)
            }

            // Step indicator
            Spacer(Modifier.height(12.dp))
            if (verifyResult == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StepDot(num = 1, label = "Straight", active = captureStep >= 1)
                    Divider(modifier = Modifier.weight(1f),
                        color = if (captureStep >= 2) GBlue else Color.LightGray, thickness = 2.dp)
                    StepDot(num = 2, label = "Turn $turnDirection", active = captureStep >= 2)
                }
                Spacer(Modifier.height(12.dp))
            }

            // Camera preview — hide after result received
            if (verifyResult == null) {
                CameraWithCapture(
                    useFrontCamera = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clip(RoundedCornerShape(16.dp)),
                    onCaptureBound = { imageCapture = it }
                )
                Spacer(Modifier.height(24.dp))
            }

            // Result card
            verifyResult?.let { result ->
                val accepted = result.status == "verified"
                val cardColor = if (accepted) GGreen else Color.Red
                val icon      = if (accepted) "✓" else "✗"
                val label     = if (accepted) "Attendance Marked!" else when (result.status) {
                    "no_face"       -> "No face detected"
                    "user_not_found"-> "User not enrolled. Please enroll first."
                    "error"         -> "Error: ${result.error}"
                    else            -> "Verification Failed"
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(cardColor.copy(alpha = 0.1f))
                        .border(2.dp, cardColor, RoundedCornerShape(16.dp))
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(icon, fontSize = 48.sp, color = cardColor)
                    Spacer(Modifier.height(8.dp))
                    Text(label, fontSize = 16.sp, fontWeight = FontWeight.Bold,
                        color = cardColor, textAlign = TextAlign.Center)
                    if (result.similarity > 0.0) {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Similarity: ${"%.1f".format(result.similarity * 100)}%",
                            fontSize = 14.sp, color = Color.Gray
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                if (accepted) {
                    Button(
                        onClick = onSuccess,
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GGreen)
                    ) { Text("Done", fontWeight = FontWeight.Bold) }
                } else {
                    // Allow retry
                    Button(
                        onClick = {
                            verifyResult = null
                            captureStep  = 1
                            straightB64  = null
                        },
                        modifier = Modifier.fillMaxWidth().height(52.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = GBlue)
                    ) { Text("Try Again", fontWeight = FontWeight.Bold) }
                }
                return@WithCameraPermission
            }

            // Capture button
            CaptureButton(loading) {
                loading = true
                imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                    override fun onCaptureSuccess(image: ImageProxy) {
                        val b64 = imageProxyToBase64(image)
                        if (captureStep == 1) {
                            straightB64 = b64
                            loading     = false
                            captureStep = 2
                        } else {
                            // Both photos captured — send to /verify
                            val frames = listOf(straightB64!!, b64)
                            verifyFace(userId, frames) { resp ->
                                loading      = false
                                verifyResult = resp
                            }
                        }
                    }
                    override fun onError(exc: ImageCaptureException) {
                        loading      = false
                        verifyResult = VerifyResponse(status = "error", error = exc.message ?: "Capture failed")
                    }
                })
            }
        }
    }
}

// ── Shared UI helpers ─────────────────────────────────────────────────────────
@Composable
private fun CaptureButton(loading: Boolean, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        enabled = !loading,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GBlue)
    ) {
        if (loading) {
            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(10.dp))
            Text("Processing...", fontWeight = FontWeight.Bold)
        } else {
            Text("Capture", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        }
    }
}

@Composable
private fun StatusCard(message: String, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color, RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Text(message, color = color, fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth())
    }
}
