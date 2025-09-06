package com.hiittimer

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.TextView

class PresetAdapter(
    context: Context,
    private val presets: MutableList<WorkoutPreset>,
    private val onPresetLoad: (WorkoutPreset) -> Unit,
    private val onPresetDelete: (WorkoutPreset) -> Unit
) : ArrayAdapter<WorkoutPreset>(context, 0, presets) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.list_item_preset_with_delete, parent, false)

        val preset = getItem(position) as WorkoutPreset
        val titleTextView = view.findViewById<TextView>(R.id.preset_title_textview)
        val deleteButton = view.findViewById<ImageButton>(R.id.delete_preset_button)

        titleTextView.text = preset.title

        view.setOnClickListener {
            onPresetLoad(preset)
        }

        deleteButton.setOnClickListener {
            // Stop the event from propagating to the parent view's click listener
            it.cancelPendingInputEvents() 
            onPresetDelete(preset)
        }

        return view
    }

    fun removePreset(preset: WorkoutPreset) {
        presets.remove(preset)
        notifyDataSetChanged()
    }
    
    fun updatePresets(newPresets: List<WorkoutPreset>) {
        presets.clear()
        presets.addAll(newPresets)
        notifyDataSetChanged()
    }
}