const { v4: uuidv4 } = require('uuid');

// SLOT → days of week (0=Sun…6=Sat) + time windows
// These are example VIT-style slot mappings. Adjust to your institution.
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

/**
 * Given a course (with slot, startDate, endDate),
 * generate the full lecture list using SLOT_MAP.
 * Also populates the `schedules` array on the course.
 */
function populateLectures(course) {
  const slotWindows = SLOT_MAP[course.slot] || [];
  const lectures = [];
  const schedules = [];

  // Build schedules array from slot windows
  for (const w of slotWindows) {
    schedules.push({
      scheduledDay: DAY_NAMES[w.day],
      startTime: w.start,
      endTime:   w.end,
      method:    'BLE',
      switch:    false,
    });
  }

  // Walk every day from startDate to endDate
  const cursor = new Date(course.startDate);
  const end    = new Date(course.endDate);
  cursor.setHours(0, 0, 0, 0);
  end.setHours(23, 59, 59, 999);

  while (cursor <= end) {
    const dow = cursor.getDay(); // 0=Sun…6=Sat
    for (const w of slotWindows) {
      if (w.day === dow) {
        const [hh, mm] = w.start.split(':').map(Number);
        const scheduledTime = new Date(cursor);
        scheduledTime.setHours(hh, mm, 0, 0);

        lectures.push({
          lectureUID:    uuidv4(),
          scheduledTime,
          cancelled:     false,
        });
      }
    }
    cursor.setDate(cursor.getDate() + 1);
  }

  return { lectures, schedules };
}

module.exports = { populateLectures, SLOT_MAP };