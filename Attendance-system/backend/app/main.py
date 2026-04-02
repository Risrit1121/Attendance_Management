# import os
# import jwt
# import random
# from flask_cors import CORS
# from flask import Flask, request, jsonify
# from .database import Base, engine, SessionLocal
# from datetime import datetime, timezone, timedelta
# from .models import Attendance, BeaconLog, Session, User, Course, Classroom, Beacon, Classroom2Beacon, Enrollment

# app = Flask(__name__)
# CORS(app)

# # Create tables
# Base.metadata.create_all(bind=engine)

# # from .seed import * # Uncomment to seed data on startup (runs only once)

# SECRET = os.getenv("JWT_SECRET", "dev-secret")

# # ── Auth ──────────────────────────────────────────────────────────────────────

# @app.route('/login', methods=['POST'])
# def login():
#     data     = request.json
#     email    = data.get("email")
#     password = data.get("password")

#     if not email or not password:
#         return jsonify({"error": "Email and password required"}), 400

#     db   = SessionLocal()
#     user = db.query(User).filter(User.email == email).first()
#     db.close()

#     if not user:
#         return jsonify({"error": "User not found"}), 404
#     if user.password != password:
#         return jsonify({"error": "Invalid password"}), 401

#     token = jwt.encode(
#         {"user_id": user.id, "role": user.role,
#          "exp": datetime.now(timezone.utc) + timedelta(days=7)},
#         SECRET, algorithm="HS256"
#     )

#     return jsonify({
#         "user_id": user.id,
#         "name":    user.name,
#         "role":    user.role,
#         "token":   token,
#     })

# # ── Beacon ────────────────────────────────────────────────────────────────────

# @app.route('/getMinor')
# def get_minor():
#     db = SessionLocal()
#     major_value = request.args.get("major")
#     course_id   = request.args.get("course_id")

#     if major_value:
#         major_value = int(major_value)
#     elif course_id:
#         session = db.query(Session).filter(
#             Session.course_id == int(course_id),
#             Session.is_active == True
#         ).first()
#         if not session:
#             db.close()
#             return "No active session", 400
#         major_value = session.major
#     else:
#         db.close()
#         return "Provide major or course_id", 400

#     minor_value = random.randint(0, 65535)
#     log = BeaconLog(major=major_value, minor=minor_value, timestamp=datetime.now(timezone.utc))
#     db.add(log)
#     db.commit()
#     db.close()
#     return str(minor_value)

# @app.route('/validate')
# def validate():
#     try:
#         major_value = int(request.args.get('major'))
#         minor_value = int(request.args.get('minor'))
#     except:
#         return jsonify({"error": "Invalid input"}), 400

#     now = datetime.now(timezone.utc)
#     db  = SessionLocal()
#     entry = db.query(BeaconLog).filter(
#         BeaconLog.major == major_value,
#         BeaconLog.minor == minor_value
#     ).order_by(BeaconLog.timestamp.desc()).first()
#     db.close()

#     if not entry:
#         return jsonify({"valid": False})
#     if entry.timestamp.replace(tzinfo=timezone.utc) > now - timedelta(seconds=30):
#         return jsonify({"valid": True})
#     return jsonify({"valid": False, "expired": True})

# # ── Sessions ──────────────────────────────────────────────────────────────────

# @app.route('/startSession', methods=['POST'])
# def start_session():
#     db = SessionLocal()
#     try:
#         data         = request.json
#         course_id    = data.get("course_id")
#         classroom_id = data.get("classroom_id")
#         mode         = data.get("mode", "ble")

#         if not course_id:
#             return jsonify({"error": "course_id required"}), 400

#         course = db.query(Course).filter(Course.id == course_id).first()
#         if not course:
#             return jsonify({"error": "Course not found"}), 404

#         final_classroom_id = classroom_id or course.default_classroom_id
#         if not final_classroom_id:
#             return jsonify({"error": "No classroom assigned to course"}), 400

#         mapping = db.query(Classroom2Beacon).filter(
#             Classroom2Beacon.classroom_id == final_classroom_id
#         ).first()
#         if not mapping:
#             return jsonify({"error": "No beacon mapped to classroom"}), 400

#         db.query(Session).filter(
#             Session.course_id == course_id,
#             Session.is_active == True
#         ).update({"is_active": False, "end_time": datetime.now(timezone.utc)},
#                  synchronize_session=False)

#         session = Session(
#             course_id=course_id,
#             classroom_id=final_classroom_id,
#             major=mapping.major,
#             start_time=datetime.now(timezone.utc),
#             is_active=True,
#             mode=mode
#         )
#         db.add(session)
#         db.commit()

#         return jsonify({
#             "session_id": session.id,
#             "major":      session.major,
#             "mode":       session.mode,
#             "course_id":  course_id,
#         })
#     finally:
#         db.close()

# @app.route('/endSession/<int:session_id>', methods=['POST'])
# def end_session(session_id):
#     db = SessionLocal()
#     try:
#         session = db.query(Session).filter(Session.id == session_id).first()
#         if not session:
#             return jsonify({"error": "Session not found"}), 404
#         if not session.is_active:
#             return jsonify({"status": "Session already ended"})
#         session.is_active = False
#         session.end_time  = datetime.now(timezone.utc)
#         db.commit()
#         return jsonify({"status": "Session ended"})
#     finally:
#         db.close()

# @app.route('/activeSession')
# def active_session():
#     course_id = request.args.get("course_id")
#     if not course_id:
#         return jsonify({"error": "course_id required"}), 400

