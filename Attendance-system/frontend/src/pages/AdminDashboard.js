// import { useState, useEffect } from "react";
// import { getAdminStats } from "../api/client";
// import { Shield, Database, Activity, Users, AlertTriangle, CheckCircle2, Server } from "lucide-react";
// import { Badge, Spinner } from "../components/UI";

// export default function AdminDashboard() {
//   const [stats,   setStats]   = useState(null);
//   const [loading, setLoading] = useState(true);
//   const [error,   setError]   = useState(null);

//   useEffect(() => {
//     async function load() {
//       try {
//         const res = await getAdminStats();
//         setStats(res.data);
//         setError(null);
//       } catch {
//         setError("Could not load stats — check backend connection.");
//       } finally {
//         setLoading(false);
//       }
//     }
//     load();
//     const t = setInterval(load, 10_000);
//     return () => clearInterval(t);
//   }, []);

//   if (loading) return (
//     <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>
//   );

//   const services = [
//     { name: "Flask API",         status: !error, detail: "http://localhost:4040"          },
//     { name: "PostgreSQL DB",     status: !error, detail: "db:5432 / attendance"           },
//     { name: "BLE Beacon Server", status: true,   detail: "http://localhost:4040/getMinor" },
//     { name: "JWT Auth",          status: true,   detail: "HS256 / dev-secret"             },
//   ];

//   const actions = [
//     { label: "Seed Database",     icon: Database, note: "Run seed.py to populate"       },
//     { label: "View All Sessions", icon: Activity, note: "GET /admin/stats"               },
//     { label: "Manage Users",      icon: Users,    note: "Add via DB / API"               },
//     { label: "Server Logs",       icon: Server,   note: "docker logs attendance-backend" },
//   ];

//   return (
//     <div className="space-y-8">
//       {/* Header */}
//       <div className="animate-slide-up flex items-center gap-3">
//         <div className="w-10 h-10 rounded-xl bg-rose-500/15 flex items-center justify-center border border-rose-500/20">
//           <Shield size={18} className="text-rose-400" />
//         </div>
//         <div>
//           <h1 className="text-snow text-2xl font-bold tracking-tight">Admin Panel</h1>
//           <p className="text-soft text-sm mt-0.5">System management and service health</p>
//         </div>
//       </div>

//       {error && (
//         <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-2xl px-5 py-4 animate-slide-up">
//           <AlertTriangle size={16} className="text-amber-400 shrink-0 mt-0.5" />
//           <p className="text-amber-300 text-sm">{error}</p>
//         </div>
//       )}

//       {/* Quick stat strip */}
//       {stats && (
//         <div className="grid grid-cols-3 gap-4 animate-slide-up" style={{ animationDelay: "80ms" }}>
//           {[
//             { label: "Sessions",        value: stats.sessions,                        color: "text-azure-400" },
//             { label: "Attendance Marks", value: stats.attendance,                     color: "text-jade-400"  },
//             { label: "Avg / Session",   value: stats.avg_attendance?.toFixed(1) ?? 0, color: "text-violet-400" },
//           ].map((s, i) => (
//             <div key={i} className="bg-card border border-edge rounded-2xl p-4 text-center">
//               <p className="text-soft text-xs uppercase tracking-widest mb-1">{s.label}</p>
//               <p className={`text-3xl font-bold font-mono ${s.color}`}>{s.value}</p>
//             </div>
//           ))}
//         </div>
//       )}

//       {/* Service health */}
//       <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "160ms" }}>
//         <h2 className="text-snow font-semibold text-sm mb-4">Service Health</h2>
//         <div className="space-y-3">
//           {services.map((svc, i) => (
//             <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-ink border border-edge">
//               <div className={`w-2 h-2 rounded-full ${svc.status ? "bg-jade-400" : "bg-rose-400"} animate-pulse`} />
//               <div className="flex-1">
//                 <p className="text-snow text-sm font-medium">{svc.name}</p>
//                 <p className="text-dim text-xs font-mono">{svc.detail}</p>
//               </div>
//               <Badge label={svc.status ? "UP" : "DOWN"} variant={svc.status ? "live" : "danger"} />
//             </div>
//           ))}
//         </div>
//       </div>

