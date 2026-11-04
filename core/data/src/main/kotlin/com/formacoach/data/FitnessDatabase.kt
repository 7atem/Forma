package com.formacoach.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.formacoach.data.security.KeystoreManager
import com.formacoach.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.LocalDate

@Entity(tableName="profile")
data class ProfileRow(@PrimaryKey val id:Int=1,val name:String,val goal:String,val sessionsPerWeek:Int,val caloriesTarget:Int?,val proteinTarget:Double?)
@Entity(tableName="exercise_preferences")
data class ExercisePreferenceRow(@PrimaryKey val exercise:String,val favorite:Boolean,val notes:String)
@Entity(tableName="sessions",indices=[Index("day")])
data class SessionRow(@PrimaryKey val id:String,val day:Long,val templateId:String,val completedAt:Long?,
    @ColumnInfo(defaultValue="''") val label:String="",
    @ColumnInfo(defaultValue="'[]'") val exerciseOrder:String="[]",
    @ColumnInfo(defaultValue="90") val restSeconds:Int=90,
    @ColumnInfo(defaultValue="'LOCAL'") val source:String="LOCAL",
    val averageHeartRateBpm:Double?=null,val activeEnergyKcal:Double?=null,val healthKitWorkoutId:String?=null,
    @ColumnInfo(defaultValue="'[]'") val prescriptions:String="[]")
@Entity(tableName="training_sets",foreignKeys=[ForeignKey(entity=SessionRow::class,parentColumns=["id"],childColumns=["sessionId"],onDelete=ForeignKey.CASCADE)],indices=[Index("sessionId")])
data class SetRow(@PrimaryKey val id:String,val sessionId:String,val exercise:String,val reps:Int,val weightKg:Double,
    @ColumnInfo(defaultValue="'WORKING'") val kind:String="WORKING",val rpe:Double?=null)
@Entity(tableName="meals",indices=[Index("day")])
data class MealRow(@PrimaryKey val id:String,val day:Long,val name:String,val calories:Double,val protein:Double,val carbs:Double,val fat:Double,
    val foodId:String?=null,val quantity:Double?=null,val unit:String?=null,@ColumnInfo(defaultValue="''") val source:String="",
    @ColumnInfo(defaultValue="'UNSPECIFIED'") val slot:String="UNSPECIFIED")
@Entity(tableName="measurements")
data class MeasurementRow(@PrimaryKey val day:Long,val weightKg:Double?,val waistCm:Double?)
@Entity(tableName="programs")
data class ProgramRow(@PrimaryKey val id:String,val name:String,val exercises:String,val restSeconds:Int,
    @ColumnInfo(defaultValue="'[]'") val prescriptions:String="[]")
@Entity(tableName="foods")
data class FoodRow(@PrimaryKey val id:String,val name:String,val payload:String)
@Entity(tableName="check_ins",indices=[Index("day")])
data class CheckInRow(@PrimaryKey val id:String,val day:Long,val availableSessions:Int,val foodLogComplete:Boolean,val notes:String)
@Entity(tableName="plan_proposals",foreignKeys=[ForeignKey(entity=CheckInRow::class,parentColumns=["id"],childColumns=["checkInId"],onDelete=ForeignKey.CASCADE)],indices=[Index("checkInId")])
data class ProposalRow(@PrimaryKey val id:String,val checkInId:String,val baseSessions:Int,val proposedSessions:Int,val evidence:String,val status:String)

