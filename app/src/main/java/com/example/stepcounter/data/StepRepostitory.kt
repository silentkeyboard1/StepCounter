package com.example.stepcounter.data

interface StepRepository {
    suspend fun getTodaySteps(): Long
}
