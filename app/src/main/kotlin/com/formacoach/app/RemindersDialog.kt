package com.formacoach.app

import android.Manifest
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.coach.ReminderTime
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle

/**
 * Opt-in reminders. Each kind has its own switch, time and days, an example of its message and a test button.
 * Nothing is scheduled until switched on, and notifications never show logged data.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemindersDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(ReminderKind.entries.associateWith { LogReminders.read(context, it) }) }
    var skipLogged by remember { mutableStateOf(LogReminders.skipIfLogged(context)) }
    var permission by remember { mutableStateOf(LogReminders.hasPermission(context)) }
    var pendingEnable by remember { mutableStateOf<ReminderKind?>(null) }
    var denied by remember { mutableStateOf(false) }
    var picking by remember { mutableStateOf<ReminderKind?>(null) }

    fun update(kind: ReminderKind, value: ReminderSetting) {
        LogReminders.save(context, kind, value)
        settings = settings + (kind to value)
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permission = granted
        denied = !granted
        val kind = pendingEnable
        pendingEnable = null
        // Only switch on after the person allowed notifications; otherwise nothing would ever arrive.
        if (granted && kind != null) update(kind, settings.getValue(kind).copy(enabled = true))
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Text(
                    stringResource(R.string.reminders_title),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.semantics { heading() },
                )
            }
            Text(stringResource(R.string.reminders_intro), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)

            // Shown as soon as notifications are known to be blocked, with a direct way to fix it.
            if (!permission && (denied || Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)) {
                Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(stringResource(R.string.reminders_permission_note), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onErrorContainer)
                        TextButton(
                            onClick = {
                                context.startActivity(
                                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                        .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("reminders_open_settings"),
                        ) { Text(stringResource(R.string.reminders_open_settings), fontWeight = FontWeight.Bold) }
                    }
                }
            }

            ReminderKind.entries.forEach { kind ->
                val setting = settings.getValue(kind)
                ReminderCard(
                    kind = kind,
                    setting = setting,
                    onToggle = { on ->
                        if (on && !permission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            pendingEnable = kind
                            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            update(kind, setting.copy(enabled = on))
                        }
                    },
                    onPickTime = { picking = kind },
                    onDays = { update(kind, setting.copy(days = it)) },
                    onTest = { LogReminders.sendTest(context, kind) },
                    canTest = permission,
                )
            }

            Surface(color = MaterialTheme.colorScheme.surfaceContainerLow, shape = RoundedCornerShape(18.dp)) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .toggleable(value = skipLogged, role = Role.Switch) {
                            skipLogged = it
                            LogReminders.setSkipIfLogged(context, it)
                        }
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .testTag("reminders_skip_logged"),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(stringResource(R.string.reminders_skip_logged), fontWeight = FontWeight.SemiBold)
                        Text(stringResource(R.string.reminders_skip_logged_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = skipLogged, onCheckedChange = null)
                }
            }

            Text(stringResource(R.string.reminders_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Button(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("reminders_done"),
            ) { Text(stringResource(R.string.done), fontWeight = FontWeight.Bold) }
        }
    }

    picking?.let { kind ->
        val current = settings.getValue(kind)
        val state = rememberTimePickerState(current.minuteOfDay / 60, current.minuteOfDay % 60, is24Hour = android.text.format.DateFormat.is24HourFormat(context))
        AlertDialog(
            onDismissRequest = { picking = null },
            title = { Text(stringResource(kind.label)) },
            text = { TimePicker(state) },
            confirmButton = {
                TextButton(onClick = {
                    update(kind, current.copy(minuteOfDay = state.hour * 60 + state.minute))
                    picking = null
                }) { Text(stringResource(R.string.save)) }
            },
            dismissButton = { TextButton(onClick = { picking = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun ReminderKind.icon(): ImageVector = when (this) {
    ReminderKind.MEALS -> Icons.Default.Restaurant
    ReminderKind.WATER -> Icons.Default.WaterDrop
    ReminderKind.WORKOUT -> Icons.Default.FitnessCenter
    ReminderKind.WEEKLY -> Icons.Default.Insights
}

@Composable
private fun ReminderCard(
    kind: ReminderKind,
    setting: ReminderSetting,
    onToggle: (Boolean) -> Unit,
    onPickTime: () -> Unit,
    onDays: (Int) -> Unit,
    onTest: () -> Unit,
    canTest: Boolean,
) {
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val time = timeLabel(setting.minuteOfDay)
    val days = daysLabel(setting.days)
    Surface(
        color = if (setting.enabled) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f) else MaterialTheme.colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.testTag("reminder_card_${kind.name}"),
    ) {
        Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(
                    Modifier.size(40.dp).clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center,
                ) { Icon(kind.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.onSecondaryContainer, modifier = Modifier.size(20.dp)) }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(stringResource(kind.label), fontWeight = FontWeight.SemiBold)
                    Text(
                        if (setting.enabled) stringResource(R.string.reminder_summary, days, time) else stringResource(R.string.reminder_off),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = setting.enabled,
                    onCheckedChange = onToggle,
                    modifier = Modifier.testTag("reminder_switch_${kind.name}").semantics { contentDescription = context.getString(kind.label) },
                )
            }
            AnimatedVisibility(setting.enabled) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    AssistChip(
                        onClick = onPickTime,
                        label = { Text(time, fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(18.dp)) },
                        modifier = Modifier
                            .heightIn(min = 48.dp)
                            .testTag("reminder_time_${kind.name}")
                            .semantics { contentDescription = context.getString(R.string.reminder_change_time, time) },
                    )
                    // One toggle per weekday in the local week order; at least one day stays chosen.
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        LogReminders.weekOrder(locale).forEach { day ->
                            val bit = ReminderTime.dayBit(day)
                            val on = setting.days and bit != 0
                            Box(
                                Modifier
                                    .weight(1f)
                                    .heightIn(min = 48.dp)
                                    .toggleable(value = on, role = Role.Checkbox) { wantOn ->
                                        val next = if (wantOn) setting.days or bit else setting.days and bit.inv()
                                        if (next != 0) onDays(next)
                                    }
                                    .semantics { contentDescription = day.getDisplayName(TextStyle.FULL, locale) }
                                    .testTag("reminder_day_${kind.name}_${day.name}"),
                                contentAlignment = Alignment.Center,
                            ) {
                                Box(
                                    Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        day.getDisplayName(TextStyle.NARROW_STANDALONE, locale),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        color = if (on) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        stringResource(R.string.reminder_example, LogReminders.message(context, kind, LocalDate.now())),
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (canTest) {
                        TextButton(
                            onClick = onTest,
                            contentPadding = PaddingValues(horizontal = 0.dp),
                            modifier = Modifier.heightIn(min = 48.dp).testTag("reminder_test_${kind.name}"),
                        ) { Text(stringResource(R.string.reminder_send_test), fontWeight = FontWeight.Bold) }
                    }
                }
            }
        }
    }
}

@Composable
private fun daysLabel(mask: Int): String {
    if (mask == ReminderTime.EVERY_DAY) return stringResource(R.string.reminder_every_day)
    val locale = LocalConfiguration.current.locales[0]
    return LogReminders.weekOrder(locale)
        .filter { mask and ReminderTime.dayBit(it) != 0 }
        .joinToString(if (locale.language == "ar") "، " else ", ") { it.getDisplayName(TextStyle.SHORT, locale) }
}

@Composable
private fun timeLabel(minuteOfDay: Int): String {
    val locale = LocalConfiguration.current.locales[0]
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}