@Dao
interface FitnessDao {
    @RawQuery(observedEntities=[ProfileRow::class,SessionRow::class,SetRow::class,MealRow::class,MeasurementRow::class,ProgramRow::class,FoodRow::class,CheckInRow::class,ProposalRow::class,ExercisePreferenceRow::class])
    fun changes(query:SupportSQLiteQuery):Flow<Int>
    @Query("SELECT * FROM profile WHERE id=1") fun profile():Flow<ProfileRow?>
    @Query("SELECT * FROM meals ORDER BY day DESC,rowid DESC") fun meals():Flow<List<MealRow>>
    @Query("SELECT * FROM measurements ORDER BY day DESC") fun measurements():Flow<List<MeasurementRow>>
    @Query("SELECT * FROM profile WHERE id=1") suspend fun readProfile():ProfileRow?
    @Query("SELECT * FROM sessions ORDER BY day DESC,rowid DESC") suspend fun allSessions():List<SessionRow>
    @Query("SELECT * FROM training_sets ORDER BY rowid") suspend fun allSets():List<SetRow>
    @Query("SELECT * FROM meals ORDER BY day DESC,rowid DESC") suspend fun allMeals():List<MealRow>
    @Query("SELECT * FROM measurements ORDER BY day DESC") suspend fun allMeasurements():List<MeasurementRow>
    @Query("SELECT * FROM programs ORDER BY name") suspend fun allPrograms():List<ProgramRow>
    @Query("SELECT * FROM foods ORDER BY name") suspend fun allFoods():List<FoodRow>
    @Query("SELECT * FROM check_ins ORDER BY day DESC,rowid DESC") suspend fun allCheckIns():List<CheckInRow>
    @Query("SELECT * FROM plan_proposals ORDER BY rowid DESC") suspend fun allProposals():List<ProposalRow>
    @Query("SELECT * FROM exercise_preferences ORDER BY exercise") suspend fun allExercisePreferences():List<ExercisePreferenceRow>
    @Query("SELECT * FROM exercise_preferences WHERE exercise=:exercise") suspend fun exercisePreference(exercise:String):ExercisePreferenceRow?
    @Upsert suspend fun saveExercisePreference(row:ExercisePreferenceRow)
    @Transaction suspend fun favorite(exercise:String,value:Boolean) {
        saveExercisePreference((exercisePreference(exercise)?:ExercisePreferenceRow(exercise,false,"")).copy(favorite=value))
    }
    @Transaction suspend fun exerciseNotes(exercise:String,value:String) {
        saveExercisePreference((exercisePreference(exercise)?:ExercisePreferenceRow(exercise,false,"")).copy(notes=value))
    }
    @Transaction
    suspend fun readSnapshot():FitnessSnapshot=FitnessSnapshot(
        readProfile()?.let {Profile(it.name,Goal.valueOf(it.goal),it.sessionsPerWeek,it.caloriesTarget,it.proteinTarget)},
        allSessions().map {Session(it.id,LocalDate.ofEpochDay(it.day),it.templateId,it.completedAt,it.label,Json.decodeFromString(it.exerciseOrder),it.restSeconds,SessionSource.valueOf(it.source),it.averageHeartRateBpm,it.activeEnergyKcal,it.healthKitWorkoutId,Json.decodeFromString(it.prescriptions))},
        allSets().map {TrainingSet(it.id,it.sessionId,Exercise.valueOf(it.exercise),it.reps,it.weightKg,SetKind.valueOf(it.kind),it.rpe)},
        allMeals().map {Meal(it.id,LocalDate.ofEpochDay(it.day),it.name,it.calories,it.protein,it.carbs,it.fat,it.foodId,it.quantity,it.unit?.let(NutritionUnit::valueOf),it.source,MealSlot.valueOf(it.slot))},
        allMeasurements().map {Measurement(LocalDate.ofEpochDay(it.day),it.weightKg,it.waistCm)},
        allPrograms().map {TrainingProgram(it.id,it.name,Json.decodeFromString(it.exercises),it.restSeconds,Json.decodeFromString(it.prescriptions))},
        allFoods().map {Json.decodeFromString<Food>(it.payload).also {food -> check(food.id==it.id && food.name==it.name)}},
        allCheckIns().map {WeeklyCheckIn(it.id,LocalDate.ofEpochDay(it.day),it.availableSessions,it.foodLogComplete,it.notes)},
        allProposals().map {PlanProposal(it.id,it.checkInId,it.baseSessions,it.proposedSessions,Json.decodeFromString(it.evidence),ProposalStatus.valueOf(it.status))},
        allExercisePreferences().map {ExercisePreference(Exercise.valueOf(it.exercise),it.favorite,it.notes)},
    )
    @Upsert suspend fun insertProfile(row:ProfileRow)
    @Transaction suspend fun saveProfile(row:ProfileRow) {insertProfile(row);supersedeMismatched(row.sessionsPerWeek)}
    @Query("UPDATE plan_proposals SET status='SUPERSEDED' WHERE status='PENDING' AND baseSessions != :target") suspend fun supersedeMismatched(target:Int)
    @Insert suspend fun insertSession(row:SessionRow)
    @Query("SELECT COUNT(*) FROM sessions WHERE completedAt IS NULL") suspend fun draftCount():Int
    @Transaction suspend fun startSession(row:SessionRow) {check(draftCount()==0);insertSession(row)}
    @Query("SELECT * FROM sessions WHERE id=:id") suspend fun session(id:String):SessionRow?
    @Insert suspend fun insertSet(row:SetRow)
    @Query("SELECT * FROM training_sets WHERE sessionId=:id") suspend fun sessionSets(id:String):List<SetRow>
    /** Entire batch rolls back on conflicting IDs. Re-import of identical records is a no-op. */
    @Transaction suspend fun mergeWatch(workouts:List<com.formacoach.wearables.WatchWorkout>) {
        workouts.forEach {workout ->
            val incoming=workout.session().row()
            val sets=workout.trainingSets().map {it.row()}
            val existing=session(incoming.id)
            if(existing!=null) {
                check(existing==incoming && sessionSets(incoming.id).sortedBy {it.id}==sets.sortedBy {it.id})
            } else {
                check(sets.none {set(it.id)!=null})
                insertSession(incoming);sets.forEach {insertSet(it)}
            }
        }
    }
    @Transaction suspend fun addSet(row:SetRow) {check(checkNotNull(session(row.sessionId)).completedAt==null);insertSet(row)}
    @Transaction suspend fun logSetAndRead(row:SetRow):List<SetRow> {addSet(row);return sessionSets(row.sessionId)}
    @Query("SELECT * FROM training_sets WHERE id=:id") suspend fun set(id:String):SetRow?
    @Query("SELECT COUNT(*) FROM training_sets WHERE sessionId=:id") suspend fun setCount(id:String):Int
    @Update suspend fun updateSet(row:SetRow)
    @Transaction suspend fun correctSet(row:SetRow) {check(checkNotNull(set(row.id)).sessionId==row.sessionId);updateSet(row)}
    @Query("DELETE FROM training_sets WHERE id=:id") suspend fun deleteSet(id:String)
    @Transaction suspend fun removeSet(id:String) {
        val row=checkNotNull(set(id))
        check(session(row.sessionId)?.completedAt==null || setCount(row.sessionId)>1)
        deleteSet(id)
    }
    @Query("UPDATE sessions SET completedAt=:now WHERE id=:id AND completedAt IS NULL AND EXISTS (SELECT 1 FROM training_sets WHERE sessionId=:id)") suspend fun finishSession(id:String,now:Long):Int
    @Query("UPDATE sessions SET day=:day WHERE id=:id") suspend fun moveSession(id:String,day:Long)
    @Query("DELETE FROM sessions WHERE id=:id") suspend fun deleteSession(id:String)
    @Upsert suspend fun addMeal(row:MealRow)
    @Query("SELECT * FROM meals WHERE day=:day ORDER BY id") suspend fun mealsOnDay(day:Long):List<MealRow>
    @Transaction suspend fun copyMealDay(expected:List<MealRow>,target:Long) {
        require(expected.size in 1..200 && expected.all {it.day==expected.first().day} && expected.first().day!=target)
        check(mealsOnDay(expected.first().day)==expected.sortedBy {it.id})
        expected.forEach {addMeal(it.copy(id=java.util.UUID.randomUUID().toString(),day=target))}
    }
    @Query("DELETE FROM meals WHERE id=:id") suspend fun removeMeal(id:String)
    @Upsert suspend fun saveMeasurement(row:MeasurementRow)
    @Query("DELETE FROM measurements WHERE day=:day") suspend fun removeMeasurement(day:Long)
    @Upsert suspend fun saveProgram(row:ProgramRow)
    @Query("SELECT * FROM programs WHERE id=:id") suspend fun program(id:String):ProgramRow?
    @Query("DELETE FROM programs WHERE id=:id") suspend fun deleteProgram(id:String)
    @Upsert suspend fun saveFood(row:FoodRow)
    @Query("DELETE FROM foods WHERE id=:id") suspend fun deleteFood(id:String)
    @Insert suspend fun insertCheckIn(row:CheckInRow)
    @Insert suspend fun insertProposal(row:ProposalRow)
    @Query("UPDATE plan_proposals SET status='SUPERSEDED' WHERE status='PENDING'") suspend fun supersedePending()
    @Transaction suspend fun recordCheckIn(row:CheckInRow,evidence:List<String>) {
        val profile=checkNotNull(readProfile())
        insertCheckIn(row);supersedePending()
        if(row.availableSessions!=profile.sessionsPerWeek) {
            insertProposal(PlanProposal(checkInId=row.id,baseSessions=profile.sessionsPerWeek,proposedSessions=row.availableSessions,sessionEvidence=evidence.take(100)).row())
        }
    }
    @Query("SELECT * FROM plan_proposals WHERE id=:id") suspend fun proposal(id:String):ProposalRow?
    @Query("UPDATE plan_proposals SET status=:status WHERE id=:id") suspend fun resolveProposal(id:String,status:String)
    @Transaction suspend fun acceptProposal(id:String) {
        val proposal=checkNotNull(proposal(id));val profile=checkNotNull(readProfile())
        check(proposal.status=="PENDING" && proposal.baseSessions==profile.sessionsPerWeek)
        insertProfile(profile.copy(sessionsPerWeek=proposal.proposedSessions));resolveProposal(id,"ACCEPTED")
    }
    @Transaction suspend fun dismissProposal(id:String) {check(proposal(id)?.status=="PENDING");resolveProposal(id,"DISMISSED")}
    @Query("DELETE FROM plan_proposals") suspend fun clearProposals()
    @Query("DELETE FROM check_ins") suspend fun clearCheckIns()
    @Query("DELETE FROM training_sets") suspend fun clearSets()
    @Query("DELETE FROM sessions") suspend fun clearSessions()
    @Query("DELETE FROM profile") suspend fun clearProfile()
    @Query("DELETE FROM meals") suspend fun clearMeals()
    @Query("DELETE FROM measurements") suspend fun clearMeasurements()
    @Query("DELETE FROM programs") suspend fun clearPrograms()
    @Query("DELETE FROM foods") suspend fun clearFoods()
    @Query("DELETE FROM exercise_preferences") suspend fun clearExercisePreferences()
    @Insert suspend fun insertMeasurementRows(rows:List<MeasurementRow>)
    @Transaction suspend fun replaceSnapshot(data:FitnessSnapshot) {
        clearProposals();clearCheckIns();clearSets();clearSessions();clearProfile();clearMeals();clearMeasurements();clearPrograms();clearFoods();clearExercisePreferences()
        data.profile?.let {insertProfile(it.row())}
        data.programs.forEach {saveProgram(it.row())};data.foods.forEach {saveFood(it.row())}
        data.sessions.forEach {insertSession(it.row())};data.sets.forEach {insertSet(it.row())}
        data.meals.forEach {addMeal(it.row())}
        insertMeasurementRows(data.measurements.map {MeasurementRow(it.date.toEpochDay(),it.weightKg,it.waistCm)})
        data.checkIns.forEach {insertCheckIn(it.row())};data.proposals.forEach {insertProposal(it.row())}
        data.exercisePreferences.forEach {saveExercisePreference(ExercisePreferenceRow(it.exercise.name,it.favorite,it.notes))}
    }
}

