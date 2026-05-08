"""
MediaPipe Liveness v2 — fully aligned with Frontend Spec v2.0
Fixes applied vs v1:
  1. Server-side challenge contract validation (1 blink + 1 turn only)
  2. MIN_VALID_FRAMES raised to 30 (3s × 10fps — spec minimum)
  3. FPS bounds validation (rejects < 5 or > 60 fps inputs)
  4. Challenge window isolation (each challenge evaluated in its own time slice)
  5. Unmapped error strings renamed to match frontend "contains" map
  6. build_api_response() serialiser — output matches API contract exactly
  7. Server-side rate-limit hook (stateless; caller supplies storage backend)
"""

import cv2
import math
import time
import numpy as np
import mediapipe as mp
from dataclasses import dataclass, field
from typing import Any, Dict, List, Optional, Tuple


# ──────────────────────────────────────────────────────────────────────────────
#  Result container
# ──────────────────────────────────────────────────────────────────────────────
@dataclass
class LivenessResult:
    passed: bool
    best_frame: Optional[np.ndarray]
    final_confidence: float
    challenge_results: Dict[str, Any]
    failure_reasons: List[str]
    frame_coverage: float
    face_too_small: bool = False
    fps_used: float = 0.0


# ──────────────────────────────────────────────────────────────────────────────
#  Config — single place for every tunable
# ──────────────────────────────────────────────────────────────────────────────
class LivenessConfig:
    # ── Quality gates ──────────────────────────────────────────────────────────
    MIN_FACE_WIDTH_RATIO   = 0.15   # face must be ≥15 % of frame width
    MIN_FRAME_COVERAGE     = 0.60   # face found in ≥60 % of frames
    MIN_FRONTAL_FRAMES     = 5      # frontal frames needed for EAR baseline
    MIN_CONFIDENCE         = 0.40   # overall floor — below this → reject
    MIN_VALID_FRAMES       = 30     # absolute floor (3s × 10fps spec minimum)
    MAX_VALID_FRAMES       = 250    # hard ceiling (> this = wrong input)

    # ── FPS bounds (spec: 10–15 fps) ──────────────────────────────────────────
    FPS_MIN                = 5.0    # allow some tolerance below spec floor
    FPS_MAX                = 60.0   # reject suspiciously fast inputs

    # ── Challenge contract ─────────────────────────────────────────────────────
    VALID_CHALLENGES       = {"blink", "turn_left", "turn_right"}
    REQUIRED_BLINKS        = 1
    REQUIRED_TURNS         = 1      # exactly one of turn_left or turn_right
    MAX_CHALLENGES         = 2

    # ── Blink (seconds → scaled to frames at runtime) ─────────────────────────
    BLINK_MIN_DURATION_S   = 0.05   # ≥ 50 ms (below = noise spike)
    BLINK_MAX_DURATION_S   = 0.50   # ≤ 500 ms (above = photo / sleeping)
    BLINK_EAR_DROP         = 0.08   # minimum EAR drop below baseline
    BLINK_RECOVERY_RATIO   = 0.90   # eyes must reopen to ≥90 % of baseline

    # ── Head turn ─────────────────────────────────────────────────────────────
    TURN_MIN_DEGREES       = 15.0   # minimum yaw excursion
    TURN_MAX_SINGLE_JUMP   = 12.0   # max yaw Δ between consecutive frames
    TURN_RETURN_TOLERANCE  = 12.0   # must return within 12° of starting yaw
    TURN_BASELINE_FRACTION = 0.20   # first 20 % of window = baseline

    # ── Camera ────────────────────────────────────────────────────────────────
    ASSUMED_HFOV_DEG       = 70.0   # covers most phone cameras

    # ── EAR baseline (frontal-only) ────────────────────────────────────────────
    FRONTAL_YAW_THRESH     = 8.0
    FRONTAL_PITCH_THRESH   = 8.0


# ──────────────────────────────────────────────────────────────────────────────
#  Rate-limit hook (stateless — caller provides storage)
# ──────────────────────────────────────────────────────────────────────────────
class RateLimitExceeded(Exception):
    """Raised when a user exceeds the allowed failure streak."""
    def __init__(self, cooldown_remaining_s: float):
        self.cooldown_remaining_s = cooldown_remaining_s
        super().__init__(f"Rate limit exceeded. Try again in {cooldown_remaining_s:.0f}s")


