package com.iith.attendanceapp

import android.util.Log
import com.google.mlkit.vision.face.Face

/**
 * Manages a sequence of random challenges and validates them against
 * face metrics streamed from the camera analyzer.
 *
 * Usage:
 *   val detector = LivenessDetector(onChallengeChanged, onComplete)
 *   detector.start()
 *   // In camera frame callback:
 *   detector.processFace(face)
 */
class LivenessDetector(
    private val onChallengeChanged: (challenge: LivenessChallenge, index: Int, total: Int) -> Unit,
    private val onComplete: (result: LivenessResult) -> Unit,
    private val challengeCount: Int = 2,
    private val challengeTimeoutMs: Long = 5000L
) {
    // ── Thresholds ────────────────────────────────────────────────────────────
    private val EYE_CLOSED_THRESHOLD = 0.15f
    private val EYE_OPEN_THRESHOLD   = 0.75f
    private val SMILE_THRESHOLD      = 0.75f
    private val TURN_ANGLE_THRESHOLD = 22f
    private val NOD_ANGLE_THRESHOLD  = 15f

    // ── State ─────────────────────────────────────────────────────────────────
    private var challenges: List<LivenessChallenge> = emptyList()
    private var currentIndex = 0
    private var isRunning = false
    private var challengeStartTime = 0L
    private val completedChallenges = mutableListOf<LivenessChallenge>()

    // Per-challenge transient state
    private var blinkEyesWereClosed = false
    private var neutralHeadY = 0f
    private var neutralHeadX = 0f
    private var neutralCaptured = false

    fun start() {
        challenges = buildChallengeList()
        currentIndex = 0
        completedChallenges.clear()
        isRunning = true
        advanceToChallenge(0)
    }

    fun stop() { isRunning = false }

    fun processFace(face: Face?) {
        if (!isRunning || challenges.isEmpty()) return

        val metrics = face?.toMetrics() ?: FaceMetrics(
            leftEyeOpenness = 1f, rightEyeOpenness = 1f,
            smileProbability = 0f, headEulerAngleY = 0f,
            headEulerAngleX = 0f, facePresent = false
        )

        // Per-challenge timeout
        val elapsed = System.currentTimeMillis() - challengeStartTime
        if (elapsed > challengeTimeoutMs) {
            isRunning = false
            onComplete(LivenessResult(
                isLive = false,
                completedChallenges = completedChallenges.toList(),
                failureReason = "Timeout on: ${challenges[currentIndex].instruction}"
            ))
            return
        }

        // Capture neutral head pose on first valid frame of each challenge
        if (metrics.facePresent && !neutralCaptured) {
            neutralHeadY = metrics.headEulerAngleY
            neutralHeadX = metrics.headEulerAngleX
            neutralCaptured = true
        }

        val passed = when (challenges[currentIndex]) {
            LivenessChallenge.BLINK      -> checkBlink(metrics)
            LivenessChallenge.SMILE      -> checkSmile(metrics)
            LivenessChallenge.TURN_LEFT  -> checkTurnLeft(metrics)
            LivenessChallenge.TURN_RIGHT -> checkTurnRight(metrics)
            LivenessChallenge.NOD        -> checkNod(metrics)
        }

        if (passed) {
            completedChallenges.add(challenges[currentIndex])
            Log.d("LivenessDetector", "Challenge passed: ${challenges[currentIndex]}")
            val next = currentIndex + 1
            if (next >= challenges.size) {
                isRunning = false
                onComplete(LivenessResult(isLive = true, completedChallenges = completedChallenges.toList()))
            } else {
                advanceToChallenge(next)
            }
        }
    }

    // ── Challenge checkers ────────────────────────────────────────────────────

    private fun checkBlink(m: FaceMetrics): Boolean {
        val eyesClosed = m.leftEyeOpenness < EYE_CLOSED_THRESHOLD && m.rightEyeOpenness < EYE_CLOSED_THRESHOLD
        val eyesOpen   = m.leftEyeOpenness > EYE_OPEN_THRESHOLD   && m.rightEyeOpenness > EYE_OPEN_THRESHOLD
        return if (eyesClosed) { blinkEyesWereClosed = true; false }
        else blinkEyesWereClosed && eyesOpen
    }

    private fun checkSmile(m: FaceMetrics) = m.smileProbability > SMILE_THRESHOLD

    private fun checkTurnLeft(m: FaceMetrics): Boolean {
        if (!neutralCaptured) return false
        return (neutralHeadY - m.headEulerAngleY) > TURN_ANGLE_THRESHOLD
    }

    private fun checkTurnRight(m: FaceMetrics): Boolean {
        if (!neutralCaptured) return false
        return (m.headEulerAngleY - neutralHeadY) > TURN_ANGLE_THRESHOLD
    }

    private fun checkNod(m: FaceMetrics): Boolean {
        if (!neutralCaptured) return false
        return Math.abs(m.headEulerAngleX - neutralHeadX) > NOD_ANGLE_THRESHOLD
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun advanceToChallenge(index: Int) {
        currentIndex = index
        challengeStartTime = System.currentTimeMillis()
        neutralCaptured = false
        blinkEyesWereClosed = false
        onChallengeChanged(challenges[index], index + 1, challenges.size)
    }

    private fun buildChallengeList(): List<LivenessChallenge> {
        // Always include BLINK (hardest to spoof) + random others
        val pool = LivenessChallenge.entries.toMutableList()
        val selected = mutableListOf(LivenessChallenge.BLINK)
        pool.remove(LivenessChallenge.BLINK)
        pool.remove(LivenessChallenge.TURN_RIGHT) // avoid both TURN_LEFT and TURN_RIGHT together
        selected += pool.shuffled().take(challengeCount - 1)
        return selected.shuffled()
    }

    private fun Face.toMetrics() = FaceMetrics(
        leftEyeOpenness  = leftEyeOpenProbability  ?: 1f,
        rightEyeOpenness = rightEyeOpenProbability ?: 1f,
        smileProbability = smilingProbability      ?: 0f,
        headEulerAngleY  = headEulerAngleY,
        headEulerAngleX  = headEulerAngleX,
        facePresent      = true
    )
}
