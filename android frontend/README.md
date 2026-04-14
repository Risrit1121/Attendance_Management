# IITH Attendance (Jetpack Compose)

Android frontend prototype for a **Digital Intelligent Attendance Management System** with role-based UX:
- Student
- Professor
- Admin

## Open and run in Android Studio
1. Open Android Studio (Hedgehog/Iguna+ recommended).
2. **File → Open** and select this project folder (`KOTLIN_CODEX`).
3. Let Gradle sync finish.
4. Put your real app icon at: `app/src/main/res/drawable/iith_logo.png` (optional, a placeholder vector is already included).
5. Run on an emulator/device (API 26+).

## Important notes
- This project is fully Jetpack Compose (no XML layouts).
- All backend data/auth are mocked and marked with `// TODO: replace with API call`.
- Camera permission is included and requested at runtime for student QR/face steps.
- QR flow currently shows CameraX + ML Kit wiring and should be connected to your backend validation logic.
