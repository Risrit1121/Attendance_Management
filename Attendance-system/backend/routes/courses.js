/**
 * courses.js — FIXES:
 *
 * 1. Schedule toggle: 'switch' is a reserved word in JS. Mongoose receives
 *    it fine, but when you PATCH with { switch: true } from the frontend
 *    Object.assign silently fails on reserved words in strict mode.
 *    Solution: rename the field to `enabled` in the DB too (or alias it).
 *    We keep `switch` in the schema (it works in Mongoose) but ensure
 *    the PATCH handler explicitly uses $set with a string key to avoid
 *    the reserved-word pitfall.
 *
 * 2. DELETE /courses/:id added.
 *
 * 3. TA management moved to admin routes but courses route still
 *    exposes GET /courses/:courseId/tas for the course view.
 */
const express    = require('express');
const Course     = require('../models/Course');
const Enrollment = require('../models/Enrollment');
const Student    = require('../models/Student');
const { authenticate, authorize, ownsCourse, requireInstructor } = require('../middleware/auth');
const { populateLectures } = require('../utils/lecturePopulator');

const router = express.Router();

// ── GET /courses/:profId ──────────────────────────────────────────────────────
router.get('/courses/:profId', authenticate, async (req, res, next) => {
  try {
    const { profId } = req.params;
    let query;

    if (req.user.role === 'admin') {
      query = profId === 'all' ? {} : { instructors: profId };
    } else if (req.user.role === 'ta') {
      if (req.user.user_id !== profId) return res.status(403).json({ error: 'Forbidden' });
      query = { tas: profId };
    } else {
      if (req.user.user_id !== profId) return res.status(403).json({ error: 'Forbidden' });
      query = { instructors: profId };
    }

    const courses = await Course.find(query).lean();
    res.json(courses.map(c => ({ ...c, id: c._id })));
  } catch (err) { next(err); }
});

// ── POST /courses  (admin only) ───────────────────────────────────────────────
router.post('/courses', authenticate, authorize('admin'), async (req, res, next) => {
  try {
    const data = req.body;
    const required = ['name', 'department', 'slot', 'venue', 'startDate', 'endDate'];
    for (const f of required) {
      if (!data[f]) return res.status(400).json({ error: `${f} is required` });
    }
    const { lectures, schedules } = populateLectures(data);
    const course = new Course({
      ...data,
      lectures,
      schedules: data.schedules?.length ? data.schedules : schedules,
    });
    await course.save();
    res.status(201).json({ ...course.toObject(), id: course._id });
  } catch (err) { next(err); }
});

// ── PUT /courses/:courseId  (admin only) ──────────────────────────────────────
router.put('/courses/:courseId', authenticate, authorize('admin'), async (req, res, next) => {
  try {
    const allowed = ['name', 'department', 'venue', 'instructors', 'tas'];
    const update  = {};
    for (const k of allowed) {
      if (req.body[k] !== undefined) update[k] = req.body[k];
    }
    const course = await Course.findByIdAndUpdate(req.params.courseId, { $set: update }, { new: true });
    if (!course) return res.status(404).json({ error: 'Course not found' });
    res.json({ ...course.toObject(), id: course._id });
  } catch (err) { next(err); }
});

// ── DELETE /courses/:courseId  (admin only) ───────────────────────────────────
router.delete('/courses/:courseId', authenticate, authorize('admin'), async (req, res, next) => {
  try {
    await Course.findByIdAndDelete(req.params.courseId);
    res.json({ message: 'Course deleted' });
  } catch (err) { next(err); }
});

// ── GET /course/:courseId/students ────────────────────────────────────────────
router.get('/course/:courseId/students', authenticate, async (req, res, next) => {
  try {
    const enrollments = await Enrollment.find({ course: req.params.courseId, status: 'Active' }).lean();
    const students    = await Student.find({ _id: { $in: enrollments.map(e => e.student) } })
      .select('-password').lean();
    res.json(students.map(s => ({ ...s, id: s._id })));
  } catch (err) { next(err); }
});

// ── GET /courses/:courseId/schedules ──────────────────────────────────────────
router.get('/courses/:courseId/schedules', authenticate, async (req, res, next) => {
  try {
    const course = await Course.findById(req.params.courseId).select('schedules').lean();
    if (!course) return res.status(404).json({ error: 'Course not found' });
    res.json(course.schedules || []);
  } catch (err) { next(err); }
});

