// import { useState, useEffect } from "react";
// import { getAdminStats, getCourses, getCourseAnalytics, getProfAnalytics } from "../api/client";
// import { useAuth } from "../context/AuthContext";
// import {
//   BarChart, Bar, XAxis, YAxis, Tooltip, ResponsiveContainer,
//   LineChart, Line,
// } from "recharts";
// import {
//   BarChart3, TrendingUp, Users, Activity,
//   AlertTriangle, BookOpen, GraduationCap, ChevronDown, ChevronUp,
// } from "lucide-react";
// import { StatCard, ProgressBar, AttendancePct, Spinner, Empty } from "../components/UI";
// import API from "../api/client";

// // ── Helpers ───────────────────────────────────────────────────────────────────

// function parseUTC(str) {
//   if (!str) return null;
//   const s = str.endsWith("Z") || str.includes("+") ? str : str + "Z";
//   return new Date(s);
// }

// const DarkTooltip = ({ active, payload, label }) => {
//   if (!active || !payload?.length) return null;
//   return (
//     <div className="bg-card border border-edge rounded-xl px-3 py-2.5 text-xs shadow-float">
//       <p className="text-snow font-medium mb-1">{label}</p>
//       {payload.map((p, i) => (
//         <p key={i} style={{ color: p.color }}>{p.name}: {p.value}</p>
//       ))}
//     </div>
//   );
// };

// const COURSE_COLORS = ["bg-azure-500","bg-jade-500","bg-violet-500","bg-amber-500","bg-rose-500"];
// const BAR_COLORS    = ["#3B82F6","#34D399","#8B5CF6","#FBBF24","#FB7185"];
// const DAY_LABELS    = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

// function buildTrendData(allSessions) {
//   const byDay = {};
//   DAY_LABELS.forEach((d, i) => { byDay[i] = { total: 0, sessions: 0 }; });
//   allSessions.forEach(s => {
//     const d = parseUTC(s.start_time);
//     if (!d) return;
//     const ist = new Date(d.toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
//     byDay[ist.getDay()].total    += s.unique_students;
//     byDay[ist.getDay()].sessions += 1;
//   });
//   return [1,2,3,4,5,6].map(i => ({
//     day: DAY_LABELS[i],
//     pct: byDay[i].sessions > 0 ? Math.round(byDay[i].total / byDay[i].sessions) : null,
//   })).filter(d => d.pct !== null);
// }

// // ── At-Risk Students Card (professor view) ────────────────────────────────────

// function AtRiskCard({ profId }) {
//   const [data,    setData]    = useState([]);
//   const [loading, setLoading] = useState(true);
//   const [error,   setError]   = useState(null);

//   useEffect(() => {
//     API.get(`/analytics/at-risk/${profId}`)
//       .then(r => setData(r.data))
//       .catch(() => setError("Could not load."))
//       .finally(() => setLoading(false));
//   }, [profId]);

//   return (
//     <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "180ms" }}>
//       <div className="flex items-center gap-2 mb-1">
//         <AlertTriangle size={14} className="text-rose-400" />
//         <h2 className="text-snow font-semibold text-sm">At-Risk Students</h2>
//       </div>
//       <p className="text-soft text-xs mb-4">Below 25% attendance in any course</p>

//       {loading ? (
//         <div className="flex items-center justify-center h-32"><Spinner size={20} /></div>
//       ) : error ? (
//         <p className="text-rose-400 text-xs text-center py-8">{error}</p>
//       ) : data.length === 0 ? (
//         <div className="flex flex-col items-center justify-center gap-2 h-32">
//           <div className="w-8 h-8 rounded-xl bg-jade-500/15 flex items-center justify-center">
//             <Users size={14} className="text-jade-400" />
//           </div>
//           <p className="text-soft text-xs text-center">No at-risk students 🎉</p>
//         </div>
//       ) : (
//         <div className="space-y-2 max-h-64 overflow-y-auto">
//           {data.map((s, i) => (
//             <div key={i}
//               className="flex items-center gap-3 p-2.5 rounded-xl bg-ink border border-edge hover:border-rose-500/20 transition-colors"
//             >
//               <div className="w-7 h-7 rounded-lg bg-rose-500/15 flex items-center justify-center shrink-0 border border-rose-500/20">
//                 <span className="text-rose-400 text-xs font-bold">
//                   {s.student_name.charAt(0).toUpperCase()}
//                 </span>
//               </div>
//               <div className="flex-1 min-w-0">
//                 <p className="text-snow text-xs font-medium truncate">{s.student_name}</p>
//                 <p className="text-dim text-xs font-mono truncate">{s.course_name}</p>
//               </div>
//               <div className="text-right shrink-0">
//                 <p className="text-rose-400 text-xs font-bold font-mono">{s.pct}%</p>
//                 <p className="text-dim text-xs font-mono">{s.attended}/{s.total}</p>
//               </div>
//             </div>
//           ))}
//         </div>
//       )}
//       {!loading && data.length > 0 && (
//         <p className="text-dim text-xs font-mono mt-3 pt-3 border-t border-edge">
//           {data.length} student{data.length !== 1 ? "s" : ""} flagged
//         </p>
//       )}
//     </div>
//   );
// }

