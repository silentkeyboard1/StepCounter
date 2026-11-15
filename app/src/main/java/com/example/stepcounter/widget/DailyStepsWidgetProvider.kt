package com.example.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.util.Log
import android.widget.RemoteViews
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.math.roundToInt

class DailyStepsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        renderAsync(context, appWidgetManager, appWidgetIds)
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle
    ) {
        renderAsync(context, appWidgetManager, intArrayOf(appWidgetId))
    }

    private fun renderAsync(
        context: Context,
        manager: AppWidgetManager,
        ids: IntArray
    ) {
        val pendingResult = goAsync()

        executor.execute {
            try {
                render(context.applicationContext, manager, ids)
            } catch (e: Exception) {
                Log.w(TAG, "Could not render the daily steps widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "DailyStepsWidget"
        private val executor = Executors.newSingleThreadExecutor()

        internal fun ids(context: Context, manager: AppWidgetManager): IntArray =
            manager.getAppWidgetIds(
                ComponentName(context, DailyStepsWidgetProvider::class.java)
            )

        @Synchronized
        internal fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            if (ids.isEmpty()) return

            val data = DailyStepSnapshot.read(context, LocalDate.now())
            val stepsText = data?.steps?.toString() ?: "—"
            val goalText = data?.goal?.let { "/$it" }.orEmpty()

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            for (id in ids) {
                val numbers = drawNumbers(
                    context,
                    manager.getAppWidgetOptions(id),
                    stepsText,
                    goalText
                )

                val views = RemoteViews(context.packageName, R.layout.daily_steps_widget).apply {
                    setOnClickPendingIntent(R.id.daily_widget_root, openApp)
                    setImageViewBitmap(R.id.daily_widget_numbers, numbers)
                    setContentDescription(
                        R.id.daily_widget_numbers,
                        if (data == null) {
                            "No step data available for today"
                        } else {
                            "${data.steps} of ${data.goal} steps today"
                        }
                    )
                    setProgressBar(
                        R.id.daily_widget_progress,
                        data?.goal ?: 1,
                        data?.progress ?: 0,
                        false
                    )
                }

                manager.updateAppWidget(id, views)
            }
        }

        private fun drawNumbers(
            context: Context,
            options: Bundle,
            steps: String,
            goal: String
        ): Bitmap {
            val landscape =
                context.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

            val widthKey = if (landscape) {
                AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH
            }

            val heightKey = if (landscape) {
                AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT
            } else {
                AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT
            }

            val widgetWidth = options.getInt(widthKey).takeIf { it > 0 } ?: 250
            val widgetHeight = options.getInt(heightKey).takeIf { it > 0 } ?: 68
            val density = context.resources.displayMetrics.density.coerceAtMost(2f)

            // Account for the padding and progress bar in daily_steps_widget.xml.
            val width = ((widgetWidth - 20).coerceAtLeast(1) * density).roundToInt()
            val height = ((widgetHeight - 24).coerceAtLeast(1) * density).roundToInt()

            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val baseline = height * 0.82f

            val stepsPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                typeface = context.resources.getFont(R.font.oxanium_bold)
                textSize = height * 0.95f
            }

            val goalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                alpha = 153
                typeface = context.resources.getFont(R.font.oxanium_regular)
                textSize = height * 0.32f
            }

            fitText(goalPaint, goal, width * 0.4f, baseline, height - baseline)

            val goalWidth = goalPaint.measureText(goal)
            val gap = if (goal.isEmpty()) 0f else 8f * density
            val availableStepsWidth = (width - goalWidth - gap).coerceAtLeast(1f)

            fitText(stepsPaint, steps, availableStepsWidth, baseline, height - baseline)

            canvas.drawText(steps, 0f, baseline, stepsPaint)
            canvas.drawText(goal, width - goalWidth, baseline, goalPaint)

            return bitmap
        }

        private fun fitText(
            paint: Paint,
            text: String,
            maxWidth: Float,
            spaceAboveBaseline: Float,
            spaceBelowBaseline: Float
        ) {
            val scale = minOf(
                1f,
                maxWidth / paint.measureText(text).coerceAtLeast(1f),
                spaceAboveBaseline / (-paint.ascent()).coerceAtLeast(1f),
                spaceBelowBaseline / paint.descent().coerceAtLeast(1f)
            )

            paint.textSize *= scale
        }
    }
}