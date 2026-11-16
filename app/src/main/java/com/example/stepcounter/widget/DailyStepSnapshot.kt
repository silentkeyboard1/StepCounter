package com.example.stepcounter.widget

import android.content.Context
import android.util.AtomicFile
import java.io.File
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeParseException
import org.json.JSONException
import org.json.JSONObject

internal data class DailyStepData(
    val date: LocalDate,
    val steps: Long,
    val goal: Int
) {
    init {
        require(steps >= 0L) { "Step count cannot be negative." }
        require(goal > 0) { "Daily goal must be greater than zero." }
    }

    val progress: Int
        get() = steps.coerceAtMost(goal.toLong()).toInt()
}

internal object DailyStepSnapshot {

    @Synchronized
    fun read(context: Context, today: LocalDate): DailyStepData? {
        return try {
            val json = JSONObject(file(context).readFully().toString(Charsets.UTF_8))
            val date = LocalDate.parse(json.getString("date"))
            val steps = json.getLong("steps")
            val goal = json.getInt("goal")

            if (date == today && steps >= 0L && goal > 0) {
                DailyStepData(date, steps, goal)
            } else {
                null
            }
        } catch (_: IOException) {
            null
        } catch (_: JSONException) {
            null
        } catch (_: DateTimeParseException) {
            null
        }
    }

    @Synchronized
    fun write(context: Context, data: DailyStepData) {
        val bytes = JSONObject()
            .put("date", data.date.toString())
            .put("steps", data.steps)
            .put("goal", data.goal)
            .toString()
            .toByteArray(Charsets.UTF_8)

        val file = file(context)
        val stream = file.startWrite()

        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (e: IOException) {
            file.failWrite(stream)
            throw e
        }
    }

    @Synchronized
    fun writeIfUnchanged(
        context: Context,
        previous: DailyStepData?,
        updated: DailyStepData
    ): Boolean {
        if (
            updated.date != LocalDate.now() ||
            previous == updated ||
            read(context, updated.date) != previous
        ) {
            return false
        }

        write(context, updated)
        return true
    }

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.noBackupFilesDir, "daily_steps_widget.json"))
}