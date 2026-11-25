package com.formacoach.app

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.width
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

    @Test fun segmentedTabsExposeSelectionAndAccessibleTargets() {
        rule.setContent {
            MaterialTheme {
                var selected by remember { mutableStateOf(0) }
                SegmentedTabs(
                    selectedIndex = selected,
                    labels = listOf(R.string.stats_section, R.string.coach),
                    onSelect = { selected = it },
                    tagPrefix = "accessibility_tabs",
                )
            }
        }
        rule.onNodeWithTag("accessibility_tabs_0").assertIsSelected().assertHeightIsAtLeast(48.dp)
        rule.onNodeWithTag("accessibility_tabs_1").assertHeightIsAtLeast(48.dp).performClick().assertIsSelected()
        rule.onNodeWithTag("accessibility_tabs_0").assertIsNotSelected()
    }

    @Test fun headerActionsMeetMinimumTouchTarget() {
        rule.setContent {
            MaterialTheme {
                FormaScaffold(
                    tab = 0,
                    onTab = {},
                    today = LocalDate.of(2026, 11, 23),
                    snackbarHostState = remember { androidx.compose.material3.SnackbarHostState() },
                    onQuickLog = {},
                    onProfile = {},
                    onSettings = {},
                    content = {},
                )
            }
        }
        rule.onNodeWithContentDescription("Profile").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
        rule.onNodeWithContentDescription("Settings & data").assertHeightIsAtLeast(48.dp).assertWidthIsAtLeast(48.dp)
    }

    @Test fun chartsHaveTextEquivalentsBeyondColor() {
        val today = LocalDate.of(2026, 11, 23)
        rule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    ActivityHeatmapGrid(
                        today = today,
                        countsByDate = mapOf(today to 2, today.minusDays(1) to 1),
                    )
                    MacroBreakdownBar(protein = 20.0, carbs = 30.0, fat = 10.0)
                }
            }
        }
        rule.onNodeWithContentDescription("3 recorded workouts across 2 active days", substring = true).assertExists()
        rule.onNodeWithText("No recorded workout").assertExists()
        rule.onNodeWithText("1 recorded workout").assertExists()
        rule.onNodeWithText("2 or more recorded workouts").assertExists()
        rule.onNodeWithContentDescription("20 g protein, 30 g carbohydrates, 10 g fat", substring = true).assertExists()
    }

    @Test fun muscleRowsSpeakOneSentenceAndLongNamesStayReadable() {
        rule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Box(androidx.compose.ui.Modifier.width(320.dp)) {
                    MuscleVolumeBar(
                        muscleLabelText = "Abdominal and trunk stabilizers",
                        primarySets = 0,
                        secondarySets = 2,
                        maxSets = 2,
                        detailText = "Abdominal and trunk stabilizers • 0 primary / 2 supporting",
                    )
                }
            }
        }
        rule.onNodeWithContentDescription("Abdominal and trunk stabilizers • 0 primary / 2 supporting").assertExists()
        // The name gets the full row width instead of being squeezed beside the counts.
        rule.onNodeWithText("Abdominal and trunk stabilizers", useUnmergedTree = true).assertWidthIsAtLeast(200.dp)
        rule.onNodeWithText("0 main · 2 supporting sets", useUnmergedTree = true).assertExists()
    }

    @Test fun destructiveConfirmationNamesTheAction() {
        var removed = false
        rule.setContent {
            MaterialTheme {
                ConfirmButton(R.string.remove, "Removes this entry.", tag = "remove_entry") { removed = true }
            }
        }
        rule.onNodeWithTag("remove_entry").performClick()
        rule.onNodeWithTag("confirm_destructive").assertTextEquals("Remove").assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle { check(removed) }
    }

    @Test fun sparseDataExplainsWhatIsMissingInsteadOfDrawingEmptyGraphics() {
        val day = LocalDate.of(2026, 11, 25)
        rule.setContent {
            MaterialTheme {
                androidx.compose.foundation.layout.Column {
                    HistoryChart(listOf(com.formacoach.coach.HistoryPoint(day, 80.0)), "kg")
                    WeeklyMuscleHeatMap(emptyMap())
                }
            }
        }
        // One weigh-in shows its value and what a line needs, not a lone dot on a flat axis.
        rule.onNodeWithTag("chart_single_point").assertExists()
        rule.onNodeWithText("80 kg", substring = true).assertExists()
        // No working sets: a sentence replaces two grey figures and a colour legend.
        rule.onNodeWithTag("muscle_heatmap_empty").assertExists()
    }
}
