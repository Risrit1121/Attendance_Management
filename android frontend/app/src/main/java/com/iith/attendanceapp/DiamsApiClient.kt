package com.iith.attendanceapp

import android.util.Log
import android.os.Handler
import android.os.Looper
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

private const val TAG = "DiamsApi"

const val BASE_URL = "https://diams-student-backend-app-gateway.onrender.com"

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

private fun delete(url: String, token: String = "") =
    Request.Builder().url(url).delete()
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun patch(url: String, body: String, token: String = "") =
    Request.Builder().url(url).patch(body.toRequestBody(JSON_MT))
        .apply { if (token.isNotBlank()) addHeader("Authorization", "Bearer $token") }.build()

private fun String.isHtml() = trimStart().startsWith("<")
private fun main(block: () -> Unit) = Handler(Looper.getMainLooper()).post(block)

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
data class ProfCourse(val id: String, val courseCode: String, val name: String, val slot: String, val venue: String,
    val schedules: List<ProfSchedule> = emptyList())
data class ProfSchedule(val idx: Int, val scheduledDay: String, val startTime: String,
    val endTime: String, val method: String)
data class ProfSession(val sessionId: String, val courseCode: String, val mode: String,
    val professorId: String = "", val startedAt: Long = 0L, val durationSeconds: Int = 300)
data class AttendanceRecord(val studentId: String, val name: String,
    val attended: Int, val total: Int, val percentage: Double)
data class LiveAttendanceEntry(val studentId: String, val name: String,
    val verifiedVia: String, val timestamp: Long)
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

// BLE major lookup — GET /api/ble/major/:classroom
fun apiGetBeaconMajor(classroom: String, onResult: (String?) -> Unit) {
    http.newCall(get("$BASE_URL/api/ble/major/$classroom")).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { main { onResult(null) } }
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "BeaconMajor HTTP ${response.code}: $str")
            try { main { onResult(JSONObject(str).optString("major", null)) } }
            catch (e: Exception) { main { onResult(null) } }
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
                    val mongoId = c.optString("_id", c.optString("id", ""))
                    val code = c.optString("courseCode", c.optString("code", mongoId))
                    ProfCourse(mongoId, code, c.optString("name"), c.optString("slot"), c.optString("venue"))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Start session — on 400/409 (already exists), recover by fetching the active session
