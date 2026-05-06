// ── Log buffer must be initialised BEFORE anything else writes to console ─────
const { initLogBuffer, getRecentLogs } = require('./services/logBuffer');
initLogBuffer();

require('dotenv').config();
const express    = require('express');
const mongoose   = require('mongoose');
const cors       = require('cors');
const helmet     = require('helmet');
const rateLimit  = require('express-rate-limit');

const authRoutes          = require('./routes/auth');
const courseRoutes        = require('./routes/courses');
const sessionRoutes       = require('./routes/sessions');
const attendanceRoutes    = require('./routes/attendance');
const analyticsRoutes     = require('./routes/analytics');
const adminRoutes         = require('./routes/admin');
const microserviceRoutes  = require('./routes/microservices');
const qrRoutes            = require('./routes/qr');
const beaconRoutes        = require('./routes/beacons');
const studentRoutes       = require('./routes/student');

const { startAllJobs } = require('./jobs');
const { authenticate, authorize } = require('./middleware/auth');

const app = express();

app.set('trust proxy', 1);
app.use(express.json());

// ── Request logger ────────────────────────────────────────────────────────────
app.use((req, res, next) => {
  const start = Date.now();
  const ip =
    (req.headers['x-forwarded-for'] || '').split(',')[0].trim() ||
    req.socket.remoteAddress;

  console.log(`➡️  ${req.method} ${req.originalUrl} | IP: ${ip}`);
  if (req.originalUrl === '/login' && req.method === 'POST') {
    console.log('📦 LOGIN BODY:', {
      email:    req.body.email,
      password: req.body.password ? '***' : undefined,
    });
  }
  res.on('finish', () => {
    const time = Date.now() - start;
    console.log(`⬅️  ${res.statusCode} ${req.method} ${req.originalUrl} (${time}ms)`);
  });
  next();
});

// ── Security ──────────────────────────────────────────────────────────────────
app.use(helmet());
app.use(cors({ origin: process.env.FRONTEND_URL || '*' }));

const authLimiter = rateLimit({ windowMs: 15 * 60 * 1000, max: 20,
  message: { error: 'Too many login attempts' } });
app.use('/login', authLimiter);

const limiter = rateLimit({ windowMs: 60 * 1000, max: 300,
  standardHeaders: true, legacyHeaders: false });
app.use(limiter);

// ── Routes ────────────────────────────────────────────────────────────────────
app.use('/',          authRoutes);
app.use('/',          courseRoutes);
app.use('/',          sessionRoutes);
app.use('/',          attendanceRoutes);
app.use('/analytics', analyticsRoutes);
app.use('/admin',     adminRoutes);
app.use('/',          microserviceRoutes);
app.use('/',          qrRoutes);
app.use('/',          beaconRoutes);
app.use('/student',   studentRoutes);

// ── GET /admin/logs ───────────────────────────────────────────────────────────
// Returns the last N lines from the in-process log buffer.
// ?n=150 (default) up to max 500. Admin-only.
app.get('/admin/logs', authenticate, authorize('admin'), (req, res) => {
  const n = Math.min(parseInt(req.query.n || '150', 10), 500);
  res.json(getRecentLogs(n));
});

// ── Health ────────────────────────────────────────────────────────────────────
app.get('/health', (_, res) => res.json({
  status: 'ok', ts: new Date(),
  services: {
    ble: process.env.BLE_SERVICE_URL || 'https://ble-qr-microservice.onrender.com',
    qr:  process.env.QR_SERVICE_URL  || 'https://ble-qr-microservice.onrender.com',
  },
}));

// ── Error handler ─────────────────────────────────────────────────────────────
app.use((err, req, res, next) => {
  if (err.code === 11000) {
    const field = Object.keys(err.keyValue || {})[0] || 'field';
    return res.status(409).json({ error: `Duplicate value for ${field}` });
  }
  if (err.name === 'ValidationError') {
    return res.status(400).json({ error: err.message });
  }
  console.error(err);
  res.status(err.status || 500).json({ error: err.message || 'Internal server error' });
});

// ── DB + Boot ─────────────────────────────────────────────────────────────────
mongoose
  .connect(process.env.MONGO_URI || 'mongodb+srv://admin:H1nvgEoQ2gul5YDG@cluster0.ksheidh.mongodb.net/attendance=Cluster0', {
    maxPoolSize: 50,
    serverSelectionTimeoutMS: 5000,
    socketTimeoutMS: 45000,
  })
  .then(() => {
    console.log('MongoDB connected');
    startAllJobs();
    const PORT = process.env.PORT || 4040;
    app.listen(PORT, () => console.log(`Server on :${PORT}`));
  })
  .catch(err => { console.error('DB error', err); process.exit(1); });