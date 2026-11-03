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
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.formacoach.model.*
import java.time.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.json.JSONObject
import org.json.JSONArray

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

    private fun repository():FitnessRepository {
        database=FitnessDatabase.open(context)
        return FitnessRepository({database!!.dao()},Clock.fixed(Instant.parse("2026-11-03T10:00:00Z"),ZoneOffset.UTC))
    }

    @Test fun portableBackupPreviewsWithoutWritingAndRestoresWithSameDeviceKey()=runBlocking {
        val repo=repository()
        repo.saveProfile(Profile("Original",Goal.FAT_LOSS,3,2200,130.0))
        repo.addMeal(Meal(date=LocalDate.of(2026,11,2),name="Original meal",calories=500.0,protein=30.0,carbs=50.0,fat=20.0))
        val keyFile=File(context.filesDir,"forma_passphrase.bin").readBytes()
        val archive=repo.export("portable backup test password".toCharArray())
        repo.addMeal(Meal(date=LocalDate.of(2026,11,3),name="Extra meal",calories=100.0,protein=5.0,carbs=10.0,fat=2.0))
        var rejected=false
        try {repo.previewRestore(archive.copyOf(),"different test password".toCharArray())} catch(_:Exception) {rejected=true}
        assertTrue(rejected)
        assertEquals(2,database!!.dao().allMeals().size)
        val preview=repo.previewRestore(archive,"portable backup test password".toCharArray())
        assertEquals(1,preview.meals.size)
        assertEquals(2,database!!.dao().allMeals().size)
        repo.restore(preview)
        assertEquals(1,database!!.dao().allMeals().size)
        assertEquals(2200,database!!.dao().readProfile()!!.caloriesTarget)
        assertArrayEquals(keyFile,File(context.filesDir,"forma_passphrase.bin").readBytes())
    }

    @Test fun failedReplacementRollsBackEveryDeletedAndInsertedRecord()=runBlocking {
        repository().saveProfile(Profile("Keep me",Goal.BUILD_HABIT,3))
        database!!.dao().addMeal(MealRow("keep",20760,"Keep meal",100.0,0.0,0.0,0.0))
        val invalid=FitnessSnapshot(profile=Profile("Must roll back",Goal.BUILD_STRENGTH,2),
            sets=listOf(TrainingSet(sessionId="missing",exercise=Exercise.ROW,reps=8,weightKg=20.0)))
        var failed=false
        try {database!!.dao().replaceSnapshot(invalid)} catch(_:Exception) {failed=true}
        assertTrue(failed)
        assertEquals("Keep me",database!!.dao().readProfile()!!.name)
        assertEquals("keep",database!!.dao().allMeals().single().id)
        assertTrue(database!!.dao().allSets().isEmpty())
    }

    @Test fun checkInProposalsNeedConfirmationAndRejectStaleTargets()=runBlocking {
        val repo=repository()
        val profile=Profile("Test",Goal.FAT_LOSS,3,2200,130.0)
        repo.saveProfile(profile)
        repo.recordCheckIn(WeeklyCheckIn(date=LocalDate.of(2026,11,3),availableSessions=2,foodLogComplete=false,notes="Busy week"))
        assertEquals(3,database!!.dao().readProfile()!!.sessionsPerWeek)
        val first=database!!.dao().allProposals().single()
        repo.acceptProposal(first.id)
        assertEquals(2,database!!.dao().readProfile()!!.sessionsPerWeek)
        assertEquals(2200,database!!.dao().readProfile()!!.caloriesTarget)
        repo.recordCheckIn(WeeklyCheckIn(date=LocalDate.of(2026,11,3),availableSessions=4,foodLogComplete=true,notes=""))
        val second=database!!.dao().allProposals().first {it.status=="PENDING"}
        repo.saveProfile(profile)
        var rejected=false
        try {repo.acceptProposal(second.id)} catch(_:IllegalStateException) {rejected=true}
        assertTrue(rejected)
        assertEquals(3,database!!.dao().readProfile()!!.sessionsPerWeek)
        assertEquals("SUPERSEDED",database!!.dao().proposal(second.id)!!.status)
    }

    @Test fun savedFoodsAndProgramsNeverRewriteHistory()=runBlocking {
        val repo=repository()
        repo.saveProfile(Profile("Test",Goal.BUILD_HABIT,3))
        val food=Food(name="Label",referenceAmount=100.0,unit=NutritionUnit.GRAMS,nutrients=Nutrients(200.0,10.0,20.0,8.0),source="User label")
        repo.saveFood(food)
        repo.addMeal(food.portion(50.0,LocalDate.of(2026,11,1)))
        repo.saveFood(food.copy(nutrients=Nutrients(300.0,20.0,30.0,10.0)))
        assertEquals(100.0,database!!.dao().allMeals().single().calories,0.001)
        val program=TrainingProgram(name="Original program",exercises=listOf(Exercise.ROW),restSeconds=45)
        repo.saveProgram(program)
        val session=repo.startSession(program.id,LocalDate.of(2026,11,2))
        repo.addSet(TrainingSet(sessionId=session,exercise=Exercise.ROW,reps=8,weightKg=20.0))
        repo.finishSession(session)
        repo.saveProgram(program.copy(name="Changed program",exercises=listOf(Exercise.SQUAT)))
        repo.deleteProgram(program.id)
        val history=database!!.dao().readSnapshot().sessions.single()
        assertEquals("Original program",history.label)
        assertEquals(listOf(Exercise.ROW),history.plannedExercises)
        assertEquals(45,history.restSeconds)
    }

    @Test fun encryptedSchemaOneMigratesWithoutLosingExistingRecords()=runBlocking {
        System.loadLibrary("sqlcipher")
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.formacoach.data.FitnessDatabase/1.json").bufferedReader().use {it.readText()}).getJSONObject("database")
        val key=KeystoreManager(context).getOrCreateDatabasePassphrase()
        val helper=SupportOpenHelperFactory(key).create(SupportSQLiteOpenHelper.Configuration.builder(context).name("forma.db").callback(object:SupportSQLiteOpenHelper.Callback(1) {
            override fun onCreate(db:SupportSQLiteDatabase) {
                val entities=schema.getJSONArray("entities")
                for(i in 0 until entities.length()) {
                    val entity=entities.getJSONObject(i)
                    db.execSQL(entity.getString("createSql").replace("${'$'}{TABLE_NAME}",entity.getString("tableName")))
                    val indices=entity.optJSONArray("indices")?:JSONArray()
                    for(j in 0 until indices.length()) db.execSQL(indices.getJSONObject(j).getString("createSql").replace("${'$'}{TABLE_NAME}",entity.getString("tableName")))
                }
                val setup=schema.getJSONArray("setupQueries")
                for(i in 0 until setup.length()) db.execSQL(setup.getString(i))
            }
            override fun onUpgrade(db:SupportSQLiteDatabase,oldVersion:Int,newVersion:Int)=Unit
        }).build())
        helper.writableDatabase.execSQL("INSERT INTO profile VALUES(1,'Upgrade test','FAT_LOSS',3,2200,130.0)")
        helper.writableDatabase.execSQL("INSERT INTO meals VALUES('old-meal',20759,'Old meal',500.0,30.0,50.0,20.0)")
        helper.writableDatabase.execSQL("INSERT INTO sessions VALUES('old-session',20759,'B',1)")
        helper.writableDatabase.execSQL("INSERT INTO training_sets VALUES('old-set','old-session','LEG_PRESS',12,40.0)")
        helper.close()
        database=FitnessDatabase.open(context)
        val data=database!!.dao().readSnapshot()
        assertEquals("Upgrade test",data.profile!!.name)
        assertEquals("old-meal",data.meals.single().id)
        assertEquals("",data.meals.single().source)
        assertEquals(listOf(Exercise.LEG_PRESS,Exercise.LAT_PULLDOWN,Exercise.HIP_HINGE),data.sessions.single().plannedExercises)
        assertTrue(data.foods.isEmpty())
        assertTrue(data.programs.isEmpty())
    }
}
