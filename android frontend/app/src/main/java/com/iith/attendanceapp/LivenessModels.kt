package com.iith.attendanceapp

enum class LivenessChallenge(val instruction: String) {
    BLINK("Please blink your eyes"),
    SMILE("Please smile"),
    TURN_LEFT("Please turn your head left"),
    TURN_RIGHT("Please turn your head right"),
    NOD("Please nod your head")
}

data class LivenessResult(
    val isLive: Boolean,
    val completedChallenges: List<LivenessChallenge>,
    val failureReason: String? = null
)

data class FaceMetrics(
    val leftEyeOpenness: Float,
    val rightEyeOpenness: Float,
    val smileProbability: Float,
    val headEulerAngleY: Float,
    val headEulerAngleX: Float,
    val facePresent: Boolean
)
