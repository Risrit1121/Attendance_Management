/**
 * analytics.js — lecture-based attendance analytics.
 *
 * KEY FIX: lectureStats is now built from course.lectures (the canonical list),
 * not from the set of sessions. This means:
 *  - A lecture with no sessions shows up with attended=0
 *  - A lecture with sessions uses the intersection rule
 *  - The heatmap/history for a student is derived from the intersection result
 *
 * The `hasSession` flag on each lecture stat tells the frontend whether
 * any sessions have been held for that lecture.
 */
const express    = require('express');
const Attendance = require('../models/Attendance');
const Session    = require('../models/Session');
const Course     = require('../models/Course');
const Enrollment = require('../models/Enrollment');
const Student    = require('../models/Student');
const Professor  = require('../models/Professor');
const { authenticate, authorize } = require('../middleware/auth');

const router = express.Router();

// ── Core intersection ─────────────────────────────────────────────────────────
/**
 * For each (courseId, lectureUID) pair among the sessions array, computes
 * the SET of students who attended ALL sessions of that lecture.
 *
 * Returns Map< `${courseId}::${lectureUID}` , LectureEntry >
 */
function buildLectureAttendance(sessions, records) {
  const lectureMap  = new Map();
  for (const s of sessions) {
    const key = `${s.course}::${s.lectureUID}`;
    if (!lectureMap.has(key)) {
      lectureMap.set(key, {
        courseId:    String(s.course),
        lectureUID:  s.lectureUID,
        sessionUIDs: [],
        presentInAll: null,
      });
    }
    lectureMap.get(key).sessionUIDs.push(s.sessionUID);
  }

  const sessStudents = new Map();
  for (const r of records) {
    if (!sessStudents.has(r.sessionUID)) sessStudents.set(r.sessionUID, new Set());
    sessStudents.get(r.sessionUID).add(String(r.student));
  }

  for (const lec of lectureMap.values()) {
    if (!lec.sessionUIDs.length) { lec.presentInAll = new Set(); continue; }
    const sorted = [...lec.sessionUIDs].sort(
      (a, b) => (sessStudents.get(a)?.size || 0) - (sessStudents.get(b)?.size || 0)
    );
    let inter = new Set(sessStudents.get(sorted[0]) || []);
    for (let i = 1; i < sorted.length && inter.size > 0; i++) {
      const o = sessStudents.get(sorted[i]) || new Set();
      for (const sid of inter) { if (!o.has(sid)) inter.delete(sid); }
    }
    lec.presentInAll = inter;
  }
  return lectureMap;
}

function canAccessCourse(course, userId, role) {
  if (role === 'admin') return true;
  if (course.instructors.map(String).includes(String(userId))) return true;
  return Array.isArray(course.tas) && course.tas.map(String).includes(String(userId));
}

