package com.example.stepcounter.data

import java.time.LocalDate

data class DailyStepCount(
    val date: LocalDate,
    val steps: Long
)
