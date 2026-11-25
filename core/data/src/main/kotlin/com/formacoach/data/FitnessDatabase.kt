package com.formacoach.data

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQuery
import com.formacoach.data.security.DatabaseKeyMode
import com.formacoach.data.security.KeystoreManager
import com.formacoach.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.time.LocalDate

@Entity(tableName="profile")
data class ProfileRow(@PrimaryKey val id:Int=1,val name:String,val goal:String,val sessionsPerWeek:Int,val caloriesTarget:Int?,val proteinTarget:Double?,val carbsTarget:Double?=null,val fatTarget:Double?=null,val waterGoalMl:Int?=null,val heightCm:Double?=null)
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
data class MeasurementRow(@PrimaryKey val day:Long,val weightKg:Double?,val waistCm:Double?,val neckCm:Double?=null,val chestCm:Double?=null,val armsCm:Double?=null,val hipsCm:Double?=null,val thighsCm:Double?=null)
@Entity(tableName="hydration")
data class HydrationRow(@PrimaryKey val day:Long,val milliliters:Int)
@Entity(tableName="custom_exercises")
data class CustomExerciseRow(@PrimaryKey val id:String,val name:String,val equipment:String,val primaryMuscle:String,val notes:String)
@Entity(tableName="custom_sets",indices=[Index("exerciseId"),Index("day")])
data class CustomSetRow(@PrimaryKey val id:String,val exerciseId:String,val day:Long,val reps:Int,val weightKg:Double,val warmUp:Boolean)
@Entity(tableName="supplements")
data class SupplementRow(@PrimaryKey val id:String,val name:String,val dose:String)
@Entity(tableName="supplement_intakes",primaryKeys=["day","supplementId"],indices=[Index("supplementId")])
data class SupplementIntakeRow(val day:Long,val supplementId:String)
@Entity(tableName="health_weights",indices=[Index("day")])
data class HealthWeightRow(@PrimaryKey val id:String,val day:Long,val weightKg:Double,val source:String)
@Entity(tableName="health_workouts",indices=[Index("day")])
data class HealthWorkoutRow(@PrimaryKey val id:String,val day:Long,val startAt:Long,val endAt:Long,val exerciseType:Int,val title:String,val source:String)
@Entity(tableName="health_sleep")
data class HealthSleepRow(@PrimaryKey val day:Long,val asleepMinutes:Double,val deepMinutes:Double?,val remMinutes:Double?,val lightMinutes:Double?,val awakeMinutes:Double?,val source:String)
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
    @RawQuery(observedEntities=[ProfileRow::class,SessionRow::class,SetRow::class,MealRow::class,MeasurementRow::class,ProgramRow::class,FoodRow::class,CheckInRow::class,ProposalRow::class,ExercisePreferenceRow::class,HydrationRow::class,CustomExerciseRow::class,CustomSetRow::class,SupplementRow::class,SupplementIntakeRow::class])
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
    @Query("SELECT * FROM hydration ORDER BY day DESC") suspend fun allHydration():List<HydrationRow>
    @Upsert suspend fun saveHydration(row:HydrationRow)
    @Query("DELETE FROM hydration WHERE day=:day") suspend fun removeHydration(day:Long)
    @Query("DELETE FROM hydration") suspend fun clearHydration()
    @Query("SELECT * FROM custom_exercises ORDER BY rowid") suspend fun allCustomExercises():List<CustomExerciseRow>
    @Query("SELECT * FROM custom_sets ORDER BY day,rowid") suspend fun allCustomSets():List<CustomSetRow>
    @Query("SELECT * FROM supplements ORDER BY rowid") suspend fun allSupplements():List<SupplementRow>
    @Query("SELECT * FROM supplement_intakes ORDER BY day,supplementId") suspend fun allSupplementIntakes():List<SupplementIntakeRow>
    @Upsert suspend fun saveCustomExercise(row:CustomExerciseRow)
    @Upsert suspend fun saveCustomSet(row:CustomSetRow)
    @Query("SELECT COUNT(*) FROM custom_exercises WHERE id=:id") suspend fun customExerciseCount(id:String):Int
    @Query("DELETE FROM custom_sets WHERE id=:id") suspend fun removeCustomSet(id:String)
    @Query("DELETE FROM custom_sets WHERE exerciseId=:id") suspend fun removeCustomSetsFor(id:String)
    @Query("DELETE FROM custom_exercises WHERE id=:id") suspend fun removeCustomExerciseRow(id:String)
    @Transaction suspend fun deleteCustomExercise(id:String) {removeCustomSetsFor(id);removeCustomExerciseRow(id)}
    @Transaction suspend fun addCustomSet(row:CustomSetRow) {check(customExerciseCount(row.exerciseId)==1);saveCustomSet(row)}
    @Upsert suspend fun saveSupplement(row:SupplementRow)
    @Query("SELECT COUNT(*) FROM supplements WHERE id=:id") suspend fun supplementCount(id:String):Int
    @Upsert suspend fun insertSupplementIntake(row:SupplementIntakeRow)
    @Query("DELETE FROM supplement_intakes WHERE day=:day AND supplementId=:id") suspend fun removeSupplementIntake(day:Long,id:String)
    @Query("DELETE FROM supplement_intakes WHERE supplementId=:id") suspend fun removeIntakesFor(id:String)
    @Query("DELETE FROM supplements WHERE id=:id") suspend fun removeSupplementRow(id:String)
    @Transaction suspend fun deleteSupplement(id:String) {removeIntakesFor(id);removeSupplementRow(id)}
    @Transaction suspend fun setSupplementTaken(day:Long,id:String,taken:Boolean) {
        check(supplementCount(id)==1)
        if(taken) insertSupplementIntake(SupplementIntakeRow(day,id)) else removeSupplementIntake(day,id)
    }
    @Query("DELETE FROM custom_sets") suspend fun clearCustomSets()
    @Query("DELETE FROM custom_exercises") suspend fun clearCustomExercises()
    @Query("DELETE FROM supplement_intakes") suspend fun clearSupplementIntakes()
    @Query("DELETE FROM supplements") suspend fun clearSupplements()
    @Query("SELECT * FROM health_weights ORDER BY day DESC") fun healthWeights():kotlinx.coroutines.flow.Flow<List<HealthWeightRow>>
    @Query("SELECT * FROM health_workouts ORDER BY startAt DESC") fun healthWorkouts():kotlinx.coroutines.flow.Flow<List<HealthWorkoutRow>>
    @Query("SELECT * FROM health_sleep ORDER BY day DESC") fun healthSleep():kotlinx.coroutines.flow.Flow<List<HealthSleepRow>>
    @Upsert suspend fun upsertHealthSleep(rows:List<HealthSleepRow>)
    @Query("DELETE FROM health_sleep") suspend fun clearHealthSleep()
    @Upsert suspend fun upsertHealthWeights(rows:List<HealthWeightRow>)
    @Upsert suspend fun upsertHealthWorkouts(rows:List<HealthWorkoutRow>)
    @Query("DELETE FROM health_weights") suspend fun clearHealthWeights()
    @Query("DELETE FROM health_workouts") suspend fun clearHealthWorkouts()
    /** Imported copies are keyed by their Health Connect record ID, so re-importing never duplicates. */
    @Transaction suspend fun importHealth(weights:List<HealthWeightRow>,workouts:List<HealthWorkoutRow>,sleep:List<HealthSleepRow>) {upsertHealthWeights(weights);upsertHealthWorkouts(workouts);upsertHealthSleep(sleep)}
    @Transaction suspend fun removeHealthImports() {clearHealthWeights();clearHealthWorkouts();clearHealthSleep()}
    /** One-time copy of records older builds kept in plain preferences; upserts by ID so repeating it is harmless. */
    @Transaction suspend fun importLocalRecords(exercises:List<CustomExerciseRow>,sets:List<CustomSetRow>,supplements:List<SupplementRow>,intakes:List<SupplementIntakeRow>) {
        exercises.forEach {saveCustomExercise(it)};sets.forEach {addCustomSet(it)}
        supplements.forEach {saveSupplement(it)};intakes.forEach {setSupplementTaken(it.day,it.supplementId,true)}
    }
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
        readProfile()?.let {Profile(it.name,Goal.valueOf(it.goal),it.sessionsPerWeek,it.caloriesTarget,it.proteinTarget,it.carbsTarget,it.fatTarget,it.waterGoalMl,it.heightCm)},
        allSessions().map {Session(it.id,LocalDate.ofEpochDay(it.day),it.templateId,it.completedAt,it.label,Json.decodeFromString(it.exerciseOrder),it.restSeconds,SessionSource.valueOf(it.source),it.averageHeartRateBpm,it.activeEnergyKcal,it.healthKitWorkoutId,Json.decodeFromString(it.prescriptions))},
        allSets().map {TrainingSet(it.id,it.sessionId,Exercise.valueOf(it.exercise),it.reps,it.weightKg,SetKind.valueOf(it.kind),it.rpe)},
        allMeals().map {Meal(it.id,LocalDate.ofEpochDay(it.day),it.name,it.calories,it.protein,it.carbs,it.fat,it.foodId,it.quantity,it.unit?.let(NutritionUnit::valueOf),it.source,MealSlot.valueOf(it.slot))},
        allMeasurements().map {Measurement(LocalDate.ofEpochDay(it.day),it.weightKg,it.waistCm,it.neckCm,it.chestCm,it.armsCm,it.hipsCm,it.thighsCm)},
        allPrograms().map {TrainingProgram(it.id,it.name,Json.decodeFromString(it.exercises),it.restSeconds,Json.decodeFromString(it.prescriptions))},
        allFoods().map {Json.decodeFromString<Food>(it.payload).also {food -> check(food.id==it.id && food.name==it.name)}},
        allCheckIns().map {WeeklyCheckIn(it.id,LocalDate.ofEpochDay(it.day),it.availableSessions,it.foodLogComplete,it.notes)},
        allProposals().map {PlanProposal(it.id,it.checkInId,it.baseSessions,it.proposedSessions,Json.decodeFromString(it.evidence),ProposalStatus.valueOf(it.status))},
        allExercisePreferences().map {ExercisePreference(Exercise.valueOf(it.exercise),it.favorite,it.notes)},
        allHydration().map {Hydration(LocalDate.ofEpochDay(it.day),it.milliliters)},
        allCustomExercises().map {CustomExercise(it.id,it.name,Equipment.valueOf(it.equipment),Muscle.valueOf(it.primaryMuscle),it.notes)},
        allCustomSets().map {CustomExerciseSet(it.id,it.exerciseId,LocalDate.ofEpochDay(it.day),it.reps,it.weightKg,it.warmUp)},
        allSupplements().map {Supplement(it.id,it.name,it.dose)},
        allSupplementIntakes().map {SupplementIntake(LocalDate.ofEpochDay(it.day),it.supplementId)},
    )
    @Upsert suspend fun insertProfile(row:ProfileRow)
    @Transaction suspend fun saveProfile(row:ProfileRow) {insertProfile(row);supersedeMismatched(row.sessionsPerWeek)}
    @Query("UPDATE plan_proposals SET status='SUPERSEDED' WHERE status='PENDING' AND baseSessions != :target") suspend fun supersedeMismatched(target:Int)
    @Insert suspend fun insertSession(row:SessionRow)
    @Query("SELECT COUNT(*) FROM sessions WHERE completedAt IS NULL") suspend fun draftCount():Int
    @Query("SELECT * FROM sessions WHERE completedAt IS NULL ORDER BY day DESC, rowid DESC LIMIT 1") suspend fun activeSession():SessionRow?
    @Transaction suspend fun startSession(row:SessionRow) {check(draftCount()==0);insertSession(row)}
    @Transaction suspend fun quickLogExerciseSet(fallbackSession:SessionRow,set:SetRow) {
        val existing=activeSession()
        val targetId=if(existing!=null) existing.id else {insertSession(fallbackSession);fallbackSession.id}
        insertSet(set.copy(sessionId=targetId))
    }
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
    @Query("DELETE FROM training_sets WHERE sessionId=:sessionId") suspend fun deleteSessionSets(sessionId:String)
    @Transaction suspend fun removeSet(id:String) {
        val row=checkNotNull(set(id))
        check(session(row.sessionId)?.completedAt==null || setCount(row.sessionId)>1)
        deleteSet(id)
    }
    @Transaction suspend fun restoreSet(row:SetRow,orderedSessionSetIds:List<String>) {
        val sess=session(row.sessionId) ?: return
        if(sess.completedAt!=null) return
        if(set(row.id)!=null) return
        val existing=sessionSets(row.sessionId)
        val byId=(existing+row).associateBy {it.id}
        val ordered=orderedSessionSetIds.mapNotNull {byId[it]}+existing.filter {it.id !in orderedSessionSetIds}
        deleteSessionSets(row.sessionId)
        ordered.forEach {insertSet(it)}
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
        clearProposals();clearCheckIns();clearSets();clearSessions();clearProfile();clearMeals();clearMeasurements();clearPrograms();clearFoods();clearExercisePreferences();clearHydration();clearSupplementIntakes();clearSupplements();clearCustomSets();clearCustomExercises()
        data.profile?.let {insertProfile(it.row())}
        data.programs.forEach {saveProgram(it.row())};data.foods.forEach {saveFood(it.row())}
        data.sessions.forEach {insertSession(it.row())};data.sets.forEach {insertSet(it.row())}
        data.meals.forEach {addMeal(it.row())}
        insertMeasurementRows(data.measurements.map {MeasurementRow(it.date.toEpochDay(),it.weightKg,it.waistCm,it.neckCm,it.chestCm,it.armsCm,it.hipsCm,it.thighsCm)})
        data.checkIns.forEach {insertCheckIn(it.row())};data.proposals.forEach {insertProposal(it.row())}
        data.exercisePreferences.forEach {saveExercisePreference(ExercisePreferenceRow(it.exercise.name,it.favorite,it.notes))}
        data.hydration.forEach {saveHydration(HydrationRow(it.date.toEpochDay(),it.milliliters))}
        data.customExercises.forEach {saveCustomExercise(it.row())};data.customSets.forEach {saveCustomSet(it.row())}
        data.supplements.forEach {saveSupplement(it.row())};data.supplementIntakes.forEach {insertSupplementIntake(it.row())}
    }
}

