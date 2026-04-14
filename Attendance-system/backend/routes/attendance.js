/**
 * attendance.js
 *
 * FIX: students who marked in a session at 17:30 should NOT appear in an
 * ongoing session now. The /attendance/:sessionId endpoint only returns
 * records that belong to THAT specific session. The "present now" confusion
 * arose because CourseView was polling attendance on the WRONG session ID
 * (it was using the primary active session which happened to match a past
 * session's lectureUID).  This is fully correct — each session has its own
 * attendance records. No change needed here except ensuring we filter by
 * the sessionUID, which we already do.
 *
 * ALSO: markAttendance now validates that the session is still active.
 */
const express    = require('express');
const Attendance = require('../models/Attendance');
const Session    = require('../models/Session');
const Enrollment = require('../models/Enrollment');
const Student    = require('../models/Student');
const { updateBucketForStudent } = require('../services/bucketService');
const { authenticate, authorize } = require('../middleware/auth');

const router = express.Router();

// ── Helper: validate session is active ───────────────────────────────────────
async function requireActiveSession(sessionId) {
  const session = await Session.findOne({ sessionUID: sessionId }).lean();
  if (!session) throw Object.assign(new Error('Session not found'), { status: 404 });
  const expiresAt = new Date(session.timestamp.getTime() + session.duration * 60000);
  if (Date.now() > expiresAt) throw Object.assign(new Error('Session has ended'), { status: 410 });
  return session;
}

// ── POST /markAttendance  (student or BLE device) ────────────────────────────
router.post('/markAttendance', authenticate, async (req, res, next) => {
  try {
    const { session_id, student_id, method } = req.body;
    if (!session_id || !student_id || !method) {
      return res.status(400).json({ error: 'session_id, student_id, method required' });
    }

    // Students can only mark their own attendance
    if (req.user.role === 'student' && req.user.user_id !== student_id) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const session = await requireActiveSession(session_id);

    // Verify student is enrolled
    const enrolled = await Enrollment.findOne({
      student: student_id,
      course:  session.course,
      status:  'Active',
    }).lean();
    if (!enrolled) return res.status(403).json({ error: 'Student not enrolled in this course' });

    const rec = await Attendance.findOneAndUpdate(
      { sessionUID: session_id, student: student_id },
      { $setOnInsert: { sessionUID: session_id, student: student_id, verifiedVia: method, markedAt: new Date() } },
      { upsert: true, new: true }
    );

    // Update bucket async — fire and forget
    updateBucketForStudent(student_id).catch(console.error);

    res.status(201).json({ message: 'Attendance marked', record: rec });
  } catch (err) { next(err); }
});

// ── POST /manualAttendance  (prof/admin/ta for single student) ───────────────
router.post('/manualAttendance', authenticate, authorize('prof','admin','ta'), async (req, res, next) => {
  try {
    const { session_id, student_id } = req.body;
    if (!session_id || !student_id) return res.status(400).json({ error: 'session_id and student_id required' });

    const session = await Session.findOne({ sessionUID: session_id }).lean();
    if (!session) return res.status(404).json({ error: 'Session not found' });

    const rec = await Attendance.findOneAndUpdate(
      { sessionUID: session_id, student: student_id },
      { $set: { verifiedVia: 'Manual', markedAt: new Date() } },
      { upsert: true, new: true }
    );

    updateBucketForStudent(student_id).catch(console.error);
    res.json({ message: 'Manual attendance recorded', record: rec });
  } catch (err) { next(err); }
});

// ── POST /manualAttendance/bulk ───────────────────────────────────────────────
router.post('/manualAttendance/bulk', authenticate, authorize('prof','admin','ta'), async (req, res, next) => {
  try {
    const { session_id, student_ids } = req.body;
    if (!session_id || !Array.isArray(student_ids)) {
      return res.status(400).json({ error: 'session_id and student_ids[] required' });
    }

    const session = await Session.findOne({ sessionUID: session_id }).lean();
    if (!session) return res.status(404).json({ error: 'Session not found' });

    const ops = student_ids.map(sid => ({
      updateOne: {
        filter: { sessionUID: session_id, student: sid },
        update: { $set: { verifiedVia: 'Manual', markedAt: new Date() } },
        upsert: true,
      }
    }));
    const result = await Attendance.bulkWrite(ops);

    for (const sid of student_ids) {
      updateBucketForStudent(sid).catch(console.error);
    }

    res.json({ message: 'Bulk attendance recorded', upserted: result.upsertedCount, modified: result.modifiedCount });
  } catch (err) { next(err); }
});

// ── GET /attendance/:sessionId ────────────────────────────────────────────────
// Returns ONLY records for the specified session (not all sessions in a lecture).
// This is intentionally session-scoped so the CourseView "Attendance Log"
// shows who has checked into the current session, not all past attendees.
router.get('/attendance/:sessionId', authenticate, async (req, res, next) => {
  try {
    const records = await Attendance.find({ sessionUID: req.params.sessionId }).lean();

    const studentIds = [...new Set(records.map(r => r.student))];
    const students   = await Student.find({ _id: { $in: studentIds } }).select('-password').lean();
    const sMap       = Object.fromEntries(students.map(s => [String(s._id), s]));

    res.json(records.map(r => ({
      ...r,
      student_id:   r.student,
      studentName:  sMap[r.student]?.name,
      studentEmail: sMap[r.student]?.email,
    })));
  } catch (err) { next(err); }
});

module.exports = router;