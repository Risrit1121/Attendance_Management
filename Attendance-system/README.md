# Attendance System (DIAMS)

**Digital Intelligent Attendance Management System**  
IIT Hyderabad | SWE Sprint 2

Complete web-based attendance management platform with real-time session tracking, analytics, and user administration. Integrates QR codes, BLE beacons, and face recognition for flexible attendance capture.

---

## What This Does

- **Professors** can start **QR / BLE / Manual** attendance sessions and view live attendance logs
- **Live updates** every 3 seconds as students check in with real-time dashboards
- **Analytics dashboard** with charts, per-course breakdowns, trends, and at-risk student identification
- **Admin panel** for system-wide statistics, service health monitoring, session management, and user administration
- **Integration** with BLE beacon scanning and Face Recognition microservices
- **Batch operations** - bulk student enrollment/deletion via CSV
- **Data export** and backup capabilities

---

## Architecture

```
Attendance-System/
├── backend/                    # Node.js/Express API
│   ├── config/                 # Database config
│   ├── middleware/             # Auth, CORS, rate limiting
│   ├── models/                 # Mongoose schemas
│   ├── routes/                 # API endpoints
│   ├── services/               # Business logic
│   ├── jobs/                   # Background tasks (cron)
│   ├── scripts/                # Database seeding
│   ├── server.js               # Express app
│   ├── package.json
│   └── Dockerfile
│
├── frontend/                   # React/Tailwind web portal
│   ├── src/
│   │   ├── api/                # Axios API client
│   │   ├── components/         # Reusable UI components
│   │   ├── context/            # Auth + scheduling context
│   │   ├── pages/              # Page components
│   │   │   ├── Login
│   │   │   ├── ProfessorDashboard
│   │   │   ├── CourseView       # Session management
│   │   │   ├── Students         # Live attendance logs
│   │   │   ├── Analytics        # Charts & reports
│   │   │   ├── AdminDashboard   # Admin console
│   │   │   └── ...
│   │   └── App.js              # Routes
│   ├── package.json
│   └── Dockerfile
│
└── docker-compose.yml          # Multi-container orchestration
```

---

## Stack

| Component | Technology |
|-----------|-----------|
| Frontend | React 19.x · Tailwind CSS 3.x · React Router 7.x · Recharts |
| Backend | Node.js · Express.js · Mongoose 8.x |
| Database | MongoDB 7.0 |
| Authentication | JWT (HS256) |
| Deployment | Docker · Docker Compose |
| External | BLE scanner API · Face recognition API |

---

## Quick Start

### Prerequisites
- Docker & Docker Compose (for containerized setup)
- OR Node.js 14+ + npm + MongoDB (for local development)

### Local Development (with Docker Compose)

```bash
# Clone and enter directory
cd Attendance-system

# Create .env if needed (optional - has sensible defaults)
# See docker-compose.yml for environment variables

# Start all services
docker-compose up --build
```

This starts:
- **MongoDB** on port 27017 (internal only)
- **Backend** API on http://localhost:4040
- **Frontend** on http://localhost:3000

Database is seeded automatically on first run.

### Local Development (without Docker)

