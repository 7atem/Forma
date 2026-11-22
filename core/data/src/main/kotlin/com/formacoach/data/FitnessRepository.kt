package com.formacoach.data

import android.content.Context
import androidx.sqlite.db.SimpleSQLiteQuery
import com.formacoach.backup.PortableBackup
import com.formacoach.model.*
import java.time.Clock
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.withContext

class FitnessRepository(daoProvider:()->FitnessDao,private val clock:Clock=DeviceClock) {
    companion object {fun open(context:Context)=FitnessRepository({FitnessDatabase.open(context).dao()})}
    private val dao by lazy(daoProvider)
    val snapshot=flow {emitAll(dao.changes(SimpleSQLiteQuery("SELECT 1")).map {dao.readSnapshot()})}.flowOn(Dispatchers.IO)
    private fun checkDate(date:LocalDate) {require(date in LocalDate.of(1970,1,1)..LocalDate.now(clock))}
    suspend fun saveProfile(profile:Profile)=dao.saveProfile(profile.row())
    suspend fun startSession(templateId:String,date:LocalDate=LocalDate.now(clock)):String {
        checkDate(date)
        val starter=StarterTemplates.all.firstOrNull {it.id==templateId}
        val program=if(starter==null) checkNotNull(dao.program(templateId)) else null
        val session=Session(date=date,templateId=templateId,label=program?.name.orEmpty(),
            plannedExercises=starter?.exercises?:kotlinx.serialization.json.Json.decodeFromString(program!!.exercises),restSeconds=program?.restSeconds?:90,
            prescriptions=program?.let {kotlinx.serialization.json.Json.decodeFromString(it.prescriptions)}?:emptyList())
        dao.startSession(session.row());return session.id
    }
    suspend fun addSet(set:TrainingSet)=dao.addSet(set.row())
    suspend fun quickLogExerciseSet(exercise:Exercise,reps:Int,weightKg:Double,kind:SetKind=SetKind.WORKING,rpe:Double?=null,date:LocalDate=LocalDate.now(clock)) {
        checkDate(date)
        val fallback=Session(date=date,templateId="quick",label="",plannedExercises=listOf(exercise),restSeconds=90)
        val set=TrainingSet(sessionId=fallback.id,exercise=exercise,reps=reps,weightKg=weightKg,kind=kind,rpe=rpe)
        dao.quickLogExerciseSet(fallback.row(),set.row())
    }
    suspend fun logSet(set:TrainingSet):List<TrainingSet> = dao.logSetAndRead(set.row()).map {TrainingSet(it.id,it.sessionId,Exercise.valueOf(it.exercise),it.reps,it.weightKg,SetKind.valueOf(it.kind),it.rpe)}
    suspend fun correctSet(set:TrainingSet)=dao.correctSet(set.row())
    suspend fun finishSession(id:String) {check(dao.finishSession(id,clock.millis())==1)}
    suspend fun removeSet(id:String)=dao.removeSet(id)
    suspend fun restoreSet(set:TrainingSet,orderedSessionSetIds:List<String>)=dao.restoreSet(set.row(),orderedSessionSetIds)
    suspend fun moveSession(id:String,date:LocalDate) {checkDate(date);dao.moveSession(id,date.toEpochDay())}
    suspend fun deleteSession(id:String)=dao.deleteSession(id)
    suspend fun addMeal(meal:Meal) {checkDate(meal.date);dao.addMeal(meal.row())}
    suspend fun removeMeal(id:String)=dao.removeMeal(id)
    suspend fun copyMealDay(expected:List<Meal>,target:LocalDate) {
        checkDate(target);expected.forEach {checkDate(it.date)};dao.copyMealDay(expected.map {it.row()},target.toEpochDay())
    }
    suspend fun saveMeasurement(value:Measurement) {checkDate(value.date);dao.saveMeasurement(MeasurementRow(value.date.toEpochDay(),value.weightKg,value.waistCm))}
    suspend fun removeMeasurement(date:LocalDate)=dao.removeMeasurement(date.toEpochDay())
    suspend fun saveProgram(program:TrainingProgram)=dao.saveProgram(program.row())
    suspend fun deleteProgram(id:String)=dao.deleteProgram(id)
    suspend fun favorite(exercise:Exercise,value:Boolean)=dao.favorite(exercise.name,value)
    suspend fun exerciseNotes(exercise:Exercise,value:String) {
        require(value.length<=2000);dao.exerciseNotes(exercise.name,value)
    }
    /** Repeat the exercise order, never completed sets, sensor data or old IDs. */
    suspend fun repeatSession(id:String,date:LocalDate=LocalDate.now(clock)) {
        checkDate(date)
        val old=checkNotNull(dao.session(id));check(old.completedAt!=null)
        val order=(kotlinx.serialization.json.Json.decodeFromString<List<Exercise>>(old.exerciseOrder)+dao.sessionSets(id).map {Exercise.valueOf(it.exercise)}).distinct()
        require(order.size in 1..20)
        dao.startSession(Session(date=date,templateId="repeat",label=old.label,plannedExercises=order,restSeconds=old.restSeconds,
            prescriptions=kotlinx.serialization.json.Json.decodeFromString(old.prescriptions)).row())
    }
    suspend fun saveFood(food:Food)=dao.saveFood(food.row())
    suspend fun deleteFood(id:String)=dao.deleteFood(id)
    suspend fun recordCheckIn(checkIn:WeeklyCheckIn) {
        checkDate(checkIn.date)
        val data=dao.readSnapshot()
        val evidence=data.sessions.filter {it.completedAt!=null && it.date in checkIn.date.minusDays(6)..checkIn.date}.map {it.id}
        dao.recordCheckIn(checkIn.row(),evidence)
    }
    suspend fun acceptProposal(id:String)=dao.acceptProposal(id)
    suspend fun dismissProposal(id:String)=dao.dismissProposal(id)
    suspend fun importWatch(transfer:com.formacoach.wearables.WatchTransfer)=withContext(Dispatchers.IO) {
        require(transfer.workouts.isNotEmpty())
        transfer.workouts.forEach {checkDate(it.date);require(it.completedAtMillis<=clock.millis()+300_000)}
        dao.mergeWatch(transfer.workouts)
    }
    suspend fun export(password:CharArray):ByteArray=withContext(Dispatchers.IO) {
        try {PortableBackup.encrypt(dao.readSnapshot(),password,clock.millis())} finally {password.fill('\u0000')}
    }
    suspend fun previewRestore(bytes:ByteArray,password:CharArray):FitnessSnapshot=withContext(Dispatchers.IO) {
        try {PortableBackup.decrypt(bytes,password).data} finally {password.fill('\u0000');bytes.fill(0)}
    }
    suspend fun restore(data:FitnessSnapshot)=withContext(Dispatchers.IO) {PortableBackup.validate(data);dao.replaceSnapshot(data)}
}
