package com.formacoach.app

import android.content.Context
import android.content.ContextWrapper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.formacoach.data.*
import com.formacoach.data.security.KeystoreManager
import java.io.File
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class EncryptedStorageTest {
    private lateinit var context: Context
    private lateinit var directory: File
    private var database: FitnessDatabase? = null

    @Before fun createIsolatedContext() {
        val base=ApplicationProvider.getApplicationContext<Context>()
        directory=File(base.cacheDir,"storage-test-${UUID.randomUUID()}").apply {mkdirs()}
        context=object: ContextWrapper(base) {
            override fun getApplicationContext(): Context = this
            override fun getFilesDir(): File = File(directory,"files").apply {mkdirs()}
            override fun getCacheDir(): File = File(directory,"cache").apply {mkdirs()}
            override fun getDatabasePath(name: String): File = File(directory,"databases/$name").apply {parentFile!!.mkdirs()}
        }
    }

    @After fun close() {
        database?.close()
        val base=ApplicationProvider.getApplicationContext<Context>()
        check(directory.canonicalFile.toPath().startsWith(base.cacheDir.canonicalFile.toPath()))
        directory.deleteRecursively()
    }

    @Test fun encryptedDataSurvivesReopenAndDraftsCannotFinishEmpty() = runBlocking {
        database=FitnessDatabase.open(context)
        val dao=database!!.dao()
        dao.saveProfile(ProfileRow(name="Test",goal="FAT_LOSS",sessionsPerWeek=3,caloriesTarget=null,proteinTarget=null))
        dao.startSession(SessionRow("session",20760,"A",null))
        assertEquals(0,dao.finishSession("session",1))
        dao.addSet(SetRow("set","session","ROW",8,20.5))
        assertEquals(1,dao.finishSession("session",2))
        assertEquals(0,dao.finishSession("session",3))
        var rejected=false
        try { dao.addSet(SetRow("late","session","ROW",8,20.5)) } catch (_: IllegalStateException) {rejected=true}
        assertTrue(rejected)
        dao.addMeal(MealRow("meal",20760,"Portion",500.0,30.0,50.0,20.0))
        dao.saveMeasurement(MeasurementRow(20760,82.5,90.0))
        database!!.close()
        val header=context.getDatabasePath("forma.db").inputStream().use {it.readNBytes(16)}
        assertFalse(header.contentEquals("SQLite format 3\u0000".toByteArray()))
        database=FitnessDatabase.open(context)
        assertEquals("Test",database!!.dao().profile().first()!!.name)
        assertEquals(1,database!!.dao().meals().first().size)
        assertEquals(82.5,database!!.dao().measurements().first().single().weightKg!!,0.001)
    }

    @Test fun unreadableKeyIsNeverReplaced() {
        val file=File(context.filesDir,"forma_passphrase.bin")
        KeystoreManager(context).getOrCreateDatabasePassphrase().fill(0)
        val corrupt=byteArrayOf(1,2,3,4)
        file.writeBytes(corrupt)
        assertThrows(Exception::class.java) {KeystoreManager(context).getOrCreateDatabasePassphrase()}
        assertArrayEquals(corrupt,file.readBytes())
    }

    @Test fun missingKeyForExistingDatabaseFailsClosed() {
        val databaseFile=context.getDatabasePath("forma.db")
        databaseFile.writeBytes(byteArrayOf(1,2,3))
        assertThrows(IllegalStateException::class.java) {KeystoreManager(context).getOrCreateDatabasePassphrase()}
        assertFalse(File(context.filesDir,"forma_passphrase.bin").exists())
        assertArrayEquals(byteArrayOf(1,2,3),databaseFile.readBytes())
    }
}