// ── GET /analytics/course/:courseId ──────────────────────────────────────────
router.get('/course/:courseId', authenticate, async (req, res, next) => {
  try {
    const { courseId } = req.params;
    const course = await Course.findById(courseId).lean();
    if (!course) return res.status(404).json({ error: 'Course not found' });
    if (!canAccessCourse(course, req.user.user_id, req.user.role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const [sessions, enrolledCount] = await Promise.all([
      Session.find({ course: courseId }).lean(),
      Enrollment.countDocuments({ course: courseId, status: 'Active' }),
    ]);

    const records    = await Attendance.find({ sessionUID: { $in: sessions.map(s => s.sessionUID) } }).lean();
    const lectureMap = buildLectureAttendance(sessions, records);

    // Per-session detail lookup
    const sessById = Object.fromEntries(sessions.map(s => [s.sessionUID, s]));

    // Build lecture stats from course.lectures (canonical)
    const lectureStats = course.lectures.map(l => {
      const key       = `${courseId}::${l.lectureUID}`;
      const lec       = lectureMap.get(key);
      const sessionUIDs = lec?.sessionUIDs || [];
      const attended    = lec?.presentInAll?.size || 0;

      return {
        lectureUID:    l.lectureUID,
        scheduledTime: l.scheduledTime,
        cancelled:     l.cancelled,
        hasSession:    sessionUIDs.length > 0,
        sessionCount:  sessionUIDs.length,
        sessions: sessionUIDs.map(suid => ({
          sessionUID:  suid,
          method:      sessById[suid]?.method,
          timestamp:   sessById[suid]?.timestamp,
          markedCount: records.filter(r => r.sessionUID === suid).length,
        })),
        attended,
        enrolled:      enrolledCount,
        attendancePct: enrolledCount > 0
          ? parseFloat(((attended / enrolledCount) * 100).toFixed(1))
          : 0,
      };
    });

    const lecturesHeld  = lectureStats.filter(l => l.hasSession && !l.cancelled);
    const totalAttended = lectureStats.reduce((s, l) => s + l.attended, 0);
    const totalPossible = lecturesHeld.length * enrolledCount;

    res.json({
      courseId,
      courseName:    course.name,
      totalLectures: course.lectures.length,
      lecturesHeld:  lecturesHeld.length,
      enrolled:      enrolledCount,
      overallAttendancePct: totalPossible > 0
        ? parseFloat(((totalAttended / totalPossible) * 100).toFixed(1))
        : 0,
      lectureStats,
    });
  } catch (err) { next(err); }
});

// ── GET /analytics/course/:courseId/students ──────────────────────────────────
router.get('/course/:courseId/students', authenticate, async (req, res, next) => {
  try {
    const { courseId } = req.params;
    const course = await Course.findById(courseId).lean();
    if (!course) return res.status(404).json({ error: 'Course not found' });
    if (!canAccessCourse(course, req.user.user_id, req.user.role)) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const [sessions, enrollments] = await Promise.all([
      Session.find({ course: courseId }).lean(),
      Enrollment.find({ course: courseId, status: 'Active' }).lean(),
    ]);

    const studentIds  = enrollments.map(e => e.student);
    const sessionUIDs = sessions.map(s => s.sessionUID);

    const records = await Attendance.find({
      sessionUID: { $in: sessionUIDs },
      student:    { $in: studentIds },
    }).lean();

    const lectureMap = buildLectureAttendance(sessions, records);

    // Number of lectures that have ≥1 session (lectures held)
    const totalLectures = lectureMap.size;

    const studentCount = {};
    for (const lec of lectureMap.values()) {
      for (const sid of lec.presentInAll) {
        studentCount[sid] = (studentCount[sid] || 0) + 1;
      }
    }

    const students = await Student.find({ _id: { $in: studentIds } }).select('-password').lean();

    const studentStats = students.map(s => {
      const attended = studentCount[String(s._id)] || 0;
      return {
        student_id:    s._id,
        name:          s.name,
        email:         s.email,
        imageURL:      s.imageURL,
        attended,
        totalLectures,
        attendancePct: totalLectures > 0
          ? parseFloat(((attended / totalLectures) * 100).toFixed(1))
          : 0,
      };
    }).sort((a, b) => b.attended - a.attended);

    res.json({ courseId, totalLectures, enrolled: studentIds.length, studentStats });
  } catch (err) { next(err); }
});

// ── GET /analytics/prof/:profId ───────────────────────────────────────────────
router.get('/prof/:profId', authenticate, async (req, res, next) => {
  try {
    const { profId } = req.params;
    if (req.user.role !== 'admin' && req.user.user_id !== profId) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const courses = await Course.find({
      $or: [{ instructors: profId }, { tas: profId }],
    }).lean();

    if (courses.length === 0) return res.json([]);

    const courseIds = courses.map(c => c._id);
    const [sessions, enrollAgg] = await Promise.all([
      Session.find({ course: { $in: courseIds } }).lean(),
      Enrollment.aggregate([
        { $match: { course: { $in: courseIds }, status: 'Active' } },
        { $group: { _id: '$course', count: { $sum: 1 } } },
      ]),
    ]);

    const enrollMap  = Object.fromEntries(enrollAgg.map(e => [String(e._id), e.count]));
    const records    = await Attendance.find({ sessionUID: { $in: sessions.map(s => s.sessionUID) } }).lean();
    const lectureMap = buildLectureAttendance(sessions, records);

    // Group by course
    const lecsByCourse = {};
    for (const [key, lec] of lectureMap.entries()) {
      const cid = lec.courseId;
      if (!lecsByCourse[cid]) lecsByCourse[cid] = [];
      lecsByCourse[cid].push(lec);
    }

    const result = courses.map(c => {
      const cid      = String(c._id);
      const lecs     = lecsByCourse[cid] || [];
      const enrolled = enrollMap[cid] || 0;
      const totalAtt = lecs.reduce((s, l) => s + l.presentInAll.size, 0);
      const possible = lecs.length * enrolled;

      return {
        course_id:   c._id,
        course_name: c.name,
        slot:        c.slot,
        sessions:    sessions.filter(s => String(s.course) === cid).length,
        lectures:    c.lectures.length,
        lecturesHeld: lecs.length,
        enrolled,
        attendance:  totalAtt,
        avg_pct:     possible > 0
          ? parseFloat(((totalAtt / possible) * 100).toFixed(1))
          : 0,
      };
    });

    res.json(result);
  } catch (err) { next(err); }
});

// ── GET /analytics/at-risk/:profId ────────────────────────────────────────────
router.get('/at-risk/:profId', authenticate, async (req, res, next) => {
  try {
    const { profId } = req.params;
    if (req.user.role !== 'admin' && req.user.user_id !== profId) {
      return res.status(403).json({ error: 'Forbidden' });
    }

    const courses = await Course.find({
      $or: [{ instructors: profId }, { tas: profId }],
    }).lean();

    const courseIds = courses.map(c => c._id);
    const courseMap = Object.fromEntries(courses.map(c => [String(c._id), c]));

    const sessions    = await Session.find({ course: { $in: courseIds } }).lean();
    const sessionUIDs = sessions.map(s => s.sessionUID);

    const records    = await Attendance.find({ sessionUID: { $in: sessionUIDs } }).lean();
    const lectureMap = buildLectureAttendance(sessions, records);

    // Lectures held per course
    const lecsByCourse = {};
    for (const [, lec] of lectureMap.entries()) {
      if (!lecsByCourse[lec.courseId]) lecsByCourse[lec.courseId] = [];
      lecsByCourse[lec.courseId].push(lec);
    }

    // Student → course → lecturesAttended
    const matrix = {};
    for (const [, lec] of lectureMap.entries()) {
      for (const sid of lec.presentInAll) {
        if (!matrix[sid]) matrix[sid] = {};
        matrix[sid][lec.courseId] = (matrix[sid][lec.courseId] || 0) + 1;
      }
    }

    // Also include students with 0 attendance via enrollments
    const enrollments = await Enrollment.find({ course: { $in: courseIds }, status: 'Active' }).lean();
    for (const e of enrollments) {
      const sid = String(e.student);
      const cid = String(e.course);
      if (!matrix[sid]) matrix[sid] = {};
      if (matrix[sid][cid] === undefined) matrix[sid][cid] = 0;
    }

    const THRESHOLD = parseFloat(process.env.AT_RISK_THRESHOLD || '75');

    const atRisk = [];
    for (const [studentId, courseCounts] of Object.entries(matrix)) {
      for (const [courseId, attended] of Object.entries(courseCounts)) {
        const total = (lecsByCourse[courseId] || []).length;
        if (!total) continue;
        const pct = parseFloat(((attended / total) * 100).toFixed(1));
        if (pct < THRESHOLD) {
          atRisk.push({ student_id: studentId, course_id: courseId, attended, total, pct });
        }
      }
    }

    const studentIds = [...new Set(atRisk.map(r => r.student_id))];
    const students   = await Student.find({ _id: { $in: studentIds } }).select('-password').lean();
    const sMap       = Object.fromEntries(students.map(s => [String(s._id), s]));

    res.json(atRisk.map(r => ({
      ...r,
      student_name: sMap[r.student_id]?.name || r.student_id,
      email:        sMap[r.student_id]?.email,
      course_name:  courseMap[r.course_id]?.name,
    })).sort((a, b) => a.pct - b.pct));
  } catch (err) { next(err); }
});

// ── GET /analytics/admin ──────────────────────────────────────────────────────
router.get('/admin', authenticate, authorize('admin'), async (req, res, next) => {
  try {
    const [professors, courses, sessions, studentCount, enrollmentCount] = await Promise.all([
      Professor.find().select('-password').lean(),
      Course.find().lean(),
      Session.find().lean(),
      Student.countDocuments(),
      Enrollment.countDocuments({ status: 'Active' }),
    ]);

    const records    = await Attendance.find({ sessionUID: { $in: sessions.map(s => s.sessionUID) } }).lean();
    const lectureMap = buildLectureAttendance(sessions, records);

    const enrollAgg = await Enrollment.aggregate([
      { $match: { status: 'Active' } },
      { $group: { _id: '$course', count: { $sum: 1 } } },
    ]);
    const enrollMap = Object.fromEntries(enrollAgg.map(e => [String(e._id), e.count]));

    const lecsByCourse = {};
    for (const [, lec] of lectureMap.entries()) {
      if (!lecsByCourse[lec.courseId]) lecsByCourse[lec.courseId] = [];
      lecsByCourse[lec.courseId].push(lec);
    }

    const profs = professors.map(p => ({
      prof_id:   p._id,
      prof_name: p.name,
      courses: courses
        .filter(c => c.instructors.map(String).includes(String(p._id)))
        .map(c => {
          const cid      = String(c._id);
          const lecs     = lecsByCourse[cid] || [];
          const enrolled = enrollMap[cid] || 0;
          const totalAtt = lecs.reduce((s, l) => s + l.presentInAll.size, 0);
          const possible = lecs.length * enrolled;
          return {
            course_id:    c._id,
            name:         c.name,
            sessions:     sessions.filter(s => String(s.course) === cid).length,
            lectures:     c.lectures.length,
            lecturesHeld: lecs.length,
            enrolled,
            avg_pct: possible > 0
              ? parseFloat(((totalAtt / possible) * 100).toFixed(1))
              : 0,
          };
        }),
    }));

    res.json({
      totals: {
        sessions:    sessions.length,
        attendance:  records.length,
        avg:         sessions.length > 0
          ? parseFloat((records.length / sessions.length).toFixed(1))
          : 0,
        students:    studentCount,
        courses:     courses.length,
        profs:       professors.length,
        enrollments: enrollmentCount,
      },
      profs,
    });
  } catch (err) { next(err); }
});

module.exports = router;