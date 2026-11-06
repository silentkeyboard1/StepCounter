
package com.example.stepcounter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

class PermissionsRationaleActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Privacy Notice",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text(
                    text = "StepCounter reads your step count " +
                            "from Health Connect to display your " +
                            "daily steps. During development, " +
                            "this prototype displays step data " +
                            "in Android Studio. No data is " +
                            "transmitted to an online service."
                )
            }
        }
    }
}