#     db = SessionLocal()
#     try:
#         session = db.query(Session).filter(
#             Session.course_id == int(course_id),
#             Session.is_active == True
#         ).first()
#         if not session:
#             return jsonify({"error": "No active session"}), 404
#         return jsonify({
#             "session_id": session.id,
#             "major":      session.major,
#             "mode":       session.mode,
#         })
#     finally:
#         db.close()

# # ── QR ────────────────────────────────────────────────────────────────────────

# def generate_qr(session_id, major):
#     payload = {
#         "session_id": session_id,
#         "major":      major,
#         "exp":        datetime.now(timezone.utc) + timedelta(seconds=30)
#     }
#     return jwt.encode(payload, SECRET, algorithm="HS256")

# @app.route('/getQR/<int:session_id>')
# def get_qr(session_id):
#     db = SessionLocal()
#     session = db.query(Session).filter(Session.id == session_id).first()
#     db.close()

#     if not session:
#         return jsonify({"error": "Session not found"}), 404
#     if session.mode == "ble":
#         return jsonify({"error": "QR not enabled for this session"}), 400

#     return jsonify({"qr": generate_qr(session_id, session.major)})

# # ── Attendance ────────────────────────────────────────────────────────────────

# @app.route('/markAttendance', methods=['POST'])
# def mark_attendance():
#     db = SessionLocal()
#     try:
#         data       = request.json
#         session_id = data.get("session_id")
#         student_id = data.get("student_id")
#         token      = data.get("token")
#         major      = data.get("major")
#         minor      = data.get("minor")

#         if not session_id or not student_id:
#             return jsonify({"error": "session_id and student_id required"}), 400

#         session = db.query(Session).filter(Session.id == session_id).first()
#         if not session:
#             return jsonify({"error": "Session not found"}), 404
#         if not session.is_active:
#             return jsonify({"error": "Session is no longer active"}), 400

#         if session.mode == "qr" and not token:
#             return jsonify({"error": "QR required for this session"}), 400

#         if session.mode in ["ble", "hybrid"]:
#             if not minor:
#                 return jsonify({"error": "minor required for this session"}), 400
#             valid_mapping = db.query(Classroom2Beacon).filter(
#                 Classroom2Beacon.classroom_id == session.classroom_id,
#                 Classroom2Beacon.major == major
#             ).first()
#             if not valid_mapping:
#                 return jsonify({"error": "Invalid classroom beacon"}), 400
#             beacon = db.query(BeaconLog).filter(
#                 BeaconLog.major == major,
#                 BeaconLog.minor == int(minor),
#                 BeaconLog.timestamp >= datetime.now(timezone.utc) - timedelta(seconds=30)
#             ).first()
#             if not beacon:
#                 return jsonify({"error": "Invalid beacon"}), 400

#         existing = db.query(Attendance).filter(
#             Attendance.session_id == session.id,
#             Attendance.student_id == student_id
#         ).first()
#         if existing:
#             return jsonify({"message": "Already marked"})

#         attendance = Attendance(
#             session_id=session.id,
#             student_id=student_id,
#             timestamp=datetime.now(timezone.utc)
#         )
#         db.add(attendance)
#         db.commit()
#         return jsonify({"status": "Attendance marked"})
#     finally:
#         db.close()

# @app.route('/manualAttendance', methods=['POST'])
# def manual_attendance():
#     db = SessionLocal()
#     try:
#         data       = request.json
#         session_id = data.get("session_id")
#         student_id = data.get("student_id")

#         if not session_id or not student_id:
#             return jsonify({"error": "session_id and student_id required"}), 400

#         session = db.query(Session).filter(Session.id == session_id).first()
#         if not session:
#             return jsonify({"error": "Session not found"}), 404
#         if not session.is_active:
#             return jsonify({"error": "Session is no longer active"}), 400

#         existing = db.query(Attendance).filter(
#             Attendance.session_id == session_id,
#             Attendance.student_id == student_id
#         ).first()
#         if existing:
#             return jsonify({"message": "Already marked"})

#         attendance = Attendance(
#             session_id=session_id,
#             student_id=student_id,
#             timestamp=datetime.now(timezone.utc)
#         )
#         db.add(attendance)
#         db.commit()
#         return jsonify({"status": "Attendance marked manually"})
#     finally:
#         db.close()

# @app.route('/manualAttendance/bulk', methods=['POST'])
# def manual_attendance_bulk():
#     db = SessionLocal()
#     try:
#         data        = request.json
#         session_id  = data.get("session_id")
#         student_ids = data.get("student_ids", [])

#         if not session_id:
#             return jsonify({"error": "session_id required"}), 400

#         session = db.query(Session).filter(Session.id == session_id).first()
#         if not session or not session.is_active:
#             return jsonify({"error": "Invalid session"}), 400

#         added = 0
#         for sid in student_ids:
#             exists = db.query(Attendance).filter(
#                 Attendance.session_id == session_id,
#                 Attendance.student_id == sid
#             ).first()
#             if not exists:
#                 db.add(Attendance(
#                     session_id=session_id,
#                     student_id=sid,
#                     timestamp=datetime.now(timezone.utc)
#                 ))
#                 added += 1

#         db.commit()
#         return jsonify({"added": added})
#     finally:
#         db.close()

# @app.route('/attendance/<int:session_id>')
# def get_attendance(session_id):
#     db = SessionLocal()
#     try:
#         records = db.query(Attendance).filter(
#             Attendance.session_id == session_id
#         ).order_by(Attendance.timestamp.desc()).all()

#         return jsonify([
#             # Append 'Z' so the frontend unambiguously parses as UTC
#             {"student_id": r.student_id, "timestamp": r.timestamp.strftime("%Y-%m-%dT%H:%M:%SZ")}
#             for r in records
#         ])
#     finally:
#         db.close()

# # ── Courses ───────────────────────────────────────────────────────────────────

