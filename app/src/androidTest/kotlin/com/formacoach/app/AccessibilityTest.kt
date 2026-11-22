package com.formacoach.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Component-level accessibility checks; no app data or database needed. */
@RunWith(AndroidJUnit4::class)
class AccessibilityTest {
    @get:Rule val rule = createComposeRule()

    @Test fun stepperButtonsAreNamedAndLargeEnoughToTap() {
        rule.setContent {
            MaterialTheme {
                var value by remember { mutableStateOf("8") }
                StepperInput(label = R.string.reps, value = value, onChange = { value = it }, tag = "reps_input", step = 1.0, integer = true)
            }
        }
        rule.onNodeWithContentDescription("Increase Repetitions").assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("reps_input").assertTextContains("9")
        rule.onNodeWithContentDescription("Decrease Repetitions").assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("reps_input").assertTextContains("8")
    }

    @Test fun weekStripAnnouncesWhetherEachDayHasARecordedWorkout() {
        val today = LocalDate.of(2026, 11, 22)
        rule.setContent {
            MaterialTheme { WeeklyActivityStrip(today = today, activeDates = setOf(today.minusDays(2))) }
        }
        rule.onAllNodesWithContentDescription("workout recorded", substring = true).assertCountEquals(7)
        rule.onAllNodesWithContentDescription(": workout recorded", substring = true).assertCountEquals(1)
        rule.onAllNodesWithContentDescription("no workout recorded", substring = true).assertCountEquals(6)
    }
}
