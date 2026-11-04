package com.formacoach.backup

import com.formacoach.model.*
import java.io.ByteArrayInputStream
import java.time.LocalDate
import org.junit.Assert.*
import org.junit.Test

class PortableBackupTest {
    @Test fun exerciseFavoritesAndArabicNotesRoundTripAndDuplicatesFail() {
        val prefs=listOf(ExercisePreference(Exercise.DEAD_BUG,true,"ملاحظة شخصية"))
        val data=FitnessSnapshot(exercisePreferences=prefs)
        assertEquals(data,PortableBackup.decrypt(PortableBackup.encrypt(data,password,1),password).data)
        assertThrows(IllegalArgumentException::class.java) {PortableBackup.validate(data.copy(exercisePreferences=prefs+prefs))}
        assertTrue(kotlinx.serialization.json.Json.decodeFromString<FitnessSnapshot>("{}").exercisePreferences.isEmpty())
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