# @app.route('/courses/<int:prof_id>')
# def get_courses(prof_id):
#     db = SessionLocal()
#     try:
#         courses = db.query(Course).filter(Course.professor_id == prof_id).all()
#         return jsonify([{"id": c.id, "name": c.name} for c in courses])
#     finally:
#         db.close()

# @app.route('/course/<int:course_id>/students')
# def get_course_students(course_id):
#     db = SessionLocal()
#     try:
#         students = db.query(User).join(
#             Enrollment, Enrollment.student_id == User.id
#         ).filter(
#             Enrollment.course_id == course_id,
#             User.role == "student"
#         ).distinct(User.id).all()

#         return jsonify([{"id": s.id, "name": s.name} for s in students])
#     finally:
#         db.close()

# # ── Analytics ─────────────────────────────────────────────────────────────────

# @app.route('/admin/stats')
# def admin_stats():
#     db = SessionLocal()
#     try:
#         sessions   = db.query(Session).count()
#         attendance = db.query(Attendance).count()
#         avg        = round(attendance / sessions, 2) if sessions else 0
#         return jsonify({
#             "sessions":       sessions,
#             "attendance":     attendance,
#             "avg_attendance": avg,
#         })
#     finally:
#         db.close()

# @app.route('/analytics/course/<int:course_id>')
# def course_analytics(course_id):
#     db = SessionLocal()
#     try:
#         sessions = db.query(Session).filter(Session.course_id == course_id).all()
#         enrolled = db.query(Enrollment).filter(Enrollment.course_id == course_id).count()
#         result   = []

#         for s in sessions:
#             records = db.query(Attendance).filter(Attendance.session_id == s.id).all()
#             unique  = len(set(r.student_id for r in records))
#             result.append({
#                 "session_id":      s.id,
#                 # Append 'Z' so frontend unambiguously parses as UTC
#                 "start_time":      s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
#                 "end_time":        s.end_time.strftime("%Y-%m-%dT%H:%M:%SZ")   if s.end_time   else None,
#                 "mode":            s.mode,
#                 "is_active":       s.is_active,
#                 "total_marks":     len(records),
#                 "unique_students": unique,
#             })

#         return jsonify({
#             "course_id": course_id,
#             "enrolled":  enrolled,   # number of enrolled students
#             "sessions":  result,
#         })
#     finally:
#         db.close()

# @app.route('/analytics/prof/<int:prof_id>')
# def prof_analytics(prof_id):
#     db = SessionLocal()
#     try:
#         courses = db.query(Course).filter(Course.professor_id == prof_id).all()
#         data    = []

#         for c in courses:
#             sessions  = db.query(Session).filter(Session.course_id == c.id).all()
#             enrolled  = db.query(Enrollment).filter(Enrollment.course_id == c.id).count()
#             total_att = 0
#             for s in sessions:
#                 total_att += db.query(Attendance).filter(Attendance.session_id == s.id).count()
#             data.append({
#                 "course_id":   c.id,
#                 "course_name": c.name,
#                 "sessions":    len(sessions),
#                 "attendance":  total_att,
#                 "enrolled":    enrolled,
#                 "avg":         round(total_att / len(sessions), 1) if sessions else 0,
#             })

#         return jsonify(data)
#     finally:
#         db.close()


# # @app.route('/student/<int:student_id>/history/<int:course_id>')
# # def student_course_history(student_id, course_id):
# #     db = SessionLocal()
# #     try:
# #         sessions = db.query(Session).filter(
# #             Session.course_id == course_id
# #         ).order_by(Session.start_time.asc()).all()
 
# #         attended_ids = set(
# #             r.session_id for r in db.query(Attendance).filter(
# #                 Attendance.student_id == student_id,
# #                 Attendance.session_id.in_([s.id for s in sessions])
# #             ).all()
# #         )
 
# #         total    = len(sessions)
# #         attended = len(attended_ids)
 
# #         result = []
# #         for s in sessions:
# #             result.append({
# #                 "session_id": s.id,
# #                 "start_time": s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
# #                 "attended":   s.id in attended_ids,
# #             })
 
# #         return jsonify({
# #             "course_id":  course_id,
# #             "student_id": student_id,
# #             "total":      total,
# #             "attended":   attended,
# #             "sessions":   result,
# #         })
# #     finally:
# #         db.close()
 
 
# # @app.route('/analytics/at-risk/<int:prof_id>')
# # def at_risk_students(prof_id):
# #     db = SessionLocal()
# #     try:
# #         courses = db.query(Course).filter(Course.professor_id == prof_id).all()
# #         at_risk = []
 
# #         for c in courses:
# #             sessions = db.query(Session).filter(Session.course_id == c.id).all()
# #             if not sessions:
# #                 continue
 
# #             total_sessions = len(sessions)
# #             session_ids    = [s.id for s in sessions]
 
# #             enrollments = db.query(Enrollment).filter(
# #                 Enrollment.course_id == c.id
# #             ).all()
 
# #             for enroll in enrollments:
# #                 sid = enroll.student_id
# #                 attended = db.query(Attendance).filter(
# #                     Attendance.student_id == sid,
# #                     Attendance.session_id.in_(session_ids)
# #                 ).count()
 
# #                 # Count distinct sessions attended (not total marks)
# #                 attended_sessions = len(set(
# #                     r.session_id for r in db.query(Attendance).filter(
# #                         Attendance.student_id == sid,
# #                         Attendance.session_id.in_(session_ids)
# #                     ).all()
# #                 ))
 
# #                 pct = (attended_sessions / total_sessions) * 100 if total_sessions > 0 else 0
 
