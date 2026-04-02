// import { useState, useEffect } from "react";
// import { getCourses, getAttendance, getActiveSession } from "../api/client";
// import { useAuth } from "../context/AuthContext";
// import { Search, Users, CheckCircle2, Clock } from "lucide-react";
// import { Badge, Spinner, Empty } from "../components/UI";

// // Parse UTC ISO string correctly (backend omits 'Z')
// function formatIST(utcStr) {
//   if (!utcStr) return "—";
//   const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
//   return new Date(s).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });
// }

// function formatISTTime(utcStr) {
//   if (!utcStr) return "—";
//   const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
//   return new Date(s).toLocaleTimeString("en-IN", {
//     timeZone: "Asia/Kolkata",
//     hour: "2-digit", minute: "2-digit",
//   });
// }

// export default function Students() {
//   const { user }     = useAuth();
//   const [courses,    setCourses]    = useState([]);
//   const [selected,   setSelected]   = useState(null);
//   const [records,    setRecords]    = useState([]);
//   const [query,      setQuery]      = useState("");
//   const [loading,    setLoading]    = useState(true);
//   const [loadingRec, setLoadingRec] = useState(false);
//   const [hasSession, setHasSession] = useState(false);

//   useEffect(() => {
//     async function load() {
//       try {
//         const res = await getCourses(user.user_id);
//         setCourses(res.data);
//         if (res.data.length > 0) setSelected(res.data[0]);
//       } finally {
//         setLoading(false);
//       }
//     }
//     load();
//   }, [user.user_id]);

//   useEffect(() => {
//     if (!selected) return;
//     async function loadRecords() {
//       setLoadingRec(true);
//       try {
//         const sess = await getActiveSession(selected.id).catch(() => null);
//         if (sess?.data?.session_id) {
//           setHasSession(true);
//           const att = await getAttendance(sess.data.session_id);
//           setRecords(att.data);
//         } else {
//           setHasSession(false);
//           setRecords([]);
//         }
//       } finally {
//         setLoadingRec(false);
//       }
//     }
//     loadRecords();
//     const t = setInterval(loadRecords, 5000);
//     return () => clearInterval(t);
//   }, [selected]);

//   const filtered = records.filter(r =>
//     String(r.student_id).includes(query)
//   );
//   const unique = new Set(records.map(r => r.student_id)).size;
//   const lastRecord = records.length > 0 ? records[records.length - 1] : null;

//   if (loading) return (
//     <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>
//   );

//   return (
//     <div className="space-y-6">
//       <div className="animate-slide-up">
//         <h1 className="text-snow text-2xl font-bold tracking-tight">Students</h1>
//         <p className="text-soft text-sm mt-1">Live attendance records for active sessions</p>
//       </div>

//       {/* Course selector */}
//       <div className="flex flex-wrap gap-2 animate-slide-up" style={{ animationDelay: "80ms" }}>
//         {courses.map(c => (
//           <button
//             key={c.id}
//             onClick={() => { setSelected(c); setQuery(""); }}
//             className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border
//               ${selected?.id === c.id
//                 ? "bg-azure-500/15 text-azure-400 border-azure-500/30"
//                 : "bg-card text-soft border-edge hover:text-snow hover:border-dim"}`}
//           >
//             {c.name}
//           </button>
//         ))}
//       </div>

//       {selected && (
//         <div className="animate-slide-up" style={{ animationDelay: "160ms" }}>

//           {/* No active session notice */}
//           {!hasSession && (
//             <div className="mb-4 flex items-center gap-2 bg-amber-500/10 border border-amber-500/20 rounded-xl px-4 py-3">
//               <Clock size={14} className="text-amber-400 shrink-0" />
//               <p className="text-amber-300 text-sm">No active session for {selected.name}. Start a session to track attendance.</p>
//             </div>
//           )}

//           {/* Stats row */}
//           <div className="grid grid-cols-3 gap-4 mb-6">
//             <div className="bg-card border border-edge rounded-2xl p-4 text-center">
//               <p className="text-soft text-xs uppercase tracking-widest mb-1">Checked In</p>
//               <p className="text-snow text-3xl font-bold">{unique}</p>
//             </div>
//             <div className="bg-card border border-edge rounded-2xl p-4 text-center">
//               <p className="text-soft text-xs uppercase tracking-widest mb-1">Total Marks</p>
//               <p className="text-snow text-3xl font-bold">{records.length}</p>
//             </div>
//             <div className="bg-card border border-edge rounded-2xl p-4 text-center">
//               <p className="text-soft text-xs uppercase tracking-widest mb-1">Last Mark (IST)</p>
//               <p className="text-snow text-lg font-bold font-mono">
//                 {lastRecord ? formatISTTime(lastRecord.timestamp) : "—"}
//               </p>
//             </div>
//           </div>

