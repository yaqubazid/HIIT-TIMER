package com.hiittimer

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val bugReportButton: Button = findViewById(R.id.bug_report_button)
        bugReportButton.setOnClickListener {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:") // Only email apps should handle this
                putExtra(Intent.EXTRA_EMAIL, arrayOf("yaqubazid566@gmail.com"))
                putExtra(Intent.EXTRA_SUBJECT, "Bug Report for HIIT Timer App")
            }
            try {
                startActivity(intent)
            } catch (_: ActivityNotFoundException) {
                Toast.makeText(this, "No email client installed.", Toast.LENGTH_SHORT).show()
            }
        }

        val tipJarButton: Button = findViewById(R.id.tip_jar_button)
        tipJarButton.setOnClickListener {
            Toast.makeText(this, "Tip Jar clicked. Billing integration pending.", Toast.LENGTH_SHORT).show()
        }
    }
}