//       {/* Quick actions */}
//       <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "240ms" }}>
//         <h2 className="text-snow font-semibold text-sm mb-4">Quick Actions</h2>
//         <div className="grid grid-cols-2 gap-3">
//           {actions.map((a, i) => (
//             <div key={i}
//               className="p-4 rounded-xl bg-ink border border-edge hover:border-dim transition-colors cursor-pointer group"
//             >
//               <div className="flex items-start gap-3">
//                 <div className="w-8 h-8 rounded-xl bg-edge flex items-center justify-center shrink-0 group-hover:bg-dim transition-colors">
//                   <a.icon size={14} className="text-soft" />
//                 </div>
//                 <div>
//                   <p className="text-snow text-xs font-medium">{a.label}</p>
//                   <p className="text-dim text-xs mt-0.5 font-mono">{a.note}</p>
//                 </div>
//               </div>
//             </div>
//           ))}
//         </div>
//       </div>

//       {/* Timezone note */}
//       <div className="flex items-center gap-2 px-1 animate-slide-up" style={{ animationDelay: "300ms" }}>
//         <CheckCircle2 size={13} className="text-jade-400 shrink-0" />
//         <p className="text-dim text-xs font-mono">
//           All timestamps stored in UTC · displayed in IST (UTC+5:30) throughout the portal
//         </p>
//       </div>
//     </div>
//   );
// }

import { useState, useEffect, useCallback } from "react";
import { getAdminStats } from "../api/client";
import API from "../api/client";
import {
  Shield, Activity, Users, AlertTriangle, CheckCircle2,
  Server, Plus, Trash2, RefreshCw, X, ChevronDown, ChevronUp,
  Eye, EyeOff, BookOpen, Wifi, QrCode, Layers,
} from "lucide-react";
import { Badge, Spinner } from "../components/UI";

// ── Helpers ───────────────────────────────────────────────────────────────────

function formatIST(utcStr) {
  if (!utcStr) return "—";
  const s = utcStr.endsWith("Z") || utcStr.includes("+") ? utcStr : utcStr + "Z";
  return new Date(s).toLocaleString("en-IN", { timeZone: "Asia/Kolkata",
    day: "2-digit", month: "short", hour: "2-digit", minute: "2-digit" });
}

const MODE_ICONS = { ble: Wifi, qr: QrCode, hybrid: Layers };
const ROLE_COLORS = {
  admin:   "text-rose-400 bg-rose-500/15 border-rose-500/20",
  prof:    "text-azure-400 bg-azure-500/15 border-azure-500/20",
  student: "text-jade-400 bg-jade-500/15 border-jade-500/20",
};

// ── Sub-panels ────────────────────────────────────────────────────────────────

