package com.formacoach.app

import android.content.Context
import androidx.health.connect.client.HealthConnectClient
import androidx.health.connect.client.permission.HealthPermission
import androidx.health.connect.client.records.ExerciseSessionRecord
import androidx.health.connect.client.records.SleepSessionRecord
import androidx.health.connect.client.records.WeightRecord
import androidx.health.connect.client.request.ReadRecordsRequest
import androidx.health.connect.client.time.TimeRangeFilter
import com.formacoach.coach.SleepNights
import com.formacoach.coach.SleepSegment
import com.formacoach.coach.SleepStage
import com.formacoach.model.ImportedSleep
import com.formacoach.model.ImportedWeight
import com.formacoach.model.ImportedWorkout
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

internal enum class HealthConnectStatus { AVAILABLE, NEEDS_UPDATE, UNAVAILABLE }

/**
 * Read-only Health Connect access: body weight and exercise sessions only, after the person grants it.
 * Forma never writes to Health Connect, and imported copies stay separate from Forma logs and stats.
 */
internal object HealthConnectImport {
    const val IMPORT_DAYS: Long = 90
    private const val MAX_RECORDS = 2000

    val permissions: Set<String> = setOf(
        HealthPermission.getReadPermission(WeightRecord::class),
        HealthPermission.getReadPermission(ExerciseSessionRecord::class),
        HealthPermission.getReadPermission(SleepSessionRecord::class),
    )

    fun status(context: Context): HealthConnectStatus = when (HealthConnectClient.getSdkStatus(context)) {
        HealthConnectClient.SDK_AVAILABLE -> HealthConnectStatus.AVAILABLE
        HealthConnectClient.SDK_UNAVAILABLE_PROVIDER_UPDATE_REQUIRED -> HealthConnectStatus.NEEDS_UPDATE
        else -> HealthConnectStatus.UNAVAILABLE
    }

    suspend fun hasPermissions(context: Context): Boolean =
        status(context) == HealthConnectStatus.AVAILABLE &&
            HealthConnectClient.getOrCreate(context).permissionController.getGrantedPermissions().containsAll(permissions)

    /** Reads the last [IMPORT_DAYS] days, bounded, and maps records to local calendar dates. */
    data class Imported(val weights: List<ImportedWeight>, val workouts: List<ImportedWorkout>, val sleep: List<ImportedSleep>)

    suspend fun read(context: Context, now: Instant = Instant.now(), zone: ZoneId = ZoneId.systemDefault()): Imported {
        val client = HealthConnectClient.getOrCreate(context)
        val range = TimeRangeFilter.between(now.minus(IMPORT_DAYS, ChronoUnit.DAYS), now)
        val weights = mutableListOf<ImportedWeight>()
        var token: String? = null
        do {
            val page = client.readRecords(ReadRecordsRequest(WeightRecord::class, range, pageSize = 500, pageToken = token))
            page.records.forEach { record ->
                val kg = record.weight.inKilograms
                if (kg.isFinite() && kg > 0 && kg <= 1000) {
                    weights += ImportedWeight(
                        id = record.metadata.id,
                        date = record.time.atZone(record.zoneOffset ?: zone).toLocalDate(),
                        weightKg = kg,
                        source = record.metadata.dataOrigin.packageName.take(200),
                    )
                }
            }
            token = page.pageToken?.takeIf { it.isNotBlank() }
        } while (token != null && weights.size < MAX_RECORDS)

        val workouts = mutableListOf<ImportedWorkout>()
        token = null
        do {
            val page = client.readRecords(ReadRecordsRequest(ExerciseSessionRecord::class, range, pageSize = 500, pageToken = token))
            page.records.forEach { record ->
                if (!record.endTime.isBefore(record.startTime)) {
                    workouts += ImportedWorkout(
                        id = record.metadata.id,
                        date = record.startTime.atZone(record.startZoneOffset ?: zone).toLocalDate(),
                        startEpochMs = record.startTime.toEpochMilli(),
                        endEpochMs = record.endTime.toEpochMilli(),
                        exerciseType = record.exerciseType,
                        title = record.title.orEmpty().take(200),
                        source = record.metadata.dataOrigin.packageName.take(200),
                    )
                }
            }
            token = page.pageToken?.takeIf { it.isNotBlank() }
        } while (token != null && workouts.size < MAX_RECORDS)
        // Sleep: duration and stages only. Health Connect has no sleep score, and Forma does not compute one.
        val segments = mutableListOf<SleepSegment>()
        token = null
        var sessions = 0
        do {
            val page = client.readRecords(ReadRecordsRequest(SleepSessionRecord::class, range, pageSize = 500, pageToken = token))
            page.records.forEach { record ->
                sessions++
                val source = record.metadata.dataOrigin.packageName
                if (record.stages.isEmpty()) {
                    segments += SleepSegment(record.startTime, record.endTime, SleepStage.ASLEEP, source)
                } else {
                    record.stages.forEach { stage -> sleepStage(stage.stage)?.let { segments += SleepSegment(stage.startTime, stage.endTime, it, source) } }
                }
            }
            token = page.pageToken?.takeIf { it.isNotBlank() }
        } while (token != null && sessions < MAX_RECORDS)
        return Imported(weights.take(MAX_RECORDS), workouts.take(MAX_RECORDS), SleepNights.summarize(segments, zone))
    }

    private fun sleepStage(code: Int): SleepStage? = when (code) {
        SleepSessionRecord.STAGE_TYPE_SLEEPING, SleepSessionRecord.STAGE_TYPE_UNKNOWN -> SleepStage.ASLEEP
        SleepSessionRecord.STAGE_TYPE_LIGHT -> SleepStage.CORE
        SleepSessionRecord.STAGE_TYPE_DEEP -> SleepStage.DEEP
        SleepSessionRecord.STAGE_TYPE_REM -> SleepStage.REM
        SleepSessionRecord.STAGE_TYPE_AWAKE, SleepSessionRecord.STAGE_TYPE_AWAKE_IN_BED -> SleepStage.AWAKE
        else -> null // out of bed
    }
}
