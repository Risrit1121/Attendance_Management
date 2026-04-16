package com.iith.attendanceapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException

private const val TAG = "FaceApiClient"

// ── Change this to your PC's local IP address where Flask is running ──────────
// Run `ipconfig` on Windows to find your IPv4 address (e.g. 192.168.1.5)
const val BACKEND_BASE_URL = "http://192.168.0.120:5000"

data class EnrollResponse(
    val status: String,           // "enrolled" | "no_face"
    val embeddingDim: Int = 0,
    val error: String = ""
)

data class VerifyResponse(
    val status: String,           // "verified" | "rejected" | "no_face" | "user_not_found"
    val similarity: Double = 0.0,
    val error: String = ""
)

private val client = OkHttpClient()
private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

// ── Enroll: send one or more base64 frames for a user ────────────────────────
fun enrollFace(
    userId: String,
    base64Frames: List<String>,
    onResult: (EnrollResponse) -> Unit
) {
    val body = JSONObject().apply {
        put("user_id", userId)
        put("frames", JSONArray(base64Frames))
    }.toString()

    val request = Request.Builder()
        .url("$BACKEND_BASE_URL/enroll")
        .post(body.toRequestBody(JSON_MEDIA))
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Enroll network error: ${e.message}")
            onResult(EnrollResponse(status = "error", error = e.message ?: "Network error"))
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string() ?: ""
            Log.d(TAG, "Enroll response: $bodyStr")
            try {
                val json = JSONObject(bodyStr)
                onResult(
                    EnrollResponse(
                        status       = json.optString("status", "error"),
                        embeddingDim = json.optInt("embedding_dim", 0)
                    )
                )
            } catch (e: Exception) {
                onResult(EnrollResponse(status = "error", error = "Parse error: ${e.message}"))
            }
        }
    })
}

// ── Verify: send 2 base64 frames (straight + turned) for a user ──────────────
fun verifyFace(
    userId: String,
    base64Frames: List<String>,
    onResult: (VerifyResponse) -> Unit
) {
    val body = JSONObject().apply {
        put("user_id", userId)
        put("frames", JSONArray(base64Frames))
    }.toString()

    val request = Request.Builder()
        .url("$BACKEND_BASE_URL/verify")
        .post(body.toRequestBody(JSON_MEDIA))
        .build()

    client.newCall(request).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            Log.e(TAG, "Verify network error: ${e.message}")
            onResult(VerifyResponse(status = "error", error = e.message ?: "Network error"))
        }

        override fun onResponse(call: Call, response: Response) {
            val bodyStr = response.body?.string() ?: ""
            Log.d(TAG, "Verify response: $bodyStr")
            try {
                val json = JSONObject(bodyStr)
                onResult(
                    VerifyResponse(
                        status     = json.optString("status", "error"),
                        similarity = json.optDouble("similarity", 0.0)
                    )
                )
            } catch (e: Exception) {
                onResult(VerifyResponse(status = "error", error = "Parse error: ${e.message}"))
            }
        }
    })
}
