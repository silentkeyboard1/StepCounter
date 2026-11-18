package com.example.stepcounter.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.util.Log
import android.view.View
import android.widget.RemoteViews
import com.example.stepcounter.MainActivity
import com.example.stepcounter.R
import java.text.NumberFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.concurrent.Executors

class MonthlyStepsWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        val pendingResult = goAsync()

        executor.execute {
            try {
                render(context.applicationContext, appWidgetManager, appWidgetIds)
            } catch (e: Exception) {
                Log.w(TAG, "Could not render the monthly widget", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "MonthlyStepsWidget"
        private val executor = Executors.newSingleThreadExecutor()

        private val cellIds = intArrayOf(
            R.id.month_cell_00, R.id.month_cell_01, R.id.month_cell_02,
            R.id.month_cell_03, R.id.month_cell_04, R.id.month_cell_05,
            R.id.month_cell_06, R.id.month_cell_07, R.id.month_cell_08,
            R.id.month_cell_09, R.id.month_cell_10, R.id.month_cell_11,
            R.id.month_cell_12, R.id.month_cell_13, R.id.month_cell_14,
            R.id.month_cell_15, R.id.month_cell_16, R.id.month_cell_17,
            R.id.month_cell_18, R.id.month_cell_19, R.id.month_cell_20,
            R.id.month_cell_21, R.id.month_cell_22, R.id.month_cell_23,
            R.id.month_cell_24, R.id.month_cell_25, R.id.month_cell_26,
            R.id.month_cell_27, R.id.month_cell_28, R.id.month_cell_29,
            R.id.month_cell_30, R.id.month_cell_31, R.id.month_cell_32,
            R.id.month_cell_33, R.id.month_cell_34, R.id.month_cell_35,
            R.id.month_cell_36, R.id.month_cell_37, R.id.month_cell_38,
            R.id.month_cell_39, R.id.month_cell_40, R.id.month_cell_41
        )

        private val cellDrawables = intArrayOf(
            R.drawable.month_step_empty,
            R.drawable.month_step_under_30,
            R.drawable.month_step_from_30,
            R.drawable.month_step_from_50,
            R.drawable.month_step_from_80,
            R.drawable.month_step_goal
        )

        internal fun ids(
            context: Context,
            manager: AppWidgetManager
        ): IntArray = manager.getAppWidgetIds(
            ComponentName(context, MonthlyStepsWidgetProvider::class.java)
        )

        @Synchronized
        internal fun render(
            context: Context,
            manager: AppWidgetManager,
            ids: IntArray
        ) {
            if (ids.isEmpty()) return

            val today = LocalDate.now()
            val month = YearMonth.from(today)
            val snapshot = MonthStepSnapshot.read(context)
                ?.takeIf { it.month == month }
                ?: MonthStepData(month, 10_000, emptyMap())

            val numberFormat = NumberFormat.getIntegerInstance()
            val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.LONG)
            val stats = snapshot.stats(today)
            val missingValue = context.getString(R.string.month_widget_no_value)
            val best = stats.best?.let { numberFormat.format(it) }
            val average = stats.average?.let { numberFormat.format(it) }

            val openApp = PendingIntent.getActivity(
                context,
                0,
                Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                },
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val regularFont = context.resources.getFont(R.font.oxanium_regular)
            val boldFont = context.resources.getFont(R.font.oxanium_bold)

            val views = RemoteViews(context.packageName, R.layout.monthly_steps_widget).apply {
                setOnClickPendingIntent(R.id.month_widget_root, openApp)

                setImageViewBitmap(
                    R.id.month_best_value,
                    createStatBitmap(
                        context,
                        context.getString(R.string.month_widget_best),
                        best ?: missingValue,
                        regularFont,
                        boldFont
                    )
                )
                setContentDescription(
                    R.id.month_best_value,
                    best?.let { context.getString(R.string.month_widget_best_description, it) }
                        ?: context.getString(R.string.month_widget_best_missing)
                )

                setImageViewBitmap(
                    R.id.month_avg_value,
                    createStatBitmap(
                        context,
                        context.getString(R.string.month_widget_avg),
                        average ?: missingValue,
                        regularFont,
                        boldFont
                    )
                )
                setContentDescription(
                    R.id.month_avg_value,
                    average?.let { context.getString(R.string.month_widget_avg_description, it) }
                        ?: context.getString(R.string.month_widget_avg_missing)
                )

                snapshot.cells(today).forEachIndexed { index, cell ->
                    val viewId = cellIds[index]

                    if (cell == null) {
                        setViewVisibility(viewId, View.INVISIBLE)
                    } else {
                        val (date, steps, colorLevel) = cell
                        setViewVisibility(viewId, View.VISIBLE)
                        setImageViewResource(viewId, cellDrawables[colorLevel])

                        val formattedDate = date.format(dateFormat)
                        val description = steps?.let {
                            context.getString(
                                R.string.month_widget_steps,
                                formattedDate,
                                numberFormat.format(it)
                            )
                        } ?: context.getString(R.string.month_widget_no_steps, formattedDate)

                        setContentDescription(viewId, description)
                    }
                }
            }

            manager.updateAppWidget(ids, views)
        }

        private fun createStatBitmap(
            context: Context,
            label: String,
            value: String,
            regularFont: Typeface,
            boldFont: Typeface
        ): Bitmap {
            val resources = context.resources
            val cellSize = resources.getDimensionPixelSize(R.dimen.month_widget_cell_size)
            val cellMargin = resources.getDimensionPixelSize(R.dimen.month_widget_cell_margin)
            val gridWidth = 7 * (cellSize + 2 * cellMargin)
            val width = (gridWidth - 2 * cellMargin) / 2
            val height = resources.getDimensionPixelSize(R.dimen.month_widget_stats_height)
            val valueTop = resources.getDimension(R.dimen.month_widget_stat_value_top)

            val bitmap = Bitmap.createBitmap(
                resources.displayMetrics,
                width,
                height,
                Bitmap.Config.ARGB_8888
            )
            val canvas = Canvas(bitmap)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = Color.WHITE
                isSubpixelText = true
            }

            paint.typeface = regularFont
            paint.textSize = resources.getDimension(R.dimen.month_widget_stat_label_size)
            drawLine(canvas, paint, label, top = 0f, bottom = valueTop)

            paint.typeface = boldFont
            paint.textSize = resources.getDimension(R.dimen.month_widget_stat_value_size)
            drawLine(canvas, paint, value, top = valueTop, bottom = height.toFloat())

            return bitmap
        }

        private fun drawLine(
            canvas: Canvas,
            paint: Paint,
            text: String,
            top: Float,
            bottom: Float
        ) {
            val metrics = paint.fontMetrics
            val lineHeight = metrics.bottom - metrics.top
            val textWidth = paint.measureText(text).coerceAtLeast(1f)
            val scale = minOf(
                1f,
                (canvas.width - 2f) / textWidth,
                (bottom - top) / lineHeight
            )

            paint.textSize *= scale

            val baseline = top - paint.fontMetrics.top
            canvas.drawText(text, 1f, baseline, paint)
        }
    }
}