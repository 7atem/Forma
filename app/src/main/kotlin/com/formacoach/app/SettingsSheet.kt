package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.WeightUnit

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
internal fun SettingsSheet(
    language: AppLanguage,
    onLanguage: (AppLanguage) -> Unit,
    darkTheme: Boolean,
    onDarkTheme: (Boolean) -> Unit,
    weightUnit: WeightUnit,
    onWeightUnit: (WeightUnit) -> Unit,
    onProfile: () -> Unit,
    onWatch: () -> Unit,
    onBackup: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                stringResource(R.string.settings_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
            )

            // Choices wrap instead of compressing translated labels at large text sizes.
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    stringResource(R.string.language_label),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    listOf(
                        AppLanguage.SYSTEM to R.string.language_system,
                        AppLanguage.EN to R.string.language_english,
                        AppLanguage.AR to R.string.language_arabic,
                    ).forEach { (option, label) ->
                        FilterChip(
                            selected = language == option,
                            onClick = { onLanguage(option) },
                            label = { Text(stringResource(label), fontWeight = FontWeight.SemiBold) },
                            modifier = Modifier.testTag("lang_${option.name}"),
                        )
                    }
                }
            }

            FormFieldPair(
                first = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.units_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            WeightUnit.entries.forEach { u ->
                                FilterChip(
                                    selected = weightUnit == u,
                                    onClick = { onWeightUnit(u) },
                                    label = { Text(stringResource(if (u == WeightUnit.KG) R.string.kg else R.string.lb), fontWeight = FontWeight.Bold) },
                                    modifier = Modifier.weight(1f).testTag("settings_unit_${u.name}"),
                                )
                            }
                        }
                    }
                },
                second = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            stringResource(R.string.appearance_label),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            FilterChip(
                                selected = darkTheme,
                                onClick = { onDarkTheme(true) },
                                label = { Text(stringResource(R.string.theme_dark), fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f).testTag("settings_theme_dark"),
                            )
                            FilterChip(
                                selected = !darkTheme,
                                onClick = { onDarkTheme(false) },
                                label = { Text(stringResource(R.string.theme_light), fontWeight = FontWeight.SemiBold) },
                                modifier = Modifier.weight(1f).testTag("settings_theme_light"),
                            )
                        }
                    }
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            SettingsRow(
                icon = Icons.Default.AccountCircle,
                title = stringResource(R.string.profile),
                tag = "settings_profile",
                onClick = { onDismiss(); onProfile() },
            )
            SettingsRow(
                icon = Icons.Default.Watch,
                title = stringResource(R.string.apple_watch),
                tag = "watch_settings",
                onClick = { onDismiss(); onWatch() },
            )
            SettingsRow(
                icon = Icons.Default.SaveAlt,
                title = stringResource(R.string.backup),
                tag = "backup_settings",
                onClick = { onDismiss(); onBackup() },
            )
        }
    }
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    tag: String,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        modifier = Modifier.fillMaxWidth().testTag(tag),
    ) {
        Row(
            Modifier.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
