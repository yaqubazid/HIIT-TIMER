package com.hiittimer

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.MobileAds
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var intervalSetupLayout: LinearLayout
    private lateinit var setsCountText: EditText

    // New EditTexts for minutes and seconds
    private lateinit var getReadyMinutesText: EditText
    private lateinit var getReadySecondsText: EditText
    private lateinit var workMinutesText: EditText
    private lateinit var workSecondsText: EditText
    private lateinit var restMinutesText: EditText
    private lateinit var restSecondsText: EditText
    private lateinit var coolDownMinutesText: EditText
    private lateinit var coolDownSecondsText: EditText

    private lateinit var totalTimeText: EditText
    private lateinit var timerStatusText: TextView
    private lateinit var countdownTimerText: TextView

    private lateinit var setsMinusBtn: ImageButton
    private lateinit var setsPlusBtn: ImageButton
    private lateinit var getReadyMinusBtn: ImageButton
    private lateinit var getReadyPlusBtn: ImageButton
    private lateinit var workMinusBtn: ImageButton
    private lateinit var workPlusBtn: ImageButton
    private lateinit var restMinusBtn: ImageButton
    private lateinit var restPlusBtn: ImageButton
    private lateinit var coolDownMinusBtn: ImageButton
    private lateinit var coolDownPlusBtn: ImageButton

    private lateinit var startBtn: Button
    private lateinit var resetBtn: Button
    private lateinit var skipBtn: Button
    private lateinit var bottomNavigationView: BottomNavigationView
    private lateinit var adView: AdView

    private var setsCount = 5
    private var getReadyTime = 10 // Total seconds
    private var workTime = 30     // Total seconds
    private var restTime = 15     // Total seconds
    private var coolDownTime = 20 // Total seconds

    private var currentTimer: CountDownTimer? = null
    private var timeLeftInMillis: Long = 0

    private enum class TimerState {
        IDLE, GET_READY, WORK, REST, COOL_DOWN, PAUSED
    }

    private var currentTimerState: TimerState = TimerState.IDLE
    private var stateBeforePause: TimerState = TimerState.IDLE
    private var currentSet: Int = 1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        MobileAds.initialize(this) {}
        adView = findViewById(R.id.adView)
        val adRequest = AdRequest.Builder().build()
        adView.loadAd(adRequest)

        val toolbar: androidx.appcompat.widget.Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        intervalSetupLayout = findViewById(R.id.interval_setup_layout)
        setsCountText = findViewById(R.id.sets_count_text)

        // Initialize new EditTexts for minutes and seconds
        getReadyMinutesText = findViewById(R.id.get_ready_minutes)
        getReadySecondsText = findViewById(R.id.get_ready_seconds)
        workMinutesText = findViewById(R.id.work_minutes)
        workSecondsText = findViewById(R.id.work_seconds)
        restMinutesText = findViewById(R.id.rest_minutes)
        restSecondsText = findViewById(R.id.rest_seconds)
        coolDownMinutesText = findViewById(R.id.cool_down_minutes)
        coolDownSecondsText = findViewById(R.id.cool_down_seconds)

        totalTimeText = findViewById(R.id.total_workout_time_value)
        timerStatusText = findViewById(R.id.timer_status)
        countdownTimerText = findViewById(R.id.countdown_timer)

        setsMinusBtn = findViewById(R.id.sets_minus_btn)
        setsPlusBtn = findViewById(R.id.sets_plus_btn)
        getReadyMinusBtn = findViewById(R.id.get_ready_minus_btn)
        getReadyPlusBtn = findViewById(R.id.get_ready_plus_btn)
        workMinusBtn = findViewById(R.id.work_minus_btn)
        workPlusBtn = findViewById(R.id.work_plus_btn)
        restMinusBtn = findViewById(R.id.rest_minus_btn)
        restPlusBtn = findViewById(R.id.rest_plus_btn)
        coolDownMinusBtn = findViewById(R.id.cool_down_minus_btn)
        coolDownPlusBtn = findViewById(R.id.cool_down_plus_btn)

        startBtn = findViewById(R.id.start_btn)
        resetBtn = findViewById(R.id.reset_btn)
        skipBtn = findViewById(R.id.skip_btn)

        bottomNavigationView = findViewById(R.id.bottom_navigation)
        bottomNavigationView.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.navigation_save -> {
                    showSaveWorkoutDialog()
                    true
                }
                R.id.navigation_folder -> {
                    showLoadWorkoutDialog()
                    true
                }
                R.id.navigation_settings -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }

        setupEditTextListeners()

        updateAllSetupTextViews()
        updateTimerStatusText()
        updateTimerDisplay(getReadyTime)
        skipBtn.visibility = View.GONE
        skipBtn.isEnabled = false

        setsMinusBtn.setOnClickListener { if (setsCount > 0) setsCount--; updateSetsCountText(); updateTotalTimeDisplay() }
        setsPlusBtn.setOnClickListener { setsCount++; updateSetsCountText(); updateTotalTimeDisplay() }

        getReadyMinusBtn.setOnClickListener { if (getReadyTime > 0) getReadyTime -= 1; updateGetReadyTimeText(); updateTotalTimeDisplay(); if (currentTimerState == TimerState.IDLE) updateTimerDisplay(getReadyTime) }
        getReadyPlusBtn.setOnClickListener { getReadyTime += 1; updateGetReadyTimeText(); updateTotalTimeDisplay(); if (currentTimerState == TimerState.IDLE) updateTimerDisplay(getReadyTime) }
        workMinusBtn.setOnClickListener { if (workTime > 0) workTime -= 1; updateWorkTimeText(); updateTotalTimeDisplay() }
        workPlusBtn.setOnClickListener { workTime += 1; updateWorkTimeText(); updateTotalTimeDisplay() }
        restMinusBtn.setOnClickListener { if (restTime > 0) restTime -= 1; updateRestTimeText(); updateTotalTimeDisplay() }
        restPlusBtn.setOnClickListener { restTime += 1; updateRestTimeText(); updateTotalTimeDisplay() }
        coolDownMinusBtn.setOnClickListener { if (coolDownTime > 0) coolDownTime -= 1; updateCoolDownTimeText(); updateTotalTimeDisplay() }
        coolDownPlusBtn.setOnClickListener { coolDownTime += 1; updateCoolDownTimeText(); updateTotalTimeDisplay() }

        startBtn.setOnClickListener {
            when (currentTimerState) {
                TimerState.IDLE -> startTimerFlow()
                TimerState.PAUSED -> resumeTimer()
                else -> pauseTimer()
            }
        }
        resetBtn.setOnClickListener { resetTimer() }
        skipBtn.setOnClickListener {
            if (currentTimerState != TimerState.IDLE && currentTimerState != TimerState.PAUSED) {
                currentTimer?.cancel()
                startNextPhase()
            }
        }
    }

    private fun setupEditTextListeners() {
        val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager

        fun setupNumericInputListener(
            editText: EditText,
            originalValueProvider: () -> Int,
            valueUpdater: (Int) -> Unit,
            textUpdater: () -> Unit
        ) {
            var valueWhenFocused = originalValueProvider()
            editText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    valueWhenFocused = originalValueProvider()
                    editText.setText(valueWhenFocused.toString())
                    editText.selectAll()
                } else {
                    val inputText = editText.text.toString()
                    val newValue = inputText.toIntOrNull()
                    if (newValue != null && newValue >= 0) {
                        valueUpdater(newValue)
                    } else {
                        valueUpdater(valueWhenFocused) // Revert
                        Toast.makeText(this@MainActivity, "Invalid number. Must be non-negative.", Toast.LENGTH_SHORT).show()
                    }
                    textUpdater()
                    updateTotalTimeDisplay()
                }
            }
            editText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    val inputText = v.text.toString()
                    val newValue = inputText.toIntOrNull()
                    if (newValue != null && newValue >= 0) {
                        valueUpdater(newValue)
                    } else {
                        valueUpdater(valueWhenFocused) // Revert
                        Toast.makeText(this@MainActivity, "Invalid number. Must be non-negative.", Toast.LENGTH_SHORT).show()
                    }
                    textUpdater()
                    updateTotalTimeDisplay()
                    v.clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    return@setOnEditorActionListener true
                }
                false
            }
        }

        setupNumericInputListener(setsCountText, { setsCount }, { setsCount = it }, ::updateSetsCountText)

        fun setupTimeInputListeners(
            minutesEditText: EditText,
            secondsEditText: EditText,
            totalSecondsProvider: () -> Int,    // Provides current total seconds for this time type
            totalSecondsUpdater: (Int) -> Unit, // Updates the model's total seconds
            textFieldsUpdater: () -> Unit,       // Calls the function to update min/sec EditTexts (e.g., ::updateGetReadyTimeText)
            isTimerRelevantField: Boolean = false // True if this field should update countdownTimerText when idle
        ) {
            var minutesWhenFocused: Int
            var secondsWhenFocused: Int

            val processInput = {
                val minStr = minutesEditText.text.toString()
                val secStr = secondsEditText.text.toString()

                val inputMinutes = minStr.toIntOrNull() ?: 0
                val inputSeconds = secStr.toIntOrNull() ?: 0

                if (inputMinutes >= 0 && inputSeconds in 0..59) {
                    val newTotalSeconds = (inputMinutes * 60) + inputSeconds
                    totalSecondsUpdater(newTotalSeconds)
                } else {
                    // Revert to original values if input is invalid
                    Toast.makeText(this@MainActivity, "Invalid time (seconds 0-59).", Toast.LENGTH_SHORT).show()
                    // totalSecondsUpdater is not called, model keeps old value
                }
                textFieldsUpdater() // Always call to reformat/revert display
                updateTotalTimeDisplay()
                if (isTimerRelevantField && currentTimerState == TimerState.IDLE) {
                    updateTimerDisplay(totalSecondsProvider())
                }
            }

            minutesEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val currentTotal = totalSecondsProvider()
                    minutesWhenFocused = currentTotal / 60
                    secondsWhenFocused = currentTotal % 60
                    minutesEditText.setText(minutesWhenFocused.toString())
                    minutesEditText.selectAll()
                } else {
                    processInput() // Process when focus is lost from minutes field
                }
            }
            minutesEditText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_NEXT || actionId == EditorInfo.IME_ACTION_DONE) {
                    processInput()
                    if (actionId == EditorInfo.IME_ACTION_NEXT) {
                        secondsEditText.requestFocus()
                        secondsEditText.selectAll()
                    } else { // IME_ACTION_DONE
                        v.clearFocus()
                        inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    }
                    return@setOnEditorActionListener true
                }
                false
            }

            secondsEditText.setOnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    val currentTotal = totalSecondsProvider()
                    minutesWhenFocused = currentTotal / 60 // Though not directly used by seconds, good to have consistent snapshot
                    secondsWhenFocused = currentTotal % 60
                    secondsEditText.setText(secondsWhenFocused.toString())
                    secondsEditText.selectAll()
                } else {
                    processInput() // Process when focus is lost from seconds field
                }
            }
            secondsEditText.setOnEditorActionListener { v, actionId, _ ->
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    processInput()
                    v.clearFocus()
                    inputMethodManager.hideSoftInputFromWindow(v.windowToken, 0)
                    return@setOnEditorActionListener true
                }
                false
            }
        }

        setupTimeInputListeners(getReadyMinutesText, getReadySecondsText, { getReadyTime }, { getReadyTime = it }, ::updateGetReadyTimeText, true)
        setupTimeInputListeners(workMinutesText, workSecondsText, { workTime }, { workTime = it }, ::updateWorkTimeText)
        setupTimeInputListeners(restMinutesText, restSecondsText, { restTime }, { restTime = it }, ::updateRestTimeText)
        setupTimeInputListeners(coolDownMinutesText, coolDownSecondsText, { coolDownTime }, { coolDownTime = it }, ::updateCoolDownTimeText, true)
    }


    private fun showSaveWorkoutDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_save_workout, null)
        val workoutTitleEditText = dialogView.findViewById<EditText>(R.id.workout_title_edittext)

        AlertDialog.Builder(this, R.style.AlertDialogCustomStyle)
            .setView(dialogView)
            .setPositiveButton("Save") { _, _ ->
                val title = workoutTitleEditText.text.toString().trim()
                if (title.isNotBlank()) {
                    val presetToSave = WorkoutPreset(
                        title = title,
                        sets = setsCount,
                        getReadyTimeSeconds = getReadyTime,
                        workTimeSeconds = workTime,
                        restTimeSeconds = restTime,
                        coolDownTimeSeconds = coolDownTime
                    )
                    val dao = AppDatabase.getDatabase(applicationContext).workoutPresetDao()
                    lifecycleScope.launch {
                        dao.insertPreset(presetToSave)
                        Log.d("SaveWorkout", "WorkoutPreset saved to database: $presetToSave")
                        Toast.makeText(this@MainActivity, "Workout '$title' saved!", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Title cannot be empty", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .create()
            .show()
    }

    @SuppressLint("InflateParams")
    private fun showLoadWorkoutDialog() {
        val dao = AppDatabase.getDatabase(applicationContext).workoutPresetDao()
        lifecycleScope.launch {
            val presetsList: List<WorkoutPreset>? = dao.getAllPresets().firstOrNull()

            if (presetsList.isNullOrEmpty()) {
                Toast.makeText(this@MainActivity, "No saved presets found", Toast.LENGTH_SHORT).show()
                return@launch
            }

            val mutablePresets = presetsList.toMutableList()
            lateinit var loadDialog: AlertDialog

            val adapter = PresetAdapter(
                this@MainActivity,
                mutablePresets,
                onPresetLoad = { preset ->
                    applyPresetToUI(preset)
                    loadDialog.dismiss()
                },
                onPresetDelete = { presetToDelete ->
                    AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustomStyle)
                        .setTitle("Delete Preset")
                        .setMessage("Are you sure you want to delete '${presetToDelete.title}'?")
                        .setPositiveButton("Delete") { _, _ ->
                            lifecycleScope.launch {
                                dao.deletePreset(presetToDelete.title)
                                val currentAdapter = loadDialog.listView.adapter as? PresetAdapter
                                currentAdapter?.removePreset(presetToDelete)
                                Toast.makeText(this@MainActivity, "Preset '${presetToDelete.title}' deleted", Toast.LENGTH_SHORT).show()
                                if (currentAdapter?.count == 0) {
                                    loadDialog.dismiss()
                                    Toast.makeText(this@MainActivity, "No saved presets remaining", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .setNegativeButton("Cancel", null)
                        .show()
                }
            )

            // Inflate the custom title layout
            val customTitleView = LayoutInflater.from(this@MainActivity).inflate(R.layout.dialog_title_centered, null, false)

            val builder = AlertDialog.Builder(this@MainActivity, R.style.AlertDialogCustomStyle)
                .setCustomTitle(customTitleView) // Use setCustomTitle
                .setAdapter(adapter, null)
                .setNegativeButton("Cancel", null)

            loadDialog = builder.create()
            loadDialog.show()
        }
    }

    private fun applyPresetToUI(preset: WorkoutPreset) {
        setsCount = preset.sets
        getReadyTime = preset.getReadyTimeSeconds
        workTime = preset.workTimeSeconds
        restTime = preset.restTimeSeconds
        coolDownTime = preset.coolDownTimeSeconds

        updateAllSetupTextViews()
        if (currentTimerState == TimerState.IDLE) {
            updateTimerDisplay(getReadyTime)
        }
        resetTimerLogic() // Also updates timer status text implicitly
        // updateTimerStatusText() // Already called by resetTimerLogic

        Toast.makeText(this, "Workout '${preset.title}' loaded", Toast.LENGTH_SHORT).show()
    }

    private fun updateAllSetupTextViews() {
        updateSetsCountText()
        updateGetReadyTimeText()
        updateWorkTimeText()
        updateRestTimeText()
        updateCoolDownTimeText()
        updateTotalTimeDisplay()
    }

    private fun startTimerFlow() {
        if (currentTimerState != TimerState.IDLE) return
        toggleInputs(false) // MODIFIED HERE
        currentSet = 1
        skipBtn.visibility = View.VISIBLE
        skipBtn.isEnabled = true
        startNextPhase()
    }

    private fun startNextPhase() {
        currentTimer?.cancel()
        var nextTimeToCountdown = 0L
        val previousState = currentTimerState

        when (previousState) {
            TimerState.IDLE -> {
                if (getReadyTime > 0) {
                    currentTimerState = TimerState.GET_READY
                    nextTimeToCountdown = getReadyTime * 1000L
                } else {
                    currentTimerState = TimerState.GET_READY // Move to WORK/REST/COOLDOWN even if 0
                    startNextPhase()
                    return
                }
            }
            TimerState.GET_READY -> {
                if (currentSet <= setsCount && workTime > 0) {
                    currentTimerState = TimerState.WORK
                    nextTimeToCountdown = workTime * 1000L
                } else if (currentSet <= setsCount && workTime == 0 && restTime > 0) { // Skip work if 0, go to rest
                    currentTimerState = TimerState.REST
                    nextTimeToCountdown = restTime * 1000L
                } else if (coolDownTime > 0) { // No work/rest or sets finished
                    currentTimerState = TimerState.COOL_DOWN
                    nextTimeToCountdown = coolDownTime * 1000L
                } else { // No work/rest/cooldown
                    timerFinishedAllPhases()
                    return
                }
            }
            TimerState.WORK -> {
                if (restTime > 0) {
                    currentTimerState = TimerState.REST
                    nextTimeToCountdown = restTime * 1000L
                } else { // No rest time, advance set and check for next work/cooldown
                    currentSet++
                    if (currentSet <= setsCount && workTime > 0) {
                        currentTimerState = TimerState.WORK
                        nextTimeToCountdown = workTime * 1000L
                    } else if (coolDownTime > 0) { // Sets finished or no more work
                        currentTimerState = TimerState.COOL_DOWN
                        nextTimeToCountdown = coolDownTime * 1000L
                    } else { // Sets finished, no cooldown
                        timerFinishedAllPhases()
                        return
                    }
                }
            }
            TimerState.REST -> {
                currentSet++
                if (currentSet <= setsCount && workTime > 0) {
                    currentTimerState = TimerState.WORK
                    nextTimeToCountdown = workTime * 1000L
                } else if (currentSet <= setsCount && workTime == 0 && restTime > 0) { // Next set is also only rest
                    currentTimerState = TimerState.REST
                    nextTimeToCountdown = restTime * 1000L
                } else if (coolDownTime > 0) { // Sets finished
                    currentTimerState = TimerState.COOL_DOWN
                    nextTimeToCountdown = coolDownTime * 1000L
                } else { // Sets finished, no cooldown
                    timerFinishedAllPhases()
                    return
                }
            }
            TimerState.COOL_DOWN -> {
                timerFinishedAllPhases()
                return
            }
            TimerState.PAUSED -> {
                Log.e("TimerLogic", "startNextPhase called from PAUSED state unexpectedly. Trying to resume previous state.")
                currentTimerState = stateBeforePause
                if (timeLeftInMillis > 0) startCountdown(timeLeftInMillis) else timerFinishedAllPhases() // Resume or finish
                return
            }
        }

        if (nextTimeToCountdown > 0) {
            startCountdown(nextTimeToCountdown)
            startBtn.text = getString(R.string.pause)
        }
        updateTimerStatusText()
    }

    private fun pauseTimer() {
        if (currentTimerState != TimerState.PAUSED) {
            currentTimer?.cancel()
            stateBeforePause = currentTimerState
            currentTimerState = TimerState.PAUSED
            startBtn.text = getString(R.string.resume)
            updateTimerStatusText()
            skipBtn.isEnabled = false
        }
    }

    private fun resumeTimer() {
        if (timeLeftInMillis > 0 && currentTimerState == TimerState.PAUSED) {
            currentTimerState = stateBeforePause // Restore actual state
            startCountdown(timeLeftInMillis)
            startBtn.text = getString(R.string.pause)
            updateTimerStatusText()
            skipBtn.isEnabled = true
        } else {
            Log.w("TimerResume", "Resume called in invalid state or with no time left. currentTimerState: $currentTimerState, timeLeft: $timeLeftInMillis")
            if (timeLeftInMillis <= 0 && currentTimerState == TimerState.PAUSED) {
                // Timer ended while paused.
                stateBeforePause = TimerState.IDLE
                currentTimerState = TimerState.IDLE
                startNextPhase()
            } else {
                resetTimerLogic() // Fallback to a clean state if not PAUSED or timeLeft > 0
            }
        }
    }

    private fun resetTimer() {
        resetTimerLogic()
        updateAllSetupTextViews() // Ensure UI reflects model
        updateTimerDisplay(getReadyTime) // Show initial Get Ready time on countdown display
    }

    private fun resetTimerLogic() {
        currentTimer?.cancel()
        currentTimer = null
        currentTimerState = TimerState.IDLE
        stateBeforePause = TimerState.IDLE
        currentSet = 1
        timeLeftInMillis = 0
        startBtn.text = getString(R.string.start)
        updateTimerStatusText() // Update status text (e.g., "Press Start")
        toggleInputs(true) // MODIFIED HERE
        skipBtn.visibility = View.GONE
        skipBtn.isEnabled = false
    }

    private fun timerFinishedAllPhases() {
        Toast.makeText(this, "Workout Complete!", Toast.LENGTH_SHORT).show()
        resetTimerLogic()
        updateTimerDisplay(getReadyTime) // Show initial Get Ready time
    }

    // REPLACED toggleSetupControlsInteractive with toggleInputs
    private fun toggleInputs(enabled: Boolean) {
        setsCountText.isEnabled = enabled
        setsMinusBtn.isEnabled = enabled
        setsPlusBtn.isEnabled = enabled

        getReadyMinutesText.isEnabled = enabled
        getReadySecondsText.isEnabled = enabled
        getReadyMinusBtn.isEnabled = enabled
        getReadyPlusBtn.isEnabled = enabled

        workMinutesText.isEnabled = enabled
        workSecondsText.isEnabled = enabled
        workMinusBtn.isEnabled = enabled
        workPlusBtn.isEnabled = enabled

        restMinutesText.isEnabled = enabled
        restSecondsText.isEnabled = enabled
        restMinusBtn.isEnabled = enabled
        restPlusBtn.isEnabled = enabled

        coolDownMinutesText.isEnabled = enabled
        coolDownSecondsText.isEnabled = enabled
        coolDownMinusBtn.isEnabled = enabled
        coolDownPlusBtn.isEnabled = enabled

        val allControlledEditTexts = listOf(
            setsCountText,
            getReadyMinutesText, getReadySecondsText,
            workMinutesText, workSecondsText,
            restMinutesText, restSecondsText,
            coolDownMinutesText, coolDownSecondsText
        )
        allControlledEditTexts.forEach {
            it.isFocusableInTouchMode = enabled
            if (!enabled) {
                it.clearFocus()
            }
        }

        if (!enabled) { 
            val inputMethodManager = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
            currentFocus?.let {
                inputMethodManager.hideSoftInputFromWindow(it.windowToken, 0)
            }
        }
    }

    private fun startCountdown(millis: Long) {
        timeLeftInMillis = millis
        currentTimer = object : CountDownTimer(millis, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                timeLeftInMillis = millisUntilFinished
                updateTimerDisplay((millisUntilFinished / 1000).toInt())
            }

            override fun onFinish() {
                timeLeftInMillis = 0
                updateTimerDisplay(0) // Show 00:00 briefly
                startNextPhase() // Proceed to next phase
            }
        }.start()
    }

    // General time formatter for total seconds -> MM:SS string
    private fun formatTime(totalSeconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong())
        val remainingSeconds = totalSeconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    private fun updateSetsCountText() {
        setsCountText.setText(setsCount.toString())
    }

    // Update functions for individual minute/second EditTexts
    private fun updateGetReadyTimeText() {
        val minutes = getReadyTime / 60
        val seconds = getReadyTime % 60
        getReadyMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes))
        getReadySecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds))
    }

    private fun updateWorkTimeText() {
        val minutes = workTime / 60
        val seconds = workTime % 60
        workMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes))
        workSecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds))
    }

    private fun updateRestTimeText() {
        val minutes = restTime / 60
        val seconds = restTime % 60
        restMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes))
        restSecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds))
    }

    private fun updateCoolDownTimeText() {
        val minutes = coolDownTime / 60
        val seconds = coolDownTime % 60
        coolDownMinutesText.setText(String.format(Locale.getDefault(), "%02d", minutes))
        coolDownSecondsText.setText(String.format(Locale.getDefault(), "%02d", seconds))
    }

    private fun updateTotalTimeDisplay() {
        val totalWorkRestTime = (workTime + restTime) * setsCount
        val totalSeconds = (getReadyTime + totalWorkRestTime + coolDownTime)
        totalTimeText.setText(formatTime(totalSeconds))
    }

    private fun updateTimerDisplay(seconds: Int) { // This is for the main countdown display
        countdownTimerText.text = formatTime(seconds)
    }

    private fun updateTimerStatusText() {
        val status = when (currentTimerState) {
            TimerState.IDLE -> "Press Start"
            TimerState.GET_READY -> "Get Ready!"
            TimerState.WORK -> "Work (Set $currentSet/$setsCount)"
            TimerState.REST -> "Rest (Set $currentSet/$setsCount)"
            TimerState.COOL_DOWN -> "Cool Down"
            TimerState.PAUSED -> "Paused - ${
                when (stateBeforePause) {
                    TimerState.GET_READY -> "Get Ready!"
                    TimerState.WORK -> "Work (Set $currentSet/$setsCount)"
                    TimerState.REST -> "Rest (Set $currentSet/$setsCount)"
                    TimerState.COOL_DOWN -> "Cool Down"
                    TimerState.IDLE -> "Press Start" // Should ideally not be pausable from IDLE
                    else -> "" // Should not happen
                }
            }"
        }
        timerStatusText.text = status
    }
}
