package com.maternallink.smsgateway

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.telephony.SmsManager
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class SmsTestActivity : AppCompatActivity() {

    private lateinit var statusText: TextView
    private lateinit var sendButton: Button

    private val smsPermissions = arrayOf(
        Manifest.permission.SEND_SMS,
        Manifest.permission.READ_SMS
    )

    private val testPhoneNumber = "9841641268"  // YOUR TEST NUMBER
    private val testMessage = "📱 TEST: Maternal Health SMS is working!"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sms_test)

        statusText = findViewById(R.id.testStatusText)
        sendButton = findViewById(R.id.testSendButton)

        sendButton.setOnClickListener {
            if (checkSmsPermissions()) {
                sendTestSms()
            } else {
                requestSmsPermissions()
            }
        }

        updateStatus("📱 Ready to test SMS")
    }

    private fun checkSmsPermissions(): Boolean {
        return smsPermissions.all { permission ->
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun requestSmsPermissions() {
        ActivityCompat.requestPermissions(this, smsPermissions, 101)
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
            sendTestSms()
        } else {
            updateStatus("❌ SMS permissions denied")
            Toast.makeText(this, "Need SMS permissions to test", Toast.LENGTH_LONG).show()
        }
    }

    private fun sendTestSms() {
        updateStatus("📤 Sending test SMS...")

        try {
            val smsManager = SmsManager.getDefault()

            // Simple send - no callbacks for now
            smsManager.sendTextMessage(
                testPhoneNumber,  // Phone number
                null,             // Service center (null = default)
                testMessage,      // Message text
                null,             // Sent intent
                null              // Delivery intent
            )

            Log.d("SMSTest", "✅ SMS sent to $testPhoneNumber")
            updateStatus("✅ SMS sent successfully!")
            Toast.makeText(this, "SMS sent!", Toast.LENGTH_LONG).show()

        } catch (e: Exception) {
            Log.e("SMSTest", "❌ SMS send failed: ${e.message}")
            updateStatus("❌ Failed: ${e.message}")
            Toast.makeText(this, "Failed: ${e.message}", Toast.LENGTH_LONG).show()
            e.printStackTrace()
        }
    }

    private fun updateStatus(message: String) {
        statusText.text = message
        Log.d("SMSTest", "Status: $message")
    }
}