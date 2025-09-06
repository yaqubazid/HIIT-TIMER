package com.hiittimer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class WorkoutViewModel : ViewModel() {

    // Get Ready Time
    private val _getReadyTime = MutableLiveData(10) // Default 10 seconds
    val getReadyTime: LiveData<Int> = _getReadyTime

    // Work Time
    private val _workTime = MutableLiveData(30) // Default 30 seconds
    val workTime: LiveData<Int> = _workTime

    // Rest Time
    private val _restTime = MutableLiveData(30) // Default 30 seconds
    val restTime: LiveData<Int> = _restTime

    // Cool Down Time
    private val _coolDownTime = MutableLiveData(30) // Default 30 seconds
    val coolDownTime: LiveData<Int> = _coolDownTime

    // Sets
    private val _sets = MutableLiveData(1) // Default 1 set
    val sets: LiveData<Int> = _sets

    // Timer State (represents WorkoutState)
    private val _timerState = MutableLiveData<TimerState>(TimerState.Stopped)
    val timerState: LiveData<TimerState> = _timerState //MainActivity will observe this as workoutState

    // Current time of the countdown timer - Now a MediatorLiveData
    private val _currentTime = MediatorLiveData<Int>()
    val currentTime: LiveData<Int> = _currentTime // MainActivity will observe this as countdownTime

    // Current phase of the workout
    private val _currentPhase = MutableLiveData<WorkoutPhase?>(null)
    val currentPhase: LiveData<WorkoutPhase?> = _currentPhase

    // Total Workout Time -  Calculated reactively
    private val _totalWorkoutTime = MediatorLiveData<Int>()
    val totalWorkoutTime: LiveData<Int> = _totalWorkoutTime

    init {
        // Initialize _currentTime
        _currentTime.value = _getReadyTime.value ?: 10

        // Add _getReadyTime as a source to _currentTime
        // _currentTime will update if _getReadyTime changes AND the timer is stopped
        _currentTime.addSource(_getReadyTime) { newGetReadyTime ->
            if (_timerState.value == TimerState.Stopped) {
                _currentTime.value = newGetReadyTime
            }
        }
        // Also ensure _currentTime resets to getReadyTime when timer is stopped
        _currentTime.addSource(_timerState) { state ->
            if (state == TimerState.Stopped) {
                 _currentTime.value = _getReadyTime.value ?: 10
            }
        }

        // Function to update total workout time
        val updateTotal = {
            val getReady = _getReadyTime.value ?: 0
            val work = _workTime.value ?: 0
            val rest = _restTime.value ?: 0
            val setCycles = _sets.value ?: 1
            val coolDown = _coolDownTime.value ?: 0

            val newTotalTime = if (setCycles <= 0) {
                getReady + coolDown // Or just 0 if no sets means no workout
            } else {
                // Time for (work + rest) cycles, subtracting the last rest if sets > 0
                val workAndRestCyclesTime = (work + rest) * setCycles - (if (setCycles > 0) rest else 0)
                getReady + workAndRestCyclesTime + coolDown
            }
            _totalWorkoutTime.value = newTotalTime.coerceAtLeast(0)
        }

        _totalWorkoutTime.addSource(_getReadyTime) { updateTotal() }
        _totalWorkoutTime.addSource(_workTime) { updateTotal() }
        _totalWorkoutTime.addSource(_restTime) { updateTotal() }
        _totalWorkoutTime.addSource(_coolDownTime) { updateTotal() }
        _totalWorkoutTime.addSource(_sets) { updateTotal() }
        
        updateTotal() // Initial calculation for total workout time
    }

    fun incrementGetReadyTime() {
        val newValue = (_getReadyTime.value ?: 0) + 5
        _getReadyTime.value = newValue
        // _currentTime is updated reactively by MediatorLiveData if state is Stopped
    }

    fun decrementGetReadyTime() {
        val currentValue = _getReadyTime.value ?: 0
        if (currentValue > 5) {
            val newValue = currentValue - 5
            _getReadyTime.value = newValue
            // _currentTime is updated reactively by MediatorLiveData if state is Stopped
        }
    }

    fun incrementWorkTime() {
        _workTime.value = (_workTime.value ?: 0) + 5
    }

    fun decrementWorkTime() {
        if ((_workTime.value ?: 0) > 5) {
            _workTime.value = (_workTime.value ?: 0) - 5
        }
    }

    fun incrementRestTime() {
        _restTime.value = (_restTime.value ?: 0) + 5
    }

    fun decrementRestTime() {
        if ((_restTime.value ?: 0) > 5) {
            _restTime.value = (_restTime.value ?: 0) - 5
        }
    }

    fun incrementCoolDownTime() {
        _coolDownTime.value = (_coolDownTime.value ?: 0) + 5
    }

    fun decrementCoolDownTime() {
        if ((_coolDownTime.value ?: 0) > 5) {
            _coolDownTime.value = (_coolDownTime.value ?: 0) - 5
        }
    }

    fun incrementSets() {
        _sets.value = (_sets.value ?: 1) + 1
    }

    fun decrementSets() {
        if ((_sets.value ?: 1) > 1) {
            _sets.value = (_sets.value ?: 1) - 1
        }
    }

    fun startWorkout() {
        if (_timerState.value == TimerState.Stopped) {
            // _currentTime should already be reflecting getReadyTime due to MediatorLiveData
            _currentPhase.value = WorkoutPhase.GET_READY
            // TODO: Actually start a timer mechanism here that updates _currentTime and _currentPhase
            _timerState.value = TimerState.Running // This should be set after timer starts
        }
        // If it's Paused, it should resume. If Running, it could pause.
        // For now, let's just make Start toggle between Running and Paused if not Stopped
        else if (_timerState.value == TimerState.Paused) {
            _timerState.value = TimerState.Running
             // TODO: Resume timer
        } else if (_timerState.value == TimerState.Running) {
            _timerState.value = TimerState.Paused
            // TODO: Pause timer
        }
    }

    fun resetWorkout() {
        // TODO: Cancel any ongoing timer mechanism
        _timerState.value = TimerState.Stopped // This will trigger _currentTime update via MediatorLiveData
        _currentPhase.value = null
    }

    // These might be used by an external timer class.
    // If WorkoutTimer is internal to ViewModel, these might not be needed or could be private.
    // For now, keeping them to allow external timer to update state if needed.
    fun setTimerStateInternal(state: TimerState) { 
        _timerState.value = state
    }

    fun setCurrentTimeInternal(time: Int) { 
        _currentTime.value = time
    }

    fun setCurrentPhaseInternal(phase: WorkoutPhase?) { 
        _currentPhase.value = phase
    }
}

sealed class TimerState {
    object Running : TimerState()
    object Paused : TimerState() 
    object Stopped : TimerState()
}

enum class WorkoutPhase(val displayName: String) {
    GET_READY("Get Ready"),
    WORK("Work"),
    REST("Rest"),
    COOL_DOWN("Cool Down")
}
