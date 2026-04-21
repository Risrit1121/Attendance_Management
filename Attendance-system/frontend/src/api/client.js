import axios from "axios";

const API = axios.create({
  baseURL: localStorage.getItem("api_url") || "https://attendance-management-gazr.onrender.com",
  // Render free tier cold-starts in 30-50 s. 10 s was causing silent timeouts
  // that left every page showing 0 courses / 0 analytics. 55 s gives the server
  // enough time to wake, with the retry utility layered on top for transient
  // failures after the server is up.
  timeout: 55000,
});

// ── Request interceptor: attach JWT ──────────────────────────────────────────
API.interceptors.request.use(cfg => {
  const token = sessionStorage.getItem("diams_token");
  if (token) cfg.headers.Authorization = `Bearer ${token}`;
  return cfg;
});

// ── Response interceptor ──────────────────────────────────────────────────────
// 1. 401 Unauthorized → token expired or invalidated (e.g. after a cold-start
//    server restart that changed the JWT secret in memory). Clear session and
//    force re-login so the user sees the login page instead of blank data.
//
// 2. Network error / timeout → attach a human-readable `.userMessage` to the
//    error so pages can surface it instead of silently showing empty state.
//
// We store the logout callback here so the interceptor can call it without
// importing AuthContext (which would be a circular dep).
let _logoutCallback = null;
export function registerLogoutCallback(fn) { _logoutCallback = fn; }

API.interceptors.response.use(
  res => res,
  err => {
    if (err.response?.status === 401) {
      // Token expired — clear everything and redirect to login
      sessionStorage.removeItem("diams_token");
      sessionStorage.removeItem("diams_user");
      if (_logoutCallback) _logoutCallback();
    }

    // Attach a friendly message callers can read
    if (!err.response) {
      // Pure network failure or timeout
      err.userMessage =
        "Server is starting up — please wait a moment and try again.";
    } else if (err.response.status >= 500) {
      err.userMessage = "Server error — please try again in a few seconds.";
    }

    return Promise.reject(err);
  }
);

// ── Auth ──────────────────────────────────────────────────────────────────────
export const login = (data) => API.post("/login", data);

// ── Courses ───────────────────────────────────────────────────────────────────
export const getCourses        = (profId)   => API.get(`/courses/${profId}`);
export const getAllCourses      = ()         => API.get("/admin/courses");
export const createCourse      = (data)     => API.post("/courses", data);
export const updateCourse      = (id, data) => API.put(`/courses/${id}`, data);
export const deleteCourse      = (id)       => API.delete(`/courses/${id}`);
export const getCourseStudents = (courseId) => API.get(`/course/${courseId}/students`);

// ── Lectures ──────────────────────────────────────────────────────────────────
export const cancelLecture   = (courseId, lectureUID) =>
  API.patch(`/courses/${courseId}/lectures/${lectureUID}/cancel`);
export const uncancelLecture = (courseId, lectureUID) =>
  API.patch(`/courses/${courseId}/lectures/${lectureUID}/uncancel`);

// ── Schedules ─────────────────────────────────────────────────────────────────
export const getCourseSchedules = (courseId)            => API.get(`/courses/${courseId}/schedules`);
export const addSchedule        = (courseId, data)      => API.post(`/courses/${courseId}/schedule`, data);
export const updateSchedule     = (courseId, idx, data) => API.patch(`/courses/${courseId}/schedule/${idx}`, data);
export const deleteScheduleItem = (courseId, idx)       => API.delete(`/courses/${courseId}/schedule/${idx}`);
export const replaceSchedules   = (courseId, list)      => API.put(`/courses/${courseId}/schedule`, { schedules: list });

// ── TA management (admin) ─────────────────────────────────────────────────────
export const addTA    = (courseId, studentId) => API.post(`/admin/courses/${courseId}/tas`, { student_id: studentId });
export const removeTA = (courseId, studentId) => API.delete(`/admin/courses/${courseId}/tas/${studentId}`);