See detailed setup in:
- [Backend README](./backend/README.md#local-development-without-docker)
- [Frontend README](./frontend/README.md#local-development)

---

## Docker Deployment

### Production Checklist

- [ ] `.env` file configured with production values
- [ ] `MONGO_USER` and `MONGO_PASS` set to strong credentials
- [ ] `JWT_SECRET` and `QR_SECRET` updated (generate with `openssl rand -hex 64`)
- [ ] `FRONTEND_URL` points to production domain
- [ ] Firewall allows ports 4040 (backend) and 3000 (frontend) or use reverse proxy
- [ ] MongoDB volume (`mongo-data`) persisted to production storage
- [ ] Backups volume (`backups`) configured

### Full Deployment

1. **Create `.env` file**:
   ```bash
   cat > .env << EOF
   MONGO_USER=admin
   MONGO_PASS=$(openssl rand -hex 12)
   FRONTEND_URL=https://yourdomain.com
   AT_RISK_THRESHOLD=75
   EOF
   ```

2. **Start services**:
   ```bash
   docker-compose up -d
   ```

3. **Verify services**:
   ```bash
   # Check all containers running
   docker-compose ps

   # Check backend health
   curl http://localhost:4040/health

   # Access frontend
   open http://localhost:3000
   ```

4. **Seed database** (first time only):
   ```bash
   docker-compose exec backend npm run seed
   ```

5. **Monitor logs**:
   ```bash
   docker-compose logs -f backend
   docker-compose logs -f frontend
   ```

### Scaling & Updates

```bash
# Stop all services
docker-compose down

# Update images
docker-compose pull

# Restart with latest
docker-compose up -d

# View persistent data
docker volume ls
```

---

## Configuration

### Environment Variables

**Backend** (set in `docker-compose.yml` or `.env`):
```env
PORT=4040
MONGO_URI=mongodb://admin:secret@mongo:27017/attendance?authSource=admin
JWT_SECRET=<generate-strong-secret>
QR_SECRET=<generate-strong-secret>
FRONTEND_URL=http://localhost:3000
AT_RISK_THRESHOLD=75
BACKUP_DIR=/app/backups
```

**Frontend** (set in `.env` in `frontend/` directory):
```env
REACT_APP_API_URL=http://localhost:4040
```

For more details, see:
- [Backend Configuration](./backend/README.md#environment-variables)
- [Frontend Configuration](./frontend/README.md#environment-variables)

---

## Database

MongoDB collections with schema documentation:
- **Student**: User accounts with face recognition image URLs
- **Professor**: Instructor accounts
- **Course**: Courses with lecture schedules and multiple attendance methods
- **Session**: Active attendance sessions with unique session UIDs
- **Attendance**: Attendance records with verification method (BLE/QRCode/Manual)
- **Enrollment**: Student-course relationships with status tracking
- **Beacon**: BLE beacon hardware mappings to classrooms
- **Classroom**: Physical classroom metadata

**Key design features:**
- Optimized indexes on common query patterns (active sessions, course analytics)
- Session `active` flag for explicit control vs duration-based expiry
- Multiple indexes for high-performance queries
- Unique constraints to prevent duplicate enrollments/attendance

For complete schema details, see [Backend Database Schema](./backend/README.md#database-schema).

---

## Usage

### Admin Setup

**Default credentials** (after seeding):
- Email: `admin@test.com`
- Password: `test123`

**Admin capabilities**:
- Create professors and students
- Manage courses and schedules
- Monitor all active sessions
- View system-wide analytics
- Manage user roles
- Trigger backups

### Professor Workflow

1. Log in with professor credentials
2. Select course from dashboard
3. View course details, schedules, enrolled students
4. **Start Session** - choose method:
   - **BLE** - Beacon-based (students scan with mobile app)
   - **QR Code** - Display QR code for students to scan
   - **Manual** - Manually mark attendance
5. Monitor live attendance log (updates every 3 seconds)
6. End session when complete
7. View analytics and reports

### Features by Role

| Feature | Professor | Admin |
|---------|-----------|-------|
| View Dashboard | ✓ | ✓ |
| Start Sessions | ✓ | ✓ |
| View Attendance | ✓ | ✓ (all courses) |
| View Analytics | ✓ | ✓ |
| Manage Courses | ✗ | ✓ |
| Manage Users | ✗ | ✓ |
| System Stats | ✗ | ✓ |
| Backups | ✗ | ✓ |

---

## API Overview

### Key Endpoints

```
Authentication
  POST   /login                    - Login with email/password
  POST   /logout                   - Logout user

Courses
  GET    /courses                  - List user's courses
  POST   /courses                  - Create course (admin)
  GET    /courses/:id/schedules    - Get course schedules
  POST   /courses/:id/schedules    - Add schedule

Sessions
  POST   /sessions                 - Start new session
  PUT    /sessions/:id/end         - End session
  GET    /sessions/:id/active      - Get active session

Attendance
  GET    /attendance/:courseId     - Get records
  POST   /attendance/mark          - Mark attendance
  POST   /attendance/manual        - Manual attendance

Analytics
  GET    /analytics/course/:id     - Course analytics
  GET    /analytics/admin          - System-wide stats
  GET    /analytics/at-risk        - At-risk students

Admin
  GET    /admin/users              - List all users
  POST   /admin/professors         - Create professor
  POST   /admin/students           - Create student
  POST   /admin/backup             - Backup database
```

Full API documentation available in [Backend README](./backend/README.md#api-routes)

---

## Troubleshooting

### Services Won't Start
```bash
# Check Docker is running
docker ps

# View logs
docker-compose logs backend
docker-compose logs mongo

# Restart everything
docker-compose down -v
docker-compose up --build
```

### Can't Connect to API
- Verify backend is running: `docker ps | grep attendance-backend`
- Check frontend env var `REACT_APP_API_URL` matches backend URL
- Restart containers: `docker-compose restart backend frontend`

### Database Connection Failed
- MongoDB must be healthy before backend starts
- Check MongoDB logs: `docker-compose logs mongo`
- Verify credentials in `docker-compose.yml`
- Reset database: `docker volume rm attendance-system_mongo-data`

### High Memory Usage
- Check logs for memory warnings
- Restart containers: `docker-compose restart`
- Monitor with: `docker stats`

### Login Issues
- Clear browser localStorage and try again
- Check backend logs for auth errors
- Verify JWT_SECRET is consistent

For more troubleshooting, see:
- [Backend Troubleshooting](./backend/README.md#troubleshooting)
- [Frontend Troubleshooting](./frontend/README.md#troubleshooting)

---

## Development

### Adding Features

1. **Backend change**: Add route/service in `backend/routes/` or `backend/services/`
2. **Frontend change**: Add page/component in `frontend/src/pages/` or `frontend/src/components/`
3. **API integration**: Update `frontend/src/api/client.js` with new endpoint
4. **Test**: `docker-compose exec backend npm run dev` and `docker-compose exec frontend npm start`

### Running Tests

```bash
# Backend
docker-compose exec backend npm test

# Frontend
docker-compose exec frontend npm test
```

### Database Migrations

```bash
# Create indexes
docker-compose exec backend npm run indexes

# Reseed data
docker-compose exec backend npm run seed
```

---

## Performance & Security

### Performance Optimization
- MongoDB indexes on frequently queried fields
- Response compression enabled
- Rate limiting configured (300 req/min general, 20 req/15min auth)
- Connection pooling for database

### Security Features
- Helmet.js headers (XSS, CSRF protection)
- CORS properly configured
- JWT authentication on all protected routes
- Password hashing with bcryptjs
- Rate limiting on login attempts
- Input validation and sanitization

### Production Recommendations
- Use reverse proxy (Nginx/Caddy) for SSL/TLS termination
- Run on dedicated server with firewall rules
- Regular backups of MongoDB volume
- Monitor disk space and logs
- Keep Docker images updated

---

## Monitoring & Maintenance

### Health Check
```bash
curl http://localhost:4040/health
```

Returns status of backend, MongoDB, and external services.

### Backups
```bash
# Manual backup
docker-compose exec backend curl -X POST http://localhost:4040/admin/backup

# Locate backup files
docker volume inspect attendance-system_backups | grep Mountpoint
```

### Logs
```bash
# All services
docker-compose logs -f

# Specific service
docker-compose logs -f backend
docker-compose logs -f mongo
```

### Database Access
```bash
# Connect to MongoDB shell
docker-compose exec mongo mongosh -u admin -p secret
# Then: use attendance
#       db.courses.find()
```

---

## File Organization

- **Backend Details**: [backend/README.md](./backend/README.md)
- **Frontend Details**: [frontend/README.md](./frontend/README.md)
- **Full API Reference**: Backend README API Routes section
- **Component Guide**: Frontend README Components section

---

## Related Projects

This Attendance System integrates with:
- **BLE Module** - Bluetooth beacon scanning (separate microservice)
- **Face Recognition Module** - Face verification and enrollment (separate microservice)
- **Mobile Apps** - iOS and Android student/professor apps
- **Spring Boot Backend** - Legacy Java backend (if in use)

---

## License

**Digital Intelligent Attendance Management System (DIAMS)**  
IIT Hyderabad | Software Engineering Sprint 2 | 2026

---

## Support & Feedback

For issues or contributions:
1. Check the troubleshooting sections in README files
2. Review logs with `docker-compose logs`
3. Verify configuration in `.env` and `docker-compose.yml`
4. Contact the development team

| Role      | Email           | Password    |
|-----------|-----------------|-------------|
| Professor | profA@test.com  | password123 |
| Professor | profB@test.com  | password123 |
| Professor | profC@test.com  | password123 |
| Admin     | admin@test.com  | password123 |

---

## Pages

| Page       | Who    | Description                                           |
|------------|--------|-------------------------------------------------------|
| Overview   | Admin  | System-wide stats refreshed every 15 s               |
| Dashboard  | Prof   | Course cards with live session status                 |
| Courses    | Prof   | Start/end sessions, QR display, scheduler, live log  |
| Students   | Prof   | Live attendance per course, auto-refreshes            |
| Analytics  | Both   | Bar chart, trend line, per-prof breakdown (admin); course breakdown & at-risk (prof) |
| Admin      | Admin  | All sessions, user management, server log hints       |
| Settings   | Both   | API URL config, endpoint reference                    |

---

## API Endpoints

| Method | Endpoint                        | Description                           |
|--------|---------------------------------|---------------------------------------|
| POST   | `/login`                        | Authenticate                          |
| GET    | `/courses/:profId`              | Get professor's courses               |
| POST   | `/startSession`                 | Start a session                       |
| POST   | `/endSession/:id`               | End a session                         |
| GET    | `/activeSession?course_id=`     | Get active session                    |
| GET    | `/getQR/:sessionId`             | Get QR token (refreshes 5 s)          |
| POST   | `/markAttendance`               | Mark student attendance               |
| GET    | `/attendance/:sessionId`        | Get attendance log                    |
| POST   | `/manualAttendance`             | Manually mark one student             |
| POST   | `/manualAttendance/bulk`        | Bulk-mark students present            |
| GET    | `/getMinor?major=`              | BLE minor value                       |
| GET    | `/validate?major=&minor=`       | Validate BLE beacon                   |
| GET    | `/admin/stats`                  | System stats                          |
| GET    | `/admin/sessions`               | All sessions across all courses       |
| GET    | `/admin/analytics`              | Full system-wide analytics breakdown  |
| GET    | `/admin/users`                  | List all users                        |
| POST   | `/admin/users`                  | Create a user                         |
| DELETE | `/admin/users/:id`              | Delete a user                         |
| GET    | `/analytics/course/:id`         | Per-course analytics                  |
| GET    | `/analytics/prof/:id`           | Professor analytics                   |
| GET    | `/analytics/at-risk/:profId`    | Students below 25% attendance         |
| GET    | `/course/:courseId/students`    | Students enrolled in a course         |
| GET    | `/student/:id/history/:courseId`| Student attendance history            |

---

## Author

**Soham Rajesh Pawar**, CS22BTECH11055  
IIT Hyderabad · 8th Semester SWE