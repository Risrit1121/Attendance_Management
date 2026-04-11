/**
 * bucketService.js
 *
 * The Bucket is a per-student cache of which lectures they fully attended
 * (present in ALL sessions of that lecture). It is used to:
 *
 *  1. Fast read in GET /admin/student/:id/analytics (avoids scanning Attendance)
 *  2. Fast read in GET /student/:id/courses (quick attendance summary per course)
 *  3. The Students page heatmap (already uses /student/:id/history which is live,
 *     but the bucket lets us pre-compute overall pct per course cheaply)
 *
 * Write path: after every Attendance.mark, we call updateBucketForStudent(studentId)
 * which rebuilds just that student's bucket. The nightly cron rebuilds all buckets
 * as a safety net in case any incremental updates were missed.
 */

const Bucket     = require('../models/Bucket');
const Attendance = require('../models/Attendance');
const Enrollment = require('../models/Enrollment');
const Session    = require('../models/Session');

// ── Core intersection (copy of analytics logic — single source of truth) ───────
function buildLectureAttendance(sessions, records) {
  const lectureMap  = new Map();
  for (const s of sessions) {
    const key = `${s.course}::${s.lectureUID}`;
    if (!lectureMap.has(key)) {
      lectureMap.set(key, { courseId: String(s.course), lectureUID: s.lectureUID, sessionUIDs: [], presentInAll: null });
    }
    lectureMap.get(key).sessionUIDs.push(s.sessionUID);
  }
  const sessStudents = new Map();
  for (const r of records) {
    if (!sessStudents.has(r.sessionUID)) sessStudents.set(r.sessionUID, new Set());
    sessStudents.get(r.sessionUID).add(r.student);
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

/**
 * Rebuild bucket for one student.
 * Bucket shape:
 *   { studentUID, courses: [{ courseUID, lectures: [{ lectureUID }] }], lastUpdated }
 *
 * Each entry in lectures[] = a lecture where the student was present in ALL sessions.
 */
async function rebuildBucketForStudent(studentId) {
  const enrollments = await Enrollment.find({ student: studentId, status: 'Active' }).lean();
  const courseIds   = enrollments.map(e => e.course);

  if (courseIds.length === 0) {
    await Bucket.findOneAndUpdate(
      { studentUID: studentId },
      { $set: { courses: [], lastUpdated: new Date() } },
      { upsert: true }
    );
    return;
  }

  const sessions = await Session.find({ course: { $in: courseIds } }).lean();
  const records  = await Attendance.find({
    sessionUID: { $in: sessions.map(s => s.sessionUID) },
  }).lean();

  const lectureMap   = buildLectureAttendance(sessions, records);
  const courseDataMap = {};

  for (const lec of lectureMap.values()) {
    if (!lec.presentInAll.has(studentId)) continue;
    if (!courseDataMap[lec.courseId]) courseDataMap[lec.courseId] = [];
    courseDataMap[lec.courseId].push(lec.lectureUID);
  }

  const courses = courseIds.map(cid => ({
    courseUID: String(cid),
    lectures:  (courseDataMap[String(cid)] || []).map(luid => ({
      lectureUID:  luid,
      attendance:  { present: 'true' },
    })),
  }));

  await Bucket.findOneAndUpdate(
    { studentUID: studentId },
    { $set: { courses, lastUpdated: new Date() } },
    { upsert: true }
  );
}

/**
 * Fire-and-forget after each Attendance mark.
 * Rebuilds only the affected student.
 */
async function updateBucketForStudent(studentId) {
  return rebuildBucketForStudent(studentId).catch(err =>
    console.error(`[Bucket] Update failed for ${studentId}:`, err.message)
  );
}

/**
 * Full rebuild — called by nightly cron and admin /rebuild-buckets.
 */
async function rebuildAllBuckets() {
  const studentIds = await Enrollment.distinct('student');
  console.log(`[BucketJob] Rebuilding ${studentIds.length} buckets...`);
  const BATCH = 50;
  for (let i = 0; i < studentIds.length; i += BATCH) {
    await Promise.all(
      studentIds.slice(i, i + BATCH).map(sid =>
        rebuildBucketForStudent(sid).catch(e =>
          console.error(`[BucketJob] ${sid}: ${e.message}`)
        )
      )
    );
  }
  console.log('[BucketJob] Done.');
}

/**
 * Read bucket for a student — used by the students page for quick per-course summary.
 * Returns null if no bucket exists yet.
 */
async function getBucketForStudent(studentId) {
  return Bucket.findOne({ studentUID: studentId }).lean();
}

module.exports = { updateBucketForStudent, rebuildBucketForStudent, rebuildAllBuckets, getBucketForStudent };