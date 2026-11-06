
package com.example.stepcounter.domain

data class DailyGoalProgress(
    val steps: Long,
    val goal: Int
) {

    init {
        require(steps >= 0) {
            "Step count cannot be negative."
        }

        require(goal > 0) {
            "Daily goal must be greater than zero."
        }
    }

    val percentage: Int
        get() = ((steps.toDouble() / goal) * 100).toInt()

    val remainingSteps: Long
        get() = (goal.toLong() - steps).coerceAtLeast(0L)

    val isCompleted: Boolean
        get() = steps >= goal

    val progressFraction: Float
        get() = (steps.toFloat() / goal).coerceIn(0f, 1f)
}
