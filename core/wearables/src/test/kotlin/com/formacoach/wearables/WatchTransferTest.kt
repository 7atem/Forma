package com.formacoach.wearables

import com.formacoach.model.*
import java.io.ByteArrayInputStream
import org.junit.Assert.*
import org.junit.Test

class WatchTransferTest {
    @Test fun additiveExerciseNamesRoundTripWithoutBreakingLegacyPackets() {
        val legacy=WatchTransferCodec.decode(fixture())
        Exercise.entries.drop(8).forEach {exercise ->
            val packet=legacy.copy(programs=legacy.programs.map {it.copy(exercises=listOf(exercise))},workouts=legacy.workouts.map {it.copy(exercises=listOf(exercise),sets=it.sets.map {s->s.copy(exercise=exercise)})})
            assertEquals(packet,WatchTransferCodec.decode(WatchTransferCodec.encode(packet)))
        }
        assertEquals(legacy,WatchTransferCodec.decode(WatchTransferCodec.encode(legacy)))
    }
    private fun fixture()=checkNotNull(javaClass.getResourceAsStream("/transfer-v1.json")).use {it.readBytes()}
    @Test fun fixtureRoundTripsAndPreservesSourceAndEffort() {
        val packet=WatchTransferCodec.decode(fixture())
        assertEquals(packet,WatchTransferCodec.decode(WatchTransferCodec.encode(packet)))
        assertEquals(SessionSource.APPLE_WATCH_IMPORT,packet.workouts.single().session().source)
        assertEquals(SetKind.WARM_UP,packet.workouts.single().trainingSets().first().kind)
        assertEquals(4.5,packet.workouts.single().sets.first().rpe!!,0.0)
        assertNull(packet.workouts.single().activeEnergyKcal)
    }
    @Test fun rejectsUnknownExerciseAndProtocolAndUnboundedInput() {
        val source=fixture().toString(Charsets.UTF_8)
        listOf(source.replace("SQUAT","UNKNOWN"),source.replace("\"KG\"","\"LB\""),source.replace("\"version\": 1","\"version\": 2"),source.replace("\"packetId\"","\"unknownKey\""))
            .forEach {assertThrows(Exception::class.java) {WatchTransferCodec.decode(it.toByteArray())}}
        assertThrows(IllegalArgumentException::class.java) {WatchTransferCodec.read(ByteArrayInputStream(ByteArray(WatchTransferCodec.MAX_BYTES+1)))}
        assertThrows(IllegalArgumentException::class.java) {WatchTransferCodec.decode("[".repeat(30).toByteArray())}
    }
    @Test fun rejectsDuplicateSetsAndNonFiniteOrOutOfRangeValues() {
        val workout=WatchTransferCodec.decode(fixture()).workouts.single()
        assertThrows(IllegalArgumentException::class.java) {workout.copy(sets=listOf(workout.sets[0],workout.sets[0]))}
        assertThrows(IllegalArgumentException::class.java) {workout.sets[0].copy(rpe=10.1)}
        assertThrows(IllegalArgumentException::class.java) {workout.sets[0].copy(weightKg=Double.NaN)}
        assertThrows(IllegalArgumentException::class.java) {workout.copy(exercises=emptyList())}
        assertThrows(IllegalArgumentException::class.java) {WatchTransfer(workouts=listOf(workout,workout))}
    }
}
