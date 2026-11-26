package com.formacoach.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.formacoach.model.*

internal fun discomfortRegionLabel(region: ExerciseDiscomfortRegion): Int = when (region) {
    ExerciseDiscomfortRegion.NECK -> R.string.discomfort_region_neck
    ExerciseDiscomfortRegion.UPPER_BACK -> R.string.discomfort_region_upper_back
    ExerciseDiscomfortRegion.SHOULDERS -> R.string.discomfort_region_shoulders
    ExerciseDiscomfortRegion.LOWER_BACK -> R.string.discomfort_region_lower_back
}

internal fun discomfortRegionLabelShort(region: ExerciseDiscomfortRegion): Int = when (region) {
    ExerciseDiscomfortRegion.NECK -> R.string.discomfort_short_neck
    ExerciseDiscomfortRegion.UPPER_BACK -> R.string.discomfort_short_upper_back
    ExerciseDiscomfortRegion.SHOULDERS -> R.string.discomfort_region_shoulders
    ExerciseDiscomfortRegion.LOWER_BACK -> R.string.discomfort_region_lower_back
}

/**
 * Personal library preferences: areas to go easy on (marked or hidden) and muscles the person wants to train more
 * (listed first). Forma never links an area to suggested muscles; both choices are the person's own.
 */
@Composable
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
internal fun DiscomfortFocusSheet(
    regions: Set<ExerciseDiscomfortRegion>,
    mode: DiscomfortMode,
    focus: Set<Muscle>,
    onRegions: (Set<ExerciseDiscomfortRegion>) -> Unit,
    onMode: (DiscomfortMode) -> Unit,
    onFocus: (Set<Muscle>) -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(stringResource(R.string.discomfort_focus_title), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

            Text(stringResource(R.string.discomfort_areas_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.discomfort_filter_explainer), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            ExerciseDiscomfortRegion.entries.forEach { region ->
                val checked = region in regions
                val count = remember(region) { Exercise.entries.count { region.loads(it) } }
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (checked) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.surfaceContainerHigh,
                    border = BorderStroke(if (checked) 1.5.dp else 1.dp, if (checked) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.outlineVariant),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .toggleable(value = checked, role = Role.Checkbox) { onRegions(if (it) regions + region else regions - region) }
                        .testTag("discomfort_region_${region.name.lowercase()}"),
                ) {
                    Row(Modifier.padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Checkbox(checked = checked, onCheckedChange = null)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(stringResource(discomfortRegionLabel(region)), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                            Text(stringResource(R.string.discomfort_region_count, number(count.toDouble(), 0)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            if (regions.isNotEmpty()) {
                SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                    DiscomfortMode.entries.forEachIndexed { index, option ->
                        SegmentedButton(
                            selected = mode == option,
                            onClick = { onMode(option) },
                            shape = SegmentedButtonDefaults.itemShape(index, DiscomfortMode.entries.size),
                            modifier = Modifier.testTag("discomfort_mode_${option.name.lowercase()}"),
                        ) { Text(stringResource(if (option == DiscomfortMode.MARK) R.string.discomfort_mode_mark else R.string.discomfort_mode_hide)) }
                    }
                }
                Text(
                    stringResource(if (mode == DiscomfortMode.MARK) R.string.discomfort_mode_mark_note else R.string.discomfort_mode_hide_note),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HorizontalDivider()
            Text(stringResource(R.string.focus_title), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Text(stringResource(R.string.focus_note), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Muscle.entries.forEach { muscle ->
                    FilterChip(
                        selected = muscle in focus,
                        onClick = { onFocus(if (muscle in focus) focus - muscle else focus + muscle) },
                        label = { Text(stringResource(muscleLabel(muscle))) },
                        modifier = Modifier.testTag("focus_muscle_${muscle.name}"),
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.6f)),
            ) {
                Text(stringResource(R.string.discomfort_filter_limits), Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (regions.isNotEmpty() || focus.isNotEmpty()) {
                    OutlinedButton(
                        onClick = { onRegions(emptySet()); onFocus(emptySet()) },
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("clear_discomfort_filters"),
                    ) { Text(stringResource(R.string.clear_discomfort_filters)) }
                }
                Button(onClick = onDismiss, modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("discomfort_done")) { Text(stringResource(R.string.done)) }
            }
        }
    }
}
