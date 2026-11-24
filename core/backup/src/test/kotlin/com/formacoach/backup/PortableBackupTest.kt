package com.formacoach.backup

import com.formacoach.model.*
import java.io.ByteArrayInputStream
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PortableBackupTest {
    @Test fun guidedPlansAndMealGroupsRoundTripWhileLegacyRecordsHaveNoInferredTargets() {
        val order=listOf(Exercise.CHEST_PRESS,Exercise.ROW)
        val plans=order.map {ExercisePrescription(it,2,8,12,60,1,"ملاحظة البرنامج")}
        val program=TrainingProgram(name="User plan",exercises=order,prescriptions=plans)
        val session=Session(date=date,templateId=program.id,completedAt=1,plannedExercises=order,prescriptions=plans)
        val data=sample().copy(programs=listOf(program),sessions=listOf(session),sets=listOf(TrainingSet(sessionId=session.id,exercise=order[0],reps=8,weightKg=20.0)),meals=sample().meals.map {it.copy(slot=MealSlot.BREAKFAST)})
        assertEquals(data,PortableBackup.decrypt(PortableBackup.encrypt(data,password,1),password).data)
        val json=kotlinx.serialization.json.Json
        assertTrue(json.decodeFromString<TrainingProgram>("{\"name\":\"Legacy\",\"exercises\":[\"ROW\"]}").prescriptions.isEmpty())
        val old=json.decodeFromString<Meal>("{\"date\":\"2026-11-03\",\"name\":\"Legacy\",\"calories\":100,\"protein\":5,\"carbs\":10,\"fat\":2}")
        assertEquals(MealSlot.UNSPECIFIED,old.slot)
    }
    @Test fun exerciseFavoritesAndArabicNotesRoundTripAndDuplicatesFail() {
        val prefs=listOf(ExercisePreference(Exercise.DEAD_BUG,true,"ملاحظة شخصية"))
        val data=FitnessSnapshot(exercisePreferences=prefs)
        assertEquals(data,PortableBackup.decrypt(PortableBackup.encrypt(data,password,1),password).data)
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(exercisePreferences=prefs+prefs))}
        assertTrue(kotlinx.serialization.json.Json.decodeFromString<FitnessSnapshot>("{}").exercisePreferences.isEmpty())
    }
    @Test fun customExercisesAndSupplementsRoundTripAndRejectDanglingReferences() {
        val exercise=CustomExercise(name="تمرين خاص",equipment=Equipment.CABLE,primaryMuscle=Muscle.LATS,notes="note")
        val set=CustomExerciseSet(exerciseId=exercise.id,date=date,reps=10,weightKg=22.5,warmUp=true)
        val supplement=Supplement(name="Creatine",dose="my label")
        val intake=SupplementIntake(date,supplement.id)
        val data=FitnessSnapshot(customExercises=listOf(exercise),customSets=listOf(set),supplements=listOf(supplement),supplementIntakes=listOf(intake))
        assertEquals(data,PortableBackup.decrypt(PortableBackup.encrypt(data,password,1),password).data)
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(customExercises=emptyList()))}
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(supplements=emptyList()))}
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(supplementIntakes=listOf(intake,intake)))}
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(customSets=listOf(set,set)))}
        // Archives from before these records existed still decode, with nothing inferred.
        val old=kotlinx.serialization.json.Json.decodeFromString<FitnessSnapshot>("{}")
        assertTrue(old.customExercises.isEmpty()&&old.customSets.isEmpty()&&old.supplements.isEmpty()&&old.supplementIntakes.isEmpty())
        assertThrows(IllegalArgumentException::class.java) {CustomExerciseSet(exerciseId=exercise.id,date=date,reps=0,weightKg=10.0)}
        assertThrows(IllegalArgumentException::class.java) {Supplement(name=" ")}
    }
    private val password="a long test passphrase".toCharArray()
    private val date=LocalDate.of(2026,11,3)
    private fun sample()=FitnessSnapshot(profile=Profile("اختبار",Goal.FAT_LOSS,3),
        meals=listOf(Meal(date=date,name="Portion",calories=500.0,protein=30.0,carbs=50.0,fat=20.0)),
        measurements=listOf(Measurement(date,82.5,90.0)))

    @Test fun `portable authenticated archive round trips Arabic and unknown targets`() {
        val data=sample()
        val archive=PortableBackup.encrypt(data,password,1000)
        assertFalse(archive.toString(Charsets.ISO_8859_1).contains("Portion"))
        assertEquals(data,PortableBackup.decrypt(archive,password).data)
        assertNull(PortableBackup.decrypt(archive,password).data.profile!!.caloriesTarget)
    }
    @Test fun `fresh salt and nonce produce different archives`() {
        assertFalse(PortableBackup.encrypt(sample(),password,1).contentEquals(PortableBackup.encrypt(sample(),password,1)))
    }
    @Test fun `wrong password and tampered ciphertext fail authentication`() {
        val bytes=PortableBackup.encrypt(sample(),password,1)
        assertThrows(Exception::class.java) {PortableBackup.decrypt(bytes,"a different long passphrase".toCharArray())}
        bytes[bytes.lastIndex]=(bytes.last().toInt() xor 1).toByte()
        assertThrows(Exception::class.java) {PortableBackup.decrypt(bytes,password)}
    }
    @Test fun `header manipulation and truncation fail before accepting data`() {
        val bytes=PortableBackup.encrypt(sample(),password,1)
        bytes[8]=127
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.decrypt(bytes,password)}
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.decrypt(ByteArray(5),password)}
    }
    @Test fun `invalid references and duplicate records are rejected`() {
        val bad=sample().copy(sets=listOf(TrainingSet(sessionId="missing",exercise=Exercise.ROW,reps=8,weightKg=20.0)))
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(bad)}
        val data=sample()
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(meals=data.meals+data.meals))}
    }
    @Test fun `stream reader enforces a strict size limit`() {
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.readBounded(ByteArrayInputStream(ByteArray(PortableBackup.MAX_PLAINTEXT_BYTES+100)))}
    }

    @org.junit.Test fun alpha02RecordsWithoutNewFieldsKeepWorkingAndLocalDefaults() {
        val raw="""{"sessions":[{"id":"legacy-session","date":"2026-11-02","templateId":"A","completedAt":1}],"sets":[{"id":"legacy-set","sessionId":"legacy-session","exercise":"SQUAT","reps":8,"weightKg":20.5}]}"""
        val data=kotlinx.serialization.json.Json.decodeFromString<com.formacoach.model.FitnessSnapshot>(raw)
        org.junit.Assert.assertEquals(com.formacoach.model.SetKind.WORKING,data.sets.single().kind)
        org.junit.Assert.assertNull(data.sets.single().rpe)
        org.junit.Assert.assertEquals(com.formacoach.model.SessionSource.LOCAL,data.sessions.single().source)
        val bytes=PortableBackup.encrypt(data,"legacy backup test password".toCharArray(),1)
        org.junit.Assert.assertEquals(data,PortableBackup.decrypt(bytes,"legacy backup test password".toCharArray()).data)
    }
}
