
package com.example.stepcounter.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DailyGoalProgressTest {

    @Test
    fun zeroStepsMeansZeroProgress() {
        val progress = DailyGoalProgress(
            steps = 0,
            goal = 8000
        )

        assertEquals(0, progress.percentage)
        assertEquals(8000L, progress.remainingSteps)
        assertFalse(progress.isCompleted)
        assertEquals(0f, progress.progressFraction, 0.0001f)
    }

    @Test
    fun partialProgressIsCalculatedCorrectly() {
        val progress = DailyGoalProgress(
            steps = 6250,
            goal = 8000
        )

        assertEquals(78, progress.percentage)
        assertEquals(1750L, progress.remainingSteps)
        assertFalse(progress.isCompleted)
        assertEquals(0.78125f, progress.progressFraction, 0.0001f)
    }

    @Test
    fun reachingGoalMarksCompletion() {
        val progress = DailyGoalProgress(
            steps = 8000,
            goal = 8000
        )

        assertEquals(100, progress.percentage)
        assertEquals(0L, progress.remainingSteps)
        assertTrue(progress.isCompleted)
        assertEquals(1f, progress.progressFraction, 0.0001f)
    }

    @Test
    fun exceedingGoalKeepsProgressBarFull() {
        val progress = DailyGoalProgress(
            steps = 10000,
            goal = 8000
        )

        assertEquals(125, progress.percentage)
        assertEquals(0L, progress.remainingSteps)
        assertTrue(progress.isCompleted)
        assertEquals(1f, progress.progressFraction, 0.0001f)
    }

    @Test(expected = IllegalArgumentException::class)
    fun zeroGoalIsRejected() {
        DailyGoalProgress(
            steps = 100,
            goal = 0
        )
    }
}
