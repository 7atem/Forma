package com.formacoach.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Run on a disposable emulator with fresh Forma application data. */
@RunWith(AndroidJUnit4::class)
class FitnessFlowTest {
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    @Test fun profileTrainingFoodAndMeasurementPersistIntoReview() {
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("profile_name").performTextInput("Forma Tester")
        rule.onNodeWithTag("save_profile").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("start_A").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("set_reps").performScrollTo().performTextInput("٨")
        rule.onNodeWithTag("set_weight").performScrollTo().performTextInput("٢٠٫٥")
        rule.onNodeWithTag("set_weight").performImeAction()
        rule.onNodeWithTag("add_set").performScrollTo().performClick()
        rule.waitUntil(10_000) {!rule.onNodeWithTag("finish_session").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)}
        rule.onNodeWithTag("finish_session").performScrollTo().assertIsEnabled().performClick()
        rule.onNodeWithTag("tab_2").performClick()
        rule.onNodeWithTag("meal_name").performScrollTo().performTextInput("Test portion")
        listOf("meal_calories" to "500", "meal_protein" to "30", "meal_carbs" to "50", "meal_fat" to "20").forEach {(tag,value) ->
            rule.onNodeWithTag(tag).performScrollTo().performTextInput(value)
        }
        rule.onNodeWithTag("meal_fat").performImeAction()
        rule.onNodeWithTag("save_meal").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("Test portion").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithTag("body_weight").performScrollTo().performTextInput("٨٢٫٥")
        rule.onNodeWithTag("waist").performScrollTo().performTextInput("٩٠")
        rule.onNodeWithTag("waist").performImeAction()
        rule.onNodeWithTag("save_measurement").performScrollTo().performClick()
        rule.waitForIdle()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("tab_0").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_0").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.greeting,"Forma Tester")).assertExists()
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.weekly_review)).performScrollTo().assertExists()
    }
}
