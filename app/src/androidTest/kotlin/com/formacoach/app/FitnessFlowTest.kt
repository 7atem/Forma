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

    private fun ensureProfile() {
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("tab_0").fetchSemanticsNodes().isNotEmpty()}
        if(rule.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("profile_name").performTextInput("Forma Tester")
            rule.onNodeWithTag("save_profile").performScrollTo().performClick()
        }
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
    }

    @Test fun profileTrainingFoodAndMeasurementPersistIntoReview() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("start_A").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("session_form_guide").performScrollTo().performClick()
        rule.onNodeWithTag("exercise_art_SQUAT",useUnmergedTree=true).performScrollTo().assertExists()
        rule.onNodeWithTag("guide_tab_muscles").performClick()
        rule.onNodeWithTag("muscle_map_SQUAT").performScrollTo().assertExists()
        rule.onNodeWithTag("guide_close").performClick()
        rule.onNodeWithTag("toggle_rpe").performScrollTo().performClick()
        rule.onNodeWithTag("warmup_set").performScrollTo().performClick()
        rule.onNodeWithTag("set_rpe").performScrollTo().performTextInput("٧٫٥")
        rule.onNodeWithTag("set_reps").performScrollTo().performTextInput("٨")
        rule.onNodeWithTag("set_weight").performScrollTo().performTextInput("٢٠٫٥")
        rule.onNodeWithTag("set_weight").performImeAction()
        rule.onNodeWithTag("training_section_1").performScrollTo().performClick()
        rule.onNodeWithTag("training_section_0").performClick()
        rule.onNodeWithTag("set_reps").performScrollTo().assertTextContains("٨")
        rule.onNodeWithTag("set_weight").assertTextContains("٢٠٫٥")
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
        rule.onNodeWithTag("body_weight").performScrollTo().performTextClearance()
        rule.onNodeWithTag("body_weight").performTextInput("٨٢٫٥")
        rule.onNodeWithTag("waist").performScrollTo().performTextClearance()
        rule.onNodeWithTag("waist").performTextInput("٩٠")
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

    @Test fun savedProgramSurvivesRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("new_program").performScrollTo().performClick()
        rule.onNodeWithTag("program_name").performTextInput("UI test program")
        rule.onNodeWithTag("program_exercise_ROW").performScrollTo().performClick()
        rule.onNodeWithTag("save_program").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("UI test program").fetchSemanticsNodes().isNotEmpty()}
        rule.activityRule.scenario.recreate()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithText("UI test program").performScrollTo().assertExists()
    }

    @Test fun availabilityProposalRequiresConfirmation() {
        ensureProfile()
        rule.onNodeWithTag("tab_4").performClick()
        rule.onNodeWithTag("available_sessions_2").performScrollTo()
        val current=(2..4).first {rule.onNodeWithTag("available_sessions_$it").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Selected]}
        val target=if(current==2) 3 else 2
        rule.onNodeWithTag("available_sessions_$target").performClick()
        rule.onNodeWithTag("save_check_in").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("accept_proposal").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("accept_proposal").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("accept_proposal").fetchSemanticsNodes().isEmpty()}
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.profile)).performClick()
        rule.onNodeWithTag("target_sessions_$target").performScrollTo().assertIsSelected()
    }

    @Test fun foodLibraryRequiresSourceAndLogsScaledPortion() {
        ensureProfile()
        rule.onNodeWithTag("tab_2").performClick()
        rule.onNodeWithTag("food_library").performScrollTo().performClick()
        rule.onNodeWithTag("new_food").performScrollTo().performClick()
        rule.onNodeWithTag("food_name").performTextInput("UI test label food")
        listOf("food_calories" to "200","food_protein" to "10","food_carbs" to "20","food_fat" to "8").forEach {(tag,value) ->
            rule.onNodeWithTag(tag).performScrollTo().performTextInput(value)
        }
        rule.onNodeWithTag("save_food").assertIsNotEnabled()
        rule.onNodeWithTag("food_source").performScrollTo().performTextInput("User label test")
        rule.onNodeWithTag("food_source").performImeAction()
        rule.onNodeWithTag("save_food").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("choose_food").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("food_library").performScrollTo().performClick()
        rule.onNodeWithTag("choose_food").performScrollTo().performClick()
        rule.onNodeWithText("UI test label food").performClick()
        rule.onNodeWithTag("portion_quantity").performScrollTo().performTextInput("١٥٠")
        rule.onNodeWithTag("portion_quantity").performImeAction()
        rule.onNodeWithTag("log_portion").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("UI test label food").fetchSemanticsNodes().size>=2}
    }

    @Test fun optionalModelCheckNeverBlocksTheRecordedReview() {
        ensureProfile()
        rule.onNodeWithTag("tab_4").performClick()
        rule.onNodeWithTag("check_local_model").performScrollTo().performClick()
        rule.waitUntil(20_000) {!rule.onNodeWithTag("check_local_model").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)}
        rule.onNodeWithText(rule.activity.getString(R.string.weekly_review)).performScrollTo().assertExists()
        val permissions=rule.activity.packageManager.getPackageInfo(rule.activity.packageName,android.content.pm.PackageManager.GET_PERMISSIONS).requestedPermissions.orEmpty()
        org.junit.Assert.assertFalse(permissions.contains(android.Manifest.permission.INTERNET))
    }

    @Test fun muscleMapsCoverEveryExerciseAndWatchTransferIsAccessible() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        for(exercise in com.formacoach.model.Exercise.entries) {
            rule.onNodeWithTag("library_${exercise.name}").performScrollTo().performClick()
            rule.onNodeWithTag("exercise_art_${exercise.name}",useUnmergedTree=true).performScrollTo().assertExists()
            rule.onNodeWithTag("guide_tab_muscles").performClick()
            rule.onNodeWithTag("muscle_map_${exercise.name}").performScrollTo().assertExists()
            rule.onNodeWithTag("guide_close").performClick()
        }
        rule.onNodeWithTag("watch_settings").performClick()
        rule.onNodeWithTag("watch_export").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("watch_open_import").performScrollTo().assertIsEnabled()
    }

    @Test fun exerciseSearchAndEnlargedPhasesSurviveRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("exercise_search").performScrollTo().performTextInput("Romanian")
        rule.onNodeWithTag("exercise_search").performImeAction()
        rule.onNodeWithTag("library_HIP_HINGE").performScrollTo().performClick()
        rule.onNodeWithTag("open_illustration").performScrollTo().performClick()
        rule.onNodeWithTag("image_phase_2").performClick().assertIsSelected()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("image_viewer").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("image_phase_2").assertIsSelected()
        rule.onNodeWithTag("image_viewer_close").performClick()
        rule.onNodeWithTag("guide_close").performClick()
        rule.onNodeWithTag("exercise_search").performScrollTo().assertTextContains("Romanian")
    }
}