// // ── Admin Analytics view ──────────────────────────────────────────────────────

// function ProfCourseRow({ course, colorIdx }) {
//   const [open, setOpen] = useState(false);

//   return (
//     <div className="border-b border-edge last:border-0">
//       <button
//         onClick={() => setOpen(o => !o)}
//         className="w-full flex items-center gap-4 px-5 py-4 hover:bg-white/2 transition-colors text-left"
//       >
//         <div className={`w-2 h-8 rounded-full shrink-0 ${COURSE_COLORS[colorIdx % COURSE_COLORS.length]}`} />
//         <div className="flex-1 min-w-0">
//           <p className="text-snow text-sm font-medium">{course.name}</p>
//           <p className="text-soft text-xs mt-0.5">
//             {course.sessions} sessions · {course.enrolled} enrolled · avg {course.avg_pct}%
//           </p>
//         </div>
//         <div className="w-28 shrink-0">
//           <ProgressBar value={course.avg_pct} max={100} />
//         </div>
//         <AttendancePct value={course.avg_pct} />
//         {open ? <ChevronUp size={14} className="text-dim shrink-0" /> : <ChevronDown size={14} className="text-dim shrink-0" />}
//       </button>
//     </div>
//   );
// }

// function AdminAnalyticsView() {
//   const [data,    setData]    = useState(null);
//   const [loading, setLoading] = useState(true);
//   const [error,   setError]   = useState(null);

//   useEffect(() => {
//     API.get("/admin/analytics")
//       .then(r => setData(r.data))
//       .catch(() => setError("Could not load admin analytics. Make sure the /admin/analytics endpoint is deployed."))
//       .finally(() => setLoading(false));
//   }, []);

//   if (loading) return <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>;

//   if (error) return (
//     <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-2xl px-5 py-4">
//       <AlertTriangle size={16} className="text-amber-400 shrink-0 mt-0.5" />
//       <p className="text-amber-300 text-sm">{error}</p>
//     </div>
//   );

//   const { totals, profs } = data;

//   // Build bar data: one bar per course across all profs
//   const allCourses = profs.flatMap(p => p.courses.map(c => ({
//     name:    c.name.length > 10 ? c.name.slice(0, 10) + "…" : c.name,
//     prof:    p.prof_name.split(" ").slice(-1)[0], // last name
//     avg_pct: c.avg_pct,
//     sessions: c.sessions,
//     enrolled: c.enrolled,
//   })));

//   return (
//     <div className="space-y-8">
//       {/* Totals */}
//       <div className="grid grid-cols-3 lg:grid-cols-6 gap-4">
//         {[
//           { label: "Sessions",   value: totals.sessions,   color: "text-azure-400"  },
//           { label: "Att. Marks", value: totals.attendance, color: "text-jade-400"   },
//           { label: "Avg/Session",value: totals.avg,        color: "text-violet-400" },
//           { label: "Students",   value: totals.students,   color: "text-amber-400"  },
//           { label: "Courses",    value: totals.courses,    color: "text-rose-400"   },
//           { label: "Professors", value: totals.profs,      color: "text-snow"       },
//         ].map((s, i) => (
//           <div key={i} className="bg-card border border-edge rounded-2xl p-4 text-center animate-slide-up" style={{ animationDelay: `${i * 40}ms` }}>
//             <p className="text-soft text-xs uppercase tracking-widest mb-1">{s.label}</p>
//             <p className={`text-2xl font-bold font-mono ${s.color}`}>{s.value}</p>
//           </div>
//         ))}
//       </div>

