# DIAMS — Web Portal

**Digital Intelligent Attendance Management System**  
IIT Hyderabad | SWE Sprint 2

Web-based dashboard for **Professors** and **Admins** to manage attendance sessions, view analytics, and monitor students.

---

## What This Does

- Professors can start **QR / BLE / Hybrid** attendance sessions for their courses
- Live attendance log updates every 3 seconds as students check in
- Analytics dashboard with charts — per-course breakdown, trends, at-risk students
- Admin panel shows system-wide stats, service health, all sessions, and user management
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
    │   ├── context/SchedulerContext.js ← Auto session scheduling
    │   ├── components/
    │   │   ├── Layout.js           ← Sidebar + topbar
    │   │   └── UI.js               ← Shared components
    │   └── pages/
    │       ├── Login.js
    │       ├── ProfessorDashboard.js
    │       ├── CoursesPage.js
    │       ├── CourseView.js        ← Session management
    │       ├── Students.js          ← Live attendance log
    │       ├── Analytics.js         ← Charts
    │       ├── AdminDashboard.js    ← Sessions, users, logs
    │       ├── AdminOverview.js     ← System-wide stats
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

## Bug Fixes (Sprint 2 → current)

| # | Bug | Fix |
|---|-----|-----|
| 1 | **Black bars in Admin Analytics chart** | Replaced invalid `<rect>` children (plain HTML) inside Recharts `<Bar>` with the correct `<Cell>` component from recharts, which properly injects `fill` per-bar inside the SVG layer. |
| 2 | **Blank dashboard after logging out as Admin and logging in as Prof** | Added a `useEffect` in `App.js` that resets `page` to `"dashboard"` whenever `user.user_id` changes, preventing stale page state from carrying over between sessions. |
| 3 | **Lands on Analytics instead of Dashboard when switching professor accounts** | Same root cause as #2 — fixed by the same `useEffect` reset in `App.js`. |
| 4 | **"View All Sessions" in Admin panel always showed an error** | The `/admin/sessions` route was accidentally commented out in `main.py`. It has been uncommented and is now live. |

---

## Author

**Soham Rajesh Pawar**, CS22BTECH11055  
IIT Hyderabad · 8th Semester SWE