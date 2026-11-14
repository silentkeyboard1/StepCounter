package com.example.stepcounter.widget

import android.content.Context
import android.util.AtomicFile
import android.util.Log
import java.io.File
import java.io.IOException
import java.time.YearMonth
import java.time.format.DateTimeParseException
import org.json.JSONException
import org.json.JSONObject

internal object MonthStepSnapshot {
    private const val TAG = "MonthlyStepsWidget"

    @Synchronized
    fun read(context: Context): MonthStepData? {
        val file = file(context)
        if (!file.baseFile.exists() && !File(file.baseFile.path + ".bak").exists()) {
            return null
        }

        return try {
            val json = JSONObject(file.readFully().toString(Charsets.UTF_8))
            val month = YearMonth.parse(json.getString("month"))
            val goal = json.getInt("goal")
            if (goal <= 0) return null

            val savedDays = json.getJSONObject("stepsByDay")
            val steps = buildMap {
                for (day in 1..month.lengthOfMonth()) {
                    if (!savedDays.has(day.toString())) continue
                    val count = savedDays.getLong(day.toString())
                    if (count >= 0L) put(day, count)
                }
            }
            MonthStepData(month, goal, steps)
        } catch (e: IOException) {
            Log.w(TAG, "Could not read the widget snapshot", e)
            null
        } catch (e: JSONException) {
            Log.w(TAG, "Could not decode the widget snapshot", e)
            null
        } catch (e: DateTimeParseException) {
            Log.w(TAG, "Invalid month in the widget snapshot", e)
            null
        }
    }

    @Synchronized
    fun write(context: Context, data: MonthStepData) {
        val counts = JSONObject()
        data.stepsByDay.forEach { (day, steps) -> counts.put(day.toString(), steps) }
        val json = JSONObject()
            .put("month", data.month.toString())
            .put("goal", data.goal)
            .put("stepsByDay", counts)

        val file = file(context)
        val output = file.startWrite()
        try {
            output.write(json.toString().toByteArray(Charsets.UTF_8))
            file.finishWrite(output)
        } catch (e: IOException) {
            file.failWrite(output)
            throw e
        }
    }

    @Synchronized
    fun clear(context: Context) {
        file(context).delete()
    }

    private fun file(context: Context): AtomicFile =
        AtomicFile(File(context.noBackupFilesDir, "monthly_steps_widget.json"))
}