//       {/* Bar chart: avg attendance % per course */}
//       <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "80ms" }}>
//         <div className="flex items-center justify-between mb-6">
//           <div>
//             <h2 className="text-snow font-semibold text-sm">Avg Attendance % per Course</h2>
//             <p className="text-soft text-xs mt-0.5">Across all professors — unique students attended / enrolled</p>
//           </div>
//           <BarChart3 size={16} className="text-dim" />
//         </div>
//         {allCourses.length === 0 ? (
//           <Empty icon={BarChart3} title="No course data" sub="Start sessions to see data." />
//         ) : (
//           <ResponsiveContainer width="100%" height={220}>
//             <BarChart data={allCourses} barSize={24}>
//               <XAxis dataKey="name" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
//               <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} domain={[0, 100]}
//                 tickFormatter={v => `${v}%`} />
//               <Tooltip content={<DarkTooltip />} cursor={{ fill: "rgba(255,255,255,0.03)" }} />
//               {allCourses.map((_, i) => null)}
//               <Bar dataKey="avg_pct" name="Avg Attendance %" radius={[6,6,0,0]}>
//                 {allCourses.map((_, i) => (
//                   <rect key={i} fill={BAR_COLORS[i % BAR_COLORS.length]} />
//                 ))}
//               </Bar>
//             </BarChart>
//           </ResponsiveContainer>
//         )}
//       </div>

//       {/* Per-professor breakdown */}
//       {profs.map((prof, pi) => (
//         <div key={prof.prof_id}
//           className="bg-card border border-edge rounded-2xl overflow-hidden animate-slide-up"
//           style={{ animationDelay: `${160 + pi * 60}ms` }}
//         >
//           <div className="px-5 py-4 border-b border-edge flex items-center gap-3">
//             <div className="w-8 h-8 rounded-xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center">
//               <GraduationCap size={14} className="text-violet-400" />
//             </div>
//             <div>
//               <p className="text-snow font-semibold text-sm">{prof.prof_name}</p>
//               <p className="text-soft text-xs font-mono">{prof.courses.length} course{prof.courses.length !== 1 ? "s" : ""}</p>
//             </div>
//           </div>
//           {prof.courses.length === 0 ? (
//             <p className="text-soft text-sm text-center py-6">No courses assigned.</p>
//           ) : (
//             <div className="divide-y divide-edge">
//               {prof.courses.map((c, ci) => (
//                 <div key={c.course_id} className="flex items-center gap-4 px-5 py-4 hover:bg-white/2 transition-colors">
//                   <div className={`w-2 h-8 rounded-full shrink-0 ${COURSE_COLORS[ci % COURSE_COLORS.length]}`} />
//                   <div className="flex-1 min-w-0">
//                     <p className="text-snow text-sm font-medium">{c.name}</p>
//                     <p className="text-soft text-xs mt-0.5">
//                       {c.sessions} sessions · {c.enrolled} enrolled
//                     </p>
//                   </div>
//                   <div className="w-28 shrink-0">
//                     <ProgressBar value={c.avg_pct} max={100} />
//                   </div>
//                   <AttendancePct value={c.avg_pct} />
//                 </div>
//               ))}
//             </div>
//           )}
//         </div>
//       ))}
//     </div>
//   );
// }

// // ── Professor Analytics view ──────────────────────────────────────────────────

// function ProfAnalyticsView({ user }) {
//   const [loading,          setLoading]          = useState(true);
//   const [courses,          setCourses]          = useState([]);
//   const [profData,         setProfData]         = useState([]);
//   const [courseSessionMap, setCourseSessionMap] = useState({});
//   const [enrolledMap,      setEnrolledMap]      = useState({});

//   useEffect(() => {
//     async function load() {
//       try {
//         const cRes = await getCourses(user.user_id);
//         setCourses(cRes.data);
//         const pRes = await getProfAnalytics(user.user_id);
//         setProfData(pRes.data);
//         const sessMap = {}, enrollMap = {};
//         await Promise.all(cRes.data.map(async c => {
//           try {
//             const r = await getCourseAnalytics(c.id);
//             sessMap[c.id]   = r.data.sessions;
//             enrollMap[c.id] = r.data.enrolled ?? 0;
//           } catch {
//             sessMap[c.id]   = [];
//             enrollMap[c.id] = 0;
//           }
//         }));
//         setCourseSessionMap(sessMap);
//         setEnrolledMap(enrollMap);
//       } finally {
//         setLoading(false);
//       }
//     }
//     load();
//   }, [user]);

//   if (loading) return <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>;

//   const barData = courses.map(c => {
//     const sessions = courseSessionMap[c.id] || [];
//     return {
//       name:   c.name.length > 10 ? c.name.slice(0, 10) + "…" : c.name,
//       unique: Math.round(sessions.reduce((s, x) => s + x.unique_students, 0) / (sessions.length || 1)),
//       total:  Math.round(sessions.reduce((s, x) => s + x.total_marks,     0) / (sessions.length || 1)),
//     };
//   });

