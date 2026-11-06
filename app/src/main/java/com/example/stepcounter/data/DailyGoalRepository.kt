
package com.example.stepcounter.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.first

private val Context.goalDataStore by preferencesDataStore(
    name = "goal_settings"
)

class DailyGoalRepository(context: Context) {

    private val dataStore = context.applicationContext.goalDataStore

    companion object {
        private val DAILY_GOAL_KEY = intPreferencesKey("daily_goal")

        const val DEFAULT_DAILY_GOAL = 10_000
    }

    suspend fun getDailyGoal(): Int {

        val preferences = dataStore.data.first()

        return preferences[DAILY_GOAL_KEY] ?: DEFAULT_DAILY_GOAL
    }

    suspend fun setDailyGoal(newGoal: Int) {

        require(newGoal > 0) {
            "Daily goal must be greater than zero."
        }

        dataStore.edit { preferences ->
            preferences[DAILY_GOAL_KEY] = newGoal
        }
    }
}
