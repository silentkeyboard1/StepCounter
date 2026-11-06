package com.example.stepcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.example.stepcounter.ui.theme.StepCounterTheme
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log

import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.lifecycleScope
import com.example.stepcounter.data.HealthConnectStepRepository
import kotlinx.coroutines.launch
import java.time.LocalDate
import com.example.stepcounter.data.DailyGoalRepository


class MainActivity : ComponentActivity() {

    private val stepPermission =
        HealthPermission.getReadPermission(StepsRecord::class)

    private val permissionLauncher = registerForActivityResult(
        PermissionController.createRequestPermissionResultContract()
    ) { grantedPermissions ->

        if (stepPermission in grantedPermissions) {
            lifecycleScope.launch {
                logWeeklySteps()
            }
        } else {
            Log.w("StepCounter", "Access not allowed")
        }
    }

    private fun checkAndReadSteps() {

        if (HealthConnectClient.getSdkStatus(this) !=
            HealthConnectClient.SDK_AVAILABLE
        ) {
            Log.e("StepCounter", "Health Connect not available")
            return
        }

        lifecycleScope.launch {
            try {
                val client = HealthConnectClient.getOrCreate(this@MainActivity)

                val grantedPermissions =
                    client.permissionController.getGrantedPermissions()

                if (stepPermission in grantedPermissions) {
                    logWeeklySteps()
                } else {
                    permissionLauncher.launch(setOf(stepPermission))
                }

            } catch (e: Exception) {
                Log.e(
                    "StepCounter",
                    "Error while checking permissions",
                    e
                )
            }
        }
    }




    private suspend fun logWeeklySteps() {

        try {
            val repository = HealthConnectStepRepository(this)

            val history = repository.getLastSevenDays()

            val goalRepository = DailyGoalRepository(applicationContext)

            val dailyGoal = goalRepository.getDailyGoal()

            val todaySteps = history.lastOrNull()?.steps ?: 0L

            val goalReached = todaySteps >= dailyGoal
            Log.d("StepCounter", "Daily goal: $dailyGoal")
            Log.d("StepCounter", "Goal reached: $goalReached")


            history.forEach { day ->
                Log.d(
                    "StepCounter",
                    "Steps on ${day.date}: ${day.steps}"
                )
            }

            val totalSteps = history.sumOf { it.steps }

            Log.d(
                "StepCounter",
                "Steps in last 7 days: $totalSteps"
            )

        } catch (e: Exception) {
            Log.e(
                "StepCounter",
                "Failed to read weekly step counts",
                e
            )
        }
    }




    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndReadSteps()

        val androidVersion = Build.VERSION.SDK_INT

        val extensionVersion =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                SdkExtensions.getExtensionVersion(
                    Build.VERSION_CODES.UPSIDE_DOWN_CAKE
                )
            } else {
                0
            }

        val stepTrackingSupported =
            androidVersion >= 34 && extensionVersion >= 20

        Log.d("StepCounter", "Android API: $androidVersion")
        Log.d("StepCounter", "SDK Extension: $extensionVersion")
        Log.d(
            "StepCounter",
            "Step tracking supported: $stepTrackingSupported"
        )

        enableEdgeToEdge()
        setContent {
            StepCounterTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Silas",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    StepCounterTheme {
        Greeting("Android")
    }
}