fun apiStartSession(courseCode: String, mode: String, token: String, onResult: (ProfSession?, String) -> Unit) {
    val body = JSONObject().apply { put("courseCode", courseCode); put("mode", mode) }.toString()
    http.newCall(post("$BASE_URL/api/professor/session/start", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { main { onResult(null, e.message ?: "Network error") } }
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "StartSession HTTP ${response.code}: $str")
            if (response.code == 400 || response.code == 409) {
                try {
                    val json = JSONObject(str)
                    val existingId = json.optString("session_id", "")
                    if (existingId.isNotBlank()) {
                        main { onResult(ProfSession(existingId, courseCode, mode,
                            startedAt = System.currentTimeMillis(), durationSeconds = 300), "") }
                        return
                    }
                } catch (_: Exception) {}
                main { onResult(null, "ALREADY_EXISTS") }
                return
            }
            try {
                val json = JSONObject(str)
                val s = json.optJSONObject("session") ?: json
                val sid = s.optString("id", s.optString("sessionUID", s.optString("sessionId", "")))
                main { onResult(ProfSession(sid, s.optString("courseCode", courseCode),
                    s.optString("mode", mode), s.optString("professorId", ""),
                    s.optLong("startedAt", System.currentTimeMillis()),
                    s.optInt("durationSeconds", 300)), "") }
            } catch (e: Exception) { main { onResult(null, "Parse error: ${e.message}") } }
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

// Active session — fetches active session for a professor, returns first match for given courseCode
fun apiGetProfActiveSession(professorId: String, courseCode: String, token: String, onResult: (ProfSession?) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/activeSession", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) { main { onResult(null) } }
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "ActiveSession HTTP ${response.code}: $str")
            try {
                val json = JSONObject(str)
                if (json.isNull("session")) { main { onResult(null) }; return }
                val s = json.optJSONObject("session") ?: run { main { onResult(null) }; return }
                val sid = s.optString("id", s.optString("sessionUID", s.optString("sessionId", "")))
                if (sid.isBlank()) { main { onResult(null) }; return }
                main { onResult(ProfSession(sid, s.optString("courseCode", courseCode),
                    s.optString("mode", ""), s.optString("professorId", professorId),
                    s.optLong("startedAt", 0L), s.optInt("durationSeconds", 300))) }
            } catch (e: Exception) { main { onResult(null) } }
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
    val effectiveClassId = classId.ifBlank { return }
    val body = JSONObject().apply { put("class_id", effectiveClassId) }.toString()
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
        put("timestamp", System.currentTimeMillis())  // epoch milliseconds — matches iOS and Abhay's swagger
    }.toString()
    Log.d(TAG, "QrValidate → class_id=$classId hash=$hash body=$body")
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

// Course students — derived from course attendance analytics (no dedicated gateway endpoint)
fun apiGetCourseStudents(token: String, professorId: String, courseCode: String, onResult: (List<AdminUser>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/course/$courseCode/attendance", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "CourseStudents HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val arr = JSONObject(str).optJSONArray("students") ?: JSONArray()
                onResult((0 until arr.length()).map {
                    val s = arr.getJSONObject(it)
                    AdminUser(s.optString("studentId", s.optString("student", "")),
                        s.optString("name"), "", "student")
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

// Manual attendance by professor — POST /api/professor/session/:sessionId/manualAttendance
fun apiProfManualAttendance(sessionId: String, studentIds: List<String>, token: String, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply { put("studentIds", JSONArray(studentIds)) }.toString()
    http.newCall(post("$BASE_URL/api/professor/session/$sessionId/manualAttendance", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

// Schedule CRUD
data class ProfScheduleItem(val index: Int, val scheduledDay: String, val startTime: String,
    val endTime: String, val method: String, val switch: Boolean)

// Schedules are embedded in the course object — fetch via courses endpoint
fun apiGetSchedules(professorId: String, courseId: String, token: String, onResult: (List<ProfScheduleItem>?, String) -> Unit) {
    http.newCall(get("$BASE_URL/api/professor/$professorId/courses", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(null, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) {
            val str = response.body?.string() ?: ""
            Log.d(TAG, "GetSchedules (via courses) HTTP ${response.code}: $str")
            if (str.isHtml()) { onResult(null, "HTTP ${response.code}: endpoint not found"); return }
            try {
                val courses = JSONObject(str).getJSONArray("courses")
                val course = (0 until courses.length()).map { courses.getJSONObject(it) }
                    .firstOrNull { it.optString("_id", it.optString("id", "")) == courseId }
                val arr = course?.optJSONArray("schedules") ?: JSONArray()
                onResult((0 until arr.length()).map { i ->
                    val s = arr.getJSONObject(i)
                    ProfScheduleItem(i, s.optString("scheduledDay"), s.optString("startTime"),
                        s.optString("endTime"), s.optString("method", "BLE"), s.optBoolean("switch", false))
                }, "")
            } catch (e: Exception) { onResult(null, "Parse error: ${e.message}") }
        }
    })
}

fun apiAddSchedule(courseId: String, scheduledDay: String, startTime: String, endTime: String,
    method: String, token: String, onResult: (Boolean, String) -> Unit) {
    val body = JSONObject().apply {
        put("scheduledDay", scheduledDay); put("startTime", startTime)
        put("endTime", endTime); put("method", method)
    }.toString()
    http.newCall(post("$BASE_URL/api/professor/$courseId/schedule", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false, e.message ?: "Network error")
        override fun onResponse(call: Call, response: Response) =
            onResult(response.isSuccessful, if (!response.isSuccessful) (response.body?.string() ?: "") else "")
    })
}

fun apiToggleSchedule(courseId: String, scheduleIndex: Int, enabled: Boolean, token: String, onResult: (Boolean) -> Unit) {
    val body = JSONObject().apply { put("switch", enabled) }.toString()
    http.newCall(patch("$BASE_URL/api/professor/$courseId/schedule/$scheduleIndex", body, token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false)
        override fun onResponse(call: Call, response: Response) = onResult(response.isSuccessful)
    })
}

fun apiDeleteSchedule(courseId: String, scheduleIndex: Int, token: String, onResult: (Boolean) -> Unit) {
    http.newCall(delete("$BASE_URL/api/professor/$courseId/schedule/$scheduleIndex", token)).enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) = onResult(false)
        override fun onResponse(call: Call, response: Response) = onResult(response.isSuccessful)
    })
}

// Admin — local data only, no backend calls needed