//   const allSessions = Object.values(courseSessionMap).flat();
//   const trendData   = buildTrendData(allSessions);

//   return (
//     <div className="space-y-8">
//       {/* Summary stats */}
//       {profData.length > 0 && (
//         <div className="grid grid-cols-3 gap-4">
//           <StatCard
//             label="Total Sessions"
//             value={profData.reduce((s, c) => s + c.sessions, 0)}
//             icon={Activity} color="azure" delay={0}
//           />
//           <StatCard
//             label="Total Attendance Marks"
//             value={profData.reduce((s, c) => s + c.attendance, 0)}
//             icon={Users} color="jade" delay={80}
//           />
//           <StatCard
//             label="Avg Marks / Session"
//             value={(
//               profData.reduce((s, c) => s + c.attendance, 0) /
//               (profData.reduce((s, c) => s + c.sessions, 0) || 1)
//             ).toFixed(1)}
//             icon={TrendingUp} color="amber" delay={160}
//           />
//         </div>
//       )}

//       {/* Charts row */}
//       <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
//         {/* Bar chart */}
//         <div className="lg:col-span-2 bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "100ms" }}>
//           <div className="flex items-center justify-between mb-6">
//             <div>
//               <h2 className="text-snow font-semibold text-sm">Avg Attendance per Session</h2>
//               <p className="text-soft text-xs mt-0.5">Unique students &amp; total marks, averaged per session</p>
//             </div>
//             <BarChart3 size={16} className="text-dim" />
//           </div>
//           {barData.length === 0 ? (
//             <Empty icon={BarChart3} title="No data yet" sub="Start sessions to see data here." />
//           ) : (
//             <ResponsiveContainer width="100%" height={200}>
//               <BarChart data={barData} barSize={20} barGap={6}>
//                 <XAxis dataKey="name" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
//                 <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
//                 <Tooltip content={<DarkTooltip />} cursor={{ fill: "rgba(255,255,255,0.03)" }} />
//                 <Bar dataKey="unique" name="Avg Unique Students" fill="#3B82F6" radius={[6,6,0,0]} />
//                 <Bar dataKey="total"  name="Avg Total Marks"     fill="#8B5CF6" radius={[6,6,0,0]} fillOpacity={0.6} />
//               </BarChart>
//             </ResponsiveContainer>
//           )}
//         </div>

//         {/* At-risk card */}
//         <AtRiskCard profId={user.user_id} />
//       </div>

//       {/* Trend line */}
//       <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "260ms" }}>
//         <div className="flex items-center justify-between mb-6">
//           <div>
//             <h2 className="text-snow font-semibold text-sm">Weekly Trend</h2>
//             <p className="text-soft text-xs mt-0.5">
//               Avg unique students per session by day of week (IST) — computed from historical sessions
//             </p>
//           </div>
//           <TrendingUp size={16} className="text-dim" />
//         </div>
//         {trendData.length === 0 ? (
//           <Empty icon={TrendingUp} title="Not enough data" sub="Run sessions on multiple days to see the trend." />
//         ) : (
//           <ResponsiveContainer width="100%" height={160}>
//             <LineChart data={trendData}>
//               <XAxis dataKey="day" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
//               <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
//               <Tooltip content={<DarkTooltip />} />
//               <Line
//                 type="monotone" dataKey="pct" name="Avg Unique Students"
//                 stroke="#3B82F6" strokeWidth={2.5}
//                 dot={{ fill: "#3B82F6", r: 4, strokeWidth: 0 }}
//                 activeDot={{ r: 6 }}
//               />
//             </LineChart>
//           </ResponsiveContainer>
//         )}
//       </div>

//       {/* Per-course breakdown table */}
//       {profData.length > 0 && (
//         <div className="bg-card border border-edge rounded-2xl overflow-hidden animate-slide-up" style={{ animationDelay: "340ms" }}>
//           <div className="px-5 py-4 border-b border-edge">
//             <h2 className="text-snow font-semibold text-sm">Course Breakdown</h2>
//           </div>
//           <div className="divide-y divide-edge">
//             {profData.map((c, i) => {
//               const avgPct = c.sessions > 0
//                 ? Math.min(100, (c.avg / (enrolledMap[c.course_id] || Math.max(c.avg, 1))) * 100)
//                 : 0;
//               return (
//                 <div key={c.course_id} className="flex items-center gap-4 px-5 py-4 hover:bg-white/2 transition-colors">
//                   <div className={`w-2 h-8 rounded-full ${COURSE_COLORS[i % COURSE_COLORS.length]}`} />
//                   <div className="flex-1 min-w-0">
//                     <p className="text-snow text-sm font-medium">{c.course_name}</p>
//                     <p className="text-soft text-xs mt-0.5">
//                       {c.sessions} sessions · {c.attendance} total marks · avg {c.avg} / session
//                     </p>
//                   </div>
//                   <div className="w-32">
//                     <ProgressBar value={avgPct} max={100} />
//                   </div>
//                   <AttendancePct value={avgPct} />
//                 </div>
//               );
//             })}
//           </div>
//         </div>
//       )}
//     </div>
//   );
// }

