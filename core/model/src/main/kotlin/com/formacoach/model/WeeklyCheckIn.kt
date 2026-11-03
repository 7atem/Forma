@file:kotlinx.serialization.UseSerializers(com.formacoach.model.LocalDateSerializer::class)

package com.formacoach.model

import java.time.LocalDate
import java.util.UUID
import kotlinx.serialization.Serializable

@Serializable
data class WeeklyCheckIn(
    val id: String = UUID.randomUUID().toString(),
    val date: LocalDate,
    val availableSessions: Int,
    val foodLogComplete: Boolean,
    val notes: String,
) {
    init {
        require(id.isNotBlank() && id.length <= 80)
        require(availableSessions in 2..4)
        require(notes.length <= 2000)
    }
}

enum class ProposalStatus { PENDING, ACCEPTED, DISMISSED, SUPERSEDED }

/** A scheduling change explicitly requested at a check-in, not an inferred exercise prescription. */
@Serializable
data class PlanProposal(
    val id: String = UUID.randomUUID().toString(),
    val checkInId: String,
    val baseSessions: Int,
    val proposedSessions: Int,
    val sessionEvidence: List<String>,
    val status: ProposalStatus = ProposalStatus.PENDING,
) {
    init {
        require(id.isNotBlank() && id.length <= 80 && checkInId.isNotBlank() && checkInId.length <= 80)
        require(baseSessions in 2..4 && proposedSessions in 2..4 && baseSessions != proposedSessions)
        require(sessionEvidence.size <= 100 && sessionEvidence.distinct().size == sessionEvidence.size)
    }
}
