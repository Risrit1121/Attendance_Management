package com.iith.attendanceapp

import android.util.Log
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "DiamsApi"

const val BASE_URL       = "https://diams-student-backend-app-gateway.onrender.com"
const val FLASK_BASE_URL = "http://192.168.0.134:5000"

private val http = OkHttpClient.Builder()
    .connectTimeout(60, TimeUnit.SECONDS)
    .readTimeout(60, TimeUnit.SECONDS)
    .writeTimeout(60, TimeUnit.SECONDS)
    .build()

private val JSON_MT = "application/json; charset=utf-8".toMediaType()

private fun get(url: String, token: String = "") =
    Request.Builder().url(url).get()
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun post(url: String, body: String, token: String = "") =
    Request.Builder().url(url).post(body.toRequestBody(JSON_MT))
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun put(url: String, body: String = "{}", token: String = "") =
    Request.Builder().url(url).put(body.toRequestBody(JSON_MT))
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun delete(url: String, token: String = "") =
    Request.Builder().url(url).delete()
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun String.isHtml() = trimStart().startsWith("<")

// Data classes
data class LoginResult(val success: Boolean, val token: String = "", val userId: String = "",
    val name: String = "", val email: String = "", val role: String = "", val error: String = "")
data class CourseAnalytic(val code: String, val name: String, val room: String,
    val slot: String = "", val attended: Int, val total: Int, val percentage: Double)
data class ActiveSessionItem(val sessionId: String, val courseCode: String, val courseName: String,
    val room: String, val mode: String, val startedAt: Long, val durationSeconds: Int)
data class BleValidateResult(val valid: Boolean, val error: String = "")
data class FaceVerifyResult(val status: String, val error: String = "")
data class MarkAttendanceResult(val success: Boolean, val error: String = "")
data class EnrollResponse(val status: String, val imageURL: String = "", val error: String = "")
data class PhotoStatus(val enrolled: Boolean, val photoUrl: String = "",
    val isExpired: Boolean = false, val enrolledAt: Long = 0L)
data class ProfCourse(val id: String, val name: String, val slot: String, val venue: String,
    val schedules: List<ProfSchedule> = emptyList())
data class ProfSchedule(val idx: Int, val scheduledDay: String, val startTime: String,
    val endTime: String, val method: String)
data class ProfSession(val sessionId: String, val courseCode: String, val mode: String,
    val professorId: String = "", val startedAt: Long = 0L, val durationSeconds: Int = 300)
data class AttendanceRecord(val studentId: String, val name: String,
    val attended: Int, val total: Int, val percentage: Double)
data class LiveAttendanceEntry(val studentId: String, val name: String,
    val verifiedVia: String, val timestamp: Long)
data class AdminStats(val totalStudents: Int, val totalProfessors: Int,
    val totalCourses: Int, val activeSessions: Int)
data class AdminUser(val id: String, val name: String, val email: String, val role: String)

// Auth
fun apiLogin(usernameOrEmail: String, password: String, onResult: (LoginResult) -> Unit) {
    val body = JSONObject().apply {
        put("email", usernameOrEmail); put("username", usernameOrEmail); put("password", password)
    }.toString()
    Log.d(TAG, "Login → $BASE_URL/api/auth/login")
    http.newCall(post("$BASE_URL/api/auth/login", body)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) =
            onResult(LoginResult(false, error = "Network error: ${e.message}"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "Login HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(LoginResult(false, error = "HTTP ${response.code}: endpoint not found")); return }
            try {
                val json = JSONObject(str)
                val token = json.optString("token", "")
                val user  = json.optJSONObject("user")
                if (token.isNotBlank() && user != null) {
                    onResult(LoginResult(true, token, user.optString("id"), user.optString("name"),
                        user.optString("email"), user.optString("role")))
                } else {
                    onResult(LoginResult(false, error = "HTTP ${response.code}: ${json.optString("message", json.optString("error", str))}"))
                }
            } catch (e: Exception) { onResult(LoginResult(false, error = "Parse error: ${e.message}")) }
        }
    })
}