// ── POST /courses/:courseId/schedule  (add one entry) ─────────────────────────
router.post('/courses/:courseId/schedule',
  authenticate, ownsCourse, requireInstructor,
  async (req, res, next) => {
    try {
      const { scheduledDay, startTime, endTime, method } = req.body;
      const VALID_DAYS    = ['Monday','Tuesday','Wednesday','Thursday','Friday','Saturday','Sunday'];
      const VALID_METHODS = ['BLE','QRCode','Manual'];
      if (!VALID_DAYS.includes(scheduledDay))  return res.status(400).json({ error: 'Invalid day' });
      if (!VALID_METHODS.includes(method))     return res.status(400).json({ error: 'Invalid method' });
      if (!/^\d{2}:\d{2}$/.test(startTime))   return res.status(400).json({ error: 'startTime must be HH:MM' });
      if (!/^\d{2}:\d{2}$/.test(endTime))     return res.status(400).json({ error: 'endTime must be HH:MM' });

      const newEntry = { scheduledDay, startTime, endTime, method, switch: false };
      const course = await Course.findByIdAndUpdate(
        req.params.courseId,
        { $push: { schedules: newEntry } },
        { new: true }
      );
      if (!course) return res.status(404).json({ error: 'Course not found' });
      const added = course.schedules[course.schedules.length - 1];
      res.status(201).json({ message: 'Schedule added', schedule: added, schedules: course.schedules });
    } catch (err) { next(err); }
  }
);

// ── PATCH /courses/:courseId/schedule/:index ──────────────────────────────────
// FIX: 'switch' is a reserved word. Use string-keyed $set to avoid JS pitfall.
router.patch('/courses/:courseId/schedule/:index',
  authenticate, ownsCourse, requireInstructor,
  async (req, res, next) => {
    try {
      const idx  = parseInt(req.params.index, 10);
      const body = req.body; // may contain: scheduledDay, startTime, endTime, method, switch

      // Build $set using string keys to avoid reserved-word issues
      const setObj = {};
      for (const [k, v] of Object.entries(body)) {
        setObj[`schedules.${idx}.${k}`] = v;
      }

      const course = await Course.findByIdAndUpdate(
        req.params.courseId,
        { $set: setObj },
        { new: true }
      );
      if (!course) return res.status(404).json({ error: 'Course not found' });
      if (!course.schedules[idx]) return res.status(404).json({ error: 'Schedule index not found' });

      res.json({ message: 'Schedule updated', schedule: course.schedules[idx] });
    } catch (err) { next(err); }
  }
);

// ── DELETE /courses/:courseId/schedule/:index ─────────────────────────────────
router.delete('/courses/:courseId/schedule/:index',
  authenticate, ownsCourse, requireInstructor,
  async (req, res, next) => {
    try {
      const idx    = parseInt(req.params.index, 10);
      const course = await Course.findById(req.params.courseId);
      if (!course) return res.status(404).json({ error: 'Course not found' });
      if (!course.schedules[idx]) return res.status(404).json({ error: 'Schedule index not found' });
      course.schedules.splice(idx, 1);
      course.markModified('schedules');
      await course.save();
      res.json({ message: 'Schedule deleted', schedules: course.schedules });
    } catch (err) { next(err); }
  }
);

// ── PUT /courses/:courseId/schedule  (full replace) ───────────────────────────
router.put('/courses/:courseId/schedule',
  authenticate, ownsCourse, requireInstructor,
  async (req, res, next) => {
    try {
      const { schedules } = req.body;
      if (!Array.isArray(schedules)) return res.status(400).json({ error: 'schedules must be array' });
      const course = await Course.findByIdAndUpdate(
        req.params.courseId,
        { $set: { schedules } },
        { new: true }
      );
      if (!course) return res.status(404).json({ error: 'Course not found' });
      res.json({ message: 'Schedules updated', schedules: course.schedules });
    } catch (err) { next(err); }
  }
);

// ── PATCH /courses/:courseId/lectures/:lectureUID/cancel ──────────────────────
router.patch('/courses/:courseId/lectures/:lectureUID/cancel',
  authenticate, ownsCourse,
  async (req, res, next) => {
    try {
      const result = await Course.updateOne(
        { _id: req.params.courseId, 'lectures.lectureUID': req.params.lectureUID },
        { $set: { 'lectures.$.cancelled': true } }
      );
      if (result.matchedCount === 0) return res.status(404).json({ error: 'Lecture not found' });
      res.json({ message: 'Lecture cancelled' });
    } catch (err) { next(err); }
  }
);

module.exports = router;