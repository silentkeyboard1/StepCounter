
package com.example.stepcounter.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import com.example.stepcounter.ui.theme.AppFontFamily

private const val MAX_DAILY_GOAL = 1_000_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGoalBottomSheet(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSaveGoal: suspend (Int) -> Boolean
) {
    val colors = MaterialTheme.colorScheme

    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true
    )

    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current

    var goalInput by rememberSaveable(currentGoal) {
        mutableStateOf(currentGoal.toString())
    }

    var isSaving by remember {
        mutableStateOf(false)
    }

    var saveFailed by remember {
        mutableStateOf(false)
    }

    val parsedGoal = goalInput.toIntOrNull()

    val isValidGoal = parsedGoal != null &&
            parsedGoal in 1..MAX_DAILY_GOAL

    ModalBottomSheet(
        onDismissRequest = {
            if (!isSaving) {
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = colors.surface,
        contentColor = colors.onSurface,
        scrimColor = Color.Black.copy(alpha = 0.4f),
        shape = RoundedCornerShape(
            topStart = 26.dp,
            topEnd = 26.dp
        ),
        dragHandle = {
            BottomSheetDefaults.DragHandle(
                color = colors.onSurfaceVariant.copy(alpha = 0.4f)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "EDIT DAILY GOAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                color = colors.onSurface
            )

            Text(
                text = "Set your daily target",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = colors.onSurface
            )

            OutlinedTextField(
                value = goalInput,
                onValueChange = { value ->
                    if (value.length <= 7 && value.all { it.isDigit() }) {
                        goalInput = value
                        saveFailed = false
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                label = {
                    Text("Steps per day")
                },
                textStyle = TextStyle(
                    fontFamily = AppFontFamily,
                    fontSize = 30.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface
                ),
                singleLine = true,
                enabled = !isSaving,
                isError = goalInput.isNotEmpty() && !isValidGoal,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(
                    onDone = {
                        keyboardController?.hide()
                    }
                ),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    focusedLabelColor = colors.primary,
                    unfocusedBorderColor = colors.onSurfaceVariant.copy(
                        alpha = 0.5f
                    ),
                    unfocusedLabelColor = colors.onSurfaceVariant
                )
            )

            if (saveFailed) {
                Text(
                    text = "Could not save your goal. Please try again.",
                    color = colors.error,
                    fontSize = 13.sp
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = {
                        scope.launch {
                            keyboardController?.hide()
                            sheetState.hide()

                            if (!sheetState.isVisible) {
                                onDismiss()
                            }
                        }
                    },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "cancel",
                        color = colors.onSurfaceVariant,
                        fontWeight = FontWeight.Bold
                    )
                }

                Button(
                    onClick = {
                        val newGoal = parsedGoal

                        if (newGoal != null && isValidGoal) {
                            isSaving = true
                            saveFailed = false

                            scope.launch {
                                val saved = onSaveGoal(newGoal)

                                if (saved) {
                                    keyboardController?.hide()
                                    sheetState.hide()
                                    onDismiss()
                                } else {
                                    saveFailed = true
                                    isSaving = false
                                }
                            }
                        }
                    },
                    enabled = isValidGoal && !isSaving,
                    modifier = Modifier
                        .weight(2f)
                        .height(56.dp),
                    shape = RoundedCornerShape(11.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isSaving) "saving..." else "save goal",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
            }
        }
    }
}