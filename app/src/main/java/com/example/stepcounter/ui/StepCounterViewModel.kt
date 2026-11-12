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
    val weekStart: LocalDate? = null,
    val weekSteps: List<DailyStepCount> = emptyList()
)

class StepCounterViewModel(
    private val stepRepository: StepRepository,
    private val goalRepository: DailyGoalRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StepCounterUiState())
    val uiState: StateFlow<StepCounterUiState> = _uiState.asStateFlow()

    private var selectedWeekStart: LocalDate =
        LocalDate.now().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private var loadJob: Job? = null

    fun selectWeek(weekStart: LocalDate) {
        if (selectedWeekStart == weekStart && _uiState.value.weekSteps.isNotEmpty()) {
            return
        }
        selectedWeekStart = weekStart
        _uiState.update { it.copy(weekStart = weekStart, weekSteps = emptyList()) }
        refresh()
    }

    fun refresh() {
        loadJob?.cancel()
        loadJob = viewModelScope.launch {
            try {
                val today = LocalDate.now()
                val goal = goalRepository.getDailyGoal()
                val days = stepRepository.getWeekSteps(selectedWeekStart)
                val todaysSteps = days.firstOrNull { it.date == today }?.steps
                    ?: stepRepository.getTodaySteps()

                _uiState.update {
                    it.copy(
                        today = today,
                        todaySteps = todaysSteps,
                        dailyGoal = goal,
                        weekStart = selectedWeekStart,
                        weekSteps = days
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e("StepCounter", "Failed to load step data", e)
                _uiState.update { it.copy(todaySteps = null, weekSteps = emptyList()) }
            }
        }
    }

    companion object {
        fun factory(
            steps: StepRepository,
            goals: DailyGoalRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return StepCounterViewModel(steps, goals) as T
            }
        }
    }
}
