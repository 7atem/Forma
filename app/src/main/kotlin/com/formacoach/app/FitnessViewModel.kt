package com.formacoach.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formacoach.coach.WeeklyReviewEngine
import com.formacoach.coach.*
import com.formacoach.data.FitnessRepository
import com.formacoach.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.withTimeoutOrNull

data class AppState(
    val loading: Boolean = true,
    val snapshot: FitnessSnapshot = FitnessSnapshot(),
    val busy: Boolean = false,
    val error: Boolean = false,
    val loadFailed: Boolean = false,
    val modelAvailability: ModelAvailability = ModelAvailability.NOT_CHECKED,
    val aiBusy: Boolean = false,
    val aiFailed: Boolean = false,
    val highlights: List<String> = emptyList(),
)

@HiltViewModel
class FitnessViewModel @Inject constructor(private val repository: FitnessRepository,private val localModel:LocalNanoModel) : ViewModel() {
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    val reviewEngine = WeeklyReviewEngine()
    private var aiJob:Job?=null
    private var aiGeneration=0

    init {
        viewModelScope.launch {
            repository.snapshot.catch { failure ->
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(loading=false, loadFailed=true) }
            }.collect { snapshot -> mutableState.update { it.copy(loading=false,snapshot=snapshot,highlights=if(it.snapshot==snapshot) it.highlights else emptyList()) } }
        }
    }

    fun clearError() = mutableState.update { it.copy(error=false) }

    internal fun write(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        if (mutableState.value.busy) return
        mutableState.update { it.copy(busy=true, error=false) }
        viewModelScope.launch {
            try {
                block()
                onSuccess()
            } catch (failure: CancellationException) { throw failure
            } catch (_: Exception) { mutableState.update { it.copy(error=true) }
            } finally { mutableState.update { it.copy(busy=false) } }
        }
    }

    fun saveProfile(profile: Profile, done: () -> Unit) = write(done) { repository.saveProfile(profile) }
    fun start(template: String, date: LocalDate = LocalDate.now()) = write { repository.startSession(template,date) }
    fun addSet(session: String, exercise: Exercise, reps: Int, kg: Double, kind:SetKind=SetKind.WORKING,rpe:Double?=null,done: () -> Unit) = write(done) {
        repository.addSet(TrainingSet(sessionId=session, exercise=exercise, reps=reps, weightKg=kg,kind=kind,rpe=rpe))
    }
    fun finish(id: String,done:()->Unit={}) = write(done) { repository.finishSession(id) }
    fun removeSet(id: String) = write { repository.removeSet(id) }
    fun addMeal(meal: Meal, done: () -> Unit) = write(done) { repository.addMeal(meal) }
    fun repeatMeal(meal: Meal) = write { repository.addMeal(meal.copy(id=UUID.randomUUID().toString(), date=LocalDate.now())) }
    fun removeMeal(id: String) = write { repository.removeMeal(id) }
    fun copyMealDay(expected:List<Meal>,target:LocalDate,done:()->Unit)=write(done) {repository.copyMealDay(expected,target)}
    fun measure(value: Measurement, done: () -> Unit) = write(done) { repository.saveMeasurement(value) }
    fun removeMeasurement(date: LocalDate) = write { repository.removeMeasurement(date) }
    fun correctSet(set: TrainingSet, done: () -> Unit) = write(done) {repository.correctSet(set)}
    fun moveSession(id: String,date: LocalDate) = write {repository.moveSession(id,date)}
    fun deleteSession(id: String) = write {repository.deleteSession(id)}
    fun saveProgram(program: TrainingProgram,done: () -> Unit) = write(done) {repository.saveProgram(program)}
    fun deleteProgram(id: String) = write {repository.deleteProgram(id)}
    fun favorite(exercise:Exercise,value:Boolean)=write {repository.favorite(exercise,value)}
    fun exerciseNotes(exercise:Exercise,value:String,done:()->Unit)=write(done) {repository.exerciseNotes(exercise,value)}
    fun repeatSession(id:String)=write {repository.repeatSession(id)}
    fun saveFood(food: Food,done: () -> Unit) = write(done) {repository.saveFood(food)}
    fun deleteFood(id: String) = write {repository.deleteFood(id)}
    fun checkIn(value: WeeklyCheckIn,done: () -> Unit) = write(done) {repository.recordCheckIn(value)}
    fun acceptProposal(id: String) = write {repository.acceptProposal(id)}
    fun dismissProposal(id: String) = write {repository.dismissProposal(id)}
    fun importWatch(transfer:com.formacoach.wearables.WatchTransfer,done:()->Unit)=write(done) {repository.importWatch(transfer)}
    suspend fun export(password: CharArray): ByteArray = repository.export(password)
    suspend fun previewRestore(bytes: ByteArray,password: CharArray): FitnessSnapshot = repository.previewRestore(bytes,password)
    fun restore(data: FitnessSnapshot,done: () -> Unit) = write(done) {
        repository.restore(data)
        mutableState.update {it.copy(snapshot=data)}
    }
    fun cancelAi() {aiGeneration++;aiJob?.cancel();mutableState.update {it.copy(aiBusy=false)}}
    fun onDayChanged() {cancelAi();mutableState.update {it.copy(highlights=emptyList())}}
    fun checkModel()=modelAction {
        val status=withTimeoutOrNull(15_000) {localModel.status()}?:ModelAvailability.ERROR
        mutableState.update {it.copy(modelAvailability=status)}
    }
    fun downloadModel()=modelAction {
        check(localModel.status()==ModelAvailability.DOWNLOADABLE)
        localModel.download().collect {value ->mutableState.update {it.copy(modelAvailability=value)}}
    }
    fun selectHighlights(isForeground:()->Boolean)=modelAction {
        val basis=mutableState.value.snapshot
        val day=LocalDate.now()
        val facts=LocalHighlights.facts(basis,reviewEngine.review(basis,day))
        val selected=LocalHighlights.select(localModel,facts,isForeground)
        mutableState.update {it.copy(highlights=if(it.snapshot==basis && LocalDate.now()==day) selected.orEmpty() else emptyList(),aiFailed=selected==null)}
    }
    private fun modelAction(block:suspend ()->Unit) {
        if(mutableState.value.aiBusy) return
        val generation=++aiGeneration
        mutableState.update {it.copy(aiBusy=true,aiFailed=false)}
        aiJob=viewModelScope.launch {
            try {block()} catch(failure:CancellationException) {throw failure
            } catch(_:Exception) {mutableState.update {it.copy(aiFailed=true,modelAvailability=ModelAvailability.ERROR)}
            } finally {if(generation==aiGeneration) mutableState.update {it.copy(aiBusy=false)}}
        }
    }
}