# #                 if pct < 25:
# #                     student = db.query(User).filter(User.id == sid).first()
# #                     at_risk.append({
# #                         "student_id":   sid,
# #                         "student_name": student.name if student else f"Student #{sid}",
# #                         "course_id":    c.id,
# #                         "course_name":  c.name,
# #                         "attended":     attended_sessions,
# #                         "total":        total_sessions,
# #                         "pct":          round(pct, 1),
# #                     })
 
# #         # Sort by pct ascending (worst first)
# #         at_risk.sort(key=lambda x: x["pct"])
# #         return jsonify(at_risk)
# #     finally:
# #         db.close()

# # @app.route('/admin/sessions')
# # def admin_sessions():
# #     db = SessionLocal()
# #     try:
# #         sessions = db.query(Session).order_by(Session.start_time.desc()).all()
# #         result   = []
# #         for s in sessions:
# #             course = db.query(Course).filter(Course.id == s.course_id).first()
# #             prof   = db.query(User).filter(User.id == course.professor_id).first() if course else None
# #             marks  = db.query(Attendance).filter(Attendance.session_id == s.id).all()
# #             unique = len(set(r.student_id for r in marks))
# #             result.append({
# #                 "session_id":      s.id,
# #                 "course_id":       s.course_id,
# #                 "course_name":     course.name if course else "—",
# #                 "prof_name":       prof.name if prof else "—",
# #                 "start_time":      s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
# #                 "end_time":        s.end_time.strftime("%Y-%m-%dT%H:%M:%SZ")   if s.end_time   else None,
# #                 "mode":            s.mode,
# #                 "is_active":       s.is_active,
# #                 "total_marks":     len(marks),
# #                 "unique_students": unique,
# #             })
# #         return jsonify(result)
# #     finally:
# #         db.close()
 
 
# # ── Admin: list / create / delete users ───────────────────────────────────────
 
# @app.route('/admin/users', methods=['GET'])
# def admin_list_users():
#     db = SessionLocal()
#     try:
#         users = db.query(User).order_by(User.role, User.name).all()
#         return jsonify([
#             {"id": u.id, "name": u.name, "email": u.email, "role": u.role}
#             for u in users
#         ])
#     finally:
#         db.close()
 
 
# @app.route('/admin/users', methods=['POST'])
# def admin_create_user():
#     db   = SessionLocal()
#     data = request.json
#     try:
#         name     = data.get("name", "").strip()
#         email    = data.get("email", "").strip()
#         password = data.get("password", "").strip()
#         role     = data.get("role", "student").strip()
 
#         if not name or not email or not password:
#             return jsonify({"error": "name, email, and password are required"}), 400
#         if role not in ("student", "prof", "admin"):
#             return jsonify({"error": "role must be student, prof, or admin"}), 400
 
#         exists = db.query(User).filter(User.email == email).first()
#         if exists:
#             return jsonify({"error": "Email already in use"}), 409
 
#         user = User(name=name, email=email, password=password, role=role)
#         db.add(user)
#         db.commit()
#         return jsonify({"id": user.id, "name": user.name, "email": user.email, "role": user.role}), 201
#     finally:
#         db.close()
 
 
# @app.route('/admin/users/<int:user_id>', methods=['DELETE'])
# def admin_delete_user(user_id):
#     db = SessionLocal()
#     try:
#         user = db.query(User).filter(User.id == user_id).first()
#         if not user:
#             return jsonify({"error": "User not found"}), 404
#         db.delete(user)
#         db.commit()
#         return jsonify({"status": "deleted"})
#     finally:
#         db.close()
 
 
# # ── Admin: system-wide analytics breakdown ────────────────────────────────────
 
# @app.route('/admin/analytics')
# def admin_analytics():
#     db = SessionLocal()
#     try:
#         all_profs   = db.query(User).filter(User.role == "prof").all()
#         all_courses = db.query(Course).all()
#         total_sess  = db.query(Session).count()
#         total_att   = db.query(Attendance).count()
#         total_stud  = db.query(User).filter(User.role == "student").count()
#         avg_global  = round(total_att / total_sess, 2) if total_sess else 0
 
#         profs_data = []
#         for prof in all_profs:
#             courses      = db.query(Course).filter(Course.professor_id == prof.id).all()
#             courses_data = []
#             for c in courses:
#                 sessions   = db.query(Session).filter(Session.course_id == c.id).order_by(Session.start_time.desc()).all()
#                 enrolled   = db.query(Enrollment).filter(Enrollment.course_id == c.id).count()
#                 course_att = 0
#                 for s in sessions:
#                     marks       = db.query(Attendance).filter(Attendance.session_id == s.id).all()
#                     course_att += len(set(r.student_id for r in marks))
#                 avg_pct = round((course_att / (len(sessions) * enrolled)) * 100, 1) \
#                     if sessions and enrolled else 0
#                 courses_data.append({
#                     "course_id":  c.id,
#                     "name":       c.name,
#                     "sessions":   len(sessions),
#                     "enrolled":   enrolled,
#                     "attendance": course_att,
#                     "avg_pct":    avg_pct,
#                 })
#             profs_data.append({
#                 "prof_id":   prof.id,
#                 "prof_name": prof.name,
#                 "courses":   courses_data,
#             })
 
#         return jsonify({
#             "totals": {
#                 "sessions":   total_sess,
#                 "attendance": total_att,
#                 "avg":        avg_global,
#                 "students":   total_stud,
#                 "courses":    len(all_courses),
#                 "profs":      len(all_profs),
#             },
#             "profs": profs_data,
#         })
#     finally:
#         db.close()
 
 
# # ── Student history heatmap ────────────────────────────────────────────────────
 
