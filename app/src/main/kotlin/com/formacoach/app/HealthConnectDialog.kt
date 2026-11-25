package com.formacoach.app

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.health.connect.client.PermissionController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch

/**
 * Health Connect: explicit permission, read-only import of weight and workouts, clearly labelled copies
 * that never change Forma logs or stats, and a one-step removal. Also shown as the permission rationale.
 */
@Composable
internal fun HealthConnectDialog(vm: FitnessViewModel, busy: Boolean, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imports by vm.healthImports.collectAsStateWithLifecycle()
    val status = remember { HealthConnectImport.status(context) }
    var granted by remember { mutableStateOf(false) }
    var working by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<Int?>(null) }

    LaunchedEffect(status) {
        granted = runCatching { HealthConnectImport.hasPermissions(context) }.getOrDefault(false)
    }
    val permissionLauncher = rememberLauncherForActivityResult(PermissionController.createRequestPermissionResultContract()) { result ->
        granted = result.containsAll(HealthConnectImport.permissions)
        if (!granted) message = R.string.health_connect_denied
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.health_connect_title)) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.health_connect_intro), style = MaterialTheme.typography.bodyMedium)
                when (status) {
                    HealthConnectStatus.UNAVAILABLE -> Text(stringResource(R.string.health_connect_unavailable), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    HealthConnectStatus.NEEDS_UPDATE -> {
                        Text(stringResource(R.string.health_connect_update), color = MaterialTheme.colorScheme.onSurfaceVariant)
                        OutlinedButton(onClick = {
                            context.startActivity(
                                Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.healthdata&url=healthconnect%3A%2F%2Fonboarding"))
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }) { Text(stringResource(R.string.health_connect_open_store)) }
                    }
                    HealthConnectStatus.AVAILABLE -> {
                        if (!granted) {
                            Button(
                                onClick = { permissionLauncher.launch(HealthConnectImport.permissions) },
                                modifier = Modifier.fillMaxWidth().testTag("health_connect_allow"),
                            ) { Text(stringResource(R.string.health_connect_allow)) }
                        } else {
                            Button(
                                onClick = {
                                    working = true
                                    message = null
                                    scope.launch {
                                        val result = runCatching { HealthConnectImport.read(context) }
                                        result.onSuccess { imported ->
                                            vm.importHealth(imported.weights, imported.workouts, imported.sleep) { message = R.string.health_connect_imported }
                                        }.onFailure { message = R.string.health_connect_failed }
                                        working = false
                                    }
                                },
                                enabled = !working && !busy,
                                modifier = Modifier.fillMaxWidth().testTag("health_connect_import"),
                            ) { Text(stringResource(if (working) R.string.health_connect_importing else R.string.health_connect_import)) }
                        }
                    }
                }
                message?.let { Text(stringResource(it), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary) }
                Text(
                    stringResource(R.string.health_connect_counts, number(imports.weights.size.toDouble(), 0), number(imports.workouts.size.toDouble(), 0), number(imports.sleep.size.toDouble(), 0)),
                    style = MaterialTheme.typography.labelLarge,
                )
                imports.weights.take(5).forEach {
                    Text(stringResource(R.string.health_connect_weight_row, dateLabel(it.date), formatWeight(it.weightKg, 1)), style = MaterialTheme.typography.bodySmall)
                }
                imports.workouts.take(5).forEach {
                    Text(
                        stringResource(R.string.health_connect_workout_row, dateLabel(it.date), it.title.ifBlank { stringResource(R.string.health_connect_workout) }, number(it.minutes.toDouble(), 0)),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (imports.weights.isNotEmpty() || imports.workouts.isNotEmpty() || imports.sleep.isNotEmpty()) {
                    ConfirmButton(R.string.health_connect_remove, stringResource(R.string.health_connect_remove_note), !busy, tag = "health_connect_remove") {
                        vm.removeHealthImports()
                    }
                }
                Text(stringResource(R.string.health_connect_privacy), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
    )
}
