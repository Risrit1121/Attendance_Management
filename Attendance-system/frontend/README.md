# Frontend - Attendance System Portal

**React/Tailwind Web Application for DIAMS**  
IIT Hyderabad | Handover Edition

---

## 📚 Quick Navigation
- **New to this?** Start with [Quick Start](#quick-start)
- **Want to understand components?** See [Component Structure](#component-structure)
- **Need to add a page?** Check [Adding Features](#adding-features)
- **Having styling issues?** See [Tailwind CSS](#styling)

---

## 🚀 Quick Start

### Local Development

```bash
cd frontend

# Install dependencies
npm install

# Create .env file
echo "REACT_APP_API_URL=http://localhost:4040" > .env

# Start development server
npm start

# Application opens at: http://localhost:3000
```

### Docker

```bash
# From Attendance-system root
docker-compose up frontend

# Application at: http://localhost:3000
```

### Build for Production

```bash
npm run build

# Creates optimized build in build/ directory
# Can be served with any static host
```

---

## 📁 Project Structure

```
frontend/
├── src/
│   ├── api/
│   │   └── client.js                   # Axios HTTP client (155+ endpoints)
│   │
│   ├── components/                     # Reusable UI components
│   │   ├── Layout.js                   # Main sidebar + navbar
│   │   └── UI.js                       # Shared: Button, Badge, Modal, etc.
│   │
│   ├── context/                        # React Context state management
│   │   ├── AuthContext.js              # Login state, JWT token, user role
│   │   └── SchedulerContext.js         # Auto-session scheduling logic
│   │
│   ├── pages/                          # Page components (9 pages)
│   │   ├── Login.js                    # Login page (role: student/prof/admin)
│   │   ├── ProfessorDashboard.js       # Prof overview (courses, sessions)
│   │   ├── AdminDashboard.js           # Admin session monitor
│   │   ├── AdminOverview.js            # Admin system stats
│   │   ├── AdminCourses.js             # Admin course management
│   │   ├── AdminStudents.js            # Admin student management
│   │   ├── CoursesPage.js              # Prof's course list
│   │   ├── CourseView.js               # Detailed course (most complex)
│   │   ├── Students.js                 # Live attendance log
│   │   ├── Analytics.js                # Charts & trends
│   │   └── Settings.js                 # User preferences
│   │
│   ├── App.js                          # Main router
│   ├── index.js                        # React entry point
│   ├── index.css                       # Global styles
│   │
│   └── ...
│
├── public/                             # Static assets
│   ├── index.html                      # HTML template
│   ├── favicon.ico
│   └── ...
│
├── package.json                        # Dependencies & scripts
├── tailwind.config.js                  # Tailwind CSS configuration
├── postcss.config.js                   # PostCSS setup
├── Dockerfile                          # Container image
├── .env                                # Environment (REACT_APP_API_URL)
└── README.md                           # This file
```

---

## 🔑 Environment Variables

**`.env` file** (create in frontend directory):

```env
REACT_APP_API_URL=http://localhost:4040
```

**Note**: All variables must start with `REACT_APP_` to be accessible in React

---

## 🎨 Page Components

### Login.js
- **Route**: `/`
- **Access**: Public (no auth required)
- **Features**:
  - Email/password input
  - Role selection (student/professor/admin)
  - JWT token storage in localStorage
  - Role-based redirect

```javascript
// Usage:
import Login from './pages/Login';
// Redirect to this on logout
```

### ProfessorDashboard.js
- **Route**: `/professor`
- **Access**: Professor only
- **Features**:
  - List professor's courses
  - Show active sessions
  - Quick session start/end
  - Welcome message with time-based greeting
  - Course cards with attendance stats

### AdminDashboard.js
- **Route**: `/admin`
- **Access**: Admin only
- **Features**:
  - Monitor all active sessions
  - View session details
  - System-wide statistics
  - User activity

### AdminOverview.js
- **Route**: `/admin/overview`
- **Access**: Admin only
- **Features**:
  - Total students, professors, courses
  - Active sessions count
  - Average attendance across system
  - System health metrics

### AdminCourses.js
- **Route**: `/admin/courses`
- **Access**: Admin only
- **Features**:
  - List all courses
  - Add new course
  - Edit course details
  - Delete courses
  - Assign professors

### AdminStudents.js
- **Route**: `/admin/students`
- **Access**: Admin only
- **Features**:
  - List all students
  - Add student
  - View student analytics
  - Bulk operations

### CoursesPage.js
- **Route**: `/professor/courses`
- **Access**: Professor only
- **Features**:
  - List professor's courses
  - Course status badges
  - Quick links to course view

### CourseView.js (Most Complex)
- **Route**: `/professor/courses/:courseId`
- **Access**: Professor only
- **Features**:
  - **Session Management**:
    - Start new session (BLE/QR/Manual)
    - Display BLE minor code
    - Generate & display QR code
    - End session
  - **Live Attendance**:
    - Real-time student list (updates every 3s)
    - Mark/unmark students
  - **Schedule Management**:
    - Add lecture schedule
    - Edit existing schedules
    - Delete schedules
  - **Analytics**:
    - Course attendance summary
    - Per-lecture breakdown

### Students.js
- **Route**: `/professor/students`
- **Access**: Professor only
- **Features**:
  - Live attendance log
  - Real-time updates
  - Attendance statistics
  - Export to CSV

### Analytics.js
- **Route**: `/analytics`
- **Access**: Professor & Admin
- **Features**:
  - Bar charts (Recharts)
  - Attendance trends
  - Per-course breakdown
  - Per-professor breakdown (admin)
  - At-risk students alert
  - Download reports

### Settings.js
- **Route**: `/settings`
- **Access**: All authenticated users
- **Features**:
  - API endpoint configuration
  - User preferences
  - Theme settings

---

## 🔌 Components

Located in: [`src/components/`](./src/components/)

### Layout.js
**Main application layout wrapper**

```javascript
// Provides:
// - Top navigation bar
// - Sidebar navigation
// - Role-based menu items
// - Logout button
// - User profile

// Usage:
import Layout from './components/Layout';

function App() {
  return (
    <Layout>
      {/* Page content */}
    </Layout>
  );
}
```

### UI.js
**Reusable UI components**

Common components exported:
```javascript
export { Button }           // Styled button
export { Badge }            // Status badges
export { Card }             // Content cards
export { Modal }            // Popup dialogs
export { Input }            // Text inputs
export { Select }           // Dropdown
export { Table }            // Data tables
export { Spinner }          // Loading indicator
export { Alert }            // Alert messages
export { Tabs }             // Tabbed interface
export { Pagination }       // Page navigation
```

Usage:
```javascript
import { Button, Card, Input } from './components/UI';

function MyComponent() {
  return (
    <Card>
      <Input placeholder="Enter text" />
      <Button onClick={handleClick}>Submit</Button>
    </Card>
  );
}
```

---

## 🌐 API Integration

Located in: [`src/api/client.js`](./src/api/client.js)

**Axios HTTP client** with 155+ endpoint wrappers

### Authentication

```javascript
import { login, logout } from './api/client';

// Login
const { token, user } = await login(email, password, role);
localStorage.setItem('token', token);

// Logout
logout();
localStorage.removeItem('token');
```

### Courses

```javascript
import {
  getProfessorCourses,
  createCourse,
  getCourseSchedules,
  addSchedule
} from './api/client';

// Get courses
const courses = await getProfessorCourses(profId);

// Create course
const course = await createCourse({
  code: 'CS101',
  name: 'Data Structures',
  professor: profId
});
```

### Sessions

```javascript
import {
  startSession,
  endSession,
  getActiveSession
} from './api/client';

// Start BLE session
const { sessionId, minor } = await startSession(courseId, 'BLE');

// Get active session
const session = await getActiveSession(courseId);

// End session
await endSession(sessionId);
```

### Attendance

```javascript
import {
  markAttendance,
  getSessionAttendance
} from './api/client';

// Mark attendance
await markAttendance(sessionId, studentId, 'BLE', {
  major: 'FDA50693...',
  minor: 12345
});

// Get records
const records = await getSessionAttendance(sessionId);
```

### Analytics

```javascript
import {
  getCourseAnalytics,
  getStudentAnalytics,
  getAtRiskStudents
} from './api/client';

// Course analytics
const stats = await getCourseAnalytics(courseId);

// Student analytics
const studentStats = await getStudentAnalytics(courseId);

// At-risk students
const atRisk = await getAtRiskStudents(profId);
```

---

## 💾 State Management

### AuthContext.js
**Global authentication state**

```javascript
import { useAuth } from './context/AuthContext';

function MyComponent() {
  const { user, token, isLoggedIn, login, logout } = useAuth();

  return (
    <div>
      {isLoggedIn && <p>Welcome, {user.email}</p>}
      <button onClick={logout}>Logout</button>
    </div>
  );
}
```

**Provides:**
- `user` - Current user object
- `token` - JWT token
- `isLoggedIn` - Authentication status
- `role` - User role (student/professor/admin)
- `login(email, password, role)` - Login function
- `logout()` - Logout function

### SchedulerContext.js
**Auto-session scheduling logic**

```javascript
import { useScheduler } from './context/SchedulerContext';

function SessionManager() {
  const { 
    activeSessions,
    refreshSessions,
    createSession,
    endSession
  } = useScheduler();

  return (
    <div>
      {activeSessions.map(s => (
        <SessionCard key={s.id} session={s} />
      ))}
    </div>
  );
}
```

---

## 🎨 Styling

### Tailwind CSS

**Configuration**: [`tailwind.config.js`](./tailwind.config.js)

All pages use Tailwind utility classes:

```javascript
// Responsive grid
<div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">

// Flexbox layout
<div className="flex justify-between items-center">

// Spacing
<button className="px-4 py-2 m-2">
```

### Theme Colors

```javascript
// Primary colors (blue theme)
// bg-blue-500, text-blue-600, etc.

// Neutral colors (grays)
// bg-gray-100, text-gray-700, etc.

// Status colors
// bg-green-500 (success)
// bg-red-500 (error)
// bg-yellow-500 (warning)
```

### Global Styles

**File**: [`src/index.css`](./src/index.css)

Contains Tailwind imports and custom CSS:
```css
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Custom component styles */
/* Font definitions */
/* Animation keyframes */
```

---

## 🔄 Data Flow

### Typical Page Workflow

```
1. Page Component Loads
   ├─ Check AuthContext (logged in?)
   ├─ Fetch data from API
   ├─ Set local state
   └─ Render UI

2. User Interaction (click, submit, etc.)
   ├─ Call API function
   ├─ Update local state
   └─ Re-render component

3. Real-Time Updates
   ├─ Polling: setInterval(() => refresh data, 3000ms)
   ├─ Update state
   └─ Re-render with fresh data
```

### Real-Time Dashboard Updates

CourseView.js and Analytics.js update every 3 seconds:

```javascript
useEffect(() => {
  const interval = setInterval(() => {
    fetchAttendance();  // Get latest data
  }, 3000);  // 3-second interval
  
  return () => clearInterval(interval);
}, []);
```

---

## 🔐 Authentication Flow

```
Login Page
   ├─ User enters email, password, role
   ├─ POST /login to backend
   └─ Receive JWT token

Token Storage
   ├─ Save to localStorage
   ├─ Add to all API requests (Authorization header)
   └─ Persist across page reloads

Protected Routes
   ├─ Check AuthContext.isLoggedIn
   ├─ If false, redirect to Login
   └─ If true, render page

Logout
   ├─ Clear localStorage
   ├─ Clear AuthContext
   └─ Redirect to Login
```

---

## 🚀 Adding Features

### Add a New Page

1. **Create page component** in `src/pages/NewPage.js`:

```javascript
import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { Button, Card } from '../components/UI';
import Layout from '../components/Layout';
import * as api from '../api/client';

function NewPage() {
  const { user } = useAuth();
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        const result = await api.someEndpoint();
        setData(result);
      } catch (error) {
        console.error('Error:', error);
      } finally {
        setLoading(false);
      }
    };
    
    fetchData();
  }, []);

  if (loading) return <div>Loading...</div>;

  return (
    <Layout>
      <div className="p-6">
        <h1 className="text-3xl font-bold mb-4">New Page</h1>
        <Card>
          {/* Your content */}
        </Card>
      </div>
    </Layout>
  );
}

export default NewPage;
```

2. **Add route** in `App.js`:

```javascript
import NewPage from './pages/NewPage';

function App() {
  return (
    <Routes>
      <Route path="/new-page" element={<NewPage />} />
    </Routes>
  );
}
```

3. **Add link** in `Layout.js` navigation menu

### Add a New API Endpoint Wrapper

In `src/api/client.js`:

```javascript
export const myNewEndpoint = async (param1, param2) => {
  const response = await api.get(`/my-endpoint/${param1}`, {
    params: { param2 }
  });
  return response.data;
};

// Usage in component:
const result = await myNewEndpoint('value1', 'value2');
```

---

## 🧪 Testing

### Run Tests

```bash
npm test

# Tests for:
# - Component rendering
# - User interactions
# - API calls (mocked)
# - State management
```

### Manual Testing Workflow

1. **Login**: Use test credentials
2. **Navigate**: Test each page
3. **Interactions**: Test buttons, forms, dropdowns
4. **Real-time**: Check 3-second updates
5. **Errors**: Test with backend down

---

## 🔧 Development Tools

### React Developer Tools

```bash
# Install Chrome extension
# Open Dev Tools → Components tab
# View component hierarchy
# Inspect props and state
```

### Network Inspection

```bash
# Open Dev Tools → Network tab
# See all API calls
# Check response times
# Debug failed requests
```

### Local Storage Inspection

```bash
# Open Dev Tools → Application tab
# View localStorage
# Check JWT token
# Clear data if needed
```

---

## 🐛 Troubleshooting

### API Calls Failing

```javascript
// Check:
// 1. REACT_APP_API_URL in .env
console.log(process.env.REACT_APP_API_URL);

// 2. Backend running
// curl http://localhost:4040/health

// 3. JWT token valid
console.log(localStorage.getItem('token'));

// 4. CORS enabled
// Check browser console for CORS errors
```

### Page Not Rendering

```javascript
// Check:
// 1. Component mounted
// console.log inside useEffect

// 2. Data fetched
// Check Network tab

// 3. State updated
// Use React Dev Tools

// 4. Route correct
// Check URL in browser
```

### Styling Not Applied

```javascript
// Check:
// 1. Tailwind rebuild
// npm run build:css

// 2. Class names correct
// Check HTML in Dev Tools

// 3. CSS specificity
// Inspect element in Dev Tools

// 4. PostCSS config
// Check postcss.config.js
```

---

## 📊 Performance Optimization

### Code Splitting

React Router automatically handles code splitting for each route.

### Lazy Loading

```javascript
import { lazy, Suspense } from 'react';

const HeavyComponent = lazy(() => import('./HeavyComponent'));

function App() {
  return (
    <Suspense fallback={<div>Loading...</div>}>
      <HeavyComponent />
    </Suspense>
  );
}
```

### Caching API Responses

```javascript
const cache = {};

export const getCachedData = async (key) => {
  if (cache[key]) return cache[key];
  
  const data = await fetch(`/api/${key}`);
  cache[key] = data;
  return data;
};
```

---

## 🔐 Security Best Practices

✅ **Do:**
- Store JWT in secure httpOnly cookies (if possible)
- Validate user role before showing pages
- Sanitize user input
- Use HTTPS in production
- Keep dependencies updated
- Never expose API keys

❌ **Don't:**
- Store sensitive data in localStorage
- Trust user role from frontend only
- Disable CORS security
- Log sensitive data
- Commit .env to git
- Use default credentials

---

## 🧹 Maintenance

### Update Dependencies

```bash
# Check for updates
npm outdated

# Update packages
npm update

# Check for vulnerabilities
npm audit

# Fix vulnerabilities
npm audit fix
```

### Clean Build

```bash
# Remove old builds
rm -rf node_modules build

# Fresh install
npm install

# Rebuild
npm run build
```

### Logs & Debugging

```bash
# View console logs
npm start

# Browser console
F12 → Console tab

# Network debugging
F12 → Network tab
```

---

## 📚 Additional Resources

- [React Documentation](https://react.dev/)
- [Tailwind CSS Docs](https://tailwindcss.com/)
- [Axios Documentation](https://axios-http.com/)
- [React Router Docs](https://reactrouter.com/)
- [Recharts (Charts) Docs](https://recharts.org/)

---

## 💬 Notes for Team

### Project Flow

```
User visits http://localhost:3000
   ├─ Check localStorage for JWT
   ├─ If no JWT → Show Login page
   ├─ If JWT → Verify in AuthContext
   ├─ If valid → Redirect to role dashboard
   └─ If invalid → Show Login again
```

### Common Tasks

| Task | Files to Edit |
|------|--------------|
| Add page | `src/pages/NewPage.js`, `App.js`, `Layout.js` |
| Add API call | `src/api/client.js`, then use in page |
| Add component | `src/components/` + use in pages |
| Change styling | `tailwind.config.js` or `src/index.css` |
| Change layout | `src/components/Layout.js` |
| Add state | `src/context/NewContext.js` |

### Critical Code Paths

1. **Login**: `Login.js` → `AuthContext.js` → redirect to dashboard
2. **API Call**: Page component → `client.js` → Backend → Response
3. **Real-time Update**: Polling loop (3s interval) → State → Re-render
4. **Role-based Access**: `AuthContext.user.role` → Show/hide pages

---

## 📞 Support

For issues:
1. Check browser console: `F12 → Console`
2. Check network tab: `F12 → Network`
3. Verify backend running: `curl http://localhost:4040/health`
4. Check `.env` configuration
5. Review troubleshooting section

---

## Credits

**Frontend - DIAMS**  
IIT Hyderabad

**Original Developer**: Soham Rajesh Pawar (CS22BTECH11055)