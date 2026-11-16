package com.example.stepcounter.widget

import android.animation.ValueAnimator
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
import android.graphics.Rect
import android.os.Bundle
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.widget.RemoteViews
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import java.time.LocalDate
import java.util.concurrent.Executors
import kotlin.math.roundToInt
import kotlinx.coroutines.delay

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

        internal suspend fun animateUpdate(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            previous: DailyStepData?,
            updated: DailyStepData
        ) {
            if (ids.isEmpty()) return

            val screenIsOn =
                context.getSystemService(PowerManager::class.java)?.isInteractive == true

            if (
                previous == null ||
                previous.date != updated.date ||
                previous.goal != updated.goal ||
                previous.steps >= updated.steps ||
                !screenIsOn ||
                !ValueAnimator.areAnimatorsEnabled()
            ) {
                render(context, manager, ids, expected = updated)
                return
            }

            val durationMillis = 480L
            val frameIntervalMillis = 40L
            val frameCount = (durationMillis / frameIntervalMillis).toInt()
            val startedAt = SystemClock.uptimeMillis()

            try {
                for (frame in 1 until frameCount) {
                    val frameTime = startedAt + frame * frameIntervalMillis
                    val waitTime = frameTime - SystemClock.uptimeMillis()

                    if (waitTime > 0L) {
                        delay(waitTime)
                    }

                    val elapsed = SystemClock.uptimeMillis() - startedAt
                    if (elapsed >= durationMillis) break

                    val time = elapsed.toFloat() / durationMillis
                    val fraction = time * time * (3f - 2f * time)

                    if (!render(context, manager, ids, previous, updated, fraction)) {
                        return
                    }
                }

                val remaining = durationMillis - (SystemClock.uptimeMillis() - startedAt)
                if (remaining > 0L) {
                    delay(remaining)
                }
            } finally {
                render(context, manager, ids, expected = updated)
            }
        }

        @Synchronized
        internal fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray,
            previous: DailyStepData? = null,
            expected: DailyStepData? = null,
            fraction: Float = 1f
        ): Boolean {
            if (ids.isEmpty()) return false

            val data = DailyStepSnapshot.read(context, LocalDate.now())

            if (expected != null && data != expected) return false

            val stepsText = data?.steps?.toString() ?: "—"
            val goalText = data?.goal?.let { "/$it" }.orEmpty()
            val targetProgress = data?.progress ?: 0

            val progress = if (previous != null) {
                val change = targetProgress - previous.progress
                (previous.progress + change * fraction.toDouble()).roundToInt()
            } else {
                targetProgress
            }

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
                    context = context,
                    options = manager.getAppWidgetOptions(id),
                    steps = stepsText,
                    goal = goalText,
                    previousSteps = previous?.steps?.toString(),
                    fraction = fraction
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
                        progress,
                        false
                    )
                }

                manager.updateAppWidget(id, views)
            }

            return true
        }

        private fun drawNumbers(
            context: Context,
            options: Bundle,
            steps: String,
            goal: String,
            previousSteps: String?,
            fraction: Float
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

            fun drawStepDigits(
                canvas: Canvas,
                paint: Paint,
                steps: String,
                previousSteps: String?,
                baseline: Float,
                fraction: Float
            ) {
                val digitWidth = ('0'..'9').maxOf { paint.measureText(it.toString()) }
                val previousDigits = previousSteps?.padStart(steps.length)
                val travel = canvas.height.toFloat()
                val bounds = Rect()

                for ((index, digit) in steps.withIndex()) {
                    val x = index * digitWidth
                    val oldDigit = previousDigits?.getOrNull(index)

                    fun drawDigit(value: Char, offsetY: Float) {
                        val text = value.toString()
                        paint.getTextBounds(text, 0, text.length, bounds)
                        canvas.drawText(text, x - bounds.left, baseline + offsetY, paint)
                    }

                    if (oldDigit == null || oldDigit == digit || fraction >= 1f) {
                        drawDigit(digit, 0f)
                        continue
                    }

                    if (oldDigit == ' ') {
                        drawDigit(digit, travel * (1f - fraction))
                        continue
                    }

                    val from = oldDigit.digitToInt()
                    val to = digit.digitToInt()
                    val distance = (to - from + 10) % 10
                    val position = from + distance * fraction
                    val current = position.toInt()
                    val offset = (position - current) * travel

                    val currentDigit = ('0'.code + current % 10).toChar()
                    val nextDigit = ('0'.code + (current + 1) % 10).toChar()

                    drawDigit(currentDigit, -offset)
                    drawDigit(nextDigit, travel - offset)
                }
            }

            fitText(goalPaint, goal, width * 0.4f, baseline, height - baseline)

            val goalBounds = Rect()
            goalPaint.getTextBounds(goal, 0, goal.length, goalBounds)

            val gap = if (goal.isEmpty()) 0f else 8f * density
            val availableStepsWidth = (width - goalBounds.width() - gap).coerceAtLeast(1f)

            val isStepCount = steps.all { it in '0'..'9' }
            val widestDigit = ('0'..'9').maxBy { stepsPaint.measureText(it.toString()) }
            val widthSample = if (isStepCount) {
                widestDigit.toString().repeat(steps.length)
            } else {
                steps
            }

            fitText(stepsPaint, widthSample, availableStepsWidth, baseline, height - baseline)

            if (isStepCount) {
                drawStepDigits(canvas, stepsPaint, steps, previousSteps, baseline, fraction)
            } else {
                val bounds = Rect()
                stepsPaint.getTextBounds(steps, 0, steps.length, bounds)
                canvas.drawText(steps, -bounds.left.toFloat(), baseline, stepsPaint)
            }

            if (goal.isNotEmpty()) {
                canvas.drawText(goal, width.toFloat() - goalBounds.right, baseline, goalPaint)
            }

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