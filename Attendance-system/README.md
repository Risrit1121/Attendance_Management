# DIAMS — Web Portal

**Digital Intelligent Attendance Management System**  
IIT Hyderabad | SWE Sprint 2

Web-based dashboard for **Professors** and **Admins** to manage attendance sessions, view analytics, and monitor students.

---

## What This Does

- Professors can start **QR / BLE / Hybrid** attendance sessions for their courses
- Live attendance log updates every 3 seconds as students check in
- Analytics dashboard with charts — per-course breakdown, trends, at-risk students
- Admin panel shows system-wide stats and server health
- Connects to the shared Flask backend used by the mobile apps

---

## Stack

- **Frontend:** React · Tailwind CSS · Recharts · Lucide
- **Backend:** Python · Flask · SQLAlchemy · PostgreSQL
- **Auth:** JWT (HS256)
- **Deployment:** Docker + Docker Compose

---

## Project Structure

```
Attendance-system/
├── backend/
│   ├── app/
│   │   ├── main.py        ← All API routes
│   │   ├── models.py      ← Database models
│   │   ├── database.py    ← DB connection
│   │   └── seed.py        ← Dev seed data
│   ├── requirements.txt
│   ├── Dockerfile
│   └── docker-compose.yml
│
└── frontend/
    ├── src/
    │   ├── api/client.js           ← API calls
    │   ├── context/AuthContext.js  ← Login state
    │   ├── components/
    │   │   ├── Layout.js           ← Sidebar + topbar
    │   │   └── UI.js               ← Shared components
    │   └── pages/
    │       ├── Login.js
    │       ├── ProfessorDashboard.js
    │       ├── CourseView.js        ← Session management
    │       ├── Students.js          ← Live attendance log
    │       ├── Analytics.js         ← Charts
    │       ├── AdminDashboard.js
    │       └── Settings.js
    └── package.json
```

---

## Running Locally

### 1. Start the backend

```bash
cd backend
docker-compose up --build
```

Or without Docker:
```bash
pip install -r requirements.txt
python -m app.main
```

API runs at `http://localhost:4040`

Seed the database (first time only):
```bash
docker exec -it attendance-backend python -c "from app.seed import *"
```

### 2. Start the frontend

```bash
cd frontend
npm install
npm start
```

Opens at `http://localhost:3000`

---

## Login Credentials (after seeding)

| Role      | Email           | Password    |
|-----------|-----------------|-------------|
| Professor | profA@test.com   | password123 |
| Admin     | admin@test.com | password123 |

---

## Pages

| Page       | Who    | Description                                  |
|------------|--------|----------------------------------------------|
| Dashboard  | Prof   | Course cards with live session status        |
| Courses    | Prof   | Start/end sessions, QR display, live log     |
| Students   | Prof   | Live attendance per course, auto-refreshes   |
| Analytics  | Both   | Bar chart, trend line, pie chart, breakdowns |
| Admin      | Admin  | System stats, service health                 |
| Settings   | Both   | API URL config, endpoint reference           |

---

## API Endpoints

| Method | Endpoint                    | Description                  |
|--------|-----------------------------|------------------------------|
| POST   | `/login`                    | Authenticate                 |
| GET    | `/courses/:profId`          | Get professor's courses       |
| POST   | `/startSession`             | Start a session              |
| POST   | `/endSession/:id`           | End a session                |
| GET    | `/activeSession?course_id=` | Get active session           |
| GET    | `/getQR/:sessionId`         | Get QR token (refreshes 5s)  |
| POST   | `/markAttendance`           | Mark student attendance       |
| GET    | `/attendance/:sessionId`    | Get attendance log           |
| GET    | `/getMinor?major=`          | BLE minor value              |
| GET    | `/validate?major=&minor=`   | Validate BLE beacon          |
| GET    | `/admin/stats`              | System stats                 |
| GET    | `/analytics/course/:id`     | Per-course analytics         |
| GET    | `/analytics/prof/:id`       | Professor analytics          |
| GET    | `/course/<course_id>/students` | Get students of a course       |
| GET    | `/professor/<prof_id>/students` | Get deduplicated student count |

---

## Author

**Soham Rajesh Pawar**, CS22BTECH11055  
IIT Hyderabad · 8th Semester SWE