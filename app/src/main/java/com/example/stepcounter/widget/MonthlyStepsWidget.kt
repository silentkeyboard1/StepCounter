package com.example.stepcounter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateGroupByPeriodRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.example.stepcounter.data.DailyGoalRepository
import java.time.LocalDateTime
import java.time.Period
import java.time.YearMonth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

object MonthlyStepsWidget {
    private const val TAG = "MonthlyStepsWidget"
    private val updateMutex = Mutex()
    private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

    // Call from a lifecycle-aware coroutine while the Activity is resumed.
    suspend fun refresh(context: Context) {
        update(context) { appContext, manager, ids ->
            val available = HealthConnectClient.getSdkStatus(appContext) ==
                HealthConnectClient.SDK_AVAILABLE
            if (!available) {
                MonthStepSnapshot.clear(appContext)
                MonthlyStepsWidgetProvider.render(appContext, manager, ids)
                return@update
            }

            val client = HealthConnectClient.getOrCreate(appContext)
            if (readStepsPermission !in client.permissionController.getGrantedPermissions()) {
                MonthStepSnapshot.clear(appContext)
                MonthlyStepsWidgetProvider.render(appContext, manager, ids)
                return@update
            }

            val now = LocalDateTime.now()
            val month = YearMonth.from(now)
            val start = month.atDay(1).atStartOfDay()
            val stepsByDay = if (now.isAfter(start)) {
                client.aggregateGroupByPeriod(
                    AggregateGroupByPeriodRequest(
                        metrics = setOf(StepsRecord.COUNT_TOTAL),
                        timeRangeFilter = TimeRangeFilter.between(start, now),
                        timeRangeSlicer = Period.ofDays(1)
                    )
                ).mapNotNull { bucket ->
                    val date = bucket.startTime.toLocalDate()
                    val steps = bucket.result[StepsRecord.COUNT_TOTAL]
                    steps?.takeIf { it >= 0L && YearMonth.from(date) == month }
                        ?.let { date.dayOfMonth to it }
                }.toMap()
            } else {
                emptyMap()
            }

            val goal = DailyGoalRepository(appContext).getDailyGoal().coerceAtLeast(1)
            MonthStepSnapshot.write(appContext, MonthStepData(month, goal, stepsByDay))
            MonthlyStepsWidgetProvider.render(appContext, manager, ids)
        }
    }

    // Call only after the new goal was successfully saved to DataStore.
    suspend fun onGoalSaved(context: Context) {
        update(context) { appContext, manager, ids ->
            val snapshot = MonthStepSnapshot.read(appContext) ?: return@update
            val goal = DailyGoalRepository(appContext).getDailyGoal().coerceAtLeast(1)
            MonthStepSnapshot.write(appContext, snapshot.copy(goal = goal))
            MonthlyStepsWidgetProvider.render(appContext, manager, ids)
        }
    }

    private suspend fun update(
        context: Context,
        block: suspend (Context, AppWidgetManager, IntArray) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                updateMutex.withLock {
                    val appContext = context.applicationContext
                    val manager = AppWidgetManager.getInstance(appContext)
                    val ids = MonthlyStepsWidgetProvider.ids(appContext, manager)
                    if (ids.isNotEmpty()) block(appContext, manager, ids)
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Could not update the monthly widget", e)
        }
    }
}
