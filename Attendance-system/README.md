# Digital Intelligent Attendance Management System (DIAMS)
## Attendance-System Module - Handover Documentation

**Web-based Attendance Management Platform**  
IIT Hyderabad | Handover Edition | Version 2.0

---

## 📚 Table of Contents
1. [Quick Start](#quick-start)
2. [System Overview](#system-overview)
3. [Architecture](#architecture)
4. [Setup & Deployment](#setup--deployment)
5. [Project Structure](#project-structure)
6. [Key Features](#key-features)
7. [Configuration](#configuration)
8. [API & Endpoints](#api--endpoints)
9. [Development Guide](#development-guide)
10. [Troubleshooting](#troubleshooting)
11. [Maintenance](#maintenance)

---

## 🚀 Quick Start

### Prerequisites
- **Docker & Docker Compose** (recommended)
- OR **Node.js 18+**, **npm**, **MongoDB 7.0**
- Port 4040 (backend) and 3000 (frontend) available

### Docker Deployment (Fastest)

```bash
cd Attendance-system

# Create .env file
cat > .env << 'EOF'
MONGO_USER=admin
MONGO_PASS=$(openssl rand -hex 12)
JWT_SECRET=$(openssl rand -hex 32)
QR_SECRET=$(openssl rand -hex 32)
FRONTEND_URL=http://localhost:3000
AT_RISK_THRESHOLD=75
PORT=4040
EOF

# Start all services
docker-compose up --build

# Seed database (in another terminal)
docker-compose exec backend npm run seed

# Access:
# Frontend: http://localhost:3000
# Backend: http://localhost:4040
# API Docs: http://localhost:4040/api-docs
```

**Default Test Credentials:**
```
Admin:      admin@test.com / test123
Professor:  prof@test.com / prof123
Student:    student@test.com / student123
```

### Local Development (Without Docker)

See [Backend Setup](./backend/README.md#local-development) and [Frontend Setup](./frontend/README.md#local-development)

---

## 📊 System Overview

### Core Responsibilities

**DIAMS** manages attendance tracking at institutional scale through:

1. **Multi-Method Verification**
   - BLE beacons (proximity-based, 30s minor rotation)
   - QR codes (time-windowed, 5s token rotation)
   - Manual marking (professor direct entry)

2. **Intelligent Automation**
   - Auto-create fallback BLE sessions for scheduled lectures (1-min cron)
   - Auto-end expired sessions based on IST schedule (1-min cron)
   - Attendance caching with real-time + 15-min rebuild
   - Nightly automated backups (7-day retention)

3. **Real-Time Analytics**
   - Live 3-second dashboard updates during sessions
   - Course-level attendance trends
   - Per-student analytics across all courses
   - At-risk student detection
   - Professor cross-course summaries

4. **Session Management**
   - Lecture intersection logic: students "present" only if in ALL session methods
   - Multi-session support per lecture (BLE + QR redundancy)
   - Timezone-aware IST scheduling with UTC storage

5. **Fault Tolerance**
   - Microservice fallback: local HMAC if BLE/QR services unavailable
   - MongoDB atomic operations prevent duplicate sessions
   - Backup restore procedures
   - Health check monitoring

---

## 🏗️ Architecture

```
┌─────────────────────────────────────────────────┐
│         FRONTEND LAYER (React)                  │
│  ├─ Admin Dashboard (system stats & users)      │
│  ├─ Professor Portal (courses & sessions)       │
│  └─ Student Portal (attendance & enrollment)    │
└─────────────────────────────────────────────────┘
              │ (REST + JWT Auth)
              ▼
┌─────────────────────────────────────────────────┐
│    BACKEND API (Node.js/Express)                │
│  ├─ Auth Routes (JWT login/logout)              │
│  ├─ Course Routes (CRUD + schedules)            │
│  ├─ Session Routes (start/end/active)           │
│  ├─ Attendance Routes (mark via 3 methods)      │
│  ├─ Analytics Routes (stats & trends)           │
│  ├─ Microservice Proxies (BLE/QR with fallback)│
│  └─ Background Jobs (4 cron tasks)              │
└─────────────────────────────────────────────────┘
    │              │              │
    ▼              ▼              ▼
 MongoDB      BLE Service     QR Service
(persistent)   (external)    (external)

Background Jobs (cron-based):
├─ Auto-create fallback sessions (1 min)
├─ Auto-end expired sessions (1 min)
├─ Rebuild attendance cache (15 min)
└─ Nightly JSON backups (02:00 IST)
```

### Data Flow: Attendance Marking

```
Mobile App Captures Signal (BLE/QR/Manual)
                ▼
Backend Validates via Microservice
                ▼
Fallback to Local HMAC if Service Down
                ▼
Request Face Verification (optional)
                ▼
Create Attendance Record
                ▼
Update Real-Time Bucket Cache
                ▼
Return Success Response
```

---

## ⚙️ Setup & Deployment

### Environment Configuration

**Backend** (`.env` or `docker-compose.yml`):

```env
PORT=4040
MONGO_USER=admin
MONGO_PASS=<strong-password>
JWT_SECRET=<openssl rand -hex 32>
QR_SECRET=<openssl rand -hex 32>
FRONTEND_URL=http://localhost:3000
AT_RISK_THRESHOLD=75
BACKUP_DIR=/app/backups
BLE_SERVICE_URL=http://ble-service:8001    # Optional
QR_SERVICE_URL=http://qr-service:8002      # Optional
FACE_SERVICE_URL=http://face-service:8000  # Optional
```

**Frontend** (`frontend/.env`):
```env
REACT_APP_API_URL=http://localhost:4040
```

### Docker Services

The `docker-compose.yml` orchestrates:
- **MongoDB** (port 27017, internal)
- **Backend API** (port 4040)
- **Frontend** (port 3000)
- **Volumes** for data persistence and backups

### Production Deployment Checklist

- [ ] Use strong `.env` secrets (not defaults)
- [ ] Configure external service URLs (BLE, QR, Face)
- [ ] Set up reverse proxy (Nginx/Caddy) for SSL/TLS
- [ ] Enable firewall rules for required ports
- [ ] Configure MongoDB backups to cloud storage
- [ ] Set up monitoring and alerting
- [ ] Test failover procedures
- [ ] Document any custom configurations
- [ ] Verify CORS settings match production domain
- [ ] Enable HTTPS for all endpoints

---

## 📁 Project Structure

```
Attendance-system/
│
├── backend/                             # Node.js/Express API
│   ├── config/
│   │   └── db.js                        # MongoDB connection
│   │
│   ├── middleware/
│   │   └── auth.js                      # JWT verification
│   │
│   ├── models/                          # 10 Mongoose schemas
│   │   ├── Student.js                   # User credentials
│   │   ├── Professor.js                 # Faculty accounts
│   │   ├── Admin.js                     # Admin accounts
│   │   ├── Course.js                    # Courses with schedules
│   │   ├── Session.js                   # Attendance sessions
│   │   ├── Attendance.js                # Individual records
│   │   ├── Enrollment.js                # Student-course links
│   │   ├── Beacon.js                    # BLE mappings
│   │   ├── Classroom.js                 # Venue metadata
│   │   └── Bucket.js                    # Per-student cache
│   │
│   ├── routes/                          # 10 route files (60+ endpoints)
│   │   ├── auth.js                      # Login/logout
│   │   ├── courses.js                   # Course CRUD + schedules
│   │   ├── sessions.js                  # Session lifecycle
│   │   ├── attendance.js                # Attendance marking
│   │   ├── analytics.js                 # Stats & trends
│   │   ├── microservices.js             # BLE/QR proxy
│   │   ├── beacons.js                   # Beacon config
│   │   ├── qr.js                        # QR operations
│   │   ├── student.js                   # Student views
│   │   └── admin.js                     # Admin ops
│   │
│   ├── services/                        # Business logic (6 files)
│   │   ├── autoSessionService.js        # Auto-create sessions
│   │   ├── autoEndSessionService.js     # Auto-end sessions
│   │   ├── bucketService.js             # Cache management
│   │   ├── backupService.js             # Data backups
│   │   ├── keepAlive.js                 # Service health
│   │   └── logBuffer.js                 # Logging utility
│   │
│   ├── jobs/
│   │   └── index.js                     # Cron scheduler (4 jobs)
│   │
│   ├── scripts/
│   │   ├── db-indexes.js                # Create DB indexes
│   │   └── seed.js                      # Populate test data
│   │
│   ├── server.js                        # Express entry point
│   ├── package.json
│   ├── Dockerfile
│   ├── swagger.yaml                     # API documentation
│   └── README.md                        # Backend details
│
├── frontend/                            # React/Tailwind portal
│   ├── src/
│   │   ├── api/
│   │   │   └── client.js                # Axios API client
│   │   │
│   │   ├── components/
│   │   │   ├── Layout.js                # Sidebar + navbar
│   │   │   ├── UI.js                    # Shared components
│   │   │   └── ...
│   │   │
│   │   ├── context/
│   │   │   ├── AuthContext.js           # Auth state
│   │   │   └── SchedulerContext.js      # Session scheduling
│   │   │
│   │   ├── pages/                       # 11 page components
│   │   │   ├── Login.js
│   │   │   ├── ProfessorDashboard.js
│   │   │   ├── AdminDashboard.js
│   │   │   ├── CourseView.js
│   │   │   ├── CoursesPage.js
│   │   │   ├── Students.js
│   │   │   ├── Analytics.js
│   │   │   ├── Settings.js
│   │   │   └── ...
│   │   │
│   │   ├── App.js                       # Main router
│   │   └── index.js                     # React entry
│   │
│   ├── public/                          # Static assets
│   ├── tailwind.config.js               # Styling config
│   ├── package.json
│   ├── Dockerfile
│   └── README.md                        # Frontend details
│
├── docker-compose.yml                   # Multi-container setup
├── .env                                 # Environment secrets
└── README.md                            # This file
```

---

## 🎯 Key Features

### 1. BLE Beacon Attendance
- **ESP32 beacons** broadcast major ID (fixed) + minor (30-sec rotation)
- **Mobile app** captures RSSI signal strength
- **Backend validates** minor with BLE microservice
- **Fallback**: Local HMAC generation if service down
- **Benefit**: Secure, proximity-based, hard to spoof

### 2. QR Code Attendance
- **Backend generates** time-bound hash (5-sec rotation)
- **Professor displays** QR on classroom screen
- **Student scans** with mobile app
- **Backend validates** hash and timestamp
- **Benefit**: Fast, works without BLE hardware, temporary tokens

### 3. Manual Marking
- **Professor directly enters** attendance on roster
- **Bulk operations** supported for quick marking
- **Benefit**: Emergency fallback, no technology required

### 4. Session Automation
- **Auto-Create**: If lecture scheduled but no session started, create 5-min BLE session (1-min cron)
- **Auto-End**: Close sessions when IST end time passes (1-min cron)
- **Lecture Intersection**: Student marked "present" only if attended ALL methods of a lecture
- **Benefit**: Ensures coverage, prevents double-counting

### 5. Real-Time Caching
- **Per-student bucket**: Updated immediately after each mark
- **15-min rebuild**: Full cache rebuild for safety
- **Instant analytics**: Dashboard updates every 3 seconds
- **Benefit**: Fast queries, fresh data

### 6. Microservice Integration
- **BLE Service**: Generate/validate beacon minors
- **QR Service**: Generate/validate QR tokens
- **Face Service**: (Optional) Verify faces
- **Fallback**: Local HMAC if any service unavailable
- **Benefit**: Modular, resilient, scalable

---

## 🔌 API & Endpoints

### Authentication
```http
POST /login
{
  "email": "user@test.com",
  "password": "password123",
  "role": "student|professor|admin"
}
```

### Courses (8 endpoints)
```http
GET    /courses/:profId                 # Get professor's courses
POST   /courses                         # Create course
GET    /courses/:id/schedules           # Get schedules
POST   /courses/:id/schedules           # Add schedule
PUT    /courses/:id                     # Update course
DELETE /courses/:id                     # Delete course
PATCH  /courses/:id/schedules/:sid      # Update schedule
DELETE /courses/:id/schedules/:sid      # Delete schedule
```

### Sessions (4 endpoints)
```http
POST   /startSession                    # Start attendance session
POST   /endSession/:sessionId           # End session
GET    /activeSession?courseId=...      # Get active session
GET    /admin/sessions                  # List all sessions (admin)
```

### Attendance (4 endpoints)
```http
POST   /markAttendance                  # Mark (BLE/QR/Manual)
POST   /manualAttendance                # Manual single student
POST   /manualAttendance/bulk           # Bulk mark
GET    /attendance/:sessionId           # Get records
```

### Analytics (4 endpoints)
```http
GET    /analytics/course/:courseId      # Course stats
GET    /analytics/prof/:profId          # Professor stats
GET    /analytics/course/:id/students   # Per-student breakdown
GET    /analytics/at-risk/:profId       # At-risk students
```

### Microservices (6 endpoints)
```http
GET    /getMinor?major=...              # Get BLE minor (30s)
POST   /ble/validate                    # Validate beacon
GET    /getQR/:sessionId                # Get QR hash (5s)
POST   /qr/validate                     # Validate QR
GET    /decodeQR?qr=...                 # Local QR validation
POST   /validate?major=&minor=...       # Legacy beacon check
```

**Full API docs**: See [Backend README](./backend/README.md#api-routes)

---

## 👥 User Roles & Pages

| Role | Pages | Capabilities |
|------|-------|--------------|
| **Professor** | Dashboard, Courses, Students, Analytics | Start/end sessions, view stats, manual mark |
| **Admin** | Dashboard, Overview, Courses, Students, Analytics | Manage all courses/users, view system stats |
| **Student** | (Mobile only) | View enrollment, see attendance |

---

## 💾 Background Jobs

Located in: [`backend/jobs/index.js`](./backend/jobs/index.js)

| Job | Frequency | Purpose | Service |
|-----|-----------|---------|---------|
| **Auto-Create Sessions** | 1 min | Create BLE sessions for scheduled lectures | [`autoSessionService.js`](./backend/services/autoSessionService.js) |
| **Auto-End Sessions** | 1 min | Close expired sessions (IST-aware) | [`autoEndSessionService.js`](./backend/services/autoEndSessionService.js) |
| **Cache Rebuild** | 15 min | Refresh attendance cache with intersection logic | [`bucketService.js`](./backend/services/bucketService.js) |
| **Nightly Backup** | 02:00 IST | JSON backup of all 10 models (7-day retention) | [`backupService.js`](./backend/services/backupService.js) |

---

## 🔐 Database Schema

**10 MongoDB Collections** (see [Backend Models](./backend/models/)):

| Model | Purpose | Key Fields |
|-------|---------|-----------|
| [Student.js](./backend/models/Student.js) | User credentials | _id, name, password, imageURL, email, createdAt |
| [Professor.js](./backend/models/Professor.js) | Faculty accounts | _id, name, password, email, department |
| [Admin.js](./backend/models/Admin.js) | Admin accounts | _id, name, password, email |
| [Course.js](./backend/models/Course.js) | Courses with schedules | courseCode, name, professor, schedule (day+time), lectures |
| [Session.js](./backend/models/Session.js) | Attendance sessions | courseId, method (BLE/QR/Manual), startTime, endTime, active |
| [Attendance.js](./backend/models/Attendance.js) | Attendance records | studentId, sessionId, method, timestamp, verificationData |
| [Enrollment.js](./backend/models/Enrollment.js) | Student-course links | studentId, courseId, status, enrolledAt |
| [Beacon.js](./backend/models/Beacon.js) | BLE mappings | major, minor, classroomId, location |
| [Classroom.js](./backend/models/Classroom.js) | Venue metadata | name, code, capacity, location |
| [Bucket.js](./backend/models/Bucket.js) | Per-student cache | studentId, courseId, attendance (aggregated) |

**Indexes** created by: [`scripts/db-indexes.js`](./backend/scripts/db-indexes.js)

---

## 🧪 Testing & Development

### Seed Test Data

```bash
# Populate database with sample courses, users, schedules
docker-compose exec backend npm run seed

# Creates: 3 professors, 10 students, 5 courses, schedules
```

### Create Indexes

```bash
# Create MongoDB indexes for performance
docker-compose exec backend npm run indexes
```

### Running Tests

```bash
# Backend tests
docker-compose exec backend npm test

# Frontend tests
docker-compose exec frontend npm test
```

---

## 🔧 Configuration Files

| File | Purpose |
|------|---------|
| [docker-compose.yml](./docker-compose.yml) | Service orchestration |
| [backend/server.js](./backend/server.js) | Express app setup |
| [backend/config/db.js](./backend/config/db.js) | MongoDB connection |
| [frontend/src/api/client.js](./frontend/src/api/client.js) | API client |
| [backend/swagger.yaml](./backend/swagger.yaml) | API documentation |

---

## 📈 Operations & Monitoring

### Health Check

```bash
curl http://localhost:4040/health
```

### Database Access

```bash
# Connect to MongoDB shell
docker-compose exec mongo mongosh -u admin -p <password>

# Inside mongosh:
use attendance
db.courses.find()
db.sessions.find().limit(5)
db.attendance.countDocuments()
```

### View Logs

```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f frontend
docker-compose logs -f mongo
```

### Backup & Restore

```bash
# Automated: Nightly at 02:00 IST (see backupService.js)
# Location: Docker volume (attendance-system_backups)

# Manual backup
docker-compose exec backend npm run backup

# Restore procedure (manual)
# 1. Stop backend
# 2. Extract backup JSON
# 3. mongorestore --uri mongodb://... 
# 4. Restart backend
```

### Performance Monitoring

```bash
# Resource usage
docker stats

# Database performance
docker-compose exec mongo mongosh
> use attendance
> db.sessions.getIndexes()
> db.attendance.aggregate([{$group: {_id: "$courseId", count: {$sum: 1}}}])
```

---

## 🛠️ Troubleshooting

### Common Issues

**Services Won't Start**
```bash
# Check Docker
docker ps

# View logs
docker-compose logs backend
docker-compose logs mongo

# Reset & restart
docker-compose down -v
docker-compose up --build
```

**Can't Access Frontend**
```bash
# Verify running
docker-compose ps | grep frontend

# Check env var
grep REACT_APP_API_URL frontend/.env

# Restart
docker-compose restart frontend
```

**Database Connection Failed**
```bash
# Check MongoDB
docker-compose logs mongo

# Verify credentials in docker-compose.yml
# Reset database
docker volume rm attendance-system_mongo-data
docker-compose up -d mongo
```

**Authentication Issues**
```bash
# Clear browser localStorage
# Browser → Dev Tools → Application → Storage → Clear All

# Check JWT secret in .env
grep JWT_SECRET .env

# Re-login
```

**High Resource Usage**
```bash
# Monitor
docker stats

# Rebuild indexes
docker-compose exec backend npm run indexes

# Restart services
docker-compose restart
```

See [Backend Troubleshooting](./backend/README.md#troubleshooting) and [Frontend Troubleshooting](./frontend/README.md#troubleshooting) for detailed help.

---

## 📖 Detailed Documentation

For comprehensive information, see:

- **[Backend README](./backend/README.md)** - API routes, database schema, deployment
- **[Frontend README](./frontend/README.md)** - Components, pages, API integration

---

## ✅ Handover Checklist

- [ ] Read entire this README
- [ ] Complete Quick Start setup
- [ ] Access MongoDB and explore data
- [ ] Run application end-to-end
- [ ] Test each role (admin, professor, student)
- [ ] Review API endpoints with Postman/curl
- [ ] Understand background jobs in `jobs/index.js`
- [ ] Test attendance marking (BLE/QR/Manual)
- [ ] Verify analytics and caching work
- [ ] Test backup/restore procedures
- [ ] Review error logs and debugging
- [ ] Understand microservice fallback mechanism
- [ ] Set up monitoring for production
- [ ] Document any customizations made

---

## 🚨 Critical Concepts

### Lecture Intersection Logic
Students marked "present" for a lecture **only if they attended ALL sessions** of that lecture. This enables:
- Multiple backup sessions (BLE + QR)
- Robust verification
- Flexible scheduling without double-counting

See: [`bucketService.js`](./backend/services/bucketService.js) - calculates intersection

### Microservice Resilience
- **Primary**: Proxy to external BLE/QR services
- **Fallback**: Local HMAC-based generation if services down
- **Automatic**: No manual intervention needed

See: [`microservices.js`](./backend/routes/microservices.js)

### Timezone Handling
- **Schedules**: IST strings ("Monday 09:00")
- **Lectures**: UTC timestamps
- **Cron jobs**: IST-aware conversions

See: [`autoSessionService.js`](./backend/services/autoSessionService.js)

### Real-Time Caching
- **Immediate**: Updated after each attendance mark
- **Safety rebuild**: Full cache rebuild every 15 min
- **Performance**: Fast analytics queries

See: [`bucketService.js`](./backend/services/bucketService.js)

---

## 📞 Support & Contact

For issues:
1. Check logs: `docker-compose logs`
2. Verify health: `curl http://localhost:4040/health`
3. Review troubleshooting sections
4. Check API documentation

---

## 📄 Credits

**Digital Intelligent Attendance Management System (DIAMS)**  
IIT Hyderabad

**Original Developer**: Soham Rajesh Pawar (CS22BTECH11055)