//           {/* Search */}
//           <div className="relative mb-4 max-w-sm">
//             <Search size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-dim" />
//             <input
//               value={query}
//               onChange={e => setQuery(e.target.value)}
//               placeholder="Search by student ID..."
//               className="w-full bg-card border border-edge rounded-xl text-sm text-snow placeholder:text-dim
//                 focus:outline-none focus:border-azure-500 transition-all pl-10 pr-4 py-2.5"
//             />
//           </div>

//           {/* Table */}
//           <div className="bg-card border border-edge rounded-2xl overflow-hidden">
//             <div className="px-5 py-4 border-b border-edge flex items-center justify-between">
//               <h2 className="text-snow font-semibold text-sm">Attendance Log</h2>
//               <div className="flex items-center gap-2">
//                 {loadingRec && <Spinner size={14} />}
//                 <span className="text-soft text-xs font-mono">{filtered.length} records</span>
//               </div>
//             </div>

//             {filtered.length === 0 ? (
//               <Empty icon={Users} title="No records yet"
//                 sub={records.length === 0 ? "Start a session to begin tracking." : "No matching students."} />
//             ) : (
//               <div className="divide-y divide-edge">
//                 {filtered.map((r, i) => (
//                   <div key={i}
//                     className="flex items-center gap-4 px-5 py-3.5 hover:bg-white/2 transition-colors animate-slide-up"
//                     style={{ animationDelay: `${i * 30}ms` }}
//                   >
//                     <div className="w-9 h-9 rounded-xl bg-azure-500/15 flex items-center justify-center shrink-0">
//                       <span className="text-azure-400 text-sm font-bold">
//                         {String(r.student_id).slice(-2)}
//                       </span>
//                     </div>
//                     <div className="flex-1 min-w-0">
//                       <p className="text-snow text-sm font-medium">Student #{r.student_id}</p>
//                       <div className="flex items-center gap-2 mt-0.5">
//                         <Clock size={11} className="text-dim" />
//                         <span className="text-soft text-xs font-mono">{formatIST(r.timestamp)}</span>
//                       </div>
//                     </div>
//                     <CheckCircle2 size={16} className="text-jade-400 shrink-0" />
//                   </div>
//                 ))}
//               </div>
//             )}
//           </div>
//         </div>
//       )}
//     </div>
//   );
// }

import { useState, useEffect, useCallback } from "react";
import { getCourses, getAttendance, getActiveSession, getCourseStudents } from "../api/client";
import { useAuth } from "../context/AuthContext";
import { Search, Users, CheckCircle2, Clock, X, CalendarDays, TrendingUp } from "lucide-react";
import { Badge, Spinner, Empty } from "../components/UI";
import API from "../api/client";

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatIST(utcStr) {
  if (!utcStr) return "—";
  const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
  return new Date(s).toLocaleString("en-IN", { timeZone: "Asia/Kolkata" });
}

function formatISTTime(utcStr) {
  if (!utcStr) return "—";
  const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
  return new Date(s).toLocaleTimeString("en-IN", {
    timeZone: "Asia/Kolkata",
    hour: "2-digit", minute: "2-digit",
  });
}

