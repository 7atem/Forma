package com.formacoach.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formacoach.coach.WeeklyReviewEngine
import com.formacoach.coach.*
import com.formacoach.data.FitnessRepository
import com.formacoach.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
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
class FitnessViewModel @Inject constructor(
    private val repository: FitnessRepository,
    private val localModel: LocalNanoModel,
    val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    val reviewEngine = WeeklyReviewEngine(clock)
    private val writeMutex = Mutex()
    private val pendingWrites = AtomicInteger(0)
    private var aiJob: Job? = null
    private var snapshotJob: Job? = null
    private var aiGeneration = 0

    init {
        observeSnapshot()
    }

    private fun observeSnapshot() {
        snapshotJob?.cancel()
        snapshotJob = viewModelScope.launch {
            repository.snapshot.catch { failure ->
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(loading = false, loadFailed = true) }
            }.collect { snapshot ->
                mutableState.update {
                    it.copy(
                        loading = false,
                        loadFailed = false,
                        snapshot = snapshot,
                        highlights = if (it.snapshot == snapshot) it.highlights else emptyList(),
                    )
                }
            }
        }
    }

    /** Re-subscribe without deleting, replacing or resetting any persisted data. */
    fun retryLoad() {
        mutableState.update { it.copy(loading = true, loadFailed = false) }
        observeSnapshot()
    }

    fun clearError() = mutableState.update { it.copy(error = false) }

    internal fun write(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
        pendingWrites.incrementAndGet()
        mutableState.update { it.copy(busy = true, error = false) }
        viewModelScope.launch {
            try {
                writeMutex.withLock {
                    block()
                    onSuccess()
                }
            } catch (failure: CancellationException) {
                throw failure
            } catch (_: Exception) {
                mutableState.update { it.copy(error = true) }
            } finally {
                val remaining = pendingWrites.decrementAndGet().coerceAtLeast(0)
                mutableState.update { it.copy(busy = remaining > 0) }
            }
        }
    }

    fun saveProfile(profile: Profile, done: () -> Unit) = write(done) { repository.saveProfile(profile) }
    fun start(template: String, date: LocalDate = LocalDate.now(clock)) = write { repository.startSession(template, date) }
    fun startEmpty(date: LocalDate = LocalDate.now(clock)) = write { repository.startEmptySession(date) }
    fun addSet(session: String, exercise: Exercise, reps: Int, kg: Double, kind: SetKind = SetKind.WORKING, rpe: Double? = null, done: () -> Unit) = write(done) {
        repository.addSet(TrainingSet(sessionId = session, exercise = exercise, reps = reps, weightKg = kg, kind = kind, rpe = rpe))
    }
    fun quickLogExerciseSet(exercise: Exercise, reps: Int, kg: Double, kind: SetKind = SetKind.WORKING, rpe: Double? = null, date: LocalDate = LocalDate.now(clock), done: () -> Unit) = write(done) {
        repository.quickLogExerciseSet(exercise, reps, kg, kind, rpe, date)
    }
    fun finish(id: String, done: () -> Unit = {}) = write(done) { repository.finishSession(id) }
    fun logSet(value: TrainingSet, done: (List<TrainingSet>) -> Unit) = write { done(repository.logSet(value)) }
    fun removeSet(id: String) = write { repository.removeSet(id) }
    fun restoreSet(set: TrainingSet, orderedSessionSetIds: List<String>) = write { repository.restoreSet(set, orderedSessionSetIds) }
    fun addMeal(meal: Meal, done: () -> Unit) = write(done) { repository.addMeal(meal) }
    fun repeatMeal(meal: Meal) = write { repository.addMeal(meal.copy(id = UUID.randomUUID().toString(), date = LocalDate.now(clock))) }
    fun removeMeal(id: String) = write { repository.removeMeal(id) }
    fun copyMealDay(expected:List<Meal>,target:LocalDate,done:()->Unit)=write(done) {repository.copyMealDay(expected,target)}
    fun measure(value: Measurement, done: () -> Unit) = write(done) { repository.saveMeasurement(value) }
    fun removeMeasurement(date: LocalDate) = write { repository.removeMeasurement(date) }
    fun saveHydration(value: Hydration, done: () -> Unit = {}) = write(done) { repository.saveHydration(value) }
    /** Read-only Health Connect copies, observed only while a screen collects them. */
    val healthImports: StateFlow<HealthImports> =
        repository.healthImports.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HealthImports())
    fun importHealth(weights: List<ImportedWeight>, workouts: List<ImportedWorkout>, sleep: List<ImportedSleep>, done: () -> Unit) =
        write(done) { repository.importHealth(weights, workouts, sleep) }
    fun removeHealthImports() = write { repository.removeHealthImports() }
    fun saveCustomExercise(value: CustomExercise, done: () -> Unit = {}) = write(done) { repository.saveCustomExercise(value) }
    fun deleteCustomExercise(id: String) = write { repository.deleteCustomExercise(id) }
    fun addCustomSet(value: CustomExerciseSet, done: () -> Unit = {}) = write(done) { repository.addCustomSet(value) }
    fun saveCardio(value: CardioEntry, done: () -> Unit = {}) = write(done) { repository.saveCardio(value) }
    fun removeCardio(id: String) = write { repository.removeCardio(id) }
    fun saveSupplement(value: Supplement, done: () -> Unit = {}) = write(done) { repository.saveSupplement(value) }
    fun deleteSupplement(id: String) = write { repository.deleteSupplement(id) }
    fun setSupplementTaken(date: LocalDate, id: String, taken: Boolean) = write { repository.setSupplementTaken(date, id, taken) }
    /** Moves records older builds kept in plain preferences into the encrypted database; [done] runs only after the commit. */
    internal fun importLocalRecords(records: LegacyLocalRecords, done: () -> Unit) =
        write(done) { repository.importLocalRecords(records.exercises, records.sets, records.supplements, records.intakes) }
    fun correctSet(set: TrainingSet, done: () -> Unit) = write(done) {repository.correctSet(set)}
    fun moveSession(id: String,date: LocalDate) = write {repository.moveSession(id,date)}
    fun deleteSession(id: String) = write {repository.deleteSession(id)}
    fun saveProgram(program: TrainingProgram,done: () -> Unit) = write(done) {repository.saveProgram(program)}
    fun deleteProgram(id: String) = write {repository.deleteProgram(id)}
    fun favorite(exercise:Exercise,value:Boolean)=write {repository.favorite(exercise,value)}
    fun exerciseNotes(exercise:Exercise,value:String,done:()->Unit)=write(done) {repository.exerciseNotes(exercise,value)}
    fun repeatSession(id:String)=write {repository.repeatSession(id)}
    fun saveFood(food: Food,done: () -> Unit) = write(done) {repository.saveFood(food)}
    fun saveFoods(foods: List<Food>, done: () -> Unit = {}) = write(done) { foods.forEach { repository.saveFood(it) } }
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
        val day=LocalDate.now(clock)
        val facts=LocalHighlights.facts(basis,reviewEngine.review(basis,day))
        val selected=LocalHighlights.select(localModel,facts,isForeground)
        mutableState.update {it.copy(highlights=if(it.snapshot==basis && LocalDate.now(clock)==day) selected.orEmpty() else emptyList(),aiFailed=selected==null)}
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
