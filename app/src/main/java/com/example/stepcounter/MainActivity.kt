package com.example.stepcounter

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.PermissionController
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.StepsRecord
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.stepcounter.data.DailyGoalRepository
import com.example.stepcounter.data.HealthConnectStepRepository
import com.example.stepcounter.ui.StepCounterScreen
import com.example.stepcounter.ui.StepCounterViewModel
import com.example.stepcounter.ui.theme.StepCounterTheme
import kotlinx.coroutines.delay

private val readStepsPermission = HealthPermission.getReadPermission(StepsRecord::class)

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            StepCounterTheme {
                val appContext = applicationContext
                val factory = remember(appContext) {
                    StepCounterViewModel.factory(
                        steps = HealthConnectStepRepository(appContext),
                        goals = DailyGoalRepository(appContext)
                    )
                }
                val screenViewModel: StepCounterViewModel = viewModel(factory = factory)
                val screenState by screenViewModel.uiState.collectAsStateWithLifecycle()
                var permissionGranted by remember { mutableStateOf(false) }

                val permissionLauncher = rememberLauncherForActivityResult(
                    PermissionController.createRequestPermissionResultContract()
                ) { grantedPermissions ->
                    permissionGranted = readStepsPermission in grantedPermissions
                    if (!permissionGranted) {
                        Log.w("StepCounter", "Step count permission denied")
                    }
                }

                LaunchedEffect(Unit) {
                    try {
                        if (HealthConnectClient.getSdkStatus(appContext) ==
                            HealthConnectClient.SDK_AVAILABLE
                        ) {
                            val client = HealthConnectClient.getOrCreate(appContext)
                            val grantedPermissions =
                                client.permissionController.getGrantedPermissions()
                            if (readStepsPermission in grantedPermissions) {
                                permissionGranted = true
                            } else {
                                permissionLauncher.launch(setOf(readStepsPermission))
                            }
                        } else {
                            Log.e("StepCounter", "Health Connect is unavailable")
                        }
                    } catch (e: Exception) {
                        Log.e("StepCounter", "Permission check failed", e)
                    }
                }

                val lifecycleOwner = LocalLifecycleOwner.current
                LaunchedEffect(permissionGranted, lifecycleOwner) {
                    if (permissionGranted) {
                        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
                            while (true) {
                                screenViewModel.refresh()
                                delay(60_000L)
                            }
                        }
                    }
                }

                StepCounterScreen(
                    state = screenState,
                    hasStepPermission = permissionGranted,
                    onWeekSelected = screenViewModel::selectWeek
                )
            }
        }
    }
}
