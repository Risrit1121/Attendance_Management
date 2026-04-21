/**
 * bucketService.js
 *
 * The Bucket is a per-student cache of which lectures they fully attended.
 *
 * Event-loop yielding in rebuildAllBuckets:
 *   The full rebuild runs every 15 minutes via cron. With 80+ students it
 *   previously held the event loop during the synchronous parts of each
 *   batch iteration (Promise.all resolves, but the orchestration loop itself
 *   doesn't yield). This caused API requests to queue up and time out,
 *   showing as "0 courses / 0 analytics" on the frontend.
 *
 *   Fix: await a setImmediate() between batches, giving the event loop a
 *   chance to drain its I/O callback queue (pending HTTP requests, etc.)
 *   before starting the next batch of DB work.
 */

const Bucket     = require('../models/Bucket');
const Attendance = require('../models/Attendance');
const Enrollment = require('../models/Enrollment');
const Session    = require('../models/Session');

// ── Yield helper ──────────────────────────────────────────────────────────────
// Returns a promise that resolves on the next event-loop iteration, allowing
// pending I/O callbacks (incoming HTTP requests) to run between batches.
function yieldToEventLoop() {
  return new Promise(resolve => setImmediate(resolve));
}

// ── Core intersection ─────────────────────────────────────────────────────────
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

  const lectureMap    = buildLectureAttendance(sessions, records);
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

async function updateBucketForStudent(studentId) {
  return rebuildBucketForStudent(studentId).catch(err =>
    console.error(`[Bucket] Update failed for ${studentId}:`, err.message)
  );
}

/**
 * Full rebuild — called by nightly cron and admin /rebuild-buckets.
 *
 * Yields to the event loop between every batch of 50 so that incoming
 * HTTP requests are not starved during the rebuild cycle.
 */
async function rebuildAllBuckets() {
  const studentIds = await Enrollment.distinct('student');
  console.log(`[BucketJob] Rebuilding ${studentIds.length} buckets...`);
  const BATCH = 50;
  for (let i = 0; i < studentIds.length; i += BATCH) {
    // Yield between batches — lets pending HTTP requests be handled before
    // the next batch of heavy DB queries starts.
    if (i > 0) await yieldToEventLoop();

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

async function getBucketForStudent(studentId) {
  return Bucket.findOne({ studentUID: studentId }).lean();
}

module.exports = { updateBucketForStudent, rebuildBucketForStudent, rebuildAllBuckets, getBucketForStudent };