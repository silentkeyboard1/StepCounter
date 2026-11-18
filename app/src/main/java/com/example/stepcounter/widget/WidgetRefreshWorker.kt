package com.example.stepcounter.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.util.Log
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.HealthConnectFeatures
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.health.connect.client.request.AggregateRequest
import androidx.health.connect.client.time.TimeRangeFilter
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.stepcounter.data.DailyGoalRepository
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext

        try {
            val manager = AppWidgetManager.getInstance(context)
            val dailyIds = DailyStepsWidgetProvider.ids(context, manager)
            val monthlyIds = MonthlyStepsWidgetProvider.ids(context, manager)

            if (dailyIds.isEmpty() && monthlyIds.isEmpty()) {
                return@withContext Result.success()
            }

            if (HealthConnectClient.getSdkStatus(context) != HealthConnectClient.SDK_AVAILABLE) {
                return@withContext Result.success()
            }

            val client = HealthConnectClient.getOrCreate(context)
            val backgroundReadStatus = client.features.getFeatureStatus(
                HealthConnectFeatures.FEATURE_READ_HEALTH_DATA_IN_BACKGROUND
            )

            if (backgroundReadStatus != HealthConnectFeatures.FEATURE_STATUS_AVAILABLE) {
                return@withContext Result.success()
            }

            val permissions = client.permissionController.getGrantedPermissions()
            val stepsPermission = HealthPermission.getReadPermission(StepsRecord::class)
            val backgroundPermission = HealthPermission.PERMISSION_READ_HEALTH_DATA_IN_BACKGROUND

            if (stepsPermission !in permissions || backgroundPermission !in permissions) {
                Log.d(TAG, "Widget refresh skipped because a permission is missing")
                return@withContext Result.success()
            }

            if (monthlyIds.isNotEmpty()) {
                MonthlyStepsWidget.refresh(context)
            }

            if (dailyIds.isNotEmpty()) {
                val now = ZonedDateTime.now()
                val today = now.toLocalDate()
                val previous = DailyStepSnapshot.read(context, today)
                val start = today.atStartOfDay(now.zone).toInstant()
                val end = now.toInstant()

                val steps = if (end.isAfter(start)) {
                    val result = client.aggregate(
                        AggregateRequest(
                            metrics = setOf(StepsRecord.COUNT_TOTAL),
                            timeRangeFilter = TimeRangeFilter.between(start, end)
                        )
                    )
                    result[StepsRecord.COUNT_TOTAL] ?: 0L
                } else {
                    0L
                }

                val goal = DailyGoalRepository(context).getDailyGoal()
                val updated = DailyStepData(date = today, steps = steps, goal = goal)

                if (DailyStepSnapshot.writeIfUnchanged(context, previous, updated)) {
                    DailyStepsWidgetProvider.animateUpdate(context, manager, dailyIds, previous, updated)
                }
                Log.d(TAG, "Daily widget refresh completed")
            }

            Result.success()
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Log.w(TAG, "Health Connect access was denied during widget refresh", e)
            Result.success()
        } catch (e: Exception) {
            Log.w(TAG, "Widget refresh failed", e)
            if (runAttemptCount < 2) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "WidgetRefreshWorker"
        private const val WORK_NAME = "stepcounter_widget_refresh"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(
                15, TimeUnit.MINUTES
            ).build()

            WorkManager.getInstance(context.applicationContext)
                .enqueueUniquePeriodicWork(WORK_NAME, ExistingPeriodicWorkPolicy.KEEP, request)
        }
    }
}