internal fun Profile.row()=ProfileRow(name=name,goal=goal.name,sessionsPerWeek=sessionsPerWeek,caloriesTarget=caloriesTarget,proteinTarget=proteinTarget)
internal fun Session.row()=SessionRow(id,date.toEpochDay(),templateId,completedAt,label,Json.encodeToString(plannedExercises),restSeconds,source.name,averageHeartRateBpm,activeEnergyKcal,healthKitWorkoutId,Json.encodeToString(prescriptions))
internal fun TrainingSet.row()=SetRow(id,sessionId,exercise.name,reps,weightKg,kind.name,rpe)
internal fun Meal.row()=MealRow(id,date.toEpochDay(),name,calories,protein,carbs,fat,foodId,quantity,unit?.name,source,slot.name)
internal fun TrainingProgram.row()=ProgramRow(id,name,Json.encodeToString(exercises),restSeconds,Json.encodeToString(prescriptions))
internal fun Food.row()=FoodRow(id,name,Json.encodeToString(this))
internal fun WeeklyCheckIn.row()=CheckInRow(id,date.toEpochDay(),availableSessions,foodLogComplete,notes)
internal fun PlanProposal.row()=ProposalRow(id,checkInId,baseSessions,proposedSessions,Json.encodeToString(sessionEvidence),status.name)

