package com.maternallink.smsgateway

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.maternallink.smsgateway.utils.Preferences
import com.maternallink.smsgateway.networking.RetrofitClient

class SettingsActivity : AppCompatActivity() {
    private lateinit var etServerUrl: EditText
    private lateinit var btnSave: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        etServerUrl = findViewById(R.id.etServerUrl)
        btnSave = findViewById(R.id.btnSave)

        // 1. Load the existing URL when the screen opens
        etServerUrl.setText(Preferences.getServerUrl(this))

        btnSave.setOnClickListener {
            val newUrl = etServerUrl.text.toString().trim()

            // 2. Validate the URL
            if (newUrl.isEmpty() || !newUrl.startsWith("http")) {
                Toast.makeText(this, "Please enter a valid URL (http/https)", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 3. Save to Preferences
            Preferences.setServerUrl(this, newUrl)

            // 4. IMPORTANT: Reset Retrofit so it uses the new URL
            RetrofitClient.reset()

            Toast.makeText(this, "Settings Saved!", Toast.LENGTH_SHORT).show()
            finish() // Go back to MainActivity
        }
    }
}