# @app.route('/student/<int:student_id>/history/<int:course_id>')
# def student_course_history(student_id, course_id):
#     db = SessionLocal()
#     try:
#         sessions = db.query(Session).filter(
#             Session.course_id == course_id
#         ).order_by(Session.start_time.asc()).all()
#         attended_ids = set(
#             r.session_id for r in db.query(Attendance).filter(
#                 Attendance.student_id == student_id,
#                 Attendance.session_id.in_([s.id for s in sessions])
#             ).all()
#         )
#         result = []
#         for s in sessions:
#             result.append({
#                 "session_id": s.id,
#                 "start_time": s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
#                 "attended":   s.id in attended_ids,
#             })
#         return jsonify({
#             "course_id":  course_id,
#             "student_id": student_id,
#             "total":      len(sessions),
#             "attended":   len(attended_ids),
#             "sessions":   result,
#         })
#     finally:
#         db.close()
 
 
# # ── At-risk students (< 25% attendance) ───────────────────────────────────────
 
# @app.route('/analytics/at-risk/<int:prof_id>')
# def at_risk_students(prof_id):
#     db = SessionLocal()
#     try:
#         courses = db.query(Course).filter(Course.professor_id == prof_id).all()
#         at_risk = []
#         for c in courses:
#             sessions = db.query(Session).filter(Session.course_id == c.id).all()
#             if not sessions:
#                 continue
#             total_sessions = len(sessions)
#             session_ids    = [s.id for s in sessions]
#             enrollments    = db.query(Enrollment).filter(Enrollment.course_id == c.id).all()
#             for enroll in enrollments:
#                 sid = enroll.student_id
#                 attended_sessions = len(set(
#                     r.session_id for r in db.query(Attendance).filter(
#                         Attendance.student_id == sid,
#                         Attendance.session_id.in_(session_ids)
#                     ).all()
#                 ))
#                 pct = (attended_sessions / total_sessions) * 100 if total_sessions > 0 else 0
#                 if pct < 25:
#                     student = db.query(User).filter(User.id == sid).first()
#                     at_risk.append({
#                         "student_id":   sid,
#                         "student_name": student.name if student else f"Student #{sid}",
#                         "course_id":    c.id,
#                         "course_name":  c.name,
#                         "attended":     attended_sessions,
#                         "total":        total_sessions,
#                         "pct":          round(pct, 1),
#                     })
#         at_risk.sort(key=lambda x: x["pct"])
#         return jsonify(at_risk)
#     finally:
#         db.close()

# if __name__ == "__main__":
#     app.run(host="0.0.0.0", port=4040)

import os
import jwt
import random
from flask_cors import CORS
from flask import Flask, request, jsonify
from .database import Base, engine, SessionLocal
from datetime import datetime, timezone, timedelta
from .models import Attendance, BeaconLog, Session, User, Course, Classroom, Beacon, Classroom2Beacon, Enrollment

app = Flask(__name__)
CORS(app)

# Create tables
Base.metadata.create_all(bind=engine)

# from .seed import * # Uncomment to seed data on startup (runs only once)

SECRET = os.getenv("JWT_SECRET", "dev-secret")

# ── Auth ──────────────────────────────────────────────────────────────────────

@app.route('/login', methods=['POST'])
def login():
    data     = request.json
    email    = data.get("email")
    password = data.get("password")

    if not email or not password:
        return jsonify({"error": "Email and password required"}), 400

    db   = SessionLocal()
    user = db.query(User).filter(User.email == email).first()
    db.close()

    if not user:
        return jsonify({"error": "User not found"}), 404
    if user.password != password:
        return jsonify({"error": "Invalid password"}), 401

    token = jwt.encode(
        {"user_id": user.id, "role": user.role,
         "exp": datetime.now(timezone.utc) + timedelta(days=7)},
        SECRET, algorithm="HS256"
    )

    return jsonify({
        "user_id": user.id,
        "name":    user.name,
        "role":    user.role,
        "token":   token,
    })

# ── Beacon ────────────────────────────────────────────────────────────────────

@app.route('/getMinor')
def get_minor():
    db = SessionLocal()
    major_value = request.args.get("major")
    course_id   = request.args.get("course_id")

    if major_value:
        major_value = int(major_value)
    elif course_id:
        session = db.query(Session).filter(
            Session.course_id == int(course_id),
            Session.is_active == True
        ).first()
        if not session:
            db.close()
            return "No active session", 400
        major_value = session.major
    else:
        db.close()
        return "Provide major or course_id", 400

    minor_value = random.randint(0, 65535)
    log = BeaconLog(major=major_value, minor=minor_value, timestamp=datetime.now(timezone.utc))
    db.add(log)
    db.commit()
    db.close()
    return str(minor_value)

@app.route('/validate')
def validate():
    try:
        major_value = int(request.args.get('major'))
        minor_value = int(request.args.get('minor'))
    except:
        return jsonify({"error": "Invalid input"}), 400

    now = datetime.now(timezone.utc)
    db  = SessionLocal()
    entry = db.query(BeaconLog).filter(
        BeaconLog.major == major_value,
        BeaconLog.minor == minor_value
    ).order_by(BeaconLog.timestamp.desc()).first()
    db.close()

    if not entry:
        return jsonify({"valid": False})
    if entry.timestamp.replace(tzinfo=timezone.utc) > now - timedelta(seconds=30):
        return jsonify({"valid": True})
    return jsonify({"valid": False, "expired": True})

# ── Sessions ──────────────────────────────────────────────────────────────────

