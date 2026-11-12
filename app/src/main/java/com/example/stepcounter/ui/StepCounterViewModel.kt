
package com.example.stepcounter.ui

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.stepcounter.data.DailyGoalRepository
import com.example.stepcounter.data.DailyStepCount
import com.example.stepcounter.data.StepRepository
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StepCounterUiState(
    val today: LocalDate = LocalDate.now(),
    val todaySteps: Long? = null,
    val dailyGoal: Int = 10_000,
    val cachedWeeks: Map<LocalDate, List<DailyStepCount>> = emptyMap()
)

class StepCounterViewModel(
    private val stepRepository: StepRepository,
    private val goalRepository: DailyGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepCounterUiState())
    val uiState: StateFlow<StepCounterUiState> = _uiState.asStateFlow()

    private var selectedWeek: LocalDate? = null
    private var weekLoadJob: Job? = null
    private var refreshJob: Job? = null

    fun selectWeek(weekStart: LocalDate) {

        // Avoid starting the same request twice.
        if (selectedWeek == weekStart) {
            if (
                _uiState.value.cachedWeeks.containsKey(weekStart) ||
                weekLoadJob?.isActive == true
            ) {
                return
            }
        }

        selectedWeek = weekStart

        // Stop loading a week that is no longer selected.
        weekLoadJob?.cancel()

        // Already loaded weeks do not need another request.
        if (_uiState.value.cachedWeeks.containsKey(weekStart)) {
            Log.d("StepCounter", "Week loaded from cache: $weekStart")
            return
        }

        weekLoadJob = viewModelScope.launch {

            try {
                Log.d("StepCounter", "Loading week: $weekStart")

                val days = stepRepository.getWeekSteps(weekStart)

                _uiState.update { currentState ->
                    currentState.copy(
                        cachedWeeks = currentState.cachedWeeks +
                                (weekStart to days)
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    "StepCounter",
                    "Failed to load week: $weekStart",
                    e
                )
            }
        }
    }

    fun refresh() {

        refreshJob?.cancel()

        refreshJob = viewModelScope.launch {

            try {
                val today = LocalDate.now()
                val previousDate = _uiState.value.today

                // Refresh the previously active week at midnight.
                if (today != previousDate) {

                    val previousWeekStart = previousDate.with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    )

                    weekLoadJob?.cancel()

                    _uiState.update { currentState ->
                        currentState.copy(
                            today = today,
                            todaySteps = null,
                            cachedWeeks = currentState.cachedWeeks -
                                    previousWeekStart
                        )
                    }

                    selectedWeek?.let { selectWeek(it) }
                }

                // Refresh only today's steps and the saved goal.
                val goal = goalRepository.getDailyGoal()
                val steps = stepRepository.getTodaySteps()

                _uiState.update { currentState ->
                    currentState.copy(
                        today = today,
                        todaySteps = steps,
                        dailyGoal = goal
                    )
                }

            } catch (e: CancellationException) {
                throw e

            } catch (e: Exception) {
                Log.e(
                    "StepCounter",
                    "Failed to refresh step data",
                    e
                )
            }
        }
    }


    suspend fun saveDailyGoal(newGoal: Int): Boolean {

        if (newGoal !in 1..1_000_000) {
            return false
        }

        return try {
            goalRepository.setDailyGoal(newGoal)

            _uiState.update { currentState ->
                currentState.copy(
                    dailyGoal = newGoal
                )
            }

            Log.d(
                "StepCounter",
                "Daily goal updated: $newGoal"
            )

            true

        } catch (e: CancellationException) {
            throw e

        } catch (e: Exception) {
            Log.e(
                "StepCounter",
                "Failed to save daily goal",
                e
            )

            false
        }
    }

    companion object {

        fun factory(
            steps: StepRepository,
            goals: DailyGoalRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {

                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(
                    modelClass: Class<T>
                ): T {
                    return StepCounterViewModel(
                        steps,
                        goals
                    ) as T
                }
            }
    }
}
