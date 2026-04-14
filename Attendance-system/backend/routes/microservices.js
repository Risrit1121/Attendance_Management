/**
 * microservices.js
 *
 * Proxies calls to the external BLE and QR microservices.
 * Base URL configured via env vars:
 *   BLE_SERVICE_URL  = http://localhost:8000  (default)
 *   QR_SERVICE_URL   = http://localhost:8000  (same service, different prefix)
 *
 * BLE microservice API (from the spec):
 *   POST /ble/generate-minor  { major_id }  → { minor, expires_in }
 *   POST /ble/validate        { class_id, beacons: [{major,minor,rssi}] } → { valid }
 *
 * QR microservice API (from the spec):
 *   POST /qr/generate  { class_id }                → { hash, expires_in }
 *   POST /qr/validate  { class_id, hash, timestamp } → { valid }
 *
 * Our backend exposes:
 *   GET  /getMinor?major=...          → calls POST /ble/generate-minor
 *   POST /ble/validate                → proxies POST /ble/validate (adds course lookup)
 *   GET  /getQR/:sessionId            → calls POST /qr/generate
 *   POST /qr/validate                 → proxies POST /qr/validate
 *   POST /markAttendance (existing)   → unchanged, called after validation succeeds
 *
 * The BLE validate flow:
 *   Student app → POST /ble/validate (our backend) → microservice → if valid → mark attendance
 *
 * The QR flow:
 *   Prof screen → GET /getQR/:sessionId → microservice → display hash as QR
 *   Student app → POST /qr/validate    → microservice → if valid → mark attendance
 */

const express    = require('express');
const axios      = require('axios');
const Session    = require('../models/Session');
const Attendance = require('../models/Attendance');
const Enrollment = require('../models/Enrollment');
const Beacon     = require('../models/Beacon');
const { updateBucketForStudent } = require('../services/bucketService');
const { authenticate } = require('../middleware/auth');

const router = express.Router();

const BLE_URL = process.env.BLE_SERVICE_URL || 'http://localhost:8000';
const QR_URL  = process.env.QR_SERVICE_URL  || 'http://localhost:8000';

// ── Helper: call microservice with timeout ─────────────────────────────────────
async function msPost(baseUrl, path, body) {
  const res = await axios.post(`${baseUrl}${path}`, body, {
    timeout: 5000,
    headers: { 'Content-Type': 'application/json' },
  });
  return res.data;
}

// ── GET /getMinor?major=...  (ESP32 → our backend → BLE microservice) ─────────
// The ESP32 calls this to get its rotating minor value.
router.get('/getMinor', authenticate, async (req, res, next) => {
  try {
    const { major } = req.query;
    if (!major) return res.status(400).json({ error: 'major required' });

    const data = await msPost(BLE_URL, '/ble/generate-minor', { major_id: major });
    // data = { minor, expires_in }
    res.json({ minor: data.minor, expiresIn: data.expires_in, major });
  } catch (err) {
    if (err.code === 'ECONNREFUSED' || err.code === 'ETIMEDOUT') {
      return res.status(503).json({ error: 'BLE microservice unavailable' });
    }
    next(err);
  }
});

// ── POST /ble/validate  (student app → our backend → BLE microservice) ────────
// Body: { session_id, beacons: [{major, minor, rssi}] }
// Picks the strongest beacon (highest RSSI), validates with microservice,
// and if valid marks attendance for the student.
router.post('/ble/validate', authenticate, async (req, res, next) => {
  try {
    const { session_id, beacons } = req.body;
    if (!session_id || !Array.isArray(beacons) || beacons.length === 0) {
      return res.status(400).json({ error: 'session_id and beacons[] required' });
    }

    const session = await Session.findOne({ sessionUID: session_id, active: true }).lean();
    if (!session) return res.status(404).json({ error: 'No active session found' });
    if (session.method !== 'BLE') {
      return res.status(400).json({ error: 'Session is not BLE mode' });
    }

    const studentId = req.user.user_id;

    // Verify enrollment
    const enrolled = await Enrollment.findOne({
      student: studentId,
      course:  session.course,
      status:  'Active',
    }).lean();
    if (!enrolled) return res.status(403).json({ error: 'Not enrolled in this course' });

    // Look up classroom from beacon to get class_id for the microservice
    const bestBeacon = [...beacons].sort((a, b) => b.rssi - a.rssi)[0];
    const beaconDoc  = await Beacon.findOne({ bleID: bestBeacon.major }).lean();
    const classId    = beaconDoc?.classroom || session.course;

    // Call BLE microservice — send ALL beacons as per spec
    let msResult;
    try {
      msResult = await msPost(BLE_URL, '/ble/validate', {
        class_id: classId,
        beacons,
      });
    } catch (e) {
      // If microservice is down, fall back to basic beacon lookup
      const beaconValid = await Beacon.findOne({
        bleID:     bestBeacon.major,
        classroom: { $exists: true },
      }).lean();
      msResult = { valid: !!beaconValid };
    }

    if (!msResult.valid) {
      return res.status(400).json({ valid: false, error: 'Beacon validation failed' });
    }

    // Mark attendance
    await Attendance.findOneAndUpdate(
      { sessionUID: session_id, student: studentId },
      { $setOnInsert: { sessionUID: session_id, student: studentId, verifiedVia: 'BLE', markedAt: new Date() } },
      { upsert: true, new: true }
    );

    updateBucketForStudent(studentId).catch(console.error);
    res.json({ valid: true, message: 'Attendance marked via BLE' });
  } catch (err) { next(err); }
});

