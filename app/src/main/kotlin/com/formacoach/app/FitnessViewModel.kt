package com.formacoach.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.formacoach.coach.WeeklyReviewEngine
import com.formacoach.data.FitnessRepository
import com.formacoach.model.*
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class AppState(
    val loading: Boolean = true,
    val snapshot: FitnessSnapshot = FitnessSnapshot(),
    val busy: Boolean = false,
    val error: Boolean = false,
    val loadFailed: Boolean = false,
)

@HiltViewModel
class FitnessViewModel @Inject constructor(private val repository: FitnessRepository) : ViewModel() {
    private val mutableState = MutableStateFlow(AppState())
    val state = mutableState.asStateFlow()
    val reviewEngine = WeeklyReviewEngine()

    init {
        viewModelScope.launch {
            repository.snapshot.catch { failure ->
                if (failure is CancellationException) throw failure
                mutableState.update { it.copy(loading=false, loadFailed=true) }
            }.collect { snapshot -> mutableState.update { it.copy(loading=false, snapshot=snapshot) } }
        }
    }

    fun clearError() = mutableState.update { it.copy(error=false) }

    private fun write(onSuccess: () -> Unit = {}, block: suspend () -> Unit) {
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
    fun start(template: String) = write { repository.startSession(template) }
    fun addSet(session: String, exercise: Exercise, reps: Int, kg: Double, done: () -> Unit) = write(done) {
        repository.addSet(TrainingSet(sessionId=session, exercise=exercise, reps=reps, weightKg=kg))
    }
    fun finish(id: String) = write { repository.finishSession(id) }
    fun removeSet(id: String) = write { repository.removeSet(id) }
    fun addMeal(meal: Meal, done: () -> Unit) = write(done) { repository.addMeal(meal) }
    fun repeatMeal(meal: Meal) = write { repository.addMeal(meal.copy(id=UUID.randomUUID().toString(), date=LocalDate.now())) }
    fun removeMeal(id: String) = write { repository.removeMeal(id) }
    fun measure(value: Measurement, done: () -> Unit) = write(done) { repository.saveMeasurement(value) }
    fun removeMeasurement(date: LocalDate) = write { repository.removeMeasurement(date) }
}