internal fun Profile.row()=ProfileRow(name=name,goal=goal.name,sessionsPerWeek=sessionsPerWeek,caloriesTarget=caloriesTarget,proteinTarget=proteinTarget,carbsTarget=carbsTarget,fatTarget=fatTarget,waterGoalMl=waterGoalMl,heightCm=heightCm)
internal fun CustomExercise.row()=CustomExerciseRow(id,name,equipment.name,primaryMuscle.name,notes)
internal fun CustomExerciseSet.row()=CustomSetRow(id,exerciseId,date.toEpochDay(),reps,weightKg,warmUp)
internal fun Supplement.row()=SupplementRow(id,name,dose)
internal fun SupplementIntake.row()=SupplementIntakeRow(date.toEpochDay(),supplementId)
internal fun Session.row()=SessionRow(id,date.toEpochDay(),templateId,completedAt,label,Json.encodeToString(plannedExercises),restSeconds,source.name,averageHeartRateBpm,activeEnergyKcal,healthKitWorkoutId,Json.encodeToString(prescriptions))
internal fun TrainingSet.row()=SetRow(id,sessionId,exercise.name,reps,weightKg,kind.name,rpe)
internal fun Meal.row()=MealRow(id,date.toEpochDay(),name,calories,protein,carbs,fat,foodId,quantity,unit?.name,source,slot.name)
internal fun TrainingProgram.row()=ProgramRow(id,name,Json.encodeToString(exercises),restSeconds,Json.encodeToString(prescriptions))
internal fun Food.row()=FoodRow(id,name,Json.encodeToString(this))
internal fun WeeklyCheckIn.row()=CheckInRow(id,date.toEpochDay(),availableSessions,foodLogComplete,notes)
internal fun PlanProposal.row()=ProposalRow(id,checkInId,baseSessions,proposedSessions,Json.encodeToString(sessionEvidence),status.name)

