const { v4: uuidv4 } = require('uuid');

// SLOT → days of week (0=Sun…6=Sat) + time windows
// All times below are IST (Asia/Kolkata, UTC+5:30).
// populateLectures() converts them to UTC when building Date objects so that
// MongoDB stores proper UTC timestamps.  Previously setHours() was used which
// interprets the wall-clock time as the *server's local time* (usually UTC in
// Docker), so a 09:00 IST slot was stored as 09:00 UTC — 5h30m too late.
// The ±30 min window in resolveOrCreateLecture then missed the lecture and
// created a spurious ad-hoc entry every time the scheduler fired.
const SLOT_MAP = {
  A: [
    { day: 1, start: '09:00', end: '09:55' },
    { day: 3, start: '11:00', end: '11:55' },
    { day: 4, start: '10:00', end: '10:55' }
  ],

  B: [
    { day: 1, start: '10:00', end: '10:55' },
    { day: 3, start: '09:00', end: '09:55' },
    { day: 4, start: '11:00', end: '11:55' }
  ],

  C: [
    { day: 1, start: '11:00', end: '11:55' },
    { day: 3, start: '10:00', end: '10:55' },
    { day: 4, start: '09:00', end: '09:55' }
  ],

  D: [
    { day: 1, start: '12:00', end: '12:55' },
    { day: 2, start: '09:00', end: '09:55' },
    { day: 5, start: '11:00', end: '11:55' }
  ],

  E: [
    { day: 2, start: '10:00', end: '10:55' },
    { day: 4, start: '12:00', end: '12:55' },
    { day: 5, start: '09:00', end: '09:55' }
  ],

  F: [
    { day: 2, start: '11:00', end: '11:55' },
    { day: 3, start: '14:30', end: '15:55' },
    { day: 5, start: '10:00', end: '10:55' }
  ],

  G: [
    { day: 2, start: '12:00', end: '12:55' },
    { day: 3, start: '12:00', end: '12:55' },
    { day: 5, start: '12:00', end: '12:55' }
  ],

  P: [
    { day: 1, start: '14:30', end: '15:55' },
    { day: 4, start: '16:00', end: '17:25' }
  ],

  Q: [
    { day: 1, start: '16:00', end: '17:25' },
    { day: 4, start: '14:30', end: '15:55' }
  ],

  R: [
    { day: 2, start: '14:30', end: '15:55' },
    { day: 5, start: '16:00', end: '17:25' }
  ],

  S: [
    { day: 2, start: '16:00', end: '17:25' },
    { day: 5, start: '14:30', end: '15:55' }
  ],

  W: [
    { day: 1, start: '17:30', end: '19:00' },
    { day: 4, start: '17:30', end: '19:00' }
  ],

  X: [
    { day: 1, start: '19:00', end: '20:30' },
    { day: 4, start: '19:00', end: '20:30' }
  ],

  Y: [
    { day: 2, start: '17:30', end: '19:00' },
    { day: 5, start: '17:30', end: '19:00' }
  ],

  Z: [
    { day: 2, start: '19:00', end: '20:30' },
    { day: 5, start: '19:00', end: '20:30' }
  ]
};

const DAY_NAMES = ['Sunday','Monday','Tuesday','Wednesday','Thursday','Friday','Saturday'];

// IST is UTC+5:30 = 330 minutes ahead of UTC.
const IST_OFFSET_MS = 5.5 * 60 * 60 * 1000; // 19800000 ms

/**
 * Convert a wall-clock "HH:mm" IST time on a given calendar date (cursor) to
 * a proper UTC Date.
 *
 * Strategy:
 *  1. Build a UTC midnight for the cursor date using its UTC year/month/day.
 *  2. Add the IST wall-clock minutes since midnight.
 *  3. Subtract the IST offset (5h30m) to get UTC.
 *
 * This is timezone-safe regardless of where the server runs.
 */
function istToUtc(cursor, hh, mm) {
  // cursor may be a local-midnight Date — extract its calendar date in UTC
  // (the cursor loop uses setHours(0,0,0,0) which is local midnight; if the
  //  server runs in UTC those are the same, but be explicit for safety)
  const y = cursor.getUTCFullYear();
  const mo = cursor.getUTCMonth();
  const d  = cursor.getUTCDate();

  // UTC midnight of that calendar day
  const utcMidnight = Date.UTC(y, mo, d);

  // IST wall-clock minutes past midnight for this slot
  const wallClockMs = (hh * 60 + mm) * 60 * 1000;

  // UTC = IST wall-clock - IST offset
  return new Date(utcMidnight + wallClockMs - IST_OFFSET_MS);
}

/**
 * Given a course (with slot, startDate, endDate),
 * generate the full lecture list using SLOT_MAP.
 * Also populates the `schedules` array on the course.
 *
 * All scheduledTime values are stored as UTC.
 * Schedule startTime/endTime strings remain as IST wall-clock (HH:mm) because
 * the frontend and SchedulerContext always interpret them as IST.
 */
function populateLectures(course) {
  const slotWindows = SLOT_MAP[course.slot] || [];
  const lectures = [];
  const schedules = [];

  // Build schedules array from slot windows (times stay as IST strings)
  for (const w of slotWindows) {
    schedules.push({
      scheduledDay: DAY_NAMES[w.day],
      startTime: w.start,
      endTime:   w.end,
      method:    'BLE',
      switch:    false,
    });
  }

  // Walk every day from startDate to endDate.
  // Use UTC date arithmetic so DST / server-timezone do not shift the calendar.
  const startD = new Date(course.startDate);
  const endD   = new Date(course.endDate);

  // Build a UTC-midnight cursor from the start date's calendar day
  let cursorUtcMs = Date.UTC(
    startD.getUTCFullYear(),
    startD.getUTCMonth(),
    startD.getUTCDate()
  );
  const endUtcMs = Date.UTC(
    endD.getUTCFullYear(),
    endD.getUTCMonth(),
    endD.getUTCDate()
  );
  const ONE_DAY_MS = 24 * 60 * 60 * 1000;

  while (cursorUtcMs <= endUtcMs) {
    const cursorDate = new Date(cursorUtcMs);
    const dow = cursorDate.getUTCDay(); // 0=Sun…6=Sat

    for (const w of slotWindows) {
      if (w.day === dow) {
        const [hh, mm] = w.start.split(':').map(Number);
        const scheduledTime = istToUtc(cursorDate, hh, mm);

        lectures.push({
          lectureUID:    uuidv4(),
          scheduledTime, // proper UTC timestamp
          cancelled:     false,
        });
      }
    }
    cursorUtcMs += ONE_DAY_MS;
  }

  return { lectures, schedules };
}

module.exports = { populateLectures, SLOT_MAP, IST_OFFSET_MS };