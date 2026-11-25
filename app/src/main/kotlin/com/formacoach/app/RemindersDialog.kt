package com.formacoach.app

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/** Opt-in daily reminders: each kind has its own switch and time. Nothing is scheduled until switched on. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun RemindersDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    var settings by remember { mutableStateOf(ReminderKind.entries.associateWith { LogReminders.read(context, it) }) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminders_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.reminders_intro), style = MaterialTheme.typography.bodyMedium)
                ReminderKind.entries.forEach { kind ->
                    val setting = settings.getValue(kind)
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f)) {
                            Text(stringResource(kind.label), fontWeight = FontWeight.SemiBold)
                            TextButton(
                                onClick = { picking = kind },
                                contentPadding = PaddingValues(horizontal = 0.dp),
                                modifier = Modifier.testTag("reminder_time_${kind.name}"),
                            ) { Text(stringResource(R.string.reminder_at, timeLabel(setting.minuteOfDay))) }
                        }
                        Switch(
                            checked = setting.enabled,
                            onCheckedChange = { on ->
                                if (on && !permission && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    pendingEnable = kind
                                    launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                } else {
                                    update(kind, setting.copy(enabled = on))
                                }
                            },
                            modifier = Modifier.testTag("reminder_switch_${kind.name}"),
                        )
                    }
                }
                // Only warn after the person tried to switch one on and notifications were refused.
                if (denied && !permission) {
                    Text(stringResource(R.string.reminders_permission_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
                Text(stringResource(R.string.reminders_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss, modifier = Modifier.testTag("reminders_done")) { Text(stringResource(R.string.close)) } },
    )

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

@Composable
private fun timeLabel(minuteOfDay: Int): String {
    val locale = androidx.compose.ui.platform.LocalConfiguration.current.locales[0]
    return LocalTime.of(minuteOfDay / 60, minuteOfDay % 60).format(DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).withLocale(locale))
}