@Database(entities=[ProfileRow::class,SessionRow::class,SetRow::class,MealRow::class,MeasurementRow::class,ProgramRow::class,FoodRow::class,CheckInRow::class,ProposalRow::class,ExercisePreferenceRow::class,HydrationRow::class,CustomExerciseRow::class,CustomSetRow::class,SupplementRow::class,SupplementIntakeRow::class,HealthWeightRow::class,HealthWorkoutRow::class,HealthSleepRow::class],version=9,exportSchema=true)
abstract class FitnessDatabase:RoomDatabase() {
    abstract fun dao():FitnessDao
    companion object {
        val MIGRATION_8_9=object:Migration(8,9) {
            override fun migrate(db:SupportSQLiteDatabase) {
                MIGRATION_8_9_SQL.forEach(db::execSQL)
            }
        }
        val MIGRATION_7_8=object:Migration(7,8) {
            override fun migrate(db:SupportSQLiteDatabase) {
                MIGRATION_7_8_SQL.forEach(db::execSQL)
            }
        }
        val MIGRATION_6_7=object:Migration(6,7) {
            override fun migrate(db:SupportSQLiteDatabase) {
                MIGRATION_6_7_SQL.forEach(db::execSQL)
            }
        }
        val MIGRATION_5_6=object:Migration(5,6) {
            override fun migrate(db:SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE profile ADD COLUMN carbsTarget REAL")
                db.execSQL("ALTER TABLE profile ADD COLUMN fatTarget REAL")
                db.execSQL("ALTER TABLE profile ADD COLUMN waterGoalMl INTEGER")
                db.execSQL("ALTER TABLE measurements ADD COLUMN neckCm REAL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN chestCm REAL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN armsCm REAL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN hipsCm REAL")
                db.execSQL("ALTER TABLE measurements ADD COLUMN thighsCm REAL")
                db.execSQL("CREATE TABLE IF NOT EXISTS hydration (day INTEGER NOT NULL PRIMARY KEY, milliliters INTEGER NOT NULL)")
            }
        }
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
            val app=context.applicationContext
            val key=KeystoreManager(app).getOrCreateDatabasePassphrase()
            return Room.databaseBuilder(app,FitnessDatabase::class.java,"forma.db")
                .openHelperFactory(SupportOpenHelperFactory(DatabaseKeyMode.sqlcipherKey(app,key))).addMigrations(MIGRATION_1_2,MIGRATION_2_3,MIGRATION_3_4,MIGRATION_4_5,MIGRATION_5_6,MIGRATION_6_7,MIGRATION_7_8,MIGRATION_8_9).build()
        }
    }
}

