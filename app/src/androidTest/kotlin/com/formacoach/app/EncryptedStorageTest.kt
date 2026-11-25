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
    @Test fun schemaEightUpgradeAddsHeightAndSleepWithoutTouchingData()=runBlocking {
        val helper=createLegacy(8)
        helper.writableDatabase.execSQL("INSERT INTO profile (id,name,goal,sessionsPerWeek,caloriesTarget,proteinTarget) VALUES(1,'Legacy v8','BUILD_HABIT',3,NULL,NULL)")
        helper.writableDatabase.execSQL("INSERT INTO health_weights VALUES('hc-w','20759',80.0,'scale')")
        helper.close()
        val repo=repository()
        val migrated=database!!.dao().readSnapshot()
        assertEquals("Legacy v8",migrated.profile?.name)
        assertNull(migrated.profile?.heightCm)
        assertEquals(1,repo.healthImports.first().weights.size)
        repo.saveProfile(migrated.profile!!.copy(heightCm=178.0))
        val night=ImportedSleep(LocalDate.of(2026,11,2),390.0,deepMinutes=60.0,remMinutes=80.0,source="com.example.ring")
        repo.importHealth(emptyList(),emptyList(),listOf(night))
        repo.importHealth(emptyList(),emptyList(),listOf(night.copy(asleepMinutes=410.0))) // re-read night replaces
        assertEquals(listOf(410.0),repo.healthImports.first().sleep.map {it.asleepMinutes})
        val archive=repo.export("schema nine backup round trip".toCharArray())
        repo.restore(repo.previewRestore(archive,"schema nine backup round trip".toCharArray()))
        assertEquals(178.0,database!!.dao().readSnapshot().profile?.heightCm!!,0.0)
        repo.removeHealthImports()
        assertTrue(repo.healthImports.first().sleep.isEmpty())
    }
    @Test fun schemaSevenUpgradeKeepsDataAndHealthCopiesStaySeparateAndDeduplicated()=runBlocking {
        val helper=createLegacy(7)
        helper.writableDatabase.execSQL("INSERT INTO supplements VALUES('sup','Creatine','')")
        helper.close()
        val repo=repository()
        assertEquals("Creatine",database!!.dao().readSnapshot().supplements.single().name)
        val weight=ImportedWeight("hc-w1",LocalDate.of(2026,11,2),81.5,"com.example.scale")
        val workout=ImportedWorkout("hc-x1",LocalDate.of(2026,11,2),1_000L,1_000L+45*60_000L,56,"Run","com.example.watch")
        repo.importHealth(listOf(weight),listOf(workout))
        repo.importHealth(listOf(weight.copy(weightKg=81.0)),listOf(workout)) // same IDs update, never duplicate
        val imported=repo.healthImports.first()
        assertEquals(listOf(81.0),imported.weights.map {it.weightKg})
        assertEquals(45L,imported.workouts.single().minutes)
        // Copies never enter Forma logs, stats or backups.
        val snapshot=database!!.dao().readSnapshot()
        assertTrue(snapshot.measurements.isEmpty()&&snapshot.sessions.isEmpty())
        repo.removeHealthImports()
        assertTrue(repo.healthImports.first().let {it.weights.isEmpty()&&it.workouts.isEmpty()})
        assertEquals("Creatine",database!!.dao().readSnapshot().supplements.single().name)
    }
    @Test fun schemaSixUpgradeKeepsHistoryAndStoresCustomExercisesAndSupplementsInBackup()=runBlocking {
        val helper=createLegacy(6)
        helper.writableDatabase.execSQL("INSERT INTO hydration VALUES(20759,1500)")
        helper.close()
        val repo=repository()
        assertEquals(1500,database!!.dao().readSnapshot().hydration.single().milliliters)
        val exercise=CustomExercise(name="Landmine press",equipment=Equipment.BARBELL,primaryMuscle=Muscle.FRONT_DELTS)
        repo.saveCustomExercise(exercise)
        repo.addCustomSet(CustomExerciseSet(exerciseId=exercise.id,date=LocalDate.of(2026,11,3),reps=8,weightKg=30.0))
        val supplement=Supplement(name="Vitamin D3")
        repo.saveSupplement(supplement)
        repo.setSupplementTaken(LocalDate.of(2026,11,3),supplement.id,true)
        var orphanRejected=false
        try {repo.addCustomSet(CustomExerciseSet(exerciseId="missing",date=LocalDate.of(2026,11,3),reps=8,weightKg=30.0))} catch(_:IllegalStateException) {orphanRejected=true}
        assertTrue(orphanRejected)
        val saved=database!!.dao().readSnapshot()
        assertEquals(1,saved.customSets.size);assertEquals(1,saved.supplementIntakes.size)
        val archive=repo.export("schema seven backup round trip".toCharArray())
        repo.deleteCustomExercise(exercise.id);repo.deleteSupplement(supplement.id)
        val cleared=database!!.dao().readSnapshot()
        assertTrue(cleared.customSets.isEmpty()&&cleared.supplementIntakes.isEmpty())
        repo.restore(repo.previewRestore(archive,"schema seven backup round trip".toCharArray()))
        assertEquals(saved,database!!.dao().readSnapshot())
    }
    @Test fun plainPreferenceRecordsMoveIntoTheEncryptedDatabaseOnce()=runBlocking {
        LegacyLocalStore.clear(context)
        val repo=repository()
        context.getSharedPreferences("forma_custom_exercises_prefs",Context.MODE_PRIVATE).edit().putString("state_v1",
            """{"exercises":[{"id":"ex1","name":"Cable pullover","equipment":"CABLE","primaryMuscle":"LATS"}],"sets":[{"id":"s1","exerciseId":"ex1","date":"2026-11-02","reps":12,"weightKg":15.0},{"id":"s2","exerciseId":"gone","date":"2026-11-02","reps":5,"weightKg":5.0}]}""").commit()
        context.getSharedPreferences("forma_supplements_prefs",Context.MODE_PRIVATE).edit().putString("state_v1",
            """{"items":[{"id":"sup1","name":"Creatine","dose":"5 g"}],"takenByDate":{"2026-11-02":["sup1","sup1","gone"]}}""").commit()
        val records=checkNotNull(LegacyLocalStore.read(context))
        repo.importLocalRecords(records.exercises,records.sets,records.supplements,records.intakes)
        repo.importLocalRecords(records.exercises,records.sets,records.supplements,records.intakes) // repeat is harmless
        LegacyLocalStore.clear(context)
        val data=database!!.dao().readSnapshot()
        assertEquals(listOf("Cable pullover"),data.customExercises.map {it.name})
        assertEquals(listOf("s1"),data.customSets.map {it.id})
        assertEquals("5 g",data.supplements.single().dose)
        assertEquals(listOf(SupplementIntake(LocalDate.of(2026,11,2),"sup1")),data.supplementIntakes)
        assertNull(LegacyLocalStore.read(context))
        // Unreadable data is reported, not silently replaced, so the original stays in place.
        context.getSharedPreferences("forma_supplements_prefs",Context.MODE_PRIVATE).edit().putString("state_v1","{broken").commit()
        var unreadable=false
        try {LegacyLocalStore.read(context)} catch(_:Exception) {unreadable=true}
        assertTrue(unreadable)
        assertEquals("{broken",context.getSharedPreferences("forma_supplements_prefs",Context.MODE_PRIVATE).getString("state_v1",null))
        LegacyLocalStore.clear(context) // preferences are app-wide, so leave none behind for other tests
    }
    @Test fun schemaFiveUpgradeRetainsHistoryAndSupportsHydrationAndExpandedMeasurements()=runBlocking {
        val helper=createLegacy(5)
        helper.writableDatabase.execSQL("INSERT INTO profile VALUES(1,'Legacy v5','FAT_LOSS',3,2000,150.0)")
        helper.writableDatabase.execSQL("INSERT INTO measurements VALUES(20759,82.5,90.0)")
        helper.close()
        val repo=repository()
        val migrated=database!!.dao().readSnapshot()
        assertEquals("Legacy v5",migrated.profile?.name)
        assertNull(migrated.profile?.carbsTarget)
        assertNull(migrated.profile?.waterGoalMl)
        assertTrue(migrated.hydration.isEmpty())
        assertEquals(82.5,migrated.measurements.single().weightKg!!,0.001)
        assertNull(migrated.measurements.single().chestCm)
        repo.saveProfile(migrated.profile!!.copy(carbsTarget=220.0,fatTarget=65.0,waterGoalMl=2500))
        repo.saveHydration(Hydration(LocalDate.of(2026,11,3),1750))
        repo.saveMeasurement(Measurement(LocalDate.of(2026,11,3),82.0,89.5,neckCm=38.0,chestCm=102.0,armsCm=36.5,hipsCm=96.0,thighsCm=58.0))
        val updated=database!!.dao().readSnapshot()
        assertEquals(1750,updated.hydration.single().milliliters)
        assertEquals(102.0,updated.measurements.first {it.date==LocalDate.of(2026,11,3)}.chestCm!!,0.001)
        val archive=repo.export("schema six backup round trip".toCharArray())
        repo.removeHydration(LocalDate.of(2026,11,3))
        repo.restore(repo.previewRestore(archive,"schema six backup round trip".toCharArray()))
        assertEquals(updated,database!!.dao().readSnapshot())
    }
    @Test fun schemaFourUpgradeAndGuidedSnapshotsRetainTargetsGroupsNotesAndMealCategories()=runBlocking {
        val helper=createLegacy(4)
        helper.writableDatabase.execSQL("INSERT INTO programs VALUES('legacy-program','Legacy','[\"ROW\"]',90)")
        helper.writableDatabase.execSQL("INSERT INTO meals VALUES('legacy-meal',20759,'Legacy portion',300.0,20.0,30.0,10.0,NULL,150.0,'GRAMS','Original label')")
        helper.close()
        val repo=repository()
        val legacy=database!!.dao().readSnapshot()
        assertTrue(legacy.programs.single().prescriptions.isEmpty())
        assertEquals(MealSlot.UNSPECIFIED,legacy.meals.single().slot)
        val order=listOf(Exercise.CHEST_PRESS,Exercise.ROW)
        val plans=order.map {ExercisePrescription(it,2,8,12,60,1,"User notes")}
        val program=TrainingProgram(name="Guided",exercises=order,prescriptions=plans)
        repo.saveProgram(program)
        val session=repo.startSession(program.id)
        repo.saveProgram(program.copy(prescriptions=plans.map {it.copy(workingSets=5,notes="Changed program")}))
        val saved=repo.logSet(TrainingSet(sessionId=session,exercise=order[0],reps=8,weightKg=20.0))
        assertEquals(1,saved.size)
        repo.finishSession(session)
        repo.addMeal(legacy.meals.single().copy(slot=MealSlot.BREAKFAST))
        val data=database!!.dao().readSnapshot()
        assertEquals(plans,data.sessions.single().prescriptions)
        val archive=repo.export("guided workflow backup password".toCharArray())
        repo.deleteProgram(program.id)
        repo.restore(repo.previewRestore(archive,"guided workflow backup password".toCharArray()))
        assertEquals(data,database!!.dao().readSnapshot())
        repo.repeatSession(session)
        assertEquals(plans,database!!.dao().readSnapshot().sessions.single {it.completedAt==null}.prescriptions)
        val meal=data.meals.single()
        repo.copyMealDay(listOf(meal),LocalDate.of(2026,11,3))
        assertTrue(database!!.dao().readSnapshot().meals.all {it.slot==MealSlot.BREAKFAST})
    }
    @Test fun copyingMealDayPreservesPortionsAndSourcesAndRejectsStalePreviews()=runBlocking {
        val repo=repository()
        val old=Meal(date=LocalDate.of(2026,11,2),name="Label portion",calories=300.0,protein=20.0,carbs=30.0,fat=10.0,quantity=150.0,unit=NutritionUnit.GRAMS,source="Original label")
        repo.addMeal(old)
        repo.copyMealDay(listOf(old),LocalDate.of(2026,11,3))
        val copied=database!!.dao().readSnapshot().meals.single {it.id!=old.id}
        assertEquals(old.copy(id=copied.id,date=copied.date),copied)
        repo.addMeal(old.copy(calories=350.0))
        var rejected=false
        try {repo.copyMealDay(listOf(old),LocalDate.of(2026,11,3))} catch(_:IllegalStateException) {rejected=true}
        assertTrue(rejected)
        assertEquals(2,database!!.dao().allMeals().size)
    }
    @Test fun schemaThreeUpgradeRetainsHistoryAndPreferencesSurviveReopenAndRestore()=runBlocking {
        val helper=createLegacy(3)
        helper.writableDatabase.execSQL("INSERT INTO sessions(id,day,templateId,completedAt,label,exerciseOrder,restSeconds,source) VALUES('v3',20759,'A',1,'Preserved','[\"SQUAT\"]',45,'LOCAL')")
        helper.writableDatabase.execSQL("INSERT INTO training_sets(id,sessionId,exercise,reps,weightKg,kind) VALUES('v3-set','v3','SQUAT',8,40.5,'WORKING')")
        helper.close()
        database=FitnessDatabase.open(context)
        val repo=FitnessRepository({database!!.dao()},Clock.fixed(Instant.parse("2026-11-04T10:00:00Z"),ZoneOffset.UTC))
        assertTrue(database!!.dao().readSnapshot().exercisePreferences.isEmpty())
        repo.favorite(Exercise.GOBLET_SQUAT,true)
        repo.exerciseNotes(Exercise.GOBLET_SQUAT,"ملاحظة محفوظة")
        repo.favorite(Exercise.GOBLET_SQUAT,false)
        val before=database!!.dao().readSnapshot()
        assertEquals("ملاحظة محفوظة",before.exercisePreferences.single().notes)
        assertFalse(before.exercisePreferences.single().favorite)
        database!!.close();database=FitnessDatabase.open(context)
        assertEquals(before,database!!.dao().readSnapshot())
        val reopened=FitnessRepository({database!!.dao()})
        val archive=reopened.export("preferences round trip password".toCharArray())
        reopened.favorite(Exercise.GOBLET_SQUAT,true)
        reopened.restore(reopened.previewRestore(archive,"preferences round trip password".toCharArray()))
        assertEquals(before,database!!.dao().readSnapshot())
    }
    @Test fun repeatHistoryStartsFreshDraftAndGuardsAgainstConcurrentDrafts()=runBlocking {
        val repo=repository()
        val id=repo.startSession("A")
        repo.addSet(TrainingSet(sessionId=id,exercise=Exercise.PUSH_UP,reps=8,weightKg=0.0))
        repo.finishSession(id)
        repo.repeatSession(id)
        val data=database!!.dao().readSnapshot()
        val draft=data.sessions.single {it.completedAt==null}
        assertNotEquals(id,draft.id)
        assertEquals(listOf(Exercise.SQUAT,Exercise.CHEST_PRESS,Exercise.ROW,Exercise.PUSH_UP),draft.plannedExercises)
        assertTrue(data.sets.none {it.sessionId==draft.id})
        assertEquals(SessionSource.LOCAL,draft.source)
        var rejected=false
        try {repo.repeatSession(id)} catch(_:IllegalStateException) {rejected=true}
        assertTrue(rejected)
        assertEquals(2,database!!.dao().allSessions().size)
    }
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

    @Test fun newDatabaseUsesTheStoredKeyAsRawKeyAndStaysEncrypted() = runBlocking {
        database=FitnessDatabase.open(context)
        database!!.dao().saveProfile(ProfileRow(name="Raw",goal="FAT_LOSS",sessionsPerWeek=3,caloriesTarget=null,proteinTarget=null))
        database!!.close();database=null
        assertEquals("raw-v1",File(context.filesDir,"forma_db_key_mode").readText())
        val file=context.getDatabasePath("forma.db")
        assertFalse(String(file.readBytes().copyOf(15),Charsets.US_ASCII).startsWith("SQLite format"))
        val key=KeystoreManager(context).getOrCreateDatabasePassphrase()
        val raw=("x'"+key.joinToString("") {"%02x".format(it)}+"'").toByteArray(Charsets.US_ASCII)
        fun opens(secret:ByteArray)=runCatching {
            // A no-op error handler: a wrong key must never let SQLite treat the file as corrupt and delete it.
            val db=net.zetetic.database.sqlcipher.SQLiteDatabase.openDatabase(file.path,secret,null,net.zetetic.database.sqlcipher.SQLiteDatabase.OPEN_READWRITE,{_,_->},null)
            try {db.rawQuery("SELECT count(*) FROM profile").use {it.moveToFirst();it.getInt(0)}} finally {db.close()}
        }.getOrNull()
        assertEquals(1,opens(raw))
        assertEquals(null,opens(key))
        assertTrue(file.exists())
        database=FitnessDatabase.open(context)
        assertEquals("Raw",database!!.dao().readSnapshot().profile?.name)
    }
    @Test fun existingPassphraseDatabaseIsNeverRekeyedOrMarkedRaw() = runBlocking {
        val helper=createLegacy(8)
        helper.writableDatabase.execSQL("INSERT INTO meals(id,day,name,calories,protein,carbs,fat,source,slot) VALUES('legacy',20760,'Kept',100,1,1,1,'','UNSPECIFIED')")
        helper.close()
        database=FitnessDatabase.open(context)
        assertEquals("Kept",database!!.dao().readSnapshot().meals.single().name)
        assertFalse(File(context.filesDir,"forma_db_key_mode").exists())
        database!!.close();database=FitnessDatabase.open(context)
        assertEquals(1,database!!.dao().readSnapshot().meals.size)
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
        val helper=createLegacy(1)
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

    private fun createLegacy(version:Int):SupportSQLiteOpenHelper {
        System.loadLibrary("sqlcipher")
        val schema=JSONObject(InstrumentationRegistry.getInstrumentation().context.assets.open("com.formacoach.data.FitnessDatabase/${version}.json").bufferedReader().use {it.readText()}).getJSONObject("database")
        val key=KeystoreManager(context).getOrCreateDatabasePassphrase()
        val helper=SupportOpenHelperFactory(key).create(SupportSQLiteOpenHelper.Configuration.builder(context).name("forma.db").callback(object:SupportSQLiteOpenHelper.Callback(version) {
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
        return helper
    }

    @Test fun encryptedSchemaTwoRetainsAllHistoryAndAddsWorkingDefaults()=runBlocking {
        val helper=createLegacy(2)
        helper.writableDatabase.execSQL("INSERT INTO sessions VALUES('v2',20759,'A',1,'Saved name','[\"SQUAT\"]',45)")
        helper.writableDatabase.execSQL("INSERT INTO training_sets VALUES('v2-set','v2','SQUAT',8,40.5)")
        helper.close()
        database=FitnessDatabase.open(context)
        val data=database!!.dao().readSnapshot()
        assertEquals("Saved name",data.sessions.single().label)
        assertEquals(45,data.sessions.single().restSeconds)
        assertEquals(SessionSource.LOCAL,data.sessions.single().source)
        assertEquals(SetKind.WORKING,data.sets.single().kind)
        assertNull(data.sets.single().rpe)
    }

    @Test fun watchImportIsIdempotentConflictsRollbackAndNewFieldsSurviveBackup()=runBlocking {
        val repo=repository()
        repo.saveProfile(Profile("Keep profile",Goal.BUILD_HABIT,3))
        val workout=com.formacoach.wearables.WatchWorkout("watch-1",LocalDate.of(2026,11,2),"starter-A","Watch A",listOf(Exercise.SQUAT),90,1793613600000,
            listOf(com.formacoach.wearables.WatchSet("watch-set-1",Exercise.SQUAT,8,20.5,SetKind.WARM_UP,4.5)),120.0,100.0,"health-id")
        val transfer=com.formacoach.wearables.WatchTransfer(workouts=listOf(workout))
        repo.importWatch(transfer);repo.importWatch(transfer)
        assertEquals(1,database!!.dao().allSessions().size)
        assertEquals(1,database!!.dao().allSets().size)
        val second=workout.copy(id="watch-2",sets=listOf(workout.sets.single().copy(id="watch-set-2")))
        var rejected=false
        try {repo.importWatch(transfer.copy(workouts=listOf(second,workout.copy(programName="Conflicting name"))))} catch(_:Exception) {rejected=true}
        assertTrue(rejected)
        assertEquals(1,database!!.dao().allSessions().size)
        assertNull(database!!.dao().session("watch-2"))
        assertEquals("Keep profile",database!!.dao().readProfile()!!.name)
        val archive=repo.export("watch backup test password".toCharArray())
        val data=repo.previewRestore(archive,"watch backup test password".toCharArray())
        assertEquals(SetKind.WARM_UP,data.sets.single().kind)
        assertEquals(4.5,data.sets.single().rpe!!,0.0)
        assertEquals(120.0,data.sessions.single().averageHeartRateBpm!!,0.0)
        assertEquals(SessionSource.APPLE_WATCH_IMPORT,data.sessions.single().source)
        repo.restore(data)
        assertEquals(data,database!!.dao().readSnapshot())
    }
}
