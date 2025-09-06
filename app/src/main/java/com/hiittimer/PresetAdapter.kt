package com.hiittimer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView
import java.util.Locale
import java.util.concurrent.TimeUnit

class PresetAdapter(
    context: Context,
    private val presets: MutableList<WorkoutPreset>,
    private val onPresetLoad: (WorkoutPreset) -> Unit,
    private val onPresetDelete: (WorkoutPreset) -> Unit
) : ArrayAdapter<WorkoutPreset>(context, 0, presets) {

    // Helper function to format total seconds into MM:SS string
    private fun formatTime(totalSeconds: Int): String {
        val minutes = TimeUnit.SECONDS.toMinutes(totalSeconds.toLong())
        val remainingSeconds = totalSeconds - TimeUnit.MINUTES.toSeconds(minutes)
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, remainingSeconds)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_preset_with_delete, parent, false)

        val preset = getItem(position) as WorkoutPreset
        val titleTextView = view.findViewById<TextView>(R.id.preset_title_textview)
        val totalTimeTextView = view.findViewById<TextView>(R.id.preset_total_time_textview) // New TextView
        val deleteButton = view.findViewById<ImageButton>(R.id.delete_preset_button)

        titleTextView.text = preset.title

        // Calculate and set total time for the preset
        val presetTotalTimeSeconds = (preset.workTimeSeconds + preset.restTimeSeconds) * preset.sets +
                                   preset.getReadyTimeSeconds + preset.coolDownTimeSeconds
        totalTimeTextView.text = formatTime(presetTotalTimeSeconds)

        view.setOnClickListener {
            onPresetLoad(preset)
        }

        deleteButton.setOnClickListener {
            it.cancelPendingInputEvents()
            onPresetDelete(preset)
        }

        return view
    }

    fun removePreset(preset: WorkoutPreset) {
        presets.remove(preset)
        notifyDataSetChanged()
    }
}