@app.route('/startSession', methods=['POST'])
def start_session():
    db = SessionLocal()
    try:
        data         = request.json
        course_id    = data.get("course_id")
        classroom_id = data.get("classroom_id")
        mode         = data.get("mode", "ble")

        if not course_id:
            return jsonify({"error": "course_id required"}), 400

        course = db.query(Course).filter(Course.id == course_id).first()
        if not course:
            return jsonify({"error": "Course not found"}), 404

        final_classroom_id = classroom_id or course.default_classroom_id
        if not final_classroom_id:
            return jsonify({"error": "No classroom assigned to course"}), 400

        mapping = db.query(Classroom2Beacon).filter(
            Classroom2Beacon.classroom_id == final_classroom_id
        ).first()
        if not mapping:
            return jsonify({"error": "No beacon mapped to classroom"}), 400

        db.query(Session).filter(
            Session.course_id == course_id,
            Session.is_active == True
        ).update({"is_active": False, "end_time": datetime.now(timezone.utc)},
                 synchronize_session=False)

        session = Session(
            course_id=course_id,
            classroom_id=final_classroom_id,
            major=mapping.major,
            start_time=datetime.now(timezone.utc),
            is_active=True,
            mode=mode
        )
        db.add(session)
        db.commit()

        return jsonify({
            "session_id": session.id,
            "major":      session.major,
            "mode":       session.mode,
            "course_id":  course_id,
        })
    finally:
        db.close()

@app.route('/endSession/<int:session_id>', methods=['POST'])
def end_session(session_id):
    db = SessionLocal()
    try:
        session = db.query(Session).filter(Session.id == session_id).first()
        if not session:
            return jsonify({"error": "Session not found"}), 404
        if not session.is_active:
            return jsonify({"status": "Session already ended"})
        session.is_active = False
        session.end_time  = datetime.now(timezone.utc)
        db.commit()
        return jsonify({"status": "Session ended"})
    finally:
        db.close()

@app.route('/activeSession')
def active_session():
    course_id = request.args.get("course_id")
    if not course_id:
        return jsonify({"error": "course_id required"}), 400

    db = SessionLocal()
    try:
        session = db.query(Session).filter(
            Session.course_id == int(course_id),
            Session.is_active == True
        ).first()
        if not session:
            return jsonify({"error": "No active session"}), 404
        return jsonify({
            "session_id": session.id,
            "major":      session.major,
            "mode":       session.mode,
        })
    finally:
        db.close()

# ── QR ────────────────────────────────────────────────────────────────────────

def generate_qr(session_id, major):
    payload = {
        "session_id": session_id,
        "major":      major,
        "exp":        datetime.now(timezone.utc) + timedelta(seconds=30)
    }
    return jwt.encode(payload, SECRET, algorithm="HS256")

@app.route('/getQR/<int:session_id>')
def get_qr(session_id):
    db = SessionLocal()
    session = db.query(Session).filter(Session.id == session_id).first()
    db.close()

    if not session:
        return jsonify({"error": "Session not found"}), 404
    if session.mode == "ble":
        return jsonify({"error": "QR not enabled for this session"}), 400

    return jsonify({"qr": generate_qr(session_id, session.major)})

# ── Attendance ────────────────────────────────────────────────────────────────

@app.route('/markAttendance', methods=['POST'])
def mark_attendance():
    db = SessionLocal()
    try:
        data       = request.json
        session_id = data.get("session_id")
        student_id = data.get("student_id")
        token      = data.get("token")
        major      = data.get("major")
        minor      = data.get("minor")

        if not session_id or not student_id:
            return jsonify({"error": "session_id and student_id required"}), 400

        session = db.query(Session).filter(Session.id == session_id).first()
        if not session:
            return jsonify({"error": "Session not found"}), 404
        if not session.is_active:
            return jsonify({"error": "Session is no longer active"}), 400

        if session.mode == "qr" and not token:
            return jsonify({"error": "QR required for this session"}), 400

        if session.mode in ["ble", "hybrid"]:
            if not minor:
                return jsonify({"error": "minor required for this session"}), 400
            valid_mapping = db.query(Classroom2Beacon).filter(
                Classroom2Beacon.classroom_id == session.classroom_id,
                Classroom2Beacon.major == major
            ).first()
            if not valid_mapping:
                return jsonify({"error": "Invalid classroom beacon"}), 400
            beacon = db.query(BeaconLog).filter(
                BeaconLog.major == major,
                BeaconLog.minor == int(minor),
                BeaconLog.timestamp >= datetime.now(timezone.utc) - timedelta(seconds=30)
            ).first()
            if not beacon:
                return jsonify({"error": "Invalid beacon"}), 400

        existing = db.query(Attendance).filter(
            Attendance.session_id == session.id,
            Attendance.student_id == student_id
        ).first()
        if existing:
            return jsonify({"message": "Already marked"})

        attendance = Attendance(
            session_id=session.id,
            student_id=student_id,
            timestamp=datetime.now(timezone.utc)
        )
        db.add(attendance)
        db.commit()
        return jsonify({"status": "Attendance marked"})
    finally:
        db.close()

@app.route('/manualAttendance', methods=['POST'])
def manual_attendance():
    db = SessionLocal()
    try:
        data       = request.json
        session_id = data.get("session_id")
        student_id = data.get("student_id")

        if not session_id or not student_id:
            return jsonify({"error": "session_id and student_id required"}), 400

        session = db.query(Session).filter(Session.id == session_id).first()
        if not session:
            return jsonify({"error": "Session not found"}), 404
        if not session.is_active:
            return jsonify({"error": "Session is no longer active"}), 400

        existing = db.query(Attendance).filter(
            Attendance.session_id == session_id,
            Attendance.student_id == student_id
        ).first()
        if existing:
            return jsonify({"message": "Already marked"})

        attendance = Attendance(
            session_id=session_id,
            student_id=student_id,
            timestamp=datetime.now(timezone.utc)
        )
        db.add(attendance)
        db.commit()
        return jsonify({"status": "Attendance marked manually"})
    finally:
        db.close()

