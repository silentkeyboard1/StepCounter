package com.example.stepcounter.widget

import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToLong

internal data class MonthStepData(
    val month: YearMonth,
    val goal: Int,
    val stepsByDay: Map<Int, Long>
) {
    fun cells(today: LocalDate): List<MonthStepCell?> {
        val firstColumn = month.atDay(1).dayOfWeek.value - 1
        return List(42) { index ->
            val day = index - firstColumn + 1
            if (day !in 1..month.lengthOfMonth()) {
                null
            } else {
                val date = month.atDay(day)
                val steps = if (date.isAfter(today)) null else stepsByDay[day]
                MonthStepCell(date, steps, intensityOf(steps, goal))
            }
        }
    }

    fun stats(today: LocalDate): MonthStepStats {
        val measuredSteps = cells(today).mapNotNull { it?.steps }
        return MonthStepStats(
            best = measuredSteps.maxOrNull(),
            average = measuredSteps.takeIf { it.isNotEmpty() }?.average()?.roundToLong()
        )
    }
}

internal data class MonthStepCell(
    val date: LocalDate,
    val steps: Long?,
    val intensity: Int
)

internal data class MonthStepStats(val best: Long?, val average: Long?)

private fun intensityOf(steps: Long?, goal: Int): Int {
    if (steps == null || steps <= 0L) return 0
    if (steps >= goal) return 5

    // The multiplication is safe because steps is smaller than the positive Int goal.
    val percentage = steps * 100 / goal
    return when {
        percentage >= 80 -> 4
        percentage >= 50 -> 3
        percentage >= 30 -> 2
        else -> 1
    }
}
