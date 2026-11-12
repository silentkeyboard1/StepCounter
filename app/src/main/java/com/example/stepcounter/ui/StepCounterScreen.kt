
package com.example.stepcounter.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.stepcounter.data.DailyStepCount
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.stepcounter.ui.theme.AppFontFamily
import androidx.compose.foundation.layout.offset

private const val LAST_PAGE = 3
private const val WEEK_PAGE_COUNT = 4

@Composable
fun StepCounterScreen(
    state: StepCounterUiState,
    hasStepPermission: Boolean,
    onWeekSelected: (LocalDate) -> Unit,
    onSaveGoal: suspend (Int) -> Boolean,
    modifier: Modifier = Modifier
) {
    val colors = MaterialTheme.colorScheme

    var showGoalSheet by rememberSaveable {
        mutableStateOf(false)
    }

    val monday = remember(state.today) {
        state.today.with(
            TemporalAdjusters.previousOrSame(
                DayOfWeek.MONDAY
            )
        )
    }

    val pagerState = rememberPagerState(
        initialPage = LAST_PAGE,
        pageCount = { WEEK_PAGE_COUNT }
    )

    LaunchedEffect(
        pagerState.settledPage,
        monday,
        hasStepPermission
    ) {
        if (hasStepPermission) {
            val selectedWeek = monday.minusWeeks(
                (LAST_PAGE - pagerState.settledPage).toLong()
            )

            onWeekSelected(selectedWeek)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(horizontal = 20.dp)
            .padding(top = 12.dp, bottom = 12.dp)
    ) {

        TodayStepsCard(state)

        Spacer(Modifier.height(14.dp))

        WeeklyStatsCard(
            state = state,
            currentMonday = monday,
            pagerState = pagerState
        )

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {
                showGoalSheet = true
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp),
            shape = RoundedCornerShape(11.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.primary,
                contentColor = Color.White
            )
        ) {
            Text(
                text = "edit goal",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }

    if (showGoalSheet) {
        EditGoalBottomSheet(
            currentGoal = state.dailyGoal,
            onDismiss = {
                showGoalSheet = false
            },
            onSaveGoal = onSaveGoal
        )
    }
}

@Composable
private fun TodayStepsCard(
    state: StepCounterUiState
) {
    val colors = MaterialTheme.colorScheme

    val stepText = state.todaySteps?.toString() ?: "—"

    val stepFontSize = when {
        stepText.length >= 7 -> 43.sp
        stepText.length == 6 -> 54.sp
        else -> 66.sp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(108.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(
                horizontal = 10.dp,
                vertical = 9.dp
            ),
        verticalArrangement = Arrangement.SpaceBetween
    ) {

        Text(
            text = "TODAY’S STEPS",
            color = colors.onSurface,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )


        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stepText,
                modifier = Modifier
                    .weight(1f, fill = false)
                    .alignByBaseline(),
                style = TextStyle(
                    fontSize = stepFontSize,
                    lineHeight = 68.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = AppFontFamily,
                    letterSpacing = (-2).sp,
                    platformStyle = PlatformTextStyle(
                        includeFontPadding = false
                    )
                ),
                color = colors.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Clip
            )

            Text(
                text = "/${state.dailyGoal}",
                modifier = Modifier.alignByBaseline(),
                color = colors.onSurfaceVariant,
                fontSize = 20.sp,
                lineHeight = 23.sp
            )
        }

    }
}

@Composable
private fun WeeklyStatsCard(
    state: StepCounterUiState,
    currentMonday: LocalDate,
    pagerState: PagerState
) {
    val colors = MaterialTheme.colorScheme

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(225.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .padding(
                horizontal = 10.dp,
                vertical = 9.dp
            )
    ) {

        Text(
            text = "WEEKLY STATS",
            color = colors.onSurface,
            fontSize = 10.sp,
            lineHeight = 12.sp
        )

        Spacer(Modifier.height(22.dp))

        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->

            val weekStart = currentMonday.minusWeeks(
                (LAST_PAGE - page).toLong()
            )

            val days = state.cachedWeeks[weekStart].orEmpty()

            WeekBars(
                weekStart = weekStart,
                today = state.today,
                todaySteps = state.todaySteps,
                days = days,
                dailyGoal = state.dailyGoal
            )
        }
    }
}

@Composable
private fun WeekBars(
    weekStart: LocalDate,
    today: LocalDate,
    todaySteps: Long?,
    days: List<DailyStepCount>,
    dailyGoal: Int
) {
    val colors = MaterialTheme.colorScheme

    val trackColor = if (isSystemInDarkTheme()) {
        Color(0xFF212121)
    } else {
        Color(0xFFF0F0F0)
    }

    val stepCounts = remember(days) {
        days.associate { it.date to it.steps }
    }

    val goalScale = dailyGoal.coerceAtLeast(1).toFloat()

    Row(
        modifier = Modifier.fillMaxSize()
    ) {

        repeat(7) { index ->

            val date = weekStart.plusDays(index.toLong())

            val steps = if (date == today) {
                todaySteps ?: (stepCounts[date] ?: 0L)
            } else {
                stepCounts[date] ?: 0L
            }

            val fraction = (
                    steps.toFloat() / goalScale
                    ).coerceIn(0f, 1f)

            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                BoxWithConstraints(
                    modifier = Modifier
                        .weight(1f)
                        .width(23.dp)
                        .clip(RoundedCornerShape(50))
                        .background(trackColor),
                    contentAlignment = Alignment.BottomCenter
                ) {

                    if (steps > 0L) {

                        val barHeight = (
                                maxHeight * fraction
                                ).coerceAtLeast(3.dp)

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(barHeight)
                                .clip(RoundedCornerShape(50))
                                .background(colors.primary)
                        )
                    }
                }

                Spacer(Modifier.height(7.dp))

                Text(
                    text = date.dayOfMonth.toString(),
                    color = if (date == today) {
                        colors.primary
                    } else {
                        colors.onSurface
                    },
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