function toISTDate(utcStr) {
  if (!utcStr) return null;
  const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
  return new Date(new Date(s).toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
}

// ── Heatmap helpers ───────────────────────────────────────────────────────────

/**
 * Groups sessions by IST calendar date string ("YYYY-MM-DD").
 * Returns a map: dateStr → { total: N, attended: N }
 */
function buildDayMap(sessions) {
  const map = {};
  for (const s of sessions) {
    const d = toISTDate(s.start_time);
    if (!d) continue;
    const key = `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, "0")}-${String(d.getDate()).padStart(2, "0")}`;
    if (!map[key]) map[key] = { total: 0, attended: 0 };
    map[key].total += 1;
    if (s.attended) map[key].attended += 1;
  }
  return map;
}

/**
 * Given a map of dateStr → {total, attended}, build a list of weeks
 * (arrays of 7 days Sun–Sat) spanning from the first session date to today.
 */
function buildCalendarWeeks(dayMap) {
  if (Object.keys(dayMap).length === 0) return [];

  const dateKeys = Object.keys(dayMap).sort();
  const firstDate = new Date(dateKeys[0] + "T00:00:00");
  const today = new Date(new Date().toLocaleString("en-US", { timeZone: "Asia/Kolkata" }));
  today.setHours(0, 0, 0, 0);

  // Go back to Sunday of first week
  const start = new Date(firstDate);
  start.setDate(start.getDate() - start.getDay());

  // Go forward to Saturday of last week
  const end = new Date(today);
  end.setDate(end.getDate() + (6 - end.getDay()));

  const weeks = [];
  let week = [];
  const cur = new Date(start);

  while (cur <= end) {
    const key = `${cur.getFullYear()}-${String(cur.getMonth() + 1).padStart(2, "0")}-${String(cur.getDate()).padStart(2, "0")}`;
    week.push({
      key,
      day: cur.getDate(),
      month: cur.getMonth(),
      isToday: key === dateKeys[dateKeys.length - 1] || cur.toDateString() === today.toDateString(),
      data: dayMap[key] || null,
    });
    if (cur.getDay() === 6) {
      weeks.push(week);
      week = [];
    }
    cur.setDate(cur.getDate() + 1);
  }
  if (week.length > 0) weeks.push(week);
  return weeks;
}

function cellColor(data) {
  if (!data || data.total === 0) return "bg-edge";
  const ratio = data.attended / data.total;
  if (ratio === 0)   return "bg-edge";
  if (ratio < 0.5)   return "bg-jade-500/30";   // some sessions: light green
  if (ratio < 1)     return "bg-jade-500/60";   // most sessions: medium green
  return "bg-jade-500";                          // all sessions: full dark green
}

function cellTitle(data, key) {
  if (!data) return key + " — no session";
  return `${key}: attended ${data.attended}/${data.total} session${data.total !== 1 ? "s" : ""}`;
}

// ── Student History Modal ─────────────────────────────────────────────────────

function HistoryModal({ student, course, onClose }) {
  const [loading, setLoading] = useState(true);
  const [history, setHistory] = useState(null);
  const [error,   setError]   = useState(null);
  const [tooltip, setTooltip] = useState(null);

  useEffect(() => {
    async function load() {
      try {
        const res = await API.get(`/student/${student.id}/history/${course.id}`);
        setHistory(res.data);
      } catch {
        setError("Could not load attendance history.");
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [student.id, course.id]);

  const dayMap = history ? buildDayMap(history.sessions) : {};
  const weeks  = buildCalendarWeeks(dayMap);
  const pct    = history && history.total > 0
    ? Math.round((history.attended / history.total) * 100)
    : 0;

  const MONTH_ABBR = ["Jan","Feb","Mar","Apr","May","Jun","Jul","Aug","Sep","Oct","Nov","Dec"];
  const DAY_LABELS = ["S","M","T","W","T","F","S"];

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
      style={{ background: "rgba(0,0,0,0.7)", backdropFilter: "blur(4px)" }}
      onClick={e => { if (e.target === e.currentTarget) onClose(); }}
    >
      <div className="bg-card border border-edge rounded-2xl w-full max-w-2xl shadow-float animate-slide-up overflow-hidden">
        {/* Header */}
        <div className="flex items-center justify-between px-6 py-4 border-b border-edge">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-jade-500/15 flex items-center justify-center border border-jade-500/20">
              <CalendarDays size={16} className="text-jade-400" />
            </div>
            <div>
              <p className="text-snow font-semibold text-sm">{student.name}</p>
              <p className="text-soft text-xs font-mono">{course.name} · Attendance History</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="w-8 h-8 rounded-xl bg-edge flex items-center justify-center text-soft hover:text-snow transition-colors"
          >
            <X size={15} />
          </button>
        </div>

        <div className="px-6 py-5 space-y-5">
          {loading ? (
            <div className="flex items-center justify-center h-40"><Spinner size={24} /></div>
          ) : error ? (
            <p className="text-rose-400 text-sm text-center py-10">{error}</p>
          ) : (
            <>
              {/* Summary stats */}
              <div className="grid grid-cols-3 gap-3">
                {[
                  { label: "Sessions",  value: history.total },
                  { label: "Attended",  value: history.attended },
                  {
                    label: "Attendance",
                    value: <span className={pct >= 75 ? "text-jade-400" : pct >= 25 ? "text-amber-400" : "text-rose-400"}>
                      {pct}%
                    </span>
                  },
                ].map((s, i) => (
                  <div key={i} className="bg-ink border border-edge rounded-xl p-3 text-center">
                    <p className="text-soft text-xs uppercase tracking-widest mb-1">{s.label}</p>
                    <p className="text-snow text-2xl font-bold font-mono">{s.value}</p>
                  </div>
                ))}
              </div>

              {/* Heatmap */}
              {weeks.length === 0 ? (
                <Empty icon={CalendarDays} title="No sessions yet" sub="Sessions will appear here once created." />
              ) : (
                <div className="space-y-3 relative">
                  <h3 className="text-snow text-sm font-semibold">Daily Heatmap</h3>

                  {/* Day-of-week labels */}
                  <div className="flex gap-1 pl-0">
                    {DAY_LABELS.map((d, i) => (
                      <div key={i} className="w-7 text-center text-dim text-xs">{d}</div>
                    ))}
                  </div>

                  {/* Weeks — rows of 7 cells */}
                  <div className="space-y-1">
                    {weeks.map((week, wi) => {
                      // Find first day of month in this week for a label
                      const monthLabel = week.find(d => d.day === 1 || wi === 0);
                      return (
                        <div key={wi} className="flex items-center gap-1">
                          {week.map((day, di) => (
                            <div
                              key={di}
                              className={`w-7 h-7 rounded-md cursor-default transition-all relative
                                ${cellColor(day.data)}
                                ${day.isToday ? "ring-1 ring-azure-400" : ""}
                              `}
                              title={cellTitle(day.data, day.key)}
                              onMouseEnter={() => setTooltip({ ...day })}
                              onMouseLeave={() => setTooltip(null)}
                            />
                          ))}
                        </div>
                      );
                    })}
                  </div>

                  {/* Legend */}
                  <div className="flex items-center gap-4 pt-1">
                    <span className="text-dim text-xs">Less</span>
                    {[
                      { color: "bg-edge",        label: "No session" },
                      { color: "bg-jade-500/30",  label: "Some" },
                      { color: "bg-jade-500/60",  label: "Most" },
                      { color: "bg-jade-500",     label: "All" },
                    ].map((l, i) => (
                      <div key={i} className="flex items-center gap-1.5">
                        <div className={`w-4 h-4 rounded-sm ${l.color}`} />
                        <span className="text-dim text-xs">{l.label}</span>
                      </div>
                    ))}
                    <span className="text-dim text-xs">More</span>
                  </div>

                  {/* Floating tooltip */}
                  {tooltip && tooltip.data && (
                    <div className="absolute bottom-0 right-0 bg-ink border border-edge rounded-xl px-3 py-2 text-xs shadow-float pointer-events-none z-10">
                      <p className="text-snow font-medium">{tooltip.key}</p>
                      <p className="text-soft mt-0.5">
                        Attended <span className="text-jade-400 font-mono">{tooltip.data.attended}</span> of{" "}
                        <span className="text-snow font-mono">{tooltip.data.total}</span> session{tooltip.data.total !== 1 ? "s" : ""}
                      </p>
                    </div>
                  )}
                </div>
              )}
            </>
          )}
        </div>
      </div>
    </div>
  );
}

// ── Main Students component ───────────────────────────────────────────────────

export default function Students() {
  const { user }     = useAuth();
  const [courses,    setCourses]    = useState([]);
  const [selected,   setSelected]   = useState(null);
  const [records,    setRecords]    = useState([]);
  const [roster,     setRoster]     = useState([]);
  const [query,      setQuery]      = useState("");
  const [loading,    setLoading]    = useState(true);
  const [loadingRec, setLoadingRec] = useState(false);
  const [hasSession, setHasSession] = useState(false);
  const [activeTab,  setActiveTab]  = useState("live");   // "live" | "roster"
  const [viewStudent, setViewStudent] = useState(null);   // student whose heatmap to show

  useEffect(() => {
    async function load() {
      try {
        const res = await getCourses(user.user_id);
        setCourses(res.data);
        if (res.data.length > 0) setSelected(res.data[0]);
      } finally {
        setLoading(false);
      }
    }
    load();
  }, [user.user_id]);

  // Load live session records + roster when course changes
  useEffect(() => {
    if (!selected) return;

    async function loadRecords() {
      setLoadingRec(true);
      try {
        const sess = await getActiveSession(selected.id).catch(() => null);
        if (sess?.data?.session_id) {
          setHasSession(true);
          const att = await getAttendance(sess.data.session_id);
          setRecords(att.data);
        } else {
          setHasSession(false);
          setRecords([]);
        }
      } finally {
        setLoadingRec(false);
      }
    }

    async function loadRoster() {
      try {
        const res = await getCourseStudents(selected.id);
        const unique = Array.from(new Map(res.data.map(s => [s.id, s])).values());
        setRoster(unique);
      } catch {
        setRoster([]);
      }
    }

    loadRecords();
    loadRoster();
    const t = setInterval(loadRecords, 5000);
    return () => clearInterval(t);
  }, [selected]);

  const filtered = records.filter(r => String(r.student_id).includes(query));
  const filteredRoster = roster.filter(s =>
    s.name.toLowerCase().includes(query.toLowerCase()) ||
    String(s.id).includes(query)
  );
  const unique = new Set(records.map(r => r.student_id)).size;
  const lastRecord = records.length > 0 ? records[records.length - 1] : null;

  if (loading) return (
    <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>
  );

  return (
    <div className="space-y-6">
      <div className="animate-slide-up">
        <h1 className="text-snow text-2xl font-bold tracking-tight">Students</h1>
        <p className="text-soft text-sm mt-1">Live attendance and historical records per course</p>
      </div>

      {/* Course selector */}
      <div className="flex flex-wrap gap-2 animate-slide-up" style={{ animationDelay: "80ms" }}>
        {courses.map(c => (
          <button
            key={c.id}
            onClick={() => { setSelected(c); setQuery(""); setActiveTab("live"); }}
            className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border
              ${selected?.id === c.id
                ? "bg-azure-500/15 text-azure-400 border-azure-500/30"
                : "bg-card text-soft border-edge hover:text-snow hover:border-dim"}`}
          >
            {c.name}
          </button>
        ))}
      </div>

      {selected && (
        <div className="animate-slide-up" style={{ animationDelay: "160ms" }}>

          {/* No active session notice */}
          {!hasSession && activeTab === "live" && (
            <div className="mb-4 flex items-center gap-2 bg-amber-500/10 border border-amber-500/20 rounded-xl px-4 py-3">
              <Clock size={14} className="text-amber-400 shrink-0" />
              <p className="text-amber-300 text-sm">
                No active session for {selected.name}. Start a session to track live attendance.
              </p>
            </div>
          )}

          {/* Stats row */}
          <div className="grid grid-cols-3 gap-4 mb-5">
            <div className="bg-card border border-edge rounded-2xl p-4 text-center">
              <p className="text-soft text-xs uppercase tracking-widest mb-1">Checked In</p>
              <p className="text-snow text-3xl font-bold">{unique}</p>
            </div>
            <div className="bg-card border border-edge rounded-2xl p-4 text-center">
              <p className="text-soft text-xs uppercase tracking-widest mb-1">Total Marks</p>
              <p className="text-snow text-3xl font-bold">{records.length}</p>
            </div>
            <div className="bg-card border border-edge rounded-2xl p-4 text-center">
              <p className="text-soft text-xs uppercase tracking-widest mb-1">Last Mark (IST)</p>
              <p className="text-snow text-lg font-bold font-mono">
                {lastRecord ? formatISTTime(lastRecord.timestamp) : "—"}
              </p>
            </div>
          </div>

          {/* Tab switcher */}
          <div className="flex gap-2 mb-4">
            {[
              { id: "live",   label: "Live Session" },
              { id: "roster", label: `Roster (${roster.length})` },
            ].map(tab => (
              <button
                key={tab.id}
                onClick={() => { setActiveTab(tab.id); setQuery(""); }}
                className={`px-4 py-2 rounded-xl text-sm font-medium transition-all border
                  ${activeTab === tab.id
                    ? "bg-violet-500/15 text-violet-400 border-violet-500/30"
                    : "bg-card text-soft border-edge hover:text-snow hover:border-dim"}`}
              >
                {tab.label}
              </button>
            ))}
          </div>

          {/* Search */}
          <div className="relative mb-4 max-w-sm">
            <Search size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-dim" />
            <input
              value={query}
              onChange={e => setQuery(e.target.value)}
              placeholder={activeTab === "live" ? "Search by student ID..." : "Search by name or ID..."}
              className="w-full bg-card border border-edge rounded-xl text-sm text-snow placeholder:text-dim
                focus:outline-none focus:border-azure-500 transition-all pl-10 pr-4 py-2.5"
            />
          </div>

          {/* ── Live tab ─────────────────────────────────────────────────────── */}
          {activeTab === "live" && (
            <div className="bg-card border border-edge rounded-2xl overflow-hidden">
              <div className="px-5 py-4 border-b border-edge flex items-center justify-between">
                <h2 className="text-snow font-semibold text-sm">Attendance Log</h2>
                <div className="flex items-center gap-2">
                  {loadingRec && <Spinner size={14} />}
                  <span className="text-soft text-xs font-mono">{filtered.length} records</span>
                </div>
              </div>

              {filtered.length === 0 ? (
                <Empty icon={Users} title="No records yet"
                  sub={records.length === 0 ? "Start a session to begin tracking." : "No matching students."} />
              ) : (
                <div className="divide-y divide-edge">
                  {filtered.map((r, i) => (
                    <div key={i}
                      className="flex items-center gap-4 px-5 py-3.5 hover:bg-white/2 transition-colors animate-slide-up"
                      style={{ animationDelay: `${i * 30}ms` }}
                    >
                      <div className="w-9 h-9 rounded-xl bg-azure-500/15 flex items-center justify-center shrink-0">
                        <span className="text-azure-400 text-sm font-bold">
                          {String(r.student_id).slice(-2)}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-snow text-sm font-medium">Student #{r.student_id}</p>
                        <div className="flex items-center gap-2 mt-0.5">
                          <Clock size={11} className="text-dim" />
                          <span className="text-soft text-xs font-mono">{formatIST(r.timestamp)}</span>
                        </div>
                      </div>
                      <CheckCircle2 size={16} className="text-jade-400 shrink-0" />
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}

          {/* ── Roster tab ───────────────────────────────────────────────────── */}
          {activeTab === "roster" && (
            <div className="bg-card border border-edge rounded-2xl overflow-hidden">
              <div className="px-5 py-4 border-b border-edge flex items-center justify-between">
                <h2 className="text-snow font-semibold text-sm">All Enrolled Students</h2>
                <span className="text-soft text-xs font-mono">{filteredRoster.length} students</span>
              </div>

              {filteredRoster.length === 0 ? (
                <Empty icon={Users} title="No students found" sub="Try a different search." />
              ) : (
                <div className="divide-y divide-edge">
                  {filteredRoster.map((s, i) => (
                    <button
                      key={s.id}
                      onClick={() => setViewStudent(s)}
                      className="w-full flex items-center gap-4 px-5 py-3.5 hover:bg-white/2 transition-colors text-left group"
                    >
                      <div className="w-9 h-9 rounded-xl bg-violet-500/15 flex items-center justify-center shrink-0 border border-violet-500/20 group-hover:border-violet-500/40 transition-colors">
                        <span className="text-violet-400 text-sm font-bold">
                          {s.name.charAt(0).toUpperCase()}
                        </span>
                      </div>
                      <div className="flex-1 min-w-0">
                        <p className="text-snow text-sm font-medium">{s.name}</p>
                        <p className="text-soft text-xs font-mono mt-0.5">ID: {s.id}</p>
                      </div>
                      <div className="flex items-center gap-2 shrink-0">
                        <span className="text-dim text-xs group-hover:text-violet-400 transition-colors">View history</span>
                        <CalendarDays size={14} className="text-dim group-hover:text-violet-400 transition-colors" />
                      </div>
                    </button>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>
      )}

      {/* History heatmap modal */}
      {viewStudent && selected && (
        <HistoryModal
          student={viewStudent}
          course={selected}
          onClose={() => setViewStudent(null)}
        />
      )}
    </div>
  );
}