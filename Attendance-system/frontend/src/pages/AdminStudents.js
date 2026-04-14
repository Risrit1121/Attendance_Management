/**
 * AdminStudents.js — Admin "Students" page
 *
 * Features:
 *  - Search for any student by name or ID
 *  - View attendance stats per course (% attended, lectures held)
 *  - Shows whether the student is a TA for any course and which ones
 *  - Create new student / delete student
 *  - CSV bulk-create/delete (students only)
 */
import { useState, useEffect, useCallback } from "react";
import {
  createStudent, deleteStudent,
  getAdminStudentAnalytics,
  getAllStudents, getAllCourses,
  csvBulkCreate, csvBulkDelete,
} from "../api/client";
import {
  Search, Users, Trash2, Plus, Upload, X,
  CheckCircle2, AlertTriangle, GraduationCap, BookOpen,
  RefreshCw, Shield,
} from "lucide-react";
import {
  Spinner, Empty, ProgressBar, AttendancePct, Badge,
} from "../components/UI";

// ── Mini CSV panel (students only) ────────────────────────────────────────────
function StudentCsvPanel({ onDone }) {
  const [mode,   setMode]   = useState("create");
  const [file,   setFile]   = useState(null);
  const [saving, setSaving] = useState(false);
  const [result, setResult] = useState(null);
  const [err,    setErr]    = useState("");

  const TEMPLATE = {
    create: "_id,name,email,password,imageURL",
    delete: "_id",
  };

  const handleSubmit = async () => {
    if (!file) { setErr("Please select a CSV file."); return; }
    setSaving(true); setErr(""); setResult(null);
    try {
      const fd = new FormData();
      fd.append("file", file);
      fd.append("type", "students");
      const res = mode === "create" ? await csvBulkCreate(fd) : await csvBulkDelete(fd);
      setResult(res.data);
      setFile(null);
      onDone();
    } catch (e) {
      setErr(e.response?.data?.error || "CSV operation failed.");
    } finally { setSaving(false); }
  };

  return (
    <div className="bg-card border border-edge rounded-2xl p-5 space-y-4">
      <div className="flex items-center gap-2">
        <Upload size={15} className="text-violet-400" />
        <h3 className="text-snow font-semibold text-sm">CSV Bulk — Students</h3>
      </div>
      <div className="flex gap-1 p-1 bg-ink rounded-xl border border-edge w-fit">
        {["create","delete"].map(m => (
          <button key={m} onClick={() => { setMode(m); setResult(null); setErr(""); }}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors ${
              mode === m
                ? m === "delete"
                  ? "bg-rose-500/20 text-rose-400 border border-rose-500/30"
                  : "bg-azure-500/20 text-azure-400 border border-azure-500/30"
                : "text-soft hover:text-snow"
            }`}>
            {m === "create" ? "Bulk Create" : "Bulk Delete"}
          </button>
        ))}
      </div>
      <p className="text-dim text-xs font-mono bg-ink border border-edge rounded-xl px-4 py-2.5">
        {TEMPLATE[mode]}
      </p>
      <label className="flex items-center gap-3 px-4 py-3 bg-ink border border-edge border-dashed rounded-xl cursor-pointer hover:border-violet-500/40 transition-colors">
        <Upload size={16} className="text-dim shrink-0" />
        <span className="text-soft text-sm flex-1 truncate">{file ? file.name : "Click to select CSV file…"}</span>
        {file && <button onClick={e => { e.preventDefault(); setFile(null); }} className="text-dim hover:text-rose-400"><X size={14} /></button>}
        <input type="file" accept=".csv,text/csv" className="hidden"
          onChange={e => { setFile(e.target.files[0] || null); setResult(null); setErr(""); }} />
      </label>
      {err && <p className="text-rose-400 text-xs">{err}</p>}
      {result && (
        <div className="flex items-start gap-2 bg-jade-500/10 border border-jade-500/20 rounded-xl px-4 py-3">
          <CheckCircle2 size={14} className="text-jade-400 shrink-0 mt-0.5" />
          <p className="text-xs text-jade-300">
            {mode === "create"
              ? `Created: ${result.created ?? 0}  Skipped: ${result.skipped ?? 0}`
              : `Deleted: ${result.deleted ?? 0}`}
          </p>
        </div>
      )}
      <button onClick={handleSubmit} disabled={saving}
        className={`flex items-center gap-2 px-4 py-2 rounded-xl text-sm font-medium border transition-colors disabled:opacity-50
          ${mode === "delete"
            ? "bg-rose-500/15 text-rose-400 border-rose-500/20 hover:bg-rose-500/25"
            : "bg-azure-500 text-white hover:bg-azure-400"}`}>
        {saving ? <Spinner size={14} /> : <Upload size={14} />}
        {mode === "create" ? "Upload & Create" : "Upload & Delete"}
      </button>
    </div>
  );
}

// ── Student analytics modal ───────────────────────────────────────────────────
function StudentDetailPanel({ student, onClose }) {
  const [data,    setData]    = useState(null);
  const [loading, setLoading] = useState(true);

  const sid = student._id || student.id;

  useEffect(() => {
    getAdminStudentAnalytics(sid)
      .then(r => setData(r.data))
      .catch(() => {})
      .finally(() => setLoading(false));
  }, [sid]);

  // Find courses where student is a TA
  const [taCourses, setTaCourses] = useState([]);
  useEffect(() => {
    getAllCourses()
      .then(r => {
        const courses = Array.isArray(r.data) ? r.data : [];
        setTaCourses(courses.filter(c => (c.tas || []).map(String).includes(String(sid))));
      })
      .catch(() => {});
  }, [sid]);

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm px-4">
      <div className="bg-card border border-edge rounded-2xl w-full max-w-xl max-h-[90vh] flex flex-col">
        {/* Header */}
        <div className="px-5 py-4 border-b border-edge flex items-center justify-between shrink-0">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-azure-500/15 border border-azure-500/20 flex items-center justify-center">
              <span className="text-azure-400 font-bold text-sm">{(student.name || "?").charAt(0)}</span>
            </div>
            <div>
              <p className="text-snow font-semibold text-sm">{student.name}</p>
              <p className="text-dim text-xs font-mono">{sid} · {student.email}</p>
            </div>
          </div>
          <button onClick={onClose} className="text-dim hover:text-snow transition-colors p-1 rounded-lg hover:bg-white/5">
            <X size={16} />
          </button>
        </div>

        <div className="overflow-y-auto flex-1 px-5 py-4 space-y-5">
          {/* TA status */}
          {taCourses.length > 0 && (
            <div>
              <p className="text-soft text-xs font-medium uppercase tracking-wider mb-2">TA For</p>
              <div className="flex flex-wrap gap-1.5">
                {taCourses.map(c => (
                  <span key={c._id || c.id}
                    className="px-2.5 py-1 rounded-lg bg-amber-500/15 text-amber-400 text-xs border border-amber-500/20 font-mono">
                    {c.name} ({c._id || c.id})
                  </span>
                ))}
              </div>
            </div>
          )}

          {/* Attendance stats */}
          <div>
            <p className="text-soft text-xs font-medium uppercase tracking-wider mb-3">Attendance by Course</p>
            {loading ? (
              <div className="flex justify-center py-6"><Spinner size={20} /></div>
            ) : !data?.courses?.length ? (
              <p className="text-dim text-sm text-center py-4">Not enrolled in any courses.</p>
            ) : (
              <div className="space-y-3">
                {data.courses.map((c, i) => (
                  <div key={i} className="p-3 rounded-xl bg-ink border border-edge space-y-2">
                    <div className="flex items-center justify-between">
                      <div>
                        <p className="text-snow text-sm font-medium">{c.courseName}</p>
                        <p className="text-dim text-xs font-mono">Slot {c.slot}</p>
                      </div>
                      <AttendancePct value={c.attendancePct} />
                    </div>
                    <ProgressBar value={c.attendancePct} max={100} size="sm" />
                    <p className="text-dim text-xs font-mono">
                      {c.attended} / {c.sessionsHeld ?? c.totalLectures} lectures attended
                    </p>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
}

// ── Create student form ───────────────────────────────────────────────────────
function CreateStudentForm({ onCreated, onCancel }) {
  const [form, setForm] = useState({ _id: "", name: "", email: "", password: "" });
  const [saving, setSaving] = useState(false);
  const [err, setErr] = useState("");

  const handleSave = async () => {
    if (!form._id || !form.name || !form.email || !form.password) {
      return setErr("All fields required.");
    }
    setSaving(true); setErr("");
    try {
      await createStudent({ ...form });
      onCreated();
    } catch (e) {
      setErr(e.response?.data?.error || "Creation failed.");
    } finally { setSaving(false); }
  };

  return (
    <div className="border border-jade-500/20 rounded-xl p-4 bg-ink space-y-3 animate-slide-up">
      <p className="text-snow text-xs font-semibold">Add Student</p>
      {err && (
        <div className="flex items-center gap-2 bg-rose-500/10 border border-rose-500/20 rounded-lg px-3 py-2">
          <AlertTriangle size={12} className="text-rose-400 shrink-0" />
          <p className="text-rose-300 text-xs">{err}</p>
        </div>
      )}
      <div className="grid grid-cols-2 gap-2">
        {[["Student ID", "_id", "text"], ["Name", "name", "text"], ["Email", "email", "email"], ["Password", "password", "password"]].map(([lbl, key, type]) => (
          <div key={key} className="space-y-1">
            <label className="text-xs text-soft">{lbl}</label>
            <input type={type} value={form[key]} onChange={e => setForm(f => ({ ...f, [key]: e.target.value }))}
              className="w-full bg-card border border-edge rounded-xl text-xs text-snow px-3 py-1.5
                focus:outline-none focus:border-jade-500 transition-all" />
          </div>
        ))}
      </div>
      <div className="flex gap-2">
        <button onClick={handleSave} disabled={saving}
          className="flex items-center gap-1.5 px-3 py-1.5 rounded-xl bg-jade-500/15 text-jade-400 text-xs
            border border-jade-500/20 hover:bg-jade-500/25 transition-colors disabled:opacity-50">
          {saving ? <Spinner size={12} /> : <Plus size={12} />}
          {saving ? "Creating…" : "Create"}
        </button>
        <button onClick={onCancel}
          className="px-3 py-1.5 rounded-xl bg-edge text-soft text-xs hover:text-snow transition-colors">
          Cancel
        </button>
      </div>
    </div>
  );
}

// ── Main ──────────────────────────────────────────────────────────────────────
export default function AdminStudents() {
  const [students,       setStudents]       = useState([]);
  const [loading,        setLoading]        = useState(true);
  const [searchQuery,    setSearchQuery]    = useState("");
  const [viewStudent,    setViewStudent]    = useState(null);
  const [deleting,       setDeleting]       = useState(null);
  const [showCreate,     setShowCreate]     = useState(false);
  const [showCsv,        setShowCsv]        = useState(false);
  const [taCourseMap,    setTaCourseMap]    = useState({}); // studentId → [courseNames]

  const load = useCallback(async () => {
    setLoading(true);
    try {
      const [sRes, cRes] = await Promise.all([
        getAllStudents(),
        getAllCourses(),
      ]);
      const studs   = Array.isArray(sRes.data) ? sRes.data : [];
      const courses = Array.isArray(cRes.data) ? cRes.data : [];

      setStudents(studs);

      // Build TA map: studentId → list of course names they TA
      const taMap = {};
      for (const c of courses) {
        for (const taId of (c.tas || [])) {
          if (!taMap[taId]) taMap[taId] = [];
          taMap[taId].push(c.name);
        }
      }
      setTaCourseMap(taMap);
    } catch (e) {
      console.error("AdminStudents load failed:", e);
    } finally { setLoading(false); }
  }, []);

  useEffect(() => { load(); }, [load]);

  const handleDelete = async (s) => {
    if (!window.confirm(`Delete student ${s.name}?`)) return;
    const sid = s._id || s.id;
    setDeleting(sid);
    try { await deleteStudent(sid); await load(); }
    catch {} finally { setDeleting(null); }
  };

  const filtered = students.filter(s => {
    const q = searchQuery.toLowerCase();
    return (
      (s.name  || "").toLowerCase().includes(q) ||
      (s.email || "").toLowerCase().includes(q) ||
      (s._id   || s.id || "").toLowerCase().includes(q)
    );
  });

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="animate-slide-up flex items-center justify-between flex-wrap gap-3">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-jade-500/15 flex items-center justify-center border border-jade-500/20">
            <Users size={18} className="text-jade-400" />
          </div>
          <div>
            <h1 className="text-snow text-2xl font-bold tracking-tight">Students</h1>
            <p className="text-soft text-sm mt-0.5">{students.length} students · search, view attendance & manage</p>
          </div>
        </div>
        <div className="flex items-center gap-2">
          <button onClick={() => setShowCsv(v => !v)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-violet-500/15 text-violet-400 text-xs
              border border-violet-500/20 hover:bg-violet-500/25 transition-colors">
            <Upload size={12} /> {showCsv ? "Hide CSV" : "CSV Import"}
          </button>
          <button onClick={() => setShowCreate(v => !v)}
            className="flex items-center gap-1.5 px-3 py-1.5 rounded-lg bg-jade-500/15 text-jade-400 text-xs
              border border-jade-500/20 hover:bg-jade-500/25 transition-colors">
            <Plus size={12} /> {showCreate ? "Cancel" : "Add Student"}
          </button>
          <button onClick={load}
            className="p-1.5 rounded-lg bg-card border border-edge text-dim hover:text-snow transition-colors">
            <RefreshCw size={13} />
          </button>
        </div>
      </div>

      {/* CSV Panel */}
      {showCsv && <StudentCsvPanel onDone={load} />}

      {/* Create Form */}
      {showCreate && (
        <CreateStudentForm
          onCreated={() => { setShowCreate(false); load(); }}
          onCancel={() => setShowCreate(false)}
        />
      )}

      {/* Search */}
      <div className="relative max-w-md">
        <Search size={14} className="absolute left-3.5 top-1/2 -translate-y-1/2 text-dim" />
        <input
          value={searchQuery}
          onChange={e => setSearchQuery(e.target.value)}
          placeholder="Search by name, ID, or email…"
          className="w-full bg-card border border-edge rounded-xl text-sm text-snow pl-10 pr-4 py-2.5
            focus:outline-none focus:border-jade-500 transition-all placeholder:text-dim"
        />
        {searchQuery && (
          <button onClick={() => setSearchQuery("")}
            className="absolute right-3 top-1/2 -translate-y-1/2 text-dim hover:text-snow transition-colors">
            <X size={14} />
          </button>
        )}
      </div>

      {/* Results count */}
      <p className="text-dim text-xs font-mono -mt-3">
        {filtered.length} student{filtered.length !== 1 ? "s" : ""} shown
      </p>

      {/* Student list */}
      {loading ? (
        <div className="flex items-center justify-center h-40"><Spinner size={24} /></div>
      ) : filtered.length === 0 ? (
        <Empty icon={Users} title="No students found"
          sub={searchQuery ? "Try a different search query." : "Add students to get started."} />
      ) : (
        <div className="bg-card border border-edge rounded-2xl overflow-hidden">
          <div className="divide-y divide-edge max-h-[600px] overflow-y-auto">
            {filtered.map(s => {
              const sid      = s._id || s.id;
              const taCrs    = taCourseMap[sid] || [];
              const isTA     = taCrs.length > 0;
              return (
                <div key={sid} className="flex items-center gap-4 px-5 py-3.5 hover:bg-white/2 transition-colors group">
                  {/* Avatar */}
                  <div className="w-9 h-9 rounded-xl bg-violet-500/15 border border-violet-500/20 flex items-center justify-center shrink-0">
                    <span className="text-violet-400 font-bold text-sm">
                      {(s.name || "?").charAt(0).toUpperCase()}
                    </span>
                  </div>

                  {/* Info */}
                  <div className="flex-1 min-w-0">
                    <div className="flex items-center gap-2">
                      <p className="text-snow text-sm font-medium truncate">{s.name}</p>
                      {isTA && (
                        <span className="px-1.5 py-0.5 rounded text-xs bg-amber-500/15 text-amber-400 border border-amber-500/20 font-mono shrink-0">
                          TA
                        </span>
                      )}
                    </div>
                    <p className="text-dim text-xs font-mono truncate">{sid} · {s.email}</p>
                    {isTA && (
                      <p className="text-amber-400/70 text-xs truncate mt-0.5">
                        TA for: {taCrs.slice(0, 3).join(", ")}{taCrs.length > 3 ? ` +${taCrs.length - 3}` : ""}
                      </p>
                    )}
                  </div>

                  {/* Actions */}
                  <div className="flex items-center gap-2 shrink-0">
                    <button
                      onClick={() => setViewStudent(s)}
                      className="flex items-center gap-1.5 px-2.5 py-1.5 rounded-lg bg-azure-500/10 text-azure-400 text-xs
                        border border-azure-500/20 hover:bg-azure-500/20 transition-colors opacity-0 group-hover:opacity-100">
                      <BookOpen size={12} /> View Stats
                    </button>
                    <button
                      onClick={() => handleDelete(s)}
                      disabled={deleting === sid}
                      className="p-1.5 rounded-lg text-dim hover:text-rose-400 hover:bg-rose-500/10 transition-colors">
                      {deleting === sid ? <Spinner size={14} /> : <Trash2 size={14} />}
                    </button>
                  </div>
                </div>
              );
            })}
          </div>
        </div>
      )}

      {/* Detail modal */}
      {viewStudent && (
        <StudentDetailPanel
          student={viewStudent}
          onClose={() => setViewStudent(null)}
        />
      )}
    </div>
  );
}