// Student analytics
fun apiGetStudentAnalytics(studentId: String, token: String, onResult: (List<CourseAnalytic>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/student/$studentId/analytics", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "Analytics HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = JSONObject(str).getJSONArray("courses")
                onResult((0 until arr.length()).map {
                    val c = arr.getJSONObject(it)
                    CourseAnalytic(c.optString("code"), c.optString("name"), c.optString("room"),
                        c.optString("slot", ""), c.optInt("attended"), c.optInt("total"), c.optDouble("percentage"))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Student sessions
fun apiGetStudentSessions(studentId: String, token: String, onResult: (List<ActiveSessionItem>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/student/$studentId/sessions", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "Sessions HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = JSONObject(str).getJSONArray("sessions")
                onResult((0 until arr.length()).map {
                    val s = arr.getJSONObject(it)
                    ActiveSessionItem(s.optString("sessionId"), s.optString("courseCode"),
                        s.optString("courseName"), s.optString("room"), s.optString("mode"),
                        s.optLong("startedAt"), s.optInt("durationSeconds", 300))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Mark attendance
fun apiMarkAttendance(studentId: String, sessionId: String, method: String = "BLE", token: String, onResult: (MarkAttendanceResult) -> Unit) {
    val body = JSONObject().apply { put("studentId", studentId); put("sessionId", sessionId); put("method", method) }.toString()
    http.newCall(post("$BASE_URL/api/student/markAttendance", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(MarkAttendanceResult(false, e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "MarkAttendance HTTP ${response.code}: $str")
            try { onResult(MarkAttendanceResult(JSONObject(str).optBoolean("success", response.isSuccessful))) }
            catch (e: Exception) { onResult(MarkAttendanceResult(false, "Parse error: ${e.message}")) }
        }
    })
}

// Face verify
fun apiFaceVerify(userId: String, frames: List<String>, challenges: List<String>, token: String, onResult: (FaceVerifyResult) -> Unit) {
    val body = JSONObject().apply { put("userId", userId); put("frames", JSONArray(frames)) }.toString()
    http.newCall(post("$BASE_URL/api/student/faceVerify", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(FaceVerifyResult("error", e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "FaceVerify HTTP ${response.code}: $str")
            try { onResult(FaceVerifyResult(JSONObject(str).optString("status", "failed"))) }
            catch (e: Exception) { onResult(FaceVerifyResult("error", "Parse error: ${e.message}")) }
        }
    })
}

// Photo status
fun apiGetPhotoStatus(studentId: String, token: String, onResult: (PhotoStatus) -> Unit) {
    http.newCall(get("$BASE_URL/api/student/$studentId/photoStatus", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(PhotoStatus(false))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "PhotoStatus HTTP ${response.code}: $str")
            try {
                val j = JSONObject(str)
                onResult(PhotoStatus(j.optBoolean("enrolled", false), j.optString("photoUrl", ""),
                    j.optBoolean("isExpired", false), j.optLong("enrolledAt", 0L)))
            } catch (e: Exception) { onResult(PhotoStatus(false)) }
        }
    })
}

// Update photo
fun apiUpdatePhoto(studentId: String, frames: List<String>, token: String, onResult: (EnrollResponse) -> Unit) {
    val body = JSONObject().apply { put("frames", JSONArray(frames)) }.toString()
    http.newCall(post("$BASE_URL/api/student/$studentId/updatePhoto", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(EnrollResponse("error", error = e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "UpdatePhoto HTTP ${response.code}: $str")
            try { val j = JSONObject(str); onResult(EnrollResponse(j.optString("status", "failed"), j.optString("imageURL", ""))) }
            catch (e: Exception) { onResult(EnrollResponse("error", error = "Parse error: ${e.message}")) }
        }
    })
}

// BLE validate
fun apiBleValidate(classId: String, beacons: List<BleBeaconResult>, onResult: (BleValidateResult) -> Unit) {
    val body = JSONObject().apply {
        put("class_id", classId)
        put("beacons", JSONArray().apply {
            beacons.forEach { b -> put(JSONObject().apply { put("major", b.major.toString()); put("minor", b.minor.toString()); put("rssi", b.avgRssi.toInt()) }) }
        })
    }.toString()
    http.newCall(post("$BASE_URL/api/ble/validate", body)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(BleValidateResult(false, e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "BleValidate HTTP ${response.code}: $str")
            try { onResult(BleValidateResult(JSONObject(str).optBoolean("valid", false))) }
            catch (e: Exception) { onResult(BleValidateResult(false, "Parse error: ${e.message}")) }
        }
    })
}

// Professor courses
fun apiGetProfCourses(professorId: String, token: String, onResult: (List<ProfCourse>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/courses", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "ProfCourses HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = JSONObject(str).getJSONArray("courses")
                onResult((0 until arr.length()).map { i ->
                    val c = arr.getJSONObject(i)
                    ProfCourse(c.optString("_id", c.optString("id", "")), c.optString("name"), c.optString("slot"), c.optString("venue"))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Start session
fun apiStartSession(courseCode: String, mode: String, token: String, onResult: (ProfSession?, String) -> Unit) {
    val body = JSONObject().apply { put("courseCode", courseCode); put("mode", mode) }.toString()
    http.newCall(post("$BASE_URL/api/professor/session/start", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "StartSession HTTP ${response.code}: $str")
            if (response.code == 400) { onResult(null, "Active session already exists."); return }
            try {
                val json = JSONObject(str); val s = json.optJSONObject("session") ?: json
                onResult(ProfSession(s.optString("id", s.optString("sessionUID", s.optString("sessionId", ""))),
                    s.optString("courseCode", courseCode), s.optString("mode", mode),
                    s.optString("professorId", ""), s.optLong("startedAt", System.currentTimeMillis()),
                    s.optInt("durationSeconds", 300)), "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// End session
fun apiEndSession(sessionId: String, token: String, onResult: (Boolean, String) -> Unit) {
    http.newCall(post("$BASE_URL/api/professor/session/end/$sessionId", "{}", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

// Active session
fun apiGetProfActiveSession(professorId: String, token: String, onResult: (ProfSession?) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/activeSession", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null)
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            try {
                val json = JSONObject(str)
                if (json.isNull("session")) { onResult(null); return }
                val s = json.optJSONObject("session") ?: run { onResult(null); return }
                onResult(ProfSession(s.optString("id", s.optString("sessionUID", s.optString("sessionId", ""))),
                    s.optString("courseCode", ""), s.optString("mode", ""),
                    s.optString("professorId", ""), s.optLong("startedAt", 0L), s.optInt("durationSeconds", 300)))
            } catch (e: Exception) { onResult(null) }
        }
    })
}

// Session summary
fun apiGetSessionSummary(sessionId: String, token: String, onResult: (List<LiveAttendanceEntry>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/session/$sessionId/summary", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "SessionSummary HTTP ${response.code}: $str")
            try {
                val arr = JSONObject(str).optJSONArray("attendance") ?: JSONArray()
                onResult((0 until arr.length()).map {
                    val a = arr.getJSONObject(it)
                    LiveAttendanceEntry(a.optString("student"), a.optString("name", a.optString("student")),
                        a.optString("verifiedVia", "Unknown"), a.optLong("timestamp", 0L))
                }, "")
            } catch (e: Exception) { onResult(emptyList(), "") }
        }
    })
}

// Course analytics
fun apiGetCourseAnalytics(professorId: String, courseCode: String, token: String, onResult: (List<AttendanceRecord>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/course/$courseCode/attendance", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "CourseAnalytics HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = JSONObject(str).optJSONArray("students") ?: JSONArray()
                onResult((0 until arr.length()).map {
                    val s = arr.getJSONObject(it)
                    AttendanceRecord(s.optString("studentId", s.optString("student", "")),
                        s.optString("name"), s.optInt("attended"), s.optInt("total"), s.optDouble("percentage"))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// QR generate (professor — POST /api/qr/generate, call every 5s)
data class QrGenerateResult(val hash: String, val expiresIn: Int, val error: String = "")

fun apiQrGenerate(classId: String, token: String, onResult: (QrGenerateResult) -> Unit) {
    val body = JSONObject().apply { put("class_id", classId) }.toString()
    http.newCall(post("$BASE_URL/api/qr/generate", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(QrGenerateResult("", 0, e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "QrGenerate HTTP ${response.code}: $str")
            try {
                val j = JSONObject(str)
                onResult(QrGenerateResult(j.optString("hash", ""), j.optInt("expires_in", 15)))
            } catch (e: Exception) { onResult(QrGenerateResult("", 0, "Parse error: ${e.message}")) }
        }
    })
}

// QR validate (student — POST /api/qr/validate)
fun apiQrValidate(classId: String, hash: String, onResult: (Boolean) -> Unit) {
    val body = JSONObject().apply {
        put("class_id", classId)
        put("hash", hash)
        put("timestamp", System.currentTimeMillis())
    }.toString()
    http.newCall(post("$BASE_URL/api/qr/validate", body)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false)
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "QrValidate HTTP ${response.code}: $str")
            try { onResult(JSONObject(str).optBoolean("valid", false)) }
            catch (e: Exception) { onResult(false) }
        }
    })
}

// Course students
fun apiGetCourseStudents(token: String, courseId: String, onResult: (List<AdminUser>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/course/$courseId/students", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "CourseStudents HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = try { JSONArray(str) } catch (e: Exception) { JSONObject(str).optJSONArray("students") ?: JSONArray() }
                onResult((0 until arr.length()).map {
                    val s = arr.getJSONObject(it)
                    AdminUser(s.optString("_id", s.optString("id", "")), s.optString("name"), s.optString("email", ""), "student")
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Manual attendance bulk
fun apiManualAttendanceBulk(token: String, sessionUID: String, studentIds: List<String>, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("sessionUID", sessionUID); put("studentIds", JSONArray(studentIds)) }.toString()
    http.newCall(post("$BASE_URL/manualAttendance/bulk", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

// Admin
fun apiCreateCourse(token: String, name: String, slot: String, venue: String, instructors: List<String>, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("name", name); put("slot", slot); put("venue", venue); put("instructors", JSONArray(instructors)) }.toString()
    http.newCall(post("$BASE_URL/courses", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiUpdateCourse(token: String, courseId: String, name: String, slot: String, venue: String, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("name", name); put("slot", slot); put("venue", venue) }.toString()
    http.newCall(put("$BASE_URL/courses/$courseId", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiDeleteCourse(token: String, courseId: String, onResult: (Boolean, String) -> Unit) {
    http.newCall(delete("$BASE_URL/courses/$courseId", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiCreateProfessor(token: String, name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("name", name); put("email", email); put("password", password) }.toString()
    http.newCall(post("$BASE_URL/admin/professors", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiCreateStudent(token: String, name: String, email: String, password: String, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("name", name); put("email", email); put("password", password) }.toString()
    http.newCall(post("$BASE_URL/admin/students", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiDeleteUser(token: String, userId: String, onResult: (Boolean, String) -> Unit) {
    http.newCall(delete("$BASE_URL/admin/users/$userId", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

// Face enroll (Flask)
fun enrollFace(userId: String, base64Frames: List<String>, onResult: (EnrollResponse) -> Unit) {
    val body = JSONObject().apply { put("user_id", userId); put("frames", JSONArray(base64Frames)) }.toString()
    http.newCall(post("$FLASK_BASE_URL/enroll", body)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(EnrollResponse("error", error = e.message ?: "Network error"))
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "Enroll HTTP ${response.code}: $str")
            try { val j = JSONObject(str); onResult(EnrollResponse(j.optString("status", "error"), j.optString("imageURL", ""))) }
            catch (e: Exception) { onResult(EnrollResponse("error", error = "Parse error: ${e.message}")) }
        }
    })
}