function SessionsPanel() {
  const [sessions, setSessions] = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);

  const load = useCallback(async () => {
    try {
      const res = await API.get("/admin/sessions");
      setSessions(res.data);
      setError(null);
    } catch {
      setError("Could not load sessions.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  if (loading) return <div className="flex justify-center py-10"><Spinner size={20} /></div>;
  if (error)   return <p className="text-rose-400 text-sm text-center py-6">{error}</p>;

  const ModeIcon = ({ mode }) => {
    const Icon = MODE_ICONS[mode] || Activity;
    return <Icon size={12} className="text-dim" />;
  };

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-soft text-xs font-mono">{sessions.length} total sessions</p>
        <button onClick={load} className="text-dim hover:text-snow transition-colors">
          <RefreshCw size={13} />
        </button>
      </div>
      <div className="divide-y divide-edge rounded-xl border border-edge overflow-hidden max-h-96 overflow-y-auto">
        {sessions.length === 0 ? (
          <p className="text-soft text-sm text-center py-8">No sessions yet.</p>
        ) : sessions.map((s, i) => (
          <div key={s.session_id}
            className="flex items-center gap-3 px-4 py-3 bg-ink hover:bg-white/2 transition-colors"
          >
            <div className={`w-1.5 h-1.5 rounded-full shrink-0 ${s.is_active ? "bg-jade-400 animate-pulse" : "bg-edge"}`} />
            <div className="flex-1 min-w-0">
              <div className="flex items-center gap-2 flex-wrap">
                <span className="text-snow text-xs font-medium">{s.course_name}</span>
                <span className="text-dim text-xs">·</span>
                <span className="text-soft text-xs">{s.prof_name}</span>
              </div>
              <div className="flex items-center gap-2 mt-0.5">
                <ModeIcon mode={s.mode} />
                <span className="text-dim text-xs font-mono">{formatIST(s.start_time)}</span>
                {s.is_active && <Badge label="LIVE" variant="live" />}
              </div>
            </div>
            <div className="text-right shrink-0">
              <p className="text-snow text-xs font-mono">{s.unique_students} students</p>
              <p className="text-dim text-xs font-mono">{s.total_marks} marks</p>
            </div>
          </div>
        ))}
      </div>
    </div>
  );
}

function UsersPanel() {
  const [users,    setUsers]    = useState([]);
  const [loading,  setLoading]  = useState(true);
  const [error,    setError]    = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [deleting, setDeleting] = useState(null);
  const [creating, setCreating] = useState(false);
  const [formErr,  setFormErr]  = useState("");
  const [showPwd,  setShowPwd]  = useState(false);
  const [filter,   setFilter]   = useState("all");

  const [form, setForm] = useState({ name: "", email: "", password: "", role: "student" });

  const load = useCallback(async () => {
    try {
      const res = await API.get("/admin/users");
      setUsers(res.data);
      setError(null);
    } catch {
      setError("Could not load users.");
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleCreate = async () => {
    setFormErr("");
    if (!form.name || !form.email || !form.password) {
      setFormErr("All fields are required.");
      return;
    }
    setCreating(true);
    try {
      await API.post("/admin/users", form);
      setForm({ name: "", email: "", password: "", role: "student" });
      setShowForm(false);
      await load();
    } catch (e) {
      setFormErr(e.response?.data?.error || "Could not create user.");
    } finally {
      setCreating(false);
    }
  };

  const handleDelete = async (userId) => {
    setDeleting(userId);
    try {
      await API.delete(`/admin/users/${userId}`);
      setUsers(u => u.filter(x => x.id !== userId));
    } catch {
      setError("Could not delete user.");
    } finally {
      setDeleting(null);
    }
  };

  const filtered = filter === "all" ? users : users.filter(u => u.role === filter);

  if (loading) return <div className="flex justify-center py-10"><Spinner size={20} /></div>;

  return (
    <div className="space-y-4">
      {error && <p className="text-rose-400 text-xs">{error}</p>}

      {/* Toolbar */}
      <div className="flex items-center gap-3 flex-wrap">
        <div className="flex gap-1.5">
          {["all", "student", "prof", "admin"].map(r => (
            <button key={r}
              onClick={() => setFilter(r)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-all border
                ${filter === r
                  ? "bg-azure-500/15 text-azure-400 border-azure-500/30"
                  : "bg-ink text-soft border-edge hover:text-snow"}`}
            >
              {r === "all" ? `All (${users.length})` : `${r.charAt(0).toUpperCase() + r.slice(1)}s (${users.filter(u => u.role === r).length})`}
            </button>
          ))}
        </div>
        <div className="ml-auto">
          <button
            onClick={() => { setShowForm(f => !f); setFormErr(""); }}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-azure-500/15 text-azure-400 border border-azure-500/30 hover:bg-azure-500/25 transition-colors"
          >
            <Plus size={13} /> Add User
          </button>
        </div>
      </div>

      {/* Create form */}
      {showForm && (
        <div className="bg-ink border border-edge rounded-xl p-4 space-y-3 animate-slide-up">
          <p className="text-snow text-xs font-semibold mb-2">New User</p>
          <div className="grid grid-cols-2 gap-3">
            {[
              { key: "name",  placeholder: "Full name",   type: "text"     },
              { key: "email", placeholder: "Email",       type: "email"    },
            ].map(f => (
              <input key={f.key}
                type={f.type}
                placeholder={f.placeholder}
                value={form[f.key]}
                onChange={e => setForm(x => ({ ...x, [f.key]: e.target.value }))}
                className="bg-card border border-edge rounded-xl text-sm text-snow placeholder:text-dim
                  focus:outline-none focus:border-azure-500 transition-all px-3 py-2"
              />
            ))}
          </div>
          <div className="grid grid-cols-2 gap-3">
            <div className="relative">
              <input
                type={showPwd ? "text" : "password"}
                placeholder="Password"
                value={form.password}
                onChange={e => setForm(x => ({ ...x, password: e.target.value }))}
                className="w-full bg-card border border-edge rounded-xl text-sm text-snow placeholder:text-dim
                  focus:outline-none focus:border-azure-500 transition-all pl-3 pr-9 py-2"
              />
              <button type="button" onClick={() => setShowPwd(p => !p)}
                className="absolute right-3 top-1/2 -translate-y-1/2 text-dim hover:text-soft transition-colors">
                {showPwd ? <EyeOff size={13} /> : <Eye size={13} />}
              </button>
            </div>
            <select
              value={form.role}
              onChange={e => setForm(x => ({ ...x, role: e.target.value }))}
              className="bg-card border border-edge rounded-xl text-sm text-snow
                focus:outline-none focus:border-azure-500 transition-all px-3 py-2"
            >
              <option value="student">Student</option>
              <option value="prof">Professor</option>
              <option value="admin">Admin</option>
            </select>
          </div>
          {formErr && <p className="text-rose-400 text-xs">{formErr}</p>}
          <div className="flex gap-2">
            <button
              onClick={handleCreate}
              disabled={creating}
              className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-xs font-medium bg-jade-500/15 text-jade-400 border border-jade-500/30 hover:bg-jade-500/25 transition-colors disabled:opacity-50"
            >
              {creating ? <Spinner size={12} /> : <CheckCircle2 size={13} />}
              {creating ? "Creating…" : "Create"}
            </button>
            <button
              onClick={() => { setShowForm(false); setFormErr(""); }}
              className="px-3 py-1.5 rounded-lg text-xs font-medium bg-edge text-soft hover:text-snow transition-colors"
            >
              Cancel
            </button>
          </div>
        </div>
      )}

      {/* User list */}
      <div className="divide-y divide-edge rounded-xl border border-edge overflow-hidden max-h-80 overflow-y-auto">
        {filtered.length === 0 ? (
          <p className="text-soft text-sm text-center py-8">No users in this category.</p>
        ) : filtered.map(u => (
          <div key={u.id}
            className="flex items-center gap-3 px-4 py-3 bg-ink hover:bg-white/2 transition-colors"
          >
            <div className={`w-7 h-7 rounded-lg flex items-center justify-center shrink-0 border text-xs font-bold ${ROLE_COLORS[u.role]}`}>
              {u.name.charAt(0).toUpperCase()}
            </div>
            <div className="flex-1 min-w-0">
              <p className="text-snow text-xs font-medium truncate">{u.name}</p>
              <p className="text-dim text-xs font-mono truncate">{u.email}</p>
            </div>
            <span className={`text-xs font-mono px-2 py-0.5 rounded-lg border ${ROLE_COLORS[u.role]}`}>
              {u.role}
            </span>
            <button
              onClick={() => handleDelete(u.id)}
              disabled={deleting === u.id}
              className="text-dim hover:text-rose-400 transition-colors shrink-0 disabled:opacity-40"
              title="Delete user"
            >
              {deleting === u.id ? <Spinner size={13} /> : <Trash2 size={13} />}
            </button>
          </div>
        ))}
      </div>
      <p className="text-dim text-xs font-mono">{filtered.length} user{filtered.length !== 1 ? "s" : ""} shown</p>
    </div>
  );
}

function LogsPanel() {
  const LOG_NOTE = [
    "Server logs are generated by the Docker container.",
    "To view live logs, run in your terminal:",
    "  docker logs -f attendance-backend",
    "Or tail the last 100 lines:",
    "  docker logs --tail 100 attendance-backend",
  ];

  return (
    <div className="space-y-3">
      <div className="bg-ink border border-edge rounded-xl p-4 font-mono text-xs space-y-2">
        {LOG_NOTE.map((line, i) => (
          <p key={i} className={line.startsWith("  ") ? "text-jade-400 pl-4" : "text-soft"}>{line}</p>
        ))}
      </div>
      <p className="text-dim text-xs font-mono">
        Logs are not streamed to the browser — access them via Docker CLI or your hosting provider's log viewer.
      </p>
    </div>
  );
}

// ── Expandable action card ────────────────────────────────────────────────────

function ActionCard({ icon: Icon, title, subtitle, color, children }) {
  const [open, setOpen] = useState(false);

  const colorMap = {
    azure:  { bg: "bg-azure-500/15",  border: "border-azure-500/20",  text: "text-azure-400"  },
    jade:   { bg: "bg-jade-500/15",   border: "border-jade-500/20",   text: "text-jade-400"   },
    violet: { bg: "bg-violet-500/15", border: "border-violet-500/20", text: "text-violet-400" },
    amber:  { bg: "bg-amber-500/15",  border: "border-amber-500/20",  text: "text-amber-400"  },
  };
  const c = colorMap[color] || colorMap.azure;

  return (
    <div className={`rounded-xl border ${open ? "border-dim" : "border-edge"} bg-ink overflow-hidden transition-all`}>
      <button
        onClick={() => setOpen(o => !o)}
        className="w-full flex items-center gap-3 px-4 py-3.5 hover:bg-white/2 transition-colors text-left"
      >
        <div className={`w-8 h-8 rounded-xl ${c.bg} border ${c.border} flex items-center justify-center shrink-0`}>
          <Icon size={14} className={c.text} />
        </div>
        <div className="flex-1 min-w-0">
          <p className="text-snow text-xs font-medium">{title}</p>
          <p className="text-dim text-xs mt-0.5">{subtitle}</p>
        </div>
        {open ? <ChevronUp size={14} className="text-dim shrink-0" /> : <ChevronDown size={14} className="text-dim shrink-0" />}
      </button>

      {open && (
        <div className="px-4 pb-4 pt-1 border-t border-edge animate-slide-up">
          {children}
        </div>
      )}
    </div>
  );
}

// ── Main ──────────────────────────────────────────────────────────────────────

export default function AdminDashboard() {
  const [stats,   setStats]   = useState(null);
  const [loading, setLoading] = useState(true);
  const [error,   setError]   = useState(null);

  useEffect(() => {
    async function load() {
      try {
        const res = await getAdminStats();
        setStats(res.data);
        setError(null);
      } catch {
        setError("Could not load stats — check backend connection.");
      } finally {
        setLoading(false);
      }
    }
    load();
    const t = setInterval(load, 10_000);
    return () => clearInterval(t);
  }, []);

  if (loading) return (
    <div className="flex items-center justify-center h-64"><Spinner size={28} /></div>
  );

  const services = [
    { name: "Flask API",         status: !error, detail: "http://localhost:4040"          },
    { name: "PostgreSQL DB",     status: !error, detail: "db:5432 / attendance"           },
    { name: "BLE Beacon Server", status: true,   detail: "http://localhost:4040/getMinor" },
    { name: "JWT Auth",          status: true,   detail: "HS256 / dev-secret"             },
  ];

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="animate-slide-up flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-rose-500/15 flex items-center justify-center border border-rose-500/20">
          <Shield size={18} className="text-rose-400" />
        </div>
        <div>
          <h1 className="text-snow text-2xl font-bold tracking-tight">Admin Panel</h1>
          <p className="text-soft text-sm mt-0.5">System management and service health</p>
        </div>
      </div>

      {error && (
        <div className="flex items-start gap-3 bg-amber-500/10 border border-amber-500/20 rounded-2xl px-5 py-4 animate-slide-up">
          <AlertTriangle size={16} className="text-amber-400 shrink-0 mt-0.5" />
          <p className="text-amber-300 text-sm">{error}</p>
        </div>
      )}

      {/* Stats strip */}
      {stats && (
        <div className="grid grid-cols-3 gap-4 animate-slide-up" style={{ animationDelay: "80ms" }}>
          {[
            { label: "Sessions",         value: stats.sessions,                        color: "text-azure-400"  },
            { label: "Attendance Marks", value: stats.attendance,                      color: "text-jade-400"   },
            { label: "Avg / Session",    value: stats.avg_attendance?.toFixed(1) ?? 0, color: "text-violet-400" },
          ].map((s, i) => (
            <div key={i} className="bg-card border border-edge rounded-2xl p-4 text-center">
              <p className="text-soft text-xs uppercase tracking-widest mb-1">{s.label}</p>
              <p className={`text-3xl font-bold font-mono ${s.color}`}>{s.value}</p>
            </div>
          ))}
        </div>
      )}

      {/* Service health */}
      <div className="bg-card border border-edge rounded-2xl p-6 animate-slide-up" style={{ animationDelay: "160ms" }}>
        <h2 className="text-snow font-semibold text-sm mb-4">Service Health</h2>
        <div className="space-y-3">
          {services.map((svc, i) => (
            <div key={i} className="flex items-center gap-3 p-3 rounded-xl bg-ink border border-edge">
              <div className={`w-2 h-2 rounded-full ${svc.status ? "bg-jade-400" : "bg-rose-400"} animate-pulse`} />
              <div className="flex-1">
                <p className="text-snow text-sm font-medium">{svc.name}</p>
                <p className="text-dim text-xs font-mono">{svc.detail}</p>
              </div>
              <Badge label={svc.status ? "UP" : "DOWN"} variant={svc.status ? "live" : "danger"} />
            </div>
          ))}
        </div>
      </div>

      {/* Actions — now expandable panels */}
      <div className="animate-slide-up space-y-3" style={{ animationDelay: "240ms" }}>
        <h2 className="text-snow font-semibold text-sm">Quick Actions</h2>

        <ActionCard
          icon={Activity}
          title="View All Sessions"
          subtitle="Browse every session across all courses and professors"
          color="azure"
        >
          <SessionsPanel />
        </ActionCard>

        <ActionCard
          icon={Users}
          title="Manage Users"
          subtitle="View, add, or remove student / professor / admin accounts"
          color="jade"
        >
          <UsersPanel />
        </ActionCard>

        <ActionCard
          icon={Server}
          title="Server Logs"
          subtitle="Access backend logs via Docker CLI"
          color="amber"
        >
          <LogsPanel />
        </ActionCard>
      </div>
    </div>
  );
}