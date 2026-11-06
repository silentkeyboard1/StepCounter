
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

        val startOfDay = LocalDate.now()
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()

        val now = Instant.now()

        val response = client.aggregate(
            AggregateRequest(
                metrics = setOf(StepsRecord.COUNT_TOTAL),
                timeRangeFilter = TimeRangeFilter.between(
                    startOfDay,
                    now
                )
            )
        )

        return response[StepsRecord.COUNT_TOTAL] ?: 0L
    }
}