def check_rate_limit(
    user_id: str,
    storage: Dict,             # pass any dict-like object (Redis, DynamoDB, etc.)
    max_consecutive_failures: int = 3,
    cooldown_s: float = 30.0,
) -> None:
    """
    Call BEFORE evaluate(). Raises RateLimitExceeded if the user is locked.

    `storage` contract:
        storage[user_id] = {"failures": int, "locked_until": float (epoch)}

    Example with a plain dict (in-process only, not production-safe):
        _store = {}
        check_rate_limit("user_123", _store)
    """
    now = time.time()
    rec = storage.get(user_id, {"failures": 0, "locked_until": 0.0})

    if rec["locked_until"] > now:
        raise RateLimitExceeded(rec["locked_until"] - now)

    # Stale lock that has expired — reset it
    if rec["locked_until"] > 0 and rec["locked_until"] <= now:
        rec = {"failures": 0, "locked_until": 0.0}
        storage[user_id] = rec


def record_attempt_result(
    user_id: str,
    passed: bool,
    storage: Dict,
    max_consecutive_failures: int = 3,
    cooldown_s: float = 30.0,
) -> None:
    """Call AFTER evaluate() with the result to update the failure counter."""
    now = time.time()
    rec = storage.get(user_id, {"failures": 0, "locked_until": 0.0})

    if passed:
        storage[user_id] = {"failures": 0, "locked_until": 0.0}
    else:
        rec["failures"] += 1
        if rec["failures"] >= max_consecutive_failures:
            rec["locked_until"] = now + cooldown_s
        storage[user_id] = rec


# ──────────────────────────────────────────────────────────────────────────────
#  API response builder — matches spec contract exactly
# ──────────────────────────────────────────────────────────────────────────────
def build_api_response(
    result: LivenessResult,
    face_similarity: Optional[float] = None,   # from downstream InsightFace step
) -> Dict:
    """
    Converts a LivenessResult into the exact JSON shape the frontend expects.

    Success (HTTP 200):
      { "status": "verified", "similarity": 0.82, "liveness_confidence": 0.95,
        "diagnostics": { "blink": { "passed": true, "confidence": 0.98, "reason": "ok" } } }

    Failure (HTTP 200):
      { "status": "liveness_failed", "reasons": [...],
        "challenge_results": { ... }, "liveness_confidence": 0.45 }
    """
    if result.passed:
        return {
            "status": "verified",
            "similarity": round(face_similarity, 4) if face_similarity is not None else None,
            "liveness_confidence": result.final_confidence,
            "diagnostics": {
                c: {
                    "passed":     v["passed"],
                    "confidence": v["confidence"],
                    "reason":     v["reason"],
                }
                for c, v in result.challenge_results.items()
            },
        }
    else:
        return {
            "status": "liveness_failed",
            "reasons": result.failure_reasons,
            "challenge_results": {
                c: {
                    "passed":     v["passed"],
                    "confidence": v["confidence"],
                    "reason":     v["reason"],
                }
                for c, v in result.challenge_results.items()
            },
            "liveness_confidence": result.final_confidence,
        }


