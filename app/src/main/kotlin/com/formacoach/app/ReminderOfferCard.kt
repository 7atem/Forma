package com.formacoach.app

import android.Manifest
import android.content.Context
import android.os.Build
import android.text.format.DateFormat
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsActive
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

/** Kinds the one-tap offer turns on; the person can change times, days or switch them off in Settings. */
private val OFFERED = listOf(ReminderKind.MEALS, ReminderKind.WORKOUT)
private const val OFFER_DISMISSED = "reminder_offer_dismissed"

/**
 * One-time Today card shown after the first logged meal or workout while every reminder is off. Reminders stay
 * opt-in: nothing turns on without this tap and the system permission, and "No thanks" hides the card for good.
 */
@Composable
internal fun ReminderOfferCard(hasLogged: Boolean) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("forma_ui_prefs", Context.MODE_PRIVATE) }
    var dismissed by remember { mutableStateOf(prefs.getBoolean(OFFER_DISMISSED, false)) }
    val anyOn = remember(dismissed) { ReminderKind.entries.any { LogReminders.read(context, it).enabled } }
    var denied by remember { mutableStateOf(false) }
    fun close() {
        prefs.edit().putBoolean(OFFER_DISMISSED, true).apply()
        dismissed = true
    }
    fun enable() {
        OFFERED.forEach { LogReminders.save(context, it, LogReminders.read(context, it).copy(enabled = true)) }
        close()
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) enable() else denied = true
    }
    if (dismissed || anyOn || !hasLogged) return

    val times = OFFERED.map { kind ->
        val minute = LogReminders.read(context, kind).minuteOfDay
        val time = LocalTime.of(minute / 60, minute % 60)
        java.text.SimpleDateFormat(if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a", java.util.Locale.getDefault())
            .format(java.util.Date.from(time.atDate(java.time.LocalDate.of(2000, 1, 1)).atZone(java.time.ZoneId.systemDefault()).toInstant()))
    }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f)),
        modifier = Modifier.fillMaxWidth().testTag("reminder_offer_card"),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Default.NotificationsActive, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                Text(stringResource(R.string.reminder_offer_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            Text(stringResource(R.string.reminder_offer_body, times[0], times[1]), style = MaterialTheme.typography.bodyMedium)
            if (denied) {
                Text(stringResource(R.string.reminder_offer_denied), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { close() }, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminder_offer_dismiss")) {
                    Text(stringResource(R.string.reminder_offer_no))
                }
                Button(
                    onClick = {
                        if (LogReminders.hasPermission(context)) enable()
                        else if (Build.VERSION.SDK_INT >= 33) launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        else denied = true
                    },
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("reminder_offer_accept"),
                ) { Text(stringResource(R.string.reminder_offer_yes)) }
            }
        }
    }
}