// // ── Root component ────────────────────────────────────────────────────────────

// export default function Analytics() {
//   const { user } = useAuth();

//   return (
//     <div className="space-y-8">
//       <div className="animate-slide-up">
//         <h1 className="text-snow text-2xl font-bold tracking-tight">Analytics</h1>
//         <p className="text-soft text-sm mt-1">
//           {user.role === "admin"
//             ? "System-wide attendance breakdown across all professors and courses"
//             : "Attendance insights across your courses"}
//         </p>
//       </div>

//       {user.role === "admin"
//         ? <AdminAnalyticsView />
//         : <ProfAnalyticsView user={user} />
//       }
//     </div>
//   );
// }

import { useState, useEffect } from "react";
import { getCourses, getCourseAnalytics, getProfAnalytics } from "../api/client";
import { useAuth } from "../context/AuthContext";
import {
  BarChart, Bar, Cell, XAxis, YAxis, Tooltip, ResponsiveContainer,
  LineChart, Line,
} from "recharts";
import {
  BarChart3, TrendingUp, Users, Activity,
  AlertTriangle, GraduationCap, ChevronDown, ChevronUp,
} from "lucide-react";
import { StatCard, ProgressBar, AttendancePct, Spinner, Empty } from "../components/UI";
import API from "../api/client";

// ── Helpers ───────────────────────────────────────────────────────────────────

function parseUTC(str) {
  if (!str) return null;
  const s = str.endsWith("Z") || str.includes("+") ? str : str + "Z";
  return new Date(s);
}

const DarkTooltip = ({ active, payload, label }) => {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-card border border-edge rounded-xl px-3 py-2.5 text-xs shadow-float">
      <p className="text-snow font-medium mb-1">{label}</p>
      {payload.map((p, i) => (
        <p key={i} style={{ color: p.color }}>{p.name}: {p.value}</p>
      ))}
    </div>
  );
};

const COURSE_COLORS = ["bg-azure-500","bg-jade-500","bg-violet-500","bg-amber-500","bg-rose-500"];
const BAR_COLORS    = ["#3B82F6","#34D399","#8B5CF6","#FBBF24","#FB7185"];
const DAY_LABELS    = ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"];

function buildTrendData(allSessions) {
  const byDay = {};
  DAY_LABELS.forEach((d, i) => { byDay[i] = { total: 0, sessions: 0 }; });
  allSessions.forEach(s => {
    const d = parseUTC(s.start_time);
    if (!d) return;
    const ist = new Date(d.toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
    byDay[ist.getDay()].total    += s.unique_students;
    byDay[ist.getDay()].sessions += 1;
  });
  return [1,2,3,4,5,6].map(i => ({
    day: DAY_LABELS[i],
    pct: byDay[i].sessions > 0 ? Math.round(byDay[i].total / byDay[i].sessions) : null,
  })).filter(d => d.pct !== null);
}

// ── At-Risk Students Card (professor view) ────────────────────────────────────

function AtRiskCard({ profId }) {
  const [data,    setData]    = useState([]);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);

  useEffect(() => {
    API.get(`/analytics/at-risk/${profId}`)
      .then(r => setData(r.data))
      .catch(() => setError("Could not load."))
      .finally(() => setLoading(false));
  }, [profId]);

  return (
    <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "180ms" }}>
      <div className="flex items-center gap-2 mb-1">
        <AlertTriangle size={14} className="text-rose-400" />
        <h2 className="text-snow font-semibold text-sm">At-Risk Students</h2>
      </div>
      <p className="text-soft text-xs mb-4">Below 25% attendance in any course</p>

      {loading ? (
        <div className="flex items-center justify-center h-32"><Spinner size={20} /></div>
      ) : error ? (
        <p className="text-rose-400 text-xs text-center py-8">{error}</p>
      ) : data.length === 0 ? (
        <div className="flex flex-col items-center justify-center gap-2 h-32">
          <div className="w-8 h-8 rounded-xl bg-jade-500/15 flex items-center justify-center">
            <Users size={14} className="text-jade-400" />
          </div>
          <p className="text-soft text-xs text-center">No at-risk students 🎉</p>
        </div>
      ) : (
        <div className="space-y-2 max-h-64 overflow-y-auto">
          {data.map((s, i) => (
            <div key={i}
              className="flex items-center gap-3 p-2.5 rounded-xl bg-ink border border-edge hover:border-rose-500/20 transition-colors"
            >
              <div className="w-7 h-7 rounded-lg bg-rose-500/15 flex items-center justify-center shrink-0 border border-rose-500/20">
                <span className="text-rose-400 text-xs font-bold">
                  {s.student_name.charAt(0).toUpperCase()}
                </span>
              </div>
              <div className="flex-1 min-w-0">
                <p className="text-snow text-xs font-medium truncate">{s.student_name}</p>
                <p className="text-dim text-xs font-mono truncate">{s.course_name}</p>
              </div>
              <div className="text-right shrink-0">
                <p className="text-rose-400 text-xs font-bold font-mono">{s.pct}%</p>
                <p className="text-dim text-xs font-mono">{s.attended}/{s.total}</p>
              </div>
            </div>
          ))}
        </div>
      )}
      {!loading && data.length > 0 && (
        <p className="text-dim text-xs font-mono mt-3 pt-3 border-t border-edge">
          {data.length} student{data.length !== 1 ? "s" : ""} flagged
        </p>
      )}
    </div>
  );
}

