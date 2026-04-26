package com.iith.attendanceapp

import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.Face
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions

/**
 * Plugs into CameraX ImageAnalysis. Extracts faces via ML Kit on every frame
 * and forwards the first detected face (or null) to the callback.
 * Rejects frames with multiple faces to prevent spoofing.
 */
class FaceAnalyzer(
    private val onFaceDetected: (Face?) -> Unit
) : ImageAnalysis.Analyzer {

    private val detector = FaceDetection.getClient(
        FaceDetectorOptions.Builder()
            .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_FAST)
            .setLandmarkMode(FaceDetectorOptions.LANDMARK_MODE_NONE)
            .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
            .setMinFaceSize(0.15f)
            .enableTracking()
            .build()
    )

    @ExperimentalGetImage
    override fun analyze(imageProxy: ImageProxy) {
        val mediaImage = imageProxy.image ?: run { imageProxy.close(); return }
        val image = InputImage.fromMediaImage(mediaImage, imageProxy.imageInfo.rotationDegrees)
        detector.process(image)
            .addOnSuccessListener { faces ->
                // Only pass single face — multiple faces = null (force single person)
                onFaceDetected(if (faces.size == 1) faces[0] else null)
            }
            .addOnFailureListener { onFaceDetected(null) }
            .addOnCompleteListener { imageProxy.close() }
    }
}