@Database(entities=[ProfileRow::class,SessionRow::class,SetRow::class,MealRow::class,MeasurementRow::class,ProgramRow::class,FoodRow::class,CheckInRow::class,ProposalRow::class,ExercisePreferenceRow::class],version=5,exportSchema=true)
abstract class FitnessDatabase:RoomDatabase() {
    abstract fun dao():FitnessDao
    companion object {
        val MIGRATION_4_5=object:Migration(4,5) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE programs ADD COLUMN prescriptions TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN prescriptions TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE meals ADD COLUMN slot TEXT NOT NULL DEFAULT 'UNSPECIFIED'")
            }
        }
        val MIGRATION_3_4=object:Migration(3,4) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS exercise_preferences (exercise TEXT NOT NULL PRIMARY KEY, favorite INTEGER NOT NULL, notes TEXT NOT NULL)")
            }
        }
        val MIGRATION_2_3=object:Migration(2,3) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN source TEXT NOT NULL DEFAULT 'LOCAL'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN averageHeartRateBpm REAL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN activeEnergyKcal REAL")
                db.execSQL("ALTER TABLE sessions ADD COLUMN healthKitWorkoutId TEXT")
                db.execSQL("ALTER TABLE training_sets ADD COLUMN kind TEXT NOT NULL DEFAULT 'WORKING'")
                db.execSQL("ALTER TABLE training_sets ADD COLUMN rpe REAL")
            }
        }
        val MIGRATION_1_2=object:Migration(1,2) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE sessions ADD COLUMN label TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE sessions ADD COLUMN exerciseOrder TEXT NOT NULL DEFAULT '[]'")
                db.execSQL("ALTER TABLE sessions ADD COLUMN restSeconds INTEGER NOT NULL DEFAULT 90")
                db.execSQL("UPDATE sessions SET exerciseOrder = CASE templateId WHEN 'A' THEN '[\"SQUAT\",\"CHEST_PRESS\",\"ROW\"]' WHEN 'B' THEN '[\"LEG_PRESS\",\"LAT_PULLDOWN\",\"HIP_HINGE\"]' ELSE '[]' END")
                db.execSQL("ALTER TABLE meals ADD COLUMN foodId TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN quantity REAL")
                db.execSQL("ALTER TABLE meals ADD COLUMN unit TEXT")
                db.execSQL("ALTER TABLE meals ADD COLUMN source TEXT NOT NULL DEFAULT ''")
                db.execSQL("CREATE TABLE IF NOT EXISTS programs (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, exercises TEXT NOT NULL, restSeconds INTEGER NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS foods (id TEXT NOT NULL PRIMARY KEY, name TEXT NOT NULL, payload TEXT NOT NULL)")
                db.execSQL("CREATE TABLE IF NOT EXISTS check_ins (id TEXT NOT NULL PRIMARY KEY, day INTEGER NOT NULL, availableSessions INTEGER NOT NULL, foodLogComplete INTEGER NOT NULL, notes TEXT NOT NULL)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_check_ins_day ON check_ins(day)")
                db.execSQL("CREATE TABLE IF NOT EXISTS plan_proposals (id TEXT NOT NULL PRIMARY KEY, checkInId TEXT NOT NULL, baseSessions INTEGER NOT NULL, proposedSessions INTEGER NOT NULL, evidence TEXT NOT NULL, status TEXT NOT NULL, FOREIGN KEY(checkInId) REFERENCES check_ins(id) ON UPDATE NO ACTION ON DELETE CASCADE)")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_plan_proposals_checkInId ON plan_proposals(checkInId)")
            }
        }
        fun open(context:Context):FitnessDatabase {
            System.loadLibrary("sqlcipher")
            val key=KeystoreManager(context.applicationContext).getOrCreateDatabasePassphrase()
            return Room.databaseBuilder(context.applicationContext,FitnessDatabase::class.java,"forma.db")
                .openHelperFactory(SupportOpenHelperFactory(key)).addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4,MIGRATION_4_5).build()
        }
    }
}