# ──────────────────────────────────────────────────────────────────────────────
#  Core liveness class
# ──────────────────────────────────────────────────────────────────────────────
class MediaPipeLiveness:
    """
    Stateless, thread-safe liveness evaluator.
    No shared mutable state — safe for concurrent requests.
    """

    cfg = LivenessConfig()

    # ── Internal helpers ───────────────────────────────────────────────────────

    def _focal_length(self, w: int) -> float:
        """Focal length from assumed HFOV — more accurate than f=w on phone lenses."""
        half_angle = math.radians(self.cfg.ASSUMED_HFOV_DEG / 2.0)
        return w / (2.0 * math.tan(half_angle))

    def _get_ear(self, landmarks, w: int, h: int) -> float:
        def calc(indices):
            pts = [np.array([landmarks.landmark[i].x * w,
                             landmarks.landmark[i].y * h]) for i in indices]
            v1  = np.linalg.norm(pts[1] - pts[5])
            v2  = np.linalg.norm(pts[2] - pts[4])
            h_d = np.linalg.norm(pts[0] - pts[3])
            return (v1 + v2) / (2.0 * h_d) if h_d > 0 else 0.3

        left  = calc([33,  160, 158, 133, 153, 144])
        right = calc([362, 385, 387, 263, 373, 380])
        return (left + right) / 2.0

    def _get_pose(self, lm, w: int, h: int) -> Tuple[float, float]:
        model_pts = np.array([
            (0, 0, 0), (0, -330, -65),
            (-225, 170, -135), (225, 170, -135),
            (-150, -150, -125), (150, -150, -125),
        ], dtype=np.float64)

        image_pts = np.array([
            (lm.landmark[1].x   * w, lm.landmark[1].y   * h),
            (lm.landmark[152].x * w, lm.landmark[152].y * h),
            (lm.landmark[33].x  * w, lm.landmark[33].y  * h),
            (lm.landmark[263].x * w, lm.landmark[263].y * h),
            (lm.landmark[61].x  * w, lm.landmark[61].y  * h),
            (lm.landmark[291].x * w, lm.landmark[291].y * h),
        ], dtype=np.float64)

        f    = self._focal_length(w)
        cam  = np.array([[f, 0, w / 2], [0, f, h / 2], [0, 0, 1]], dtype=np.float64)
        dist = np.zeros((4, 1))

        ok, rvec, tvec = cv2.solvePnP(model_pts, image_pts, cam, dist,
                                      flags=cv2.SOLVEPNP_ITERATIVE)
        if not ok:
            return 0.0, 0.0

        rmat, _ = cv2.Rodrigues(rvec)
        proj    = np.hstack((rmat, tvec))
        angles  = cv2.decomposeProjectionMatrix(proj)[6]
        return float(angles[0, 0]), float(angles[1, 0])  # pitch, yaw

    def _face_width_ratio(self, lm, w: int) -> float:
        return abs(lm.landmark[263].x - lm.landmark[33].x)

    def _robust_ear_baseline(
        self,
        ear_series:   List[float],
        yaw_series:   List[float],
        pitch_series: List[float],
    ) -> float:
        frontal = [
            e for e, y, p in zip(ear_series, yaw_series, pitch_series)
            if abs(y) < self.cfg.FRONTAL_YAW_THRESH
            and abs(p) < self.cfg.FRONTAL_PITCH_THRESH
        ]
        if len(frontal) >= self.cfg.MIN_FRONTAL_FRAMES:
            return float(np.median(frontal))
        return float(np.median(ear_series))  # graceful fallback

    # ── Challenge contract validation ──────────────────────────────────────────

    def _validate_challenges(self, challenges: List[str]) -> Optional[str]:
        """
        Returns a failure reason string if the challenge list violates the spec,
        or None if valid.

        Rules (from frontend spec v2.0):
          - Exactly 2 challenges
          - Each challenge must be a known type
          - Exactly 1 blink and exactly 1 turn
        """
        if len(challenges) != self.cfg.MAX_CHALLENGES:
            return f"invalid_challenge_count_{len(challenges)}_expected_{self.cfg.MAX_CHALLENGES}"

        for c in challenges:
            if c not in self.cfg.VALID_CHALLENGES:
                return f"unknown_challenge_type_{c}"

        blinks = sum(1 for c in challenges if c == "blink")
        turns  = sum(1 for c in challenges if c in ("turn_left", "turn_right"))

        if blinks != self.cfg.REQUIRED_BLINKS:
            return f"invalid_blink_count_{blinks}_expected_{self.cfg.REQUIRED_BLINKS}"
        if turns != self.cfg.REQUIRED_TURNS:
            return f"invalid_turn_count_{turns}_expected_{self.cfg.REQUIRED_TURNS}"

        return None

    # ── Per-challenge evaluators ───────────────────────────────────────────────

    def _detect_blink(
        self,
        ear_w:   List[float],   # windowed EAR slice for THIS challenge
        yaw_w:   List[float],
        pitch_w: List[float],
        fps:     float,
    ) -> Tuple[bool, float, str]:
        ear = np.array(ear_w)
        n   = len(ear)

        if n < max(8, int(self.cfg.BLINK_MIN_DURATION_S * fps * 2)):
            return False, 0.0, "low_frame_coverage"

        baseline = self._robust_ear_baseline(ear_w, yaw_w, pitch_w)

        drop_mask   = (baseline - ear) > self.cfg.BLINK_EAR_DROP
        closed_idxs = np.where(drop_mask)[0]

        if len(closed_idxs) == 0:
            return False, 0.0, "no_eye_closure_detected"

        # Duration gate in seconds (spec-aligned)
        duration_frames = int(closed_idxs[-1]) - int(closed_idxs[0]) + 1
        min_f = max(1, int(self.cfg.BLINK_MIN_DURATION_S * fps))
        max_f = int(self.cfg.BLINK_MAX_DURATION_S * fps)

        if duration_frames < min_f:
            # Renamed from "blink_too_fast_likely_noise" to match frontend map
            return False, 0.0, "no_eye_closure_detected"
        if duration_frames > max_f:
            return False, 0.0, "eyes_held_closed_too_long"

        peak_drop = float(np.max(baseline - ear))
        recovery  = float(ear[-1]) > baseline * self.cfg.BLINK_RECOVERY_RATIO

        if not recovery:
            return False, 0.0, "eyes_did_not_reopen"

        confidence = min(1.0, peak_drop / 0.15)
        return True, confidence, "ok"

    def _detect_turn(
        self,
        yaw_w:     List[float],   # windowed yaw slice for THIS challenge
        direction: str,            # "left" | "right"
        fps:       float,
    ) -> Tuple[bool, float, str]:
        yaw = np.array(yaw_w)
        n   = len(yaw)

        if n < 8:
            return False, 0.0, "low_frame_coverage"

        # Smoothness gate — rejects video-replay cut artefacts
        deltas = np.abs(np.diff(yaw))
        if np.any(deltas > self.cfg.TURN_MAX_SINGLE_JUMP):
            worst = float(np.max(deltas))
            return False, 0.0, f"unnatural_yaw_jump_{worst:.1f}deg_likely_fake_video"

        n_base   = max(3, int(n * self.cfg.TURN_BASELINE_FRACTION))
        baseline = float(np.median(yaw[:n_base]))
        movement = yaw - baseline

        if direction == "left":
            excursion = float(np.min(movement))
            valid     = excursion < -self.cfg.TURN_MIN_DEGREES
            conf      = min(1.0, abs(excursion) / 25.0)
        else:
            excursion = float(np.max(movement))
            valid     = excursion > self.cfg.TURN_MIN_DEGREES
            conf      = min(1.0, abs(excursion) / 25.0)

        if not valid:
            return False, 0.0, f"insufficient_turn_{excursion:.1f}deg"

        # Return-to-center gate
        end_yaw = float(np.median(yaw[-3:]))
        if abs(end_yaw - baseline) > self.cfg.TURN_RETURN_TOLERANCE:
            return False, 0.0, "head_did_not_return_to_center"

        return True, conf, "ok"

    # ── Challenge window slicer ────────────────────────────────────────────────

    def _slice_windows(
        self,
        challenges:  List[str],
        n_frames:    int,
        fps:         float,
        duration_s:  float,
    ) -> List[slice]:
        """
        Maps each challenge to the time window in which the frontend prompted it.

        Frontend spec v2.0 timing:
          0s – 3s  → challenge 0
          3s – 6s  → challenge 1

        We use equal-sized windows by default, which aligns with the spec's
        symmetric 3s-per-challenge design. If the video is longer (v1 spec with
        1s stabilization prefix), the first window absorbs the extra time, which
        is safe — the stabilization frames are frontal and help the baseline.
        """
        n = len(challenges)
        step = n_frames // n
        return [
            slice(i * step, (i + 1) * step if i < n - 1 else n_frames)
            for i in range(n)
        ]

    # ── Main evaluate ──────────────────────────────────────────────────────────

    def evaluate(
        self,
        frames:           List[np.ndarray],
        challenges:       List[str],
        video_duration_s: float,
    ) -> LivenessResult:

        failure_reasons:   List[str]      = []
        challenge_results: Dict[str, Any] = {}

        # ── 1. Challenge contract gate ─────────────────────────────────────────
        contract_error = self._validate_challenges(challenges)
        if contract_error:
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={}, failure_reasons=[contract_error],
                frame_coverage=0.0,
            )

        # ── 2. Basic input gate ────────────────────────────────────────────────
        if not frames or video_duration_s <= 0:
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={}, failure_reasons=["no_frames_or_zero_duration"],
                frame_coverage=0.0,
            )

        n_input = len(frames)

        if n_input < self.cfg.MIN_VALID_FRAMES:
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={},
                failure_reasons=[f"low_frame_coverage"],
                frame_coverage=0.0,
            )

        if n_input > self.cfg.MAX_VALID_FRAMES:
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={},
                failure_reasons=["too_many_frames_possible_replay_attack"],
                frame_coverage=0.0,
            )

        # ── 3. FPS bounds gate ─────────────────────────────────────────────────
        fps = n_input / video_duration_s

        if fps < self.cfg.FPS_MIN or fps > self.cfg.FPS_MAX:
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={},
                failure_reasons=[f"invalid_fps_{fps:.1f}_expected_{self.cfg.FPS_MIN}-{self.cfg.FPS_MAX}"],
                frame_coverage=0.0,
                fps_used=round(fps, 2),
            )

        h, w, _ = frames[0].shape

        # ── 4. Per-frame feature extraction ───────────────────────────────────
        ear_series:    List[float] = []
        yaw_series:    List[float] = []
        pitch_series:  List[float] = []
        frame_indices: List[int]   = []  # original frame index (for window slicing)

        face_too_small_count = 0
        best_frame           = None
        best_center_score    = float("inf")

        with mp.solutions.face_mesh.FaceMesh(
            static_image_mode=False,
            max_num_faces=1,
            min_detection_confidence=0.6,
            min_tracking_confidence=0.6,
        ) as face_mesh:

            for idx, frame in enumerate(frames):
                res = face_mesh.process(frame)
                if not res.multi_face_landmarks:
                    continue

                lm = res.multi_face_landmarks[0]

                if self._face_width_ratio(lm, w) < self.cfg.MIN_FACE_WIDTH_RATIO:
                    face_too_small_count += 1
                    continue

                ear        = self._get_ear(lm, w, h)
                pitch, yaw = self._get_pose(lm, w, h)

                ear_series.append(ear)
                yaw_series.append(yaw)
                pitch_series.append(pitch)
                frame_indices.append(idx)

                center_score = abs(yaw) + abs(pitch)
                if center_score < best_center_score:
                    best_center_score = center_score
                    best_frame        = frame

        # ── 5. Coverage gates ──────────────────────────────────────────────────
        n_valid        = len(ear_series)
        frame_coverage = n_valid / max(1, n_input)
        face_too_small = face_too_small_count > n_input * 0.5

        if face_too_small:
            failure_reasons.append("face_too_small")

        if frame_coverage < self.cfg.MIN_FRAME_COVERAGE:
            failure_reasons.append(
                f"low_frame_coverage_{frame_coverage:.0%}"
            )

        if best_frame is None or n_valid < self.cfg.MIN_VALID_FRAMES:
            failure_reasons.append("low_frame_coverage")
            return LivenessResult(
                passed=False, best_frame=None, final_confidence=0.0,
                challenge_results={},
                failure_reasons=list(set(failure_reasons)),
                frame_coverage=round(frame_coverage, 3),
                face_too_small=face_too_small,
                fps_used=round(fps, 2),
            )

        # ── 6. Challenge window isolation ──────────────────────────────────────
        windows = self._slice_windows(challenges, n_valid, fps, video_duration_s)

        # ── 7. Per-challenge evaluation ────────────────────────────────────────
        total_conf = 0.0
        passed     = True

        for challenge, window in zip(challenges, windows):
            ear_w   = ear_series[window]
            yaw_w   = yaw_series[window]
            pitch_w = pitch_series[window]

            if challenge == "blink":
                ok, conf, reason = self._detect_blink(ear_w, yaw_w, pitch_w, fps)
            elif challenge in ("turn_left", "turn_right"):
                direction = "left" if challenge == "turn_left" else "right"
                ok, conf, reason = self._detect_turn(yaw_w, direction, fps)
            else:
                continue  # guarded by _validate_challenges — should never reach here

            challenge_results[challenge] = {
                "passed":     ok,
                "confidence": round(conf, 3),
                "reason":     reason,
            }
            if not ok:
                passed = False
                failure_reasons.append(f"{reason}")

            total_conf += conf

        # ── 8. Confidence floor ────────────────────────────────────────────────
        final_conf = total_conf / max(1, len(challenges))

        if final_conf < self.cfg.MIN_CONFIDENCE and passed:
            passed = False
            failure_reasons.append(
                f"confidence_{final_conf:.2f}_below_floor_{self.cfg.MIN_CONFIDENCE}"
            )

        # Propagate coverage failure
        if frame_coverage < self.cfg.MIN_FRAME_COVERAGE:
            passed = False

        return LivenessResult(
            passed=passed,
            best_frame=best_frame,
            final_confidence=round(final_conf, 3),
            challenge_results=challenge_results,
            failure_reasons=list(set(failure_reasons)),
            frame_coverage=round(frame_coverage, 3),
            face_too_small=face_too_small,
            fps_used=round(fps, 2),
        )


