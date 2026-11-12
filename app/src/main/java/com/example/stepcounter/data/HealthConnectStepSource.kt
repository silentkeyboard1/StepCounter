
package com.example.stepcounter.data

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class HealthConnectStepRepository(
    context: Context
) : StepRepository {

    private val client = HealthConnectClient.getOrCreate(context)

    override suspend fun getTodaySteps(): Long {
        return getStepsForDate(LocalDate.now())
    }

    override suspend fun getStepsForDate(date: LocalDate): Long {

        val zone = ZoneId.systemDefault()

        val startOfDay = date
            .atStartOfDay(zone)
            .toInstant()

        val endOfDay = date
            .plusDays(1)
            .atStartOfDay(zone)
            .toInstant()

        val endTime = minOf(endOfDay, Instant.now())

        if (!endTime.isAfter(startOfDay)) {
            return 0L
        }

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startOfDay,
                    endTime
                )
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }

    override suspend fun getLastSevenDays(): List<DailyStepCount> {

        val today = LocalDate.now()

        val history = mutableListOf<DailyStepCount>()

        for (daysAgo in 6 downTo 0) {

            val date = today.minusDays(daysAgo.toLong())

            val steps = getStepsForDate(date)

            history.add(
                DailyStepCount(
                    date = date,
                    steps = steps
                )
            )
        }

        return history
    }

    override suspend fun getWeekSteps(
        weekStart: LocalDate
    ): List<DailyStepCount> {

        val today = LocalDate.now()

        return (0L..6L).map { offset ->

            val date = weekStart.plusDays(offset)

            val steps = if (date.isAfter(today)) {
                0L
            } else {
                getStepsForDate(date)
            }

            DailyStepCount(
                date = date,
                steps = steps
            )
        }
    }


}