// ── GET /getQR/:sessionId  (prof screen → our backend → QR microservice) ──────
// Returns a hash that the frontend renders as a QR code.
// Refreshes every 5 seconds (frontend polls).
router.get('/getQR/:sessionId', authenticate, async (req, res, next) => {
  try {
    const session = await Session.findOne({ sessionUID: req.params.sessionId }).lean();
    if (!session) return res.status(404).json({ error: 'Session not found' });

    let hash, expiresIn;
    try {
      const data = await msPost(QR_URL, '/qr/generate', { class_id: session.course });
      hash      = data.hash;
      expiresIn = data.expires_in * 1000; // convert to ms
    } catch (e) {
      // Toy fallback (microservice down)
      const crypto = require('crypto');
      const window  = Math.floor(Date.now() / 5000);
      hash      = crypto.createHmac('sha256', process.env.QR_SECRET || 'qr-secret')
        .update(`${session.sessionUID}:${window}`).digest('hex').slice(0, 8);
      expiresIn = 5000 - (Date.now() % 5000);
    }

    res.json({ qr: hash, expiresIn, sessionId: req.params.sessionId });
  } catch (err) { next(err); }
});

// ── POST /qr/validate  (student app → our backend → QR microservice) ──────────
// Body: { session_id, hash }
// Validates with microservice and marks attendance if valid.
router.post('/qr/validate', authenticate, async (req, res, next) => {
  try {
    const { session_id, hash } = req.body;
    if (!session_id || !hash) {
      return res.status(400).json({ error: 'session_id and hash required' });
    }

    const session = await Session.findOne({ sessionUID: session_id, active: true }).lean();
    if (!session) return res.status(404).json({ error: 'No active session found' });
    if (session.method !== 'QRCode') {
      return res.status(400).json({ error: 'Session is not QR mode' });
    }

    const studentId = req.user.user_id;
    const enrolled  = await Enrollment.findOne({
      student: studentId, course: session.course, status: 'Active',
    }).lean();
    if (!enrolled) return res.status(403).json({ error: 'Not enrolled in this course' });

    // Validate with QR microservice — timestamp is NOW (per spec: Date.now() at scan)
    let msResult;
    try {
      msResult = await msPost(QR_URL, '/qr/validate', {
        class_id:  session.course,
        hash,
        timestamp: Date.now(),
      });
    } catch (e) {
      // Toy fallback
      const crypto = require('crypto');
      const window  = Math.floor(Date.now() / 5000);
      const expected = crypto.createHmac('sha256', process.env.QR_SECRET || 'qr-secret')
        .update(`${session_id}:${window}`).digest('hex').slice(0, 8);
      const prevWindow = Math.floor((Date.now() - 5000) / 5000);
      const prevExpected = crypto.createHmac('sha256', process.env.QR_SECRET || 'qr-secret')
        .update(`${session_id}:${prevWindow}`).digest('hex').slice(0, 8);
      msResult = { valid: hash === expected || hash === prevExpected };
    }

    if (!msResult.valid) {
      return res.status(400).json({ valid: false, error: 'QR code invalid or expired' });
    }

    await Attendance.findOneAndUpdate(
      { sessionUID: session_id, student: studentId },
      { $setOnInsert: { sessionUID: session_id, student: studentId, verifiedVia: 'QRCode', markedAt: new Date() } },
      { upsert: true, new: true }
    );

    updateBucketForStudent(studentId).catch(console.error);
    res.json({ valid: true, message: 'Attendance marked via QR' });
  } catch (err) { next(err); }
});

// ── GET /validate?major=...&minor=... (legacy beacon check) ───────────────────
router.get('/validate', authenticate, async (req, res, next) => {
  try {
    const { major, minor } = req.query;
    if (!major || !minor) return res.status(400).json({ error: 'major and minor required' });
    const beacon = await Beacon.findOne({ bleID: major }).lean();
    res.json({ valid: !!beacon, beacon: beacon || null });
  } catch (err) { next(err); }
});

module.exports = router;