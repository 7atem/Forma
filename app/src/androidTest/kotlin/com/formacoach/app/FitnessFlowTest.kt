package com.formacoach.app

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import com.formacoach.coach.ReminderTime
import org.junit.runner.RunWith

/** Run on a disposable emulator with fresh Forma application data. */
@RunWith(AndroidJUnit4::class)
class FitnessFlowTest {
    @Test fun guidedProgramSupersetsAndCalendarWorkAcrossRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("new_program").performScrollTo().performClick()
        rule.onNodeWithTag("program_name").performTextInput("UI guided workflow")
        for(exercise in listOf("CHEST_PRESS","ROW")) {
            rule.onNodeWithTag("program_exercise_$exercise").performScrollTo().performClick()
            rule.onNodeWithTag("plan_details_$exercise").performScrollTo().performClick()
            rule.onNodeWithTag("plan_sets").performTextInput("١")
            rule.onNodeWithTag("plan_rep_min").performScrollTo().performTextInput("٨")
            rule.onNodeWithTag("plan_rep_max").performScrollTo().performTextInput("١٢")
            rule.onNodeWithTag("plan_exercise_rest").performScrollTo().performTextInput("٤٥")
            rule.onNodeWithTag("plan_exercise_rest").performImeAction()
            rule.onNodeWithTag("save_plan_details").performClick()
        }
        rule.onNodeWithTag("link_previous_ROW").performScrollTo().performClick()
        rule.onNodeWithTag("save_program").performClick()
        val cardMatcher=SemanticsMatcher("saved program card") {node ->
            val key=androidx.compose.ui.semantics.SemanticsProperties.TestTag
            node.config.contains(key)&&node.config[key].startsWith("card_start_")
        } and hasAnyDescendant(hasText("UI guided workflow"))
        rule.waitUntil(10_000) {rule.onAllNodes(cardMatcher).fetchSemanticsNodes().isNotEmpty()}
        val tag=rule.onNode(cardMatcher).fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag].removePrefix("card_")
        rule.onNodeWithTag(tag).performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("set_reps").performScrollTo().performTextInput("8")
        rule.onNodeWithTag("set_weight").performTextInput("20")
        rule.onNodeWithTag("set_weight").performImeAction()
        rule.onNodeWithTag("add_set").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("workout_flow_notice").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("exercise_picker").performScrollTo().assertTextContains(rule.activity.getString(exerciseLabel(com.formacoach.model.Exercise.ROW)))
        rule.onNodeWithTag("set_weight").assert(SemanticsMatcher.expectValue(androidx.compose.ui.semantics.SemanticsProperties.EditableText,androidx.compose.ui.text.AnnotatedString("")))
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("set_reps").performScrollTo().performTextInput("8")
        rule.onNodeWithTag("set_weight").performTextInput("20")
        rule.onNodeWithTag("set_weight").performImeAction()
        rule.onNodeWithTag("add_set").performScrollTo().performClick()
        rule.onNodeWithTag("finish_session").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("toggle_workout_calendar").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("toggle_workout_calendar").performScrollTo().performClick()
        rule.onNodeWithTag("calendar_day_${java.time.LocalDate.now()}").performScrollTo().performClick()
        rule.onNodeWithTag("workout_date_filter").assertExists()
        rule.onNodeWithText("UI guided workflow").performScrollTo().assertExists()
    }
    @Test fun foodOverflowOpensCopyDayAndThemeChoiceSurvivesRecreation() {
        ensureProfile();rule.onNodeWithTag("tab_2").performClick()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.more_food_actions))
            .assertHeightIsAtLeast(48.dp).performClick()
        rule.onNodeWithTag("family_pot_btn").assertDoesNotExist()
        rule.onNodeWithTag("copy_meal_day").performClick()
        rule.onNodeWithTag("confirm_copy_day").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.cancel)).performClick()
        rule.onNodeWithTag("confirm_copy_day").assertDoesNotExist()
        rule.onNodeWithTag("open_settings").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("settings_theme_light").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("settings_theme_system").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("settings_theme_light").performClick().assertIsSelected()
        rule.onNodeWithTag("settings_theme_system").assertIsNotSelected()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("tab_0").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("open_settings").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("settings_theme_light").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("settings_theme_light").performScrollTo().assertIsSelected()
        rule.onNodeWithTag("settings_theme_system").performClick().assertIsSelected()
    }
    @Test fun customExerciseSetsPersistAcrossRecreationAndDeleteNeedsConfirmation() {
        ensureProfile();rule.onNodeWithTag("tab_1").performClick();rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("add_custom_exercise_btn").performScrollTo().performClick()
        rule.onNodeWithTag("custom_exercise_name").performTextInput("UI cable pullover")
        rule.onNodeWithTag("save_custom_exercise").performClick()
        val logMatcher=SemanticsMatcher("custom log button") {node ->
            val key=androidx.compose.ui.semantics.SemanticsProperties.TestTag
            node.config.contains(key)&&node.config[key].startsWith("log_custom_")
        }
        rule.waitUntil(10_000) {rule.onAllNodes(logMatcher).fetchSemanticsNodes().isNotEmpty()}
        val logTag=rule.onAllNodes(logMatcher).fetchSemanticsNodes().first().config[androidx.compose.ui.semantics.SemanticsProperties.TestTag]
        val id=logTag.removePrefix("log_custom_")
        rule.onNodeWithTag(logTag).performScrollTo().performClick()
        rule.onNodeWithTag("custom_set_reps").performTextReplacement("12")
        rule.onNodeWithTag("custom_set_weight").performTextInput("15")
        rule.onNodeWithTag("save_custom_set").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("save_custom_set").fetchSemanticsNodes().isEmpty()}
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_1").performClick();rule.onNodeWithTag("training_section_1").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("remove_custom_$id").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("UI cable pullover").performScrollTo().assertExists()
        rule.onNodeWithText("× 12",substring=true).assertExists()
        rule.onNodeWithTag("remove_custom_$id").performScrollTo().performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.cancel)).performClick()
        rule.onNodeWithText("UI cable pullover").assertExists()
        rule.onNodeWithTag("remove_custom_$id").performScrollTo().performClick()
        rule.onNodeWithTag("confirm_destructive").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("UI cable pullover").fetchSemanticsNodes().isEmpty()}
    }
    @Test fun cardioEntryAcceptsArabicDigitsPersistsAndRemovalNeedsConfirmation() {
        ensureProfile();rule.onNodeWithTag("tab_1").performClick();rule.onNodeWithTag("training_section_0").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("log_cardio").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("log_cardio").performScrollTo().performClick()
        rule.onNodeWithTag("save_cardio").assertIsNotEnabled()
        rule.onNodeWithTag("cardio_activity_ROW").performClick()
        rule.onNodeWithTag("cardio_minutes").performTextInput("٢٠")
        rule.onNodeWithTag("cardio_seconds").performTextInput("75")
        rule.onNodeWithTag("save_cardio").assertIsNotEnabled()
        rule.onNodeWithTag("cardio_seconds").performTextReplacement("30")
        rule.onNodeWithTag("cardio_note").performTextInput("UI erg piece")
        rule.onNodeWithTag("save_cardio").assertIsEnabled().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("save_cardio").fetchSemanticsNodes().isEmpty()}
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_1").performClick();rule.onNodeWithTag("training_section_0").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("UI erg piece").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("Row · 20 min 30 s",substring=true).assertExists()
        rule.onNodeWithTag("cardio_week_summary").assertExists()
        val removeMatcher=SemanticsMatcher("cardio remove") {node ->
            val key=androidx.compose.ui.semantics.SemanticsProperties.TestTag
            node.config.contains(key)&&node.config[key].startsWith("remove_cardio_")
        }
        val before=rule.onAllNodes(removeMatcher).fetchSemanticsNodes().size
        rule.onAllNodes(removeMatcher)[0].performScrollTo().performClick()
        rule.onNodeWithTag("confirm_destructive").performClick()
        rule.waitUntil(10_000) {rule.onAllNodes(removeMatcher).fetchSemanticsNodes().size==before-1}
    }
    @Test fun remindersAreOffByDefaultAndSwitchOnAndOff() {
        val instrumentation=androidx.test.platform.app.InstrumentationRegistry.getInstrumentation()
        if(android.os.Build.VERSION.SDK_INT>=33) instrumentation.uiAutomation.grantRuntimePermission(rule.activity.packageName,android.Manifest.permission.POST_NOTIFICATIONS)
        ensureProfile()
        rule.onNodeWithTag("open_settings").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("reminders_settings").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminders_settings").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("reminder_switch_WATER").fetchSemanticsNodes().isNotEmpty()}
        ReminderKind.entries.forEach {rule.onNodeWithTag("reminder_switch_${it.name}").performScrollTo().assertIsOff()}
        // Days, example and test button appear only once a reminder is on.
        rule.onNodeWithTag("reminder_test_WATER").assertDoesNotExist()
        rule.onNodeWithTag("reminder_switch_WATER").performScrollTo().performClick().assertIsOn()
        rule.runOnIdle {check(LogReminders.read(rule.activity,ReminderKind.WATER).enabled)}
        rule.waitUntil(5_000) {rule.onAllNodesWithTag("reminder_test_WATER").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("reminder_time_WATER").assertHeightIsAtLeast(48.dp)
        // Turning off a weekday is saved; the last remaining day cannot be removed.
        rule.onNodeWithTag("reminder_day_WATER_MONDAY").performScrollTo().assertHeightIsAtLeast(48.dp).performClick()
        rule.runOnIdle {check(LogReminders.read(rule.activity,ReminderKind.WATER).days==ReminderTime.EVERY_DAY and ReminderTime.dayBit(java.time.DayOfWeek.MONDAY).inv())}
        java.time.DayOfWeek.entries.filter {it!=java.time.DayOfWeek.MONDAY}.forEach {rule.onNodeWithTag("reminder_day_WATER_${it.name}").performScrollTo().performClick()}
        rule.runOnIdle {check(LogReminders.read(rule.activity,ReminderKind.WATER).days!=0)}
        // Skipping already-logged days is on by default and can be switched off.
        rule.onNodeWithTag("reminders_skip_logged").performScrollTo().assertIsOn().performClick().assertIsOff()
        rule.runOnIdle {check(!LogReminders.skipIfLogged(rule.activity))}
        rule.onNodeWithTag("reminders_skip_logged").performClick().assertIsOn()
        rule.onNodeWithTag("reminder_switch_WATER").performScrollTo().performClick().assertIsOff()
        rule.runOnIdle {check(!LogReminders.read(rule.activity,ReminderKind.WATER).enabled)}
        rule.runOnIdle {LogReminders.save(rule.activity,ReminderKind.WATER,LogReminders.read(rule.activity,ReminderKind.WATER).copy(days=ReminderTime.EVERY_DAY))}
        rule.onNodeWithTag("reminders_done").performScrollTo().performClick()
    }
    @Test fun heightInProfileShowsBmiFromLoggedWeight() {
        ensureProfile()
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithTag("log_measurement").performScrollTo().performClick()
        rule.onNodeWithTag("body_weight").performScrollTo().performTextReplacement("80")
        rule.onNodeWithTag("save_measurement").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("bmi_card").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText(rule.activity.getString(R.string.bmi_need_height)).performScrollTo().assertExists()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.profile)).performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("profile_height").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("profile_height").performScrollTo().performTextReplacement("180")
        rule.onNodeWithTag("save_profile").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("tab_3").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_3").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("24.7").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("24.7").performScrollTo().assertExists()
    }
    @Test fun ArabicExerciseSearchWorksInEnglishInterface() {
        ensureProfile();rule.onNodeWithTag("tab_1").performClick();rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("exercise_search").performTextInput("الحَشَرة المَيّتة")
        rule.onNodeWithTag("exercise_search").performImeAction()
        rule.onNodeWithTag("library_DEAD_BUG").performScrollTo().assertExists()
        rule.onNodeWithTag("library_SQUAT").assertDoesNotExist()
    }
    @Test fun mealCategoryPersistsWithRecordedPortion() {
        ensureProfile();rule.onNodeWithTag("tab_2").performClick()
        // Semantic click: on wide layouts the horizontal chip row can be clipped after scrolling.
        rule.onNodeWithTag("meal_slot_BREAKFAST").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.onNodeWithTag("meal_slot_BREAKFAST").assertIsSelected()
        openManualMeal()
        rule.onNodeWithTag("meal_name").performScrollTo().performTextInput("Breakfast workflow")
        listOf("meal_calories" to "300","meal_protein" to "20","meal_carbs" to "30","meal_fat" to "10").forEach {(tag,value)->rule.onNodeWithTag(tag).performScrollTo().performTextInput(value)}
        rule.onNodeWithTag("meal_fat").performImeAction()
        rule.onNodeWithTag("save_meal").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("meal_group_BREAKFAST").fetchSemanticsNodes().isNotEmpty()}
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("tab_2").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_2").performClick()
        rule.onNodeWithTag("meal_group_BREAKFAST").performScrollTo().assertExists()
    }
    @Test fun plateToolAcceptsArabicDigitsAndShowsBalancedResult() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("plate_calculator").performScrollTo().performClick()
        rule.onNodeWithTag("plate_target").performTextReplacement("٨٠")
        rule.onNodeWithTag("plate_target").performImeAction()
        rule.onNodeWithTag("calculate_plates").performClick()
        // Several two-plate combinations can be equally optimal; do not pin a tie choice.
        rule.onNodeWithTag("plate_result").performScrollTo().assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.close)).performClick()
    }
    @get:Rule val rule = createAndroidComposeRule<MainActivity>()

    /** The manual meal form opens on demand under "Add food". */
    private fun openManualMeal() {
        if(rule.onAllNodesWithTag("open_manual_meal").fetchSemanticsNodes().isNotEmpty()) rule.onNodeWithTag("open_manual_meal").performScrollTo().performClick()
    }
    private fun ensureProfile() {
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty() || rule.onAllNodesWithTag("tab_0").fetchSemanticsNodes().isNotEmpty()}
        if(rule.onAllNodesWithTag("profile_name").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("profile_name").performTextInput("Forma Tester")
            rule.onNodeWithTag("save_profile").performScrollTo().performClick()
        }
        // The first save on a fresh install also creates the encrypted database, which is slow on loaded emulators.
        rule.waitUntil(30_000) {rule.onAllNodesWithTag("tab_1").fetchSemanticsNodes().isNotEmpty()}
    }

    @Test fun profileTrainingFoodAndMeasurementPersistIntoReview() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("start_A").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("session_form_guide").performScrollTo().performClick()
        rule.onNodeWithTag("guide_mode_photos").performClick()
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
        openManualMeal()
        rule.onNodeWithTag("meal_name").performScrollTo().performTextInput("Test portion")
        listOf("meal_calories" to "500", "meal_protein" to "30", "meal_carbs" to "50", "meal_fat" to "20").forEach {(tag,value) ->
            rule.onNodeWithTag(tag).performScrollTo().performTextInput(value)
        }
        rule.onNodeWithTag("meal_fat").performImeAction()
        rule.onNodeWithTag("save_meal").performScrollTo().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText("Test portion").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithTag("log_measurement").performScrollTo().performClick()
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
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithTag("progress_section_1").performClick()
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
        rule.onNodeWithTag("tab_3").performClick()
        rule.onNodeWithTag("progress_section_1").performClick()
        rule.onNodeWithTag("check_local_model").performScrollTo().performClick()
        rule.waitUntil(20_000) {!rule.onNodeWithTag("check_local_model").fetchSemanticsNode().config.contains(androidx.compose.ui.semantics.SemanticsProperties.Disabled)}
        rule.onNodeWithText(rule.activity.getString(R.string.weekly_review)).performScrollTo().assertExists()
    }

    @Test fun muscleHighlightToggleWorksWithVideoAndSurvivesRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("exercise_search").performTextReplacement(rule.activity.getString(exerciseLabel(com.formacoach.model.Exercise.SQUAT)))
        rule.onNodeWithTag("exercise_search").performImeAction()
        rule.onNodeWithTag("library_SQUAT").performScrollTo().performClick()
        rule.onNodeWithTag("guide_mode_video").performClick()
        rule.onNodeWithTag("exercise_video").assertExists()
        rule.onNodeWithTag("compact_muscle_overlay_SQUAT",useUnmergedTree=true).assertExists()
        val activation=com.formacoach.model.ExerciseMuscles.of(com.formacoach.model.Exercise.SQUAT)
        val primary=activation.primaryDetail.joinToString {rule.activity.getString(muscleDetailLabel(it))}
        val secondary=activation.secondaryDetail.joinToString {rule.activity.getString(muscleDetailLabel(it))}
        rule.onNodeWithText(rule.activity.getString(R.string.muscle_primary,primary)).assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.muscle_secondary,secondary)).assertExists()
        rule.onNodeWithTag("muscle_highlights_toggle").assertHeightIsAtLeast(androidx.compose.ui.unit.Dp(48f)).performScrollTo().performClick()
        rule.onNodeWithTag("muscle_highlights_toggle").assertIsOff()
        rule.onNodeWithTag("compact_muscle_overlay_SQUAT",useUnmergedTree=true).assertDoesNotExist()
        rule.onNodeWithTag("demo_muscle_legend_SQUAT").assertExists()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("muscle_highlights_toggle").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("muscle_highlights_toggle").assertIsOff()
        rule.onNodeWithTag("compact_muscle_overlay_SQUAT",useUnmergedTree=true).assertDoesNotExist()
        rule.onNodeWithTag("muscle_highlights_toggle").performScrollTo().performClick()
        rule.onNodeWithTag("muscle_highlights_toggle").assertIsOn()
        rule.onNodeWithTag("compact_muscle_overlay_SQUAT",useUnmergedTree=true).assertExists()
        rule.onNodeWithTag("guide_close").performClick()
    }

    @Test fun muscleMapsCoverEveryExerciseAndWatchTransferIsAccessible() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        for(exercise in com.formacoach.model.Exercise.entries) {
            rule.onNodeWithTag("exercise_search").performScrollTo().performTextReplacement(rule.activity.getString(exerciseLabel(exercise)))
            rule.onNodeWithTag("exercise_search").performImeAction()
            rule.onNodeWithTag("library_${exercise.name}").performScrollTo().performClick()
            if(rule.onAllNodesWithTag("guide_mode_photos").fetchSemanticsNodes().isNotEmpty()) {
                rule.onNodeWithTag("guide_mode_photos").performClick()
            }
            rule.onNodeWithTag("exercise_art_${exercise.name}",useUnmergedTree=true).performScrollTo().assertExists()
            rule.onNodeWithTag("muscle_highlights_toggle").assertExists()
            rule.onNodeWithTag("demo_muscle_legend_${exercise.name}").assertExists()
            rule.onNodeWithTag("guide_tab_muscles").performClick()
            rule.onNodeWithTag("muscle_map_${exercise.name}").performScrollTo().assertExists()
            rule.onNodeWithTag("guide_close").performClick()
        }
        rule.onNodeWithTag("open_settings").performClick()
        rule.onNodeWithTag("watch_settings").performClick()
        rule.onNodeWithTag("watch_export").performScrollTo().assertIsNotEnabled()
        rule.onNodeWithTag("watch_open_import").performScrollTo().assertIsEnabled()
    }

    @Test fun equipmentFavoritesAndPersonalNotesPersistAfterRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("exercise_search").performTextInput("Goblet")
        rule.onNodeWithTag("exercise_search").performImeAction()
        rule.onNodeWithTag("equipment_filter").performScrollTo().performClick()
        rule.onNodeWithTag("filter_equipment_DUMBBELLS").performClick()
        rule.onNodeWithTag("favorite_GOBLET_SQUAT").performScrollTo().performClick()
        rule.onNodeWithTag("favorites_filter").performScrollTo().performClick()
        rule.onNodeWithTag("library_GOBLET_SQUAT").performScrollTo().performClick()
        rule.onNodeWithTag("exercise_notes").performScrollTo().performClick()
        rule.onNodeWithTag("exercise_notes_input").performTextInput("My machine setup")
        rule.onNodeWithTag("save_exercise_notes").performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("exercise_notes_input").fetchSemanticsNodes().isEmpty()}
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("guide_close").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithText("My machine setup").performScrollTo().assertExists()
        rule.onNodeWithTag("guide_close").performClick()
        rule.onNodeWithTag("favorites_filter").assertIsSelected()
        rule.onNodeWithTag("library_GOBLET_SQUAT").performScrollTo().assertExists()
        rule.onNodeWithTag("muscle_filter").performScrollTo().performClick()
        rule.onNodeWithTag("filter_muscle_CHEST").performClick()
        rule.onNodeWithText(rule.activity.getString(R.string.no_matching_exercises)).assertExists()
        rule.onNodeWithTag("clear_exercise_filters").performScrollTo().performClick()
        rule.onNodeWithTag("library_SQUAT").performScrollTo().assertExists()
    }

    @Test fun exerciseSearchAndEnlargedPhasesSurviveRecreation() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        rule.onNodeWithTag("training_section_1").performClick()
        rule.onNodeWithTag("exercise_search").performScrollTo().performTextInput("Romanian")
        rule.onNodeWithTag("exercise_search").performImeAction()
        rule.onNodeWithTag("library_HIP_HINGE").performScrollTo().performClick()
        if (rule.onAllNodesWithTag("guide_mode_photos").fetchSemanticsNodes().isNotEmpty()) {
            rule.onNodeWithTag("guide_mode_photos").performClick()
        }
        rule.onNodeWithTag("open_illustration").performScrollTo().performClick()
        rule.onNodeWithTag("image_phase_2").performClick().assertIsSelected()
        rule.activityRule.scenario.recreate()
        rule.waitUntil(20_000) {rule.onAllNodesWithTag("image_viewer").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("image_phase_2").assertIsSelected()
        rule.onNodeWithTag("image_viewer_close").performClick()
        rule.onNodeWithTag("guide_close").performClick()
        rule.onNodeWithTag("exercise_search").performScrollTo().assertTextContains("Romanian")
    }

    @Test fun invalidProfileTargetExplainsHowToRestoreSaving() {
        ensureProfile()
        rule.onNodeWithContentDescription(rule.activity.getString(R.string.profile)).performClick()
        rule.onNodeWithTag("profile_calories").performScrollTo().performTextInput("0")
        rule.onNodeWithText(rule.activity.getString(R.string.calorie_target_error)).assertExists()
        rule.onNodeWithTag("save_profile").assertIsNotEnabled()
        rule.onNodeWithTag("profile_calories").performTextReplacement("2500")
        rule.onNodeWithTag("save_profile").assertIsEnabled()
    }

    @Test fun invalidMealValuesShowFieldErrorsWithoutLosingInput() {
        ensureProfile()
        rule.onNodeWithTag("tab_2").performClick()
        openManualMeal()
        rule.onNodeWithTag("meal_name").performScrollTo().performTextInput("Validation meal")
        rule.onNodeWithTag("meal_calories").performScrollTo().performTextInput("-1")
        rule.onNodeWithText(rule.activity.getString(R.string.calories_error)).assertExists()
        rule.onNodeWithTag("save_meal").assertIsNotEnabled()
        rule.onNodeWithTag("meal_calories").performTextReplacement("100")
        listOf("meal_protein", "meal_carbs", "meal_fat").forEach { tag ->
            rule.onNodeWithTag(tag).performScrollTo().performTextInput("0")
        }
        rule.onNodeWithTag("meal_name").assertTextContains("Validation meal")
        rule.onNodeWithTag("save_meal").assertIsEnabled()
    }

    @Test fun bodyweightSetSavesWithoutLoadAndQuickLogStaysOutOfTheWay() {
        ensureProfile()
        rule.onNodeWithTag("tab_1").performClick()
        if(rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isEmpty()) {
            rule.onNodeWithTag("start_empty_workout").performScrollTo().performClick()
        }
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("set_reps").fetchSemanticsNodes().isNotEmpty()}
        // The quick-log button would cover set controls during an active workout.
        rule.onNodeWithTag("quick_log_fab").assertDoesNotExist()
        rule.onNodeWithTag("exercise_picker").performScrollTo().performClick()
        rule.onNodeWithTag("picker_exercise_search").performTextInput("zzzz no such move")
        rule.onNodeWithTag("picker_no_results").assertExists()
        rule.onNodeWithTag("picker_exercise_search").performTextReplacement("")
        rule.onNodeWithTag("exercise_SQUAT").performScrollTo().performClick()
        rule.onNodeWithTag("set_reps").performScrollTo().performTextReplacement("")
        rule.onNodeWithTag("set_weight").performTextReplacement("")
        rule.onNodeWithTag("add_set").assertIsNotEnabled()
        rule.onNodeWithTag("set_save_hint").performScrollTo().assertTextContains(rule.activity.getString(R.string.set_needs_reps))
        rule.onNodeWithTag("set_reps").performScrollTo().performTextReplacement("12")
        rule.onNodeWithTag("add_set").performScrollTo().assertIsEnabled().performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithText(rule.activity.getString(R.string.bodyweight_load),substring=true).fetchSemanticsNodes().size>1}
        rule.onNodeWithText(rule.activity.getString(R.string.cancel_session)).performScrollTo().performClick()
        rule.onNodeWithTag("confirm_destructive").assertTextEquals(rule.activity.getString(R.string.cancel_session)).performClick()
        rule.waitUntil(10_000) {rule.onAllNodesWithTag("start_empty_workout").fetchSemanticsNodes().isNotEmpty()}
        rule.onNodeWithTag("quick_log_fab").assertExists()
    }

    @Test fun oneTapMealRepeatConfirmsAndCanBeUndone() {
        ensureProfile()
        rule.onNodeWithTag("tab_2").performClick()
        val name="Undo workflow ${System.nanoTime()%100000}"
        openManualMeal()
        rule.onNodeWithTag("meal_name").performScrollTo().performTextInput(name)
        listOf("meal_calories" to "300","meal_protein" to "20","meal_carbs" to "30","meal_fat" to "10").forEach {(tag,value)->rule.onNodeWithTag(tag).performScrollTo().performTextInput(value)}
        rule.onNodeWithTag("save_meal").performScrollTo().performClick()
        val titles=hasText(name) and hasClickAction().not()
        rule.waitUntil(10_000) {rule.onAllNodes(titles).fetchSemanticsNodes().isNotEmpty()}
        val before=rule.onAllNodes(titles).fetchSemanticsNodes().size
        // The saved form resets without flagging its now-empty name as an error.
        rule.onNodeWithText(rule.activity.getString(R.string.meal_name_error)).assertDoesNotExist()
        // Drive the clock by hand so the short snackbar is not auto-advanced away before it is read.
        rule.onNodeWithTag("quick_repeat_$name").performSemanticsAction(androidx.compose.ui.semantics.SemanticsActions.OnClick)
        rule.mainClock.autoAdvance=false
        val added=rule.activity.getString(R.string.meal_added_undo,name)
        var snack=false
        var count=before
        repeat(80) {
            if(!(snack&&count==before+1)) {
                rule.mainClock.advanceTimeBy(50)
                Thread.sleep(50)
                snack=snack||rule.onAllNodesWithText(added).fetchSemanticsNodes().isNotEmpty()
                count=rule.onAllNodes(titles).fetchSemanticsNodes().size
            }
        }
        check(snack&&count==before+1) {"snackbar=$snack entries=$count expected=${before+1}"}
        rule.onNodeWithText(rule.activity.getString(R.string.undo)).performClick()
        rule.mainClock.autoAdvance=true
        rule.waitUntil(10_000) {rule.onAllNodes(titles).fetchSemanticsNodes().size==before}
    }
}
