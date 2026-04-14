/**
 * autoEndSessionService.js
 *
 * Called by the cron job every minute.
 * Finds all active sessions whose course schedule's endTime has passed (in IST)
 * and marks them as ended.
 *
 * This is the server-side counterpart to the SchedulerContext's client-side
 * auto-end logic.  The client-side logic only works while the browser tab is
 * open; this cron ensures sessions always end even if no one is logged in.
 *
 * Logic:
 *  1. Find all active sessions.
 *  2. For each session, load the course's schedules.
 *  3. Find the matching schedule entry (same day + lectureUID's IST time falls
 *     within startTime..endTime).
 *  4. If current IST time is >= endTime, end the session.
 */
const Session = require('../models/Session');
const Course  = require('../models/Course');

const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000;
const DAY_NAMES     = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];

function nowIST() {
  const d = new Date(Date.now() + IST_OFFSET_MS);
  return {
    dayName:  DAY_NAMES[d.getUTCDay()],
    totalMin: d.getUTCHours() * 60 + d.getUTCMinutes(),
  };
}

async function autoEndExpiredSessions() {
  const activeSessions = await Session.find({ active: true }).lean();
  if (activeSessions.length === 0) return;

  const { dayName, totalMin } = nowIST();

  // Group by course to minimise DB round-trips
  const byCourse = {};
  for (const s of activeSessions) {
    const cid = String(s.course);
    if (!byCourse[cid]) byCourse[cid] = [];
    byCourse[cid].push(s);
  }

  for (const [courseId, sessions] of Object.entries(byCourse)) {
    const course = await Course.findById(courseId).select('schedules').lean();
    if (!course?.schedules?.length) continue;

    for (const session of sessions) {
      // Find which schedule covers this session's lectureUID.
      // The scheduledTime is stored in UTC; convert to IST for comparison.
      const istMs      = new Date(session.scheduledTime).getTime() + IST_OFFSET_MS;
      const istDate    = new Date(istMs);
      const lecMin     = istDate.getUTCHours() * 60 + istDate.getUTCMinutes();
      const lecDayName = DAY_NAMES[istDate.getUTCDay()];

      const matchingSchedule = course.schedules.find(sch => {
        if (sch.scheduledDay !== lecDayName) return false;
        const [sh, sm] = sch.startTime.split(':').map(Number);
        const [eh, em] = sch.endTime.split(':').map(Number);
        return lecMin >= sh * 60 + sm && lecMin < eh * 60 + em;
      });

      if (!matchingSchedule) continue; // no schedule → manual session, don't touch

      const [eh, em] = matchingSchedule.endTime.split(':').map(Number);
      const scheduleEndMin = eh * 60 + em;

      // Only end if the session's schedule day matches today and endTime passed
      if (lecDayName === dayName && totalMin >= scheduleEndMin) {
        await Session.findOneAndUpdate(
          { sessionUID: session.sessionUID },
          { $set: { active: false, endedAt: new Date() } }
        );
        console.log(`[AutoEnd] Ended session ${session.sessionUID} (${matchingSchedule.endTime} IST passed)`);
      }
    }
  }
}

module.exports = { autoEndExpiredSessions };