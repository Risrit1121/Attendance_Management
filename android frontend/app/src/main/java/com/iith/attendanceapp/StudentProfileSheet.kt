package com.iith.attendanceapp

import android.graphics.Bitmap
import android.util.Base64
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.ImageProxy
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.rememberAsyncImagePainter
import java.io.ByteArrayOutputStream
import java.util.concurrent.Executors

// ── Profile sheet (inside ModalBottomSheet) ───────────────────────────────────
@Composable
fun StudentProfileSheet(
    userId: String,
    userName: String,
    email: String,
    token: String,
    photoUrl: String?,
    photoEnrolled: Boolean,
    onUploadPhoto: () -> Unit,          // triggers full-screen camera outside sheet
    onPhotoUpdated: (String) -> Unit,
    onLogout: () -> Unit,
    onClose: () -> Unit
) {
    var loading   by remember { mutableStateOf(false) }
    var statusMsg by remember { mutableStateOf<String?>(null) }
    var isError   by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 32.dp, start = 28.dp, end = 28.dp, bottom = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Profile photo circle
        Box(
            modifier = Modifier.size(90.dp).clip(CircleShape).border(3.dp, GBlue, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            if (photoUrl != null) {
                Image(painter = rememberAsyncImagePainter(photoUrl),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop)
            } else {
                Icon(Icons.Default.AccountCircle, contentDescription = null,
                    tint = GBlue, modifier = Modifier.size(80.dp))
            }
        }

        Spacer(Modifier.height(12.dp))
        Text(userName, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(email, fontSize = 13.sp, color = Color.Gray)
        Text(userId, fontSize = 13.sp, color = Color.Gray)

        if (!photoEnrolled) {
            Spacer(Modifier.height(8.dp))
            Text("Face not enrolled. Upload a photo to enable face verification.",
                fontSize = 12.sp, color = Color(0xFFFF8C00), textAlign = TextAlign.Center)
        }

        if (statusMsg != null) {
            Spacer(Modifier.height(8.dp))
            Text(statusMsg!!, fontSize = 13.sp,
                color = if (isError) Color.Red else GGreen,
                textAlign = TextAlign.Center)
        }

        Spacer(Modifier.height(24.dp))
        Divider()
        Spacer(Modifier.height(16.dp))

        // Tapping this closes the sheet and opens full-screen camera
        Button(
            onClick = onUploadPhoto,
            enabled = !loading,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GBlue)
        ) {
            Text(if (photoEnrolled) "Update Photo" else "Upload Photo", fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(12.dp))

        Button(
            onClick = onLogout,
            modifier = Modifier.fillMaxWidth().height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0x1AFF0000), contentColor = Color.Red)
        ) { Text("Log Out", fontWeight = FontWeight.Bold) }

        Spacer(Modifier.height(12.dp))
        TextButton(onClick = onClose) { Text("Cancel", color = Color.Gray) }
    }
}

// ── Full-screen photo capture (rendered at top level, not inside a sheet) ─────
@Composable
fun PhotoCaptureFullScreen(
    userId: String,
    token: String,
    onDone: (imageUrl: String) -> Unit,
    onCancel: () -> Unit
) {
    var imageCapture by remember { mutableStateOf<ImageCapture?>(null) }
    var capturing    by remember { mutableStateOf(false) }
    var uploading    by remember { mutableStateOf(false) }
    var errorMsg     by remember { mutableStateOf<String?>(null) }
    val executor     = remember { Executors.newSingleThreadExecutor() }

    WithCameraPermission {
        Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {

            // Camera fills entire screen — no overlay, no oval
            CameraWithCapture(
                useFrontCamera = true,
                modifier = Modifier.fillMaxSize(),
                onCaptureBound = { imageCapture = it }
            )

            // Cancel button — top left
            TextButton(
                onClick = onCancel,
                modifier = Modifier.align(Alignment.TopStart).padding(16.dp)
            ) {
                Text("← Cancel", color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Title — top center
            Text(
                "Take Profile Photo",
                fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 20.dp)
            )

            // Controls pinned to bottom — always visible without scrolling
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Look straight at the camera",
                    fontSize = 13.sp, color = Color.White.copy(alpha = 0.85f))

                if (errorMsg != null) {
                    Spacer(Modifier.height(6.dp))
                    Text(errorMsg!!, fontSize = 12.sp, color = Color.Red, textAlign = TextAlign.Center)
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = {
                        capturing = true; errorMsg = null
                        imageCapture?.takePicture(executor, object : ImageCapture.OnImageCapturedCallback() {
                            override fun onCaptureSuccess(image: ImageProxy) {
                                val bitmap = image.toBitmap()
                                // Apply correct rotation + front camera mirror
                                val matrix = android.graphics.Matrix().apply {
                                    postRotate(image.imageInfo.rotationDegrees.toFloat())
                                    postScale(-1f, 1f)  // mirror for front camera
                                }
                                val corrected = android.graphics.Bitmap.createBitmap(
                                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
                                val stream = ByteArrayOutputStream()
                                corrected.compress(Bitmap.CompressFormat.JPEG, 85, stream)
                                image.close()
                                val b64 = Base64.encodeToString(stream.toByteArray(), Base64.NO_WRAP)
                                capturing  = false
                                uploading  = true
                                apiUpdatePhoto(userId, listOf(b64), token) { resp ->
                                    uploading = false
                                    when (resp.status) {
                                        "uploaded" -> onDone(resp.imageURL)
                                        "no_face"  -> errorMsg = "No face detected. Try again."
                                        else       -> errorMsg = "Upload failed. Try again."
                                    }
                                }
                            }
                            override fun onError(exc: ImageCaptureException) {
                                capturing = false
                                errorMsg  = "Capture failed: ${exc.message}"
                            }
                        })
                    },
                    enabled  = !capturing && !uploading,
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape    = RoundedCornerShape(10.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = GBlue)
                ) {
                    when {
                        capturing -> { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Capturing...") }
                        uploading -> { CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp); Spacer(Modifier.width(8.dp)); Text("Uploading...") }
                        else      -> Text("Capture", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}