/** Must match the Room v7 schema export exactly (schemas/com.formacoach.data.FitnessDatabase/7.json). */
internal val MIGRATION_6_7_SQL=listOf(
    "CREATE TABLE IF NOT EXISTS `custom_exercises` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `equipment` TEXT NOT NULL, `primaryMuscle` TEXT NOT NULL, `notes` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `custom_sets` (`id` TEXT NOT NULL, `exerciseId` TEXT NOT NULL, `day` INTEGER NOT NULL, `reps` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `warmUp` INTEGER NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_custom_sets_exerciseId` ON `custom_sets` (`exerciseId`)",
    "CREATE INDEX IF NOT EXISTS `index_custom_sets_day` ON `custom_sets` (`day`)",
    "CREATE TABLE IF NOT EXISTS `supplements` (`id` TEXT NOT NULL, `name` TEXT NOT NULL, `dose` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE TABLE IF NOT EXISTS `supplement_intakes` (`day` INTEGER NOT NULL, `supplementId` TEXT NOT NULL, PRIMARY KEY(`day`, `supplementId`))",
    "CREATE INDEX IF NOT EXISTS `index_supplement_intakes_supplementId` ON `supplement_intakes` (`supplementId`)",
)

/** Must match the Room v8 schema export exactly (schemas/com.formacoach.data.FitnessDatabase/8.json). */
internal val MIGRATION_7_8_SQL=listOf(
    "CREATE TABLE IF NOT EXISTS `health_weights` (`id` TEXT NOT NULL, `day` INTEGER NOT NULL, `weightKg` REAL NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_health_weights_day` ON `health_weights` (`day`)",
    "CREATE TABLE IF NOT EXISTS `health_workouts` (`id` TEXT NOT NULL, `day` INTEGER NOT NULL, `startAt` INTEGER NOT NULL, `endAt` INTEGER NOT NULL, `exerciseType` INTEGER NOT NULL, `title` TEXT NOT NULL, `source` TEXT NOT NULL, PRIMARY KEY(`id`))",
    "CREATE INDEX IF NOT EXISTS `index_health_workouts_day` ON `health_workouts` (`day`)",
)

/** Must match the Room v9 schema export exactly (schemas/com.formacoach.data.FitnessDatabase/9.json). */
internal val MIGRATION_8_9_SQL=listOf(
    "ALTER TABLE profile ADD COLUMN heightCm REAL",
    "CREATE TABLE IF NOT EXISTS `health_sleep` (`day` INTEGER NOT NULL, `asleepMinutes` REAL NOT NULL, `deepMinutes` REAL, `remMinutes` REAL, `lightMinutes` REAL, `awakeMinutes` REAL, `source` TEXT NOT NULL, PRIMARY KEY(`day`))",
)