@app.route('/manualAttendance/bulk', methods=['POST'])
def manual_attendance_bulk():
    db = SessionLocal()
    try:
        data        = request.json
        session_id  = data.get("session_id")
        student_ids = data.get("student_ids", [])

        if not session_id:
            return jsonify({"error": "session_id required"}), 400

        session = db.query(Session).filter(Session.id == session_id).first()
        if not session or not session.is_active:
            return jsonify({"error": "Invalid session"}), 400

        added = 0
        for sid in student_ids:
            exists = db.query(Attendance).filter(
                Attendance.session_id == session_id,
                Attendance.student_id == sid
            ).first()
            if not exists:
                db.add(Attendance(
                    session_id=session_id,
                    student_id=sid,
                    timestamp=datetime.now(timezone.utc)
                ))
                added += 1

        db.commit()
        return jsonify({"added": added})
    finally:
        db.close()

@app.route('/attendance/<int:session_id>')
def get_attendance(session_id):
    db = SessionLocal()
    try:
        records = db.query(Attendance).filter(
            Attendance.session_id == session_id
        ).order_by(Attendance.timestamp.desc()).all()

        return jsonify([
            {"student_id": r.student_id, "timestamp": r.timestamp.strftime("%Y-%m-%dT%H:%M:%SZ")}
            for r in records
        ])
    finally:
        db.close()

# ── Courses ───────────────────────────────────────────────────────────────────

@app.route('/courses/<int:prof_id>')
def get_courses(prof_id):
    db = SessionLocal()
    try:
        courses = db.query(Course).filter(Course.professor_id == prof_id).all()
        return jsonify([{"id": c.id, "name": c.name} for c in courses])
    finally:
        db.close()

@app.route('/course/<int:course_id>/students')
def get_course_students(course_id):
    db = SessionLocal()
    try:
        students = db.query(User).join(
            Enrollment, Enrollment.student_id == User.id
        ).filter(
            Enrollment.course_id == course_id,
            User.role == "student"
        ).distinct(User.id).all()

        return jsonify([{"id": s.id, "name": s.name} for s in students])
    finally:
        db.close()

# ── Analytics ─────────────────────────────────────────────────────────────────

@app.route('/admin/stats')
def admin_stats():
    db = SessionLocal()
    try:
        sessions   = db.query(Session).count()
        attendance = db.query(Attendance).count()
        avg        = round(attendance / sessions, 2) if sessions else 0
        return jsonify({
            "sessions":       sessions,
            "attendance":     attendance,
            "avg_attendance": avg,
        })
    finally:
        db.close()

@app.route('/analytics/course/<int:course_id>')
def course_analytics(course_id):
    db = SessionLocal()
    try:
        sessions = db.query(Session).filter(Session.course_id == course_id).all()
        enrolled = db.query(Enrollment).filter(Enrollment.course_id == course_id).count()
        result   = []

        for s in sessions:
            records = db.query(Attendance).filter(Attendance.session_id == s.id).all()
            unique  = len(set(r.student_id for r in records))
            result.append({
                "session_id":      s.id,
                "start_time":      s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
                "end_time":        s.end_time.strftime("%Y-%m-%dT%H:%M:%SZ")   if s.end_time   else None,
                "mode":            s.mode,
                "is_active":       s.is_active,
                "total_marks":     len(records),
                "unique_students": unique,
            })

        return jsonify({
            "course_id": course_id,
            "enrolled":  enrolled,
            "sessions":  result,
        })
    finally:
        db.close()

@app.route('/analytics/prof/<int:prof_id>')
def prof_analytics(prof_id):
    db = SessionLocal()
    try:
        courses = db.query(Course).filter(Course.professor_id == prof_id).all()
        data    = []

        for c in courses:
            sessions  = db.query(Session).filter(Session.course_id == c.id).all()
            enrolled  = db.query(Enrollment).filter(Enrollment.course_id == c.id).count()
            total_att = 0
            for s in sessions:
                total_att += db.query(Attendance).filter(Attendance.session_id == s.id).count()
            data.append({
                "course_id":   c.id,
                "course_name": c.name,
                "sessions":    len(sessions),
                "attendance":  total_att,
                "enrolled":    enrolled,
                "avg":         round(total_att / len(sessions), 1) if sessions else 0,
            })

        return jsonify(data)
    finally:
        db.close()


# ── Student history heatmap ────────────────────────────────────────────────────

@app.route('/student/<int:student_id>/history/<int:course_id>')
def student_course_history(student_id, course_id):
    db = SessionLocal()
    try:
        sessions = db.query(Session).filter(
            Session.course_id == course_id
        ).order_by(Session.start_time.asc()).all()
        attended_ids = set(
            r.session_id for r in db.query(Attendance).filter(
                Attendance.student_id == student_id,
                Attendance.session_id.in_([s.id for s in sessions])
            ).all()
        )
        result = []
        for s in sessions:
            result.append({
                "session_id": s.id,
                "start_time": s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
                "attended":   s.id in attended_ids,
            })
        return jsonify({
            "course_id":  course_id,
            "student_id": student_id,
            "total":      len(sessions),
            "attended":   len(attended_ids),
            "sessions":   result,
        })
    finally:
        db.close()


# ── At-risk students (< 25% attendance) ───────────────────────────────────────