// ── Sessions ──────────────────────────────────────────────────────────────────
export const startSession     = (data)      => API.post("/startSession", data);
export const endSession       = (sessionId) => API.post(`/endSession/${sessionId}`);
export const getActiveSession = (courseId)  => API.get(`/activeSession?course_id=${courseId}`);

// ── BLE microservice ──────────────────────────────────────────────────────────
export const getMinor       = (major)        => API.get(`/getMinor?major=${encodeURIComponent(major)}`);
export const bleValidate    = ({ session_id, beacons }) => API.post("/ble/validate", { session_id, beacons });
export const validateBeacon = (major, minor) => API.get(`/validate?major=${encodeURIComponent(major)}&minor=${encodeURIComponent(minor)}`);

// ── QR microservice ───────────────────────────────────────────────────────────
export const getQR      = (sessionId)            => API.get(`/getQR/${sessionId}`);
export const qrValidate = ({ session_id, hash }) => API.post("/qr/validate", { session_id, hash });
export const decodeQR   = (qr)                   => API.get(`/decodeQR?qr=${encodeURIComponent(qr)}`);

// ── Attendance ────────────────────────────────────────────────────────────────
export const getAttendance        = (sessionId) => API.get(`/attendance/${sessionId}`);
export const markAttendance       = (data)      => API.post("/markAttendance", data);
export const manualAttendance     = (data)      => API.post("/manualAttendance", data);
export const manualAttendanceBulk = (data)      => API.post("/manualAttendance/bulk", data);

// ── Admin – Stats & Logs ──────────────────────────────────────────────────────
export const getAdminStats  = () => API.get("/admin/stats");
// Returns last n log entries from the in-process ring buffer (admin only).
// Each entry: { ts: ISO string, level: "info"|"warn"|"error", text: string }
export const getServerLogs  = (n = 150) => API.get(`/admin/logs?n=${n}`);
export const getAdminAnalytics        = ()                    => API.get("/analytics/admin");
export const getCourseAnalytics       = (courseId)            => API.get(`/analytics/course/${courseId}`);
export const getCourseStudentStats    = (courseId)            => API.get(`/analytics/course/${courseId}/students`);
export const getProfAnalytics         = (profId)              => API.get(`/analytics/prof/${profId}`);
export const getStudentCourseHistory  = (studentId, courseId) => API.get(`/student/${studentId}/history/${courseId}`);
export const getAtRiskStudents        = (profId)              => API.get(`/analytics/at-risk/${profId}`);
export const getAdminStudentAnalytics = (studentId)           => API.get(`/admin/student/${studentId}/analytics`);
export const getAdminCourseAnalytics  = (courseId)            => API.get(`/admin/courses/${courseId}/analytics`);

// ── Admin – Users ─────────────────────────────────────────────────────────────
export const getAllUsers      = ()     => API.get("/admin/users");
export const getAllStudents   = ()     => API.get("/admin/students");
export const createProfessor = (data) => API.post("/admin/professors", data);
export const deleteProfessor = (id)   => API.delete(`/admin/professors/${id}`);
export const createStudent   = (data) => API.post("/admin/students", data);
export const deleteStudent   = (id)   => API.delete(`/admin/students/${id}`);

// ── Admin – Enrollment ────────────────────────────────────────────────────────
export const enrollStudent     = (data) => API.post("/admin/enroll", data);
export const enrollBulk        = (data) => API.post("/admin/enroll/bulk", data);
export const unenrollStudent   = (data) => API.post("/admin/unenroll", data);
export const getCourseEnrolled = (cId)  => API.get(`/admin/enrollments/${cId}`);

// ── Admin – CSV bulk ──────────────────────────────────────────────────────────
export const csvBulkCreate = (formData) => API.post("/admin/csv/bulk-create", formData, {
  headers: { "Content-Type": "multipart/form-data" },
});
export const csvBulkDelete = (formData) => API.post("/admin/csv/bulk-delete", formData, {
  headers: { "Content-Type": "multipart/form-data" },
});

export default API;