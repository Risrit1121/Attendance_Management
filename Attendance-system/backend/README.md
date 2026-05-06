# Backend - Attendance System API

**Node.js/Express RESTful API for DIAMS**  
IIT Hyderabad | Handover Edition

---

## 📚 Quick Navigation
- **New to this?** Start with [Quick Start](#quick-start)
- **Want to understand the code?** See [Project Structure](#project-structure)
- **Need to add/modify endpoints?** Check [Adding Features](#adding-features)
- **Having issues?** See [Troubleshooting](#troubleshooting)

---

## 🚀 Quick Start

### Local Development

```bash
cd backend

# Install dependencies
npm install

# Create .env file
cat > .env << 'EOF'
PORT=4040
MONGO_URI=mongodb://localhost:27017/attendance
JWT_SECRET=$(openssl rand -hex 32)
QR_SECRET=$(openssl rand -hex 32)
FRONTEND_URL=http://localhost:3000
AT_RISK_THRESHOLD=75
BLE_SERVICE_URL=http://localhost:8001
QR_SERVICE_URL=http://localhost:8002
FACE_SERVICE_URL=http://localhost:8000
EOF

# Start MongoDB
mongod

# Start backend (in another terminal)
npm start

# Backend runs at: http://localhost:4040
```

### Docker (Recommended)

```bash
# From Attendance-system root
docker-compose up backend

# Backend runs at: http://localhost:4040
```

### Seed Test Data

```bash
npm run seed
# Creates: 3 professors, 10 students, 5 courses with schedules
```

### Create Indexes

```bash
npm run indexes
# Optimizes MongoDB queries
```

---

## 📁 Project Structure

```
backend/
├── config/
│   └── db.js                      # MongoDB connection setup
│
├── middleware/
│   └── auth.js                    # JWT verification middleware
│
├── models/                        # 10 Mongoose schemas
│   ├── Student.js                 # User credentials (name, email, password, imageURL)
│   ├── Professor.js               # Faculty accounts (name, email, password, department)
│   ├── Admin.js                   # Admin accounts (name, email, password)
│   ├── Course.js                  # Courses (code, name, professor, schedule, lectures)
│   ├── Session.js                 # Attendance sessions (courseId, method, timestamps, active)
│   ├── Attendance.js              # Records (studentId, sessionId, method, timestamp)
│   ├── Enrollment.js              # Links (studentId, courseId, status)
│   ├── Beacon.js                  # BLE mappings (major, minor, classroomId, location)
│   ├── Classroom.js               # Venues (name, code, capacity, location)
│   └── Bucket.js                  # Per-student cache (studentId, courseId, attendance)
│
├── routes/                        # 10 API route files
│   ├── auth.js                    # POST /login, POST /logout
│   ├── courses.js                 # GET/POST/PUT/DELETE /courses/:id
│   ├── sessions.js                # POST /startSession, POST /endSession/:id
│   ├── attendance.js              # POST /markAttendance, GET /attendance/:sessionId
│   ├── analytics.js               # GET /analytics/course/:id, /prof/:id, etc.
│   ├── microservices.js           # GET /getMinor, POST /ble/validate, etc.
│   ├── beacons.js                 # Beacon configuration endpoints
│   ├── qr.js                      # QR code operations
│   ├── student.js                 # GET /student/:id/history, /courses, etc.
│   └── admin.js                   # Admin-only operations (under development)
│
├── services/                      # Business logic & utilities
│   ├── autoSessionService.js      # Auto-create fallback sessions for lectures
│   ├── autoEndSessionService.js   # Auto-end expired sessions (IST-aware)
│   ├── bucketService.js           # Attendance caching with intersection logic
│   ├── backupService.js           # Nightly JSON backups (7-day retention)
│   ├── keepAlive.js               # Service health monitoring
│   └── logBuffer.js               # Logging utility
│
├── jobs/
│   └── index.js                   # Cron scheduler (4 background jobs)
│       ├─ Auto-create sessions (1 min)
│       ├─ Auto-end sessions (1 min)
│       ├─ Cache rebuild (15 min)
│       └─ Nightly backup (02:00 IST)
│
├── scripts/
│   ├── db-indexes.js              # Create MongoDB indexes
│   └── seed.js                    # Populate test data
│
├── server.js                      # Express app entry point
├── package.json                   # Dependencies & scripts
├── Dockerfile                     # Container image
├── swagger.yaml                   # OpenAPI documentation
├── .env                           # Environment variables (not in git)
└── README.md                      # This file
```

---

## 🔑 Environment Variables

**`.env` file** (create in backend directory):

```env
# Server
PORT=4040
NODE_ENV=development

# Database
MONGO_URI=mongodb://admin:password@mongo:27017/attendance?authSource=admin
MONGO_USER=admin
MONGO_PASS=<strong-password>

# Security
JWT_SECRET=<openssl rand -hex 32>
QR_SECRET=<openssl rand -hex 32>

# Frontend
FRONTEND_URL=http://localhost:3000

# Microservices (optional)
BLE_SERVICE_URL=http://localhost:8001
QR_SERVICE_URL=http://localhost:8002
FACE_SERVICE_URL=http://localhost:8000

# Config
AT_RISK_THRESHOLD=75              # Attendance % for warnings
BACKUP_DIR=./backups              # Backup location
LOG_LEVEL=info
```

**Security Note**: Never commit `.env` to git. Use a secret manager in production.

---

## 📡 API Routes & Endpoints

### Authentication (1 endpoint)

**`auth.js`**

```http
POST /login
Content-Type: application/json

{
  "email": "user@test.com",
  "password": "password123",
  "role": "student|professor|admin"
}

Response:
{
  "success": true,
  "token": "eyJhbGc...",
  "user": {
    "id": "user_123",
    "email": "user@test.com",
    "role": "professor"
  }
}
```

### Courses (8 endpoints)

**`courses.js`**

```http
# List professor's courses
GET /courses/:profId

# Create course (admin only)
POST /courses
{
  "code": "CS101",
  "name": "Data Structures",
  "professor": "prof_123",
  "classroom": "room_101"
}

# Get course details
GET /courses/:courseId

# Update course
PUT /courses/:courseId
{ "name": "New Name" }

# Delete course (admin only)
DELETE /courses/:courseId

# Get schedules
GET /courses/:courseId/schedules

# Add schedule
POST /courses/:courseId/schedules
{
  "dayOfWeek": "Monday",
  "startTime": "09:00",
  "endTime": "10:30",
  "lectureCount": 1
}

# Update schedule
PATCH /courses/:courseId/schedules/:scheduleId

# Delete schedule
DELETE /courses/:courseId/schedules/:scheduleId
```

### Sessions (4 endpoints)

**`sessions.js`**

```http
# Start attendance session
POST /startSession
{
  "courseId": "course_123",
  "method": "BLE"  # or "QR", "Manual"
}

Response:
{
  "sessionId": "session_123",
  "minor": "12345"  # if BLE
}

# Get active session
GET /activeSession?courseId=course_123

# End session
POST /endSession/:sessionId

# List all sessions (admin)
GET /admin/sessions
```

### Attendance (4 endpoints)

**`attendance.js`**

```http
# Mark attendance (BLE/QR/Manual)
POST /markAttendance
{
  "sessionId": "session_123",
  "studentId": "student_123",
  "method": "BLE",
  "verificationData": { "major": "...", "minor": 12345 }
}

# Manual attendance (single)
POST /manualAttendance
{
  "sessionId": "session_123",
  "studentId": "student_123",
  "status": "present"  # or "absent"
}

# Bulk manual attendance
POST /manualAttendance/bulk
{
  "sessionId": "session_123",
  "attendanceMap": {
    "student1": "present",
    "student2": "absent"
  }
}

# Get attendance for session
GET /attendance/:sessionId
```

### Analytics (4 endpoints)

**`analytics.js`**

```http
# Course-level stats
GET /analytics/course/:courseId
Response: {
  "courseId": "...",
  "totalLectures": 10,
  "lectures": [
    {
      "lectureId": "...",
      "date": "2026-05-06",
      "totalStudents": 50,
      "presentCount": 45,
      "attendancePercentage": 90
    }
  ]
}

# Per-student breakdown
GET /analytics/course/:courseId/students
Response: {
  "students": [
    {
      "studentId": "...",
      "name": "John Doe",
      "lecturesAttended": 8,
      "totalLectures": 10,
      "percentage": 80
    }
  ]
}

# Professor's cross-course stats
GET /analytics/prof/:profId

# At-risk students
GET /analytics/at-risk/:profId
Response: {
  "atRiskStudents": [
    {
      "studentId": "...",
      "name": "Jane Doe",
      "avgAttendance": 60,
      "courses": ["CS101", "CS201"]
    }
  ]
}
```

### Microservices (6 endpoints)

**`microservices.js`** - Proxies to external services with fallback

```http
# Get BLE minor (30s rotation)
GET /getMinor?major=FDA50693-A4E2-4FB1-AFCF-C6EB07647825
Response: { "minor": 12345, "expiresAt": "..." }

# Validate BLE beacon
POST /ble/validate
{
  "major": "FDA50693-A4E2-4FB1-AFCF-C6EB07647825",
  "minor": 12345,
  "rssi": -65
}

# Get QR code (5s rotation)
GET /getQR/:sessionId
Response: { "qr": "hash_123456", "expiresAt": "..." }

# Validate QR code
POST /qr/validate
{
  "sessionId": "session_123",
  "hash": "hash_123456",
  "timestamp": 1709702100
}

# Decode QR (local HMAC)
GET /decodeQR?qr=hash_123456

# Legacy beacon validation
POST /validate?major=...&minor=...
```

### Beacons (endpoints)

**`beacons.js`** - Beacon configuration

```http
GET /beacons                    # List all beacons
POST /beacons                   # Create beacon
GET /beacons/:id                # Get beacon details
PUT /beacons/:id                # Update beacon
DELETE /beacons/:id             # Delete beacon
```

### Student (3 endpoints)

**`student.js`** - Student views

```http
# Get attendance history for course
GET /student/:studentId/history/:courseId

# Get enrolled courses
GET /student/:studentId/courses

# Get student profile
GET /student/:studentId/profile
```

### Admin (endpoints)

**`admin.js`** - Admin operations (under development)

```http
GET /admin/stats                # System statistics
POST /admin/users               # Create user
DELETE /admin/users/:id         # Delete user
GET /admin/backup               # Trigger backup
```

---

## 💾 Database Models

Each model in `models/` defines MongoDB schema. Key ones:

### Student.js
```javascript
{
  _id: String,              // Unique student ID
  name: String,
  email: String (unique),
  password: String (hashed),
  imageURL: String,         // Face image for verification
  createdAt: Date
}
```

### Course.js
```javascript
{
  courseCode: String (unique),
  name: String,
  professor: ObjectId (ref: Professor),
  classroom: ObjectId (ref: Classroom),
  schedule: [{
    dayOfWeek: String,    // "Monday", "Tuesday", etc.
    startTime: String,    // "09:00"
    endTime: String,      // "10:30"
    lectureCount: Number
  }],
  lectures: [{
    date: Date (UTC),
    startTime: Date (UTC),
    endTime: Date (UTC)
  }],
  createdAt: Date
}
```

### Session.js
```javascript
{
  courseId: ObjectId (ref: Course),
  method: String,         // "BLE", "QR", "Manual"
  startTime: Date,
  endTime: Date,
  active: Boolean,
  professsor: ObjectId (ref: Professor),
  attendanceCount: Number
}
```

### Attendance.js
```javascript
{
  studentId: ObjectId (ref: Student),
  sessionId: ObjectId (ref: Session),
  method: String,         // "BLE", "QR", "Manual"
  timestamp: Date,
  verificationData: {     // Varies by method
    major: String,        // BLE
    minor: Number,
    rssi: Number,
    // OR
    qrHash: String,       // QR
    // OR
    manualBy: ObjectId    // Manual
  }
}
```

### Bucket.js
```javascript
{
  studentId: ObjectId (ref: Student),
  courseId: ObjectId (ref: Course),
  attendance: [{
    lectureId: ObjectId,
    status: String,       // "present", "absent"
    sessionsMethods: [String],  // Methods attended
    allMethodsAttended: Boolean // Intersection logic
  }],
  lastUpdated: Date
}
```

See [models/](./models/) for complete schemas.

---

## 🔄 Background Jobs

Located in: [`jobs/index.js`](./jobs/index.js)

### Job 1: Auto-Create Sessions (1 min)
**File**: [`services/autoSessionService.js`](./services/autoSessionService.js)

```javascript
// For each scheduled lecture with no active BLE session:
// - Create 5-minute BLE session
// - Ensure all courses have attendance coverage
// - Runs every 1 minute
// - IST-aware: checks lecture schedule
```

### Job 2: Auto-End Sessions (1 min)
**File**: [`services/autoEndSessionService.js`](./services/autoEndSessionService.js)

```javascript
// For each active session:
// - Check if lecture end time (IST) has passed
// - Automatically close session if expired
// - Runs every 1 minute
// - Safety net: also checks in mobile app
```

### Job 3: Cache Rebuild (15 min)
**File**: [`services/bucketService.js`](./services/bucketService.js)

```javascript
// For each student-course pair:
// - Recalculate attendance using intersection logic
// - Update Bucket collection
// - Runs every 15 minutes
// - Also runs in real-time after each attendance mark
```

### Job 4: Nightly Backup (02:00 IST)
**File**: [`services/backupService.js`](./services/backupService.js)

```javascript
// At 02:00 IST:
// - Export all 10 models to JSON
// - Save to backups/ directory with timestamp
// - Keep last 7 backups (clean up older ones)
// - Automatic retention policy
```

---

## 🔐 Authentication & Security

### JWT Authentication

**Middleware**: [`middleware/auth.js`](./middleware/auth.js)

```javascript
// All protected routes require JWT token in header:
// Authorization: Bearer <token>

// Token contains:
{
  userId: String,
  email: String,
  role: "student|professor|admin"
}

// Verified with JWT_SECRET
```

### Password Security

- Passwords hashed with bcryptjs before storage
- Never returned in API responses
- Changed via secure endpoints only

### CORS

Configured in `server.js`:
```javascript
cors({
  origin: process.env.FRONTEND_URL,
  credentials: true
})
```

---

## 🧪 Testing

### Seed Database

```bash
npm run seed

# Creates:
# - 3 professors (with 5 courses each)
# - 10 students (enrolled in multiple courses)
# - Schedules for all courses
# - Sample data for testing
```

### Create Indexes

```bash
npm run indexes

# Creates database indexes for:
# - Fast session lookups
# - Efficient attendance queries
# - Optimized analytics calculations
```

### Run Tests

```bash
npm test

# Tests for:
# - API endpoints
# - Authentication
# - Business logic
# - Edge cases
```

### Manual Testing

```bash
# Test login
curl -X POST http://localhost:4040/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "prof@test.com",
    "password": "prof123",
    "role": "professor"
  }'

# Test get courses (requires token)
curl -H "Authorization: Bearer <token>" \
  http://localhost:4040/courses/prof_id

# Test health
curl http://localhost:4040/health
```

---

## 🚀 Adding Features

### Adding a New Endpoint

1. **Create route handler** in appropriate file in `routes/`:

```javascript
// In routes/attendance.js
router.post('/my-new-endpoint', authenticateJWT, async (req, res) => {
  try {
    // Your logic here
    res.json({ success: true });
  } catch (error) {
    res.status(500).json({ error: error.message });
  }
});
```

2. **Export from server.js**:

```javascript
app.use('/api', require('./routes/attendance'));
```

3. **Test endpoint**:

```bash
curl -X POST http://localhost:4040/api/my-new-endpoint \
  -H "Authorization: Bearer <token>"
```

### Adding a New Model

1. **Create model file** in `models/NewModel.js`:

```javascript
const mongoose = require('mongoose');

const newSchema = new mongoose.Schema({
  field1: String,
  field2: Number,
  createdAt: { type: Date, default: Date.now }
});

module.exports = mongoose.model('NewModel', newSchema);
```

2. **Use in routes**:

```javascript
const NewModel = require('../models/NewModel');
const doc = await NewModel.create({ field1: 'value' });
```

---

## 📊 Microservice Integration

### BLE Service Integration

**Route**: [`routes/microservices.js`](./routes/microservices.js)

```javascript
// Primary: Call external BLE service
const response = await axios.post(BLE_SERVICE_URL + '/ble/generate-minor', {
  major_id: major
});

// Fallback: Generate locally if service down
if (error) {
  const localMinor = generateLocalMinor();  // HMAC-based
  return res.json({ minor: localMinor, fallback: true });
}
```

### QR Service Integration

Similar pattern:
```javascript
// Primary: External QR service
// Fallback: Local HMAC generation
```

### Face Service (Optional)

For face verification after BLE/QR:
```javascript
const faceResult = await axios.post(
  FACE_SERVICE_URL + '/verify-face/',
  { user_id: studentId, frames: [...] }
);
```

---

## 🔍 Debugging

### Enable Verbose Logging

```bash
NODE_DEBUG=mongodb npm start
DEBUG=* npm start
```

### Check Active Requests

```bash
# MongoDB
docker-compose exec mongo mongosh
> db.currentOp()

# Node.js memory
node --inspect server.js
# Open chrome://inspect
```

### View Scheduled Jobs

```javascript
// In jobs/index.js, each cron task logs its execution
// Check logs for "[CRON]" entries
```

### Database Queries

```bash
# Check slow queries
mongosh
> use attendance
> db.system.profile.find().limit(5)
```

---

## ⚠️ Troubleshooting

### Port Already in Use

```bash
# Find process using port 4040
lsof -i :4040

# Kill it
kill -9 <PID>
```

### MongoDB Connection Failed

```bash
# Check MongoDB is running
mongod --version

# Verify connection string in .env
# MONGO_URI=mongodb://localhost:27017/attendance
```

### JWT Token Expired

```bash
# Token expiry controlled by JWT_SECRET
# Users need to login again
# No token refresh endpoint currently (can be added)
```

### Microservice Fallback Not Working

```bash
# Check fallback logic in microservices.js
# Verify HMAC generation matches mobile app
# Enable DEBUG logging to see fallback triggers
```

### High Memory Usage

```bash
# Check for memory leaks
node --max-old-space-size=2048 server.js

# Monitor
watch -n 1 'ps aux | grep node'
```

### Slow Analytics Queries

```bash
# Create indexes
npm run indexes

# Check query execution
mongosh
> use attendance
> db.attendance.explain("executionStats").find({})
```

---

## 📈 Performance Optimization

### Database Indexes

```bash
npm run indexes

# Creates indexes on:
# - Active sessions
# - Course analytics queries
# - Per-student attendance lookup
```

### Caching Strategy

- **Bucket collection**: Per-student aggregated attendance
- **Rebuilt every 15 min**: Safety net
- **Updated in real-time**: After each mark

### Connection Pooling

```javascript
// MongoDB connection pool size
const conn = mongoose.connect(uri, {
  maxPoolSize: 10
});
```

### API Response Compression

```javascript
const compression = require('compression');
app.use(compression());
```

---

## 🔐 Security Best Practices

✅ **Do:**
- Use strong JWT_SECRET and QR_SECRET
- Hash passwords with bcryptjs
- Validate all inputs
- Sanitize database queries
- Use HTTPS in production
- Implement rate limiting
- Keep dependencies updated

❌ **Don't:**
- Store passwords in plaintext
- Commit .env to git
- Expose sensitive data in logs
- Trust client-side validation only
- Use default credentials
- Disable CORS without reason

---

## 🧹 Maintenance

### Regular Tasks

```bash
# Check logs
tail -f logs/*.log

# Verify backups
ls -lah backups/

# Monitor database size
mongosh
> use attendance
> db.stats()

# Check indexes
npm run indexes
```

### Updating Dependencies

```bash
# Check for updates
npm outdated

# Update safely
npm install

# Test after update
npm test
npm start
```

---

## 📚 Additional Resources

- [Express.js Documentation](https://expressjs.com/)
- [MongoDB/Mongoose Documentation](https://mongoosejs.com/)
- [Node.js Best Practices](https://github.com/goldbergyoni/nodebestpractices)
- [JWT Authentication Guide](https://jwt.io/introduction)

---

## 💬 Notes for Team

### Key Files to Know

| File | Purpose | When to Edit |
|------|---------|-------------|
| `server.js` | Entry point | Changing app config |
| `config/db.js` | DB connection | Database connection issues |
| `middleware/auth.js` | Authentication | Changing auth logic |
| `routes/*.js` | API endpoints | Adding/modifying endpoints |
| `models/*.js` | Data schemas | Changing data structure |
| `services/` | Business logic | Core functionality changes |
| `jobs/index.js` | Background jobs | Scheduling changes |

### Common Modifications

**Add a new course field:**
1. Update `models/Course.js` schema
2. Update creation in `routes/courses.js`
3. Run `npm run indexes` if needed
4. Test with `npm test`

**Add an analytics metric:**
1. Create calculation in `services/`
2. Add route in `routes/analytics.js`
3. Add to frontend in `frontend/src/api/client.js`

**Change backup frequency:**
1. Edit `jobs/index.js` cron schedule
2. Update `services/backupService.js` logic
3. Test backup creation

---

## 📞 Support

For issues:
1. Check logs: `npm start` (development)
2. Test endpoints: `curl` or Postman
3. Verify .env configuration
4. Check MongoDB: `mongosh`
5. Review troubleshooting section above

---

## Credits

**Backend - DIAMS**  
IIT Hyderabad

**Original Developer**: Soham Rajesh Pawar (CS22BTECH11055)