@app.route('/analytics/at-risk/<int:prof_id>')
def at_risk_students(prof_id):
    db = SessionLocal()
    try:
        courses = db.query(Course).filter(Course.professor_id == prof_id).all()
        at_risk = []
        for c in courses:
            sessions = db.query(Session).filter(Session.course_id == c.id).all()
            if not sessions:
                continue
            total_sessions = len(sessions)
            session_ids    = [s.id for s in sessions]
            enrollments    = db.query(Enrollment).filter(Enrollment.course_id == c.id).all()
            for enroll in enrollments:
                sid = enroll.student_id
                attended_sessions = len(set(
                    r.session_id for r in db.query(Attendance).filter(
                        Attendance.student_id == sid,
                        Attendance.session_id.in_(session_ids)
                    ).all()
                ))
                pct = (attended_sessions / total_sessions) * 100 if total_sessions > 0 else 0
                if pct < 25:
                    student = db.query(User).filter(User.id == sid).first()
                    at_risk.append({
                        "student_id":   sid,
                        "student_name": student.name if student else f"Student #{sid}",
                        "course_id":    c.id,
                        "course_name":  c.name,
                        "attended":     attended_sessions,
                        "total":        total_sessions,
                        "pct":          round(pct, 1),
                    })
        at_risk.sort(key=lambda x: x["pct"])
        return jsonify(at_risk)
    finally:
        db.close()


# ── Admin: list all sessions (FIX: was commented out, breaking View Sessions) ─

@app.route('/admin/sessions')
def admin_sessions():
    db = SessionLocal()
    try:
        sessions = db.query(Session).order_by(Session.start_time.desc()).all()
        result   = []
        for s in sessions:
            course = db.query(Course).filter(Course.id == s.course_id).first()
            prof   = db.query(User).filter(User.id == course.professor_id).first() if course else None
            marks  = db.query(Attendance).filter(Attendance.session_id == s.id).all()
            unique = len(set(r.student_id for r in marks))
            result.append({
                "session_id":      s.id,
                "course_id":       s.course_id,
                "course_name":     course.name if course else "—",
                "prof_name":       prof.name if prof else "—",
                "start_time":      s.start_time.strftime("%Y-%m-%dT%H:%M:%SZ") if s.start_time else None,
                "end_time":        s.end_time.strftime("%Y-%m-%dT%H:%M:%SZ")   if s.end_time   else None,
                "mode":            s.mode,
                "is_active":       s.is_active,
                "total_marks":     len(marks),
                "unique_students": unique,
            })
        return jsonify(result)
    finally:
        db.close()


# ── Admin: list / create / delete users ───────────────────────────────────────

@app.route('/admin/users', methods=['GET'])
def admin_list_users():
    db = SessionLocal()
    try:
        users = db.query(User).order_by(User.role, User.name).all()
        return jsonify([
            {"id": u.id, "name": u.name, "email": u.email, "role": u.role}
            for u in users
        ])
    finally:
        db.close()


@app.route('/admin/users', methods=['POST'])
def admin_create_user():
    db   = SessionLocal()
    data = request.json
    try:
        name     = data.get("name", "").strip()
        email    = data.get("email", "").strip()
        password = data.get("password", "").strip()
        role     = data.get("role", "student").strip()

        if not name or not email or not password:
            return jsonify({"error": "name, email, and password are required"}), 400
        if role not in ("student", "prof", "admin"):
            return jsonify({"error": "role must be student, prof, or admin"}), 400

        exists = db.query(User).filter(User.email == email).first()
        if exists:
            return jsonify({"error": "Email already in use"}), 409

        user = User(name=name, email=email, password=password, role=role)
        db.add(user)
        db.commit()
        return jsonify({"id": user.id, "name": user.name, "email": user.email, "role": user.role}), 201
    finally:
        db.close()


@app.route('/admin/users/<int:user_id>', methods=['DELETE'])
def admin_delete_user(user_id):
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.id == user_id).first()
        if not user:
            return jsonify({"error": "User not found"}), 404
        db.delete(user)
        db.commit()
        return jsonify({"status": "deleted"})
    finally:
        db.close()


# ── Admin: system-wide analytics breakdown ────────────────────────────────────

@app.route('/admin/analytics')
def admin_analytics():
    db = SessionLocal()
    try:
        all_profs   = db.query(User).filter(User.role == "prof").all()
        all_courses = db.query(Course).all()
        total_sess  = db.query(Session).count()
        total_att   = db.query(Attendance).count()
        total_stud  = db.query(User).filter(User.role == "student").count()
        avg_global  = round(total_att / total_sess, 2) if total_sess else 0

        profs_data = []
        for prof in all_profs:
            courses      = db.query(Course).filter(Course.professor_id == prof.id).all()
            courses_data = []
            for c in courses:
                sessions   = db.query(Session).filter(Session.course_id == c.id).order_by(Session.start_time.desc()).all()
                enrolled   = db.query(Enrollment).filter(Enrollment.course_id == c.id).count()
                course_att = 0
                for s in sessions:
                    marks       = db.query(Attendance).filter(Attendance.session_id == s.id).all()
                    course_att += len(set(r.student_id for r in marks))
                avg_pct = round((course_att / (len(sessions) * enrolled)) * 100, 1) \
                    if sessions and enrolled else 0
                courses_data.append({
                    "course_id":  c.id,
                    "name":       c.name,
                    "sessions":   len(sessions),
                    "enrolled":   enrolled,
                    "attendance": course_att,
                    "avg_pct":    avg_pct,
                })
            profs_data.append({
                "prof_id":   prof.id,
                "prof_name": prof.name,
                "courses":   courses_data,
            })

        return jsonify({
            "totals": {
                "sessions":   total_sess,
                "attendance": total_att,
                "avg":        avg_global,
                "students":   total_stud,
                "courses":    len(all_courses),
                "profs":      len(all_profs),
            },
            "profs": profs_data,
        })
    finally:
        db.close()


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=4040)