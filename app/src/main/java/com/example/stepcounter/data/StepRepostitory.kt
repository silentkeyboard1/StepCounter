
package com.example.stepcounter.data

import java.time.LocalDate

interface StepRepository {

    suspend fun getTodaySteps(): Long

    suspend fun getStepsForDate(date: LocalDate): Long

    suspend fun getLastSevenDays(): List<DailyStepCount>
}