// ── Admin Analytics view ──────────────────────────────────────────────────────

function AdminAnalyticsView() {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);

  useEffect(() => {
    API.get("/admin/analytics")
      .then(r => setData(r.data))
      .catch(() => setError("Could not load admin analytics. Make sure the /admin/analytics endpoint is deployed."))
      .finally(() => setLoading(false));
  }, []);

  if (loading) return <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>;

  if (error) return (
    <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-2xl px-5 py-4">
      <AlertTriangle size={16} className="text-amber-400 shrink-0 mt-0.5" />
      <p className="text-amber-300 text-sm">{error}</p>
    </div>
  );

  const { totals, profs } = data;

  // Build bar data: one bar per course across all profs
  const allCourses = profs.flatMap(p => p.courses.map(c => ({
    name:     c.name.length > 10 ? c.name.slice(0, 10) + "…" : c.name,
    prof:     p.prof_name.split(" ").slice(-1)[0],
    avg_pct:  c.avg_pct,
    sessions: c.sessions,
    enrolled: c.enrolled,
  })));

  return (
    <div className="space-y-8">
      {/* Totals */}
      <div className="grid grid-cols-3 lg:grid-cols-6 gap-4">
        {[
          { label: "Sessions",    value: totals.sessions,   color: "text-azure-400"  },
          { label: "Att. Marks",  value: totals.attendance, color: "text-jade-400"   },
          { label: "Avg/Session", value: totals.avg,        color: "text-violet-400" },
          { label: "Students",    value: totals.students,   color: "text-amber-400"  },
          { label: "Courses",     value: totals.courses,    color: "text-rose-400"   },
          { label: "Professors",  value: totals.profs,      color: "text-snow"       },
        ].map((s, i) => (
          <div key={i} className="bg-card border border-edge rounded-2xl p-4 text-center animate-slide-up" style={{ animationDelay: `${i * 40}ms` }}>
            <p className="text-soft text-xs uppercase tracking-widest mb-1">{s.label}</p>
            <p className={`text-2xl font-bold font-mono ${s.color}`}>{s.value}</p>
          </div>
        ))}
      </div>

      {/* FIX: Bar chart — was using <rect> (plain HTML) which Recharts ignores,
          causing all bars to render black. Replace with Recharts <Cell> which
          correctly injects fill per-bar inside the SVG layer. */}
      <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "80ms" }}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-snow font-semibold text-sm">Avg Attendance % per Course</h2>
            <p className="text-soft text-xs mt-0.5">Across all professors — unique students attended / enrolled</p>
          </div>
          <BarChart3 size={16} className="text-dim" />
        </div>
        {allCourses.length === 0 ? (
          <Empty icon={BarChart3} title="No course data" sub="Start sessions to see data." />
        ) : (
          <ResponsiveContainer width="100%" height={220}>
            <BarChart data={allCourses} barSize={24}>
              <XAxis dataKey="name" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} domain={[0, 100]}
                tickFormatter={v => `${v}%`} />
              <Tooltip content={<DarkTooltip />} cursor={{ fill: "rgba(255,255,255,0.03)" }} />
              <Bar dataKey="avg_pct" name="Avg Attendance %" radius={[6,6,0,0]}>
                {allCourses.map((_, i) => (
                  <Cell key={`cell-${i}`} fill={BAR_COLORS[i % BAR_COLORS.length]} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Per-professor breakdown */}
      {profs.map((prof, pi) => (
        <div key={prof.prof_id}
          className="bg-card border border-edge rounded-2xl overflow-hidden animate-slide-up"
          style={{ animationDelay: `${160 + pi * 60}ms` }}
        >
          <div className="px-5 py-4 border-b border-edge flex items-center gap-3">
            <div className="w-8 h-8 rounded-xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center">
              <GraduationCap size={14} className="text-violet-400" />
            </div>
            <div>
              <p className="text-snow font-semibold text-sm">{prof.prof_name}</p>
              <p className="text-soft text-xs font-mono">{prof.courses.length} course{prof.courses.length !== 1 ? "s" : ""}</p>
            </div>
          </div>
          {prof.courses.length === 0 ? (
            <p className="text-soft text-sm text-center py-6">No courses assigned.</p>
          ) : (
            <div className="divide-y divide-edge">
              {prof.courses.map((c, ci) => (
                <div key={c.course_id} className="flex items-center gap-4 px-5 py-4 hover:bg-white/2 transition-colors">
                  <div className={`w-2 h-8 rounded-full shrink-0 ${COURSE_COLORS[ci % COURSE_COLORS.length]}`} />
                  <div className="flex-1 min-w-0">
                    <p className="text-snow text-sm font-medium">{c.name}</p>
                    <p className="text-soft text-xs mt-0.5">
                      {c.sessions} sessions · {c.enrolled} enrolled
                    </p>
                  </div>
                  <div className="w-28 shrink-0">
                    <ProgressBar value={c.avg_pct} max={100} />
                  </div>
                  <AttendancePct value={c.avg_pct} />
                </div>
              ))}
            </div>
          )}
        </div>
      ))}
    </div>
  );
}

// ── Professor Analytics view ──────────────────────────────────────────────────

function ProfAnalyticsView({ user }) {
  const [loading,          setLoading]          = useState(true);
  const [courses,          setCourses]          = useState([]);
  const [profData,         setProfData]         = useState([]);
  const [courseSessionMap, setCourseSessionMap] = useState({});
  const [enrolledMap,      setEnrolledMap]      = useState({});

  useEffect(() => {
    async function load() {
      try {
        const cRes = await getCourses(user.user_id);
        setCourses(cRes.data);
        const pRes = await getProfAnalytics(user.user_id);
        setProfData(pRes.data);
        const sessMap = {}, enrollMap = {};
        await Promise.all(cRes.data.map(async c => {
          try {
            const r = await getCourseAnalytics(c.id);
            sessMap[c.id]   = r.data.sessions;
            enrollMap[c.id] = r.data.enrolled ?? 0;
          } catch {
            sessMap[c.id]   = [];
            enrollMap[c.id] = 0;
          }
        }));
        setCourseSessionMap(sessMap);
        setEnrolledMap(enrollMap);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [user]);

  if (loading) return <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>;

  const barData = courses.map(c => {
    const sessions = courseSessionMap[c.id] || [];
    return {
      name:   c.name.length > 10 ? c.name.slice(0, 10) + "…" : c.name,
      unique: Math.round(sessions.reduce((s, x) => s + x.unique_students, 0) / (sessions.length || 1)),
      total:  Math.round(sessions.reduce((s, x) => s + x.total_marks,     0) / (sessions.length || 1)),
    };
  });

  const allSessions = Object.values(courseSessionMap).flat();
  const trendData   = buildTrendData(allSessions);

  return (
    <div className="space-y-8">
      {/* Summary stats */}
      {profData.length > 0 && (
        <div className="grid grid-cols-3 gap-4">
          <StatCard
            label="Total Sessions"
            value={profData.reduce((s, c) => s + c.sessions, 0)}
            icon={Activity} color="azure" delay={0}
          />
          <StatCard
            label="Total Attendance Marks"
            value={profData.reduce((s, c) => s + c.attendance, 0)}
            icon={Users} color="jade" delay={80}
          />
          <StatCard
            label="Avg Marks / Session"
            value={(
              profData.reduce((s, c) => s + c.attendance, 0) /
              (profData.reduce((s, c) => s + c.sessions, 0) || 1)
            ).toFixed(1)}
            icon={TrendingUp} color="amber" delay={160}
          />
        </div>
      )}

      {/* Charts row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Bar chart */}
        <div className="lg:col-span-2 bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "100ms" }}>
          <div className="flex items-center justify-between mb-6">
            <div>
              <h2 className="text-snow font-semibold text-sm">Avg Attendance per Session</h2>
              <p className="text-soft text-xs mt-0.5">Unique students &amp; total marks, averaged per session</p>
            </div>
            <BarChart3 size={16} className="text-dim" />
          </div>
          {barData.length === 0 ? (
            <Empty icon={BarChart3} title="No data yet" sub="Start sessions to see data here." />
          ) : (
            <ResponsiveContainer width="100%" height={200}>
              <BarChart data={barData} barSize={20} barGap={6}>
                <XAxis dataKey="name" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
                <Tooltip content={<DarkTooltip />} cursor={{ fill: "rgba(255,255,255,0.03)" }} />
                <Bar dataKey="unique" name="Avg Unique Students" fill="#3B82F6" radius={[6,6,0,0]} />
                <Bar dataKey="total"  name="Avg Total Marks"     fill="#8B5CF6" radius={[6,6,0,0]} fillOpacity={0.6} />
              </BarChart>
            </ResponsiveContainer>
          )}
        </div>

        {/* At-risk card */}
        <AtRiskCard profId={user.user_id} />
      </div>

      {/* Trend line */}
      <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "260ms" }}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h2 className="text-snow font-semibold text-sm">Weekly Trend</h2>
            <p className="text-soft text-xs mt-0.5">
              Avg unique students per session by day of week (IST) — computed from historical sessions
            </p>
          </div>
          <TrendingUp size={16} className="text-dim" />
        </div>
        {trendData.length === 0 ? (
          <Empty icon={TrendingUp} title="Not enough data" sub="Run sessions on multiple days to see the trend." />
        ) : (
          <ResponsiveContainer width="100%" height={160}>
            <LineChart data={trendData}>
              <XAxis dataKey="day" tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
              <YAxis tick={{ fill: "#6B7499", fontSize: 11 }} axisLine={false} tickLine={false} />
              <Tooltip content={<DarkTooltip />} />
              <Line
                type="monotone" dataKey="pct" name="Avg Unique Students"
                stroke="#3B82F6" strokeWidth={2.5}
                dot={{ fill: "#3B82F6", r: 4, strokeWidth: 0 }}
                activeDot={{ r: 6 }}
              />
            </LineChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Per-course breakdown table */}
      {profData.length > 0 && (
        <div className="bg-card border border-edge rounded-2xl overflow-hidden animate-slide-up" style={{ animationDelay: "340ms" }}>
          <div className="px-5 py-4 border-b border-edge">
            <h2 className="text-snow font-semibold text-sm">Course Breakdown</h2>
          </div>
          <div className="divide-y divide-edge">
            {profData.map((c, i) => {
              const avgPct = c.sessions > 0
                ? Math.min(100, (c.avg / (enrolledMap[c.course_id] || Math.max(c.avg, 1))) * 100)
                : 0;
              return (
                <div key={c.course_id} className="flex items-center gap-4 px-5 py-4 hover:bg-white/2 transition-colors">
                  <div className={`w-2 h-8 rounded-full ${COURSE_COLORS[i % COURSE_COLORS.length]}`} />
                  <div className="flex-1 min-w-0">
                    <p className="text-snow text-sm font-medium">{c.course_name}</p>
                    <p className="text-soft text-xs mt-0.5">
                      {c.sessions} sessions · {c.attendance} total marks · avg {c.avg} / session
                    </p>
                  </div>
                  <div className="w-32">
                    <ProgressBar value={avgPct} max={100} />
                  </div>
                  <AttendancePct value={avgPct} />
                </div>
              );
            })}
          </div>
        </div>
      )}
    </div>
  );
}

// ── Root component ────────────────────────────────────────────────────────────

export default function Analytics() {
  const { user } = useAuth();

  return (
    <div className="space-y-8">
      <div className="animate-slide-up">
        <h1 className="text-snow text-2xl font-bold tracking-tight">Analytics</h1>
        <p className="text-soft text-sm mt-1">
          {user.role === "admin"
            ? "System-wide attendance breakdown across all professors and courses"
            : "Attendance insights across your courses"}
        </p>
      </div>

      {user.role === "admin"
        ? <AdminAnalyticsView />
        : <ProfAnalyticsView user={user} />
      }
    </div>
  );
}