package com.maternallink.smsgateway

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import android.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope // Required for coroutines in Activity
import kotlinx.coroutines.launch
import com.maternallink.smsgateway.networking.SmsSender
import androidx.work.*
import com.maternallink.smsgateway.networking.RetrofitClient
import com.maternallink.smsgateway.services.SmsService
import com.maternallink.smsgateway.utils.Preferences
import com.maternallink.smsgateway.workers.SyncWorker
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE = 100
        private const val BATTERY_OPTIMIZATION_REQUEST = 101
        private val REQUIRED_PERMISSIONS = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.POST_NOTIFICATIONS
            )
        } else {
            arrayOf(
                Manifest.permission.SEND_SMS,
                Manifest.permission.RECEIVE_SMS,
                Manifest.permission.READ_SMS
            )
        }
    }

    private lateinit var btnToggleService: Button
    private lateinit var btnSettings: Button
    private lateinit var btnSyncNow: Button
    private lateinit var btnPermissions: Button
    private lateinit var btnBattery: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvLastSync: TextView
    private lateinit var tvStats: TextView
    private lateinit var tvVersion: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initViews()
        checkPermissions()
        updateUI()

        if (!Preferences.isBatteryOptimizationDisabled(this)) {
            checkBatteryOptimization()
        }

        if (Preferences.isServiceEnabled(this) && hasRequiredPermissions()) {
            startSmsService()
        }
    }

    override fun onResume() {
        super.onResume()
        updateUI()
    }

    private fun initViews() {
        btnToggleService = findViewById(R.id.btnToggleService)
        btnSettings = findViewById(R.id.btnSettings)
        btnSyncNow = findViewById(R.id.btnSyncNow)
        btnPermissions = findViewById(R.id.btnPermissions)
        btnBattery = findViewById(R.id.btnBattery)
        tvStatus = findViewById(R.id.tvStatus)
        tvLastSync = findViewById(R.id.tvLastSync)
        tvStats = findViewById(R.id.tvStats)
        tvVersion = findViewById(R.id.tvVersion)

        btnToggleService.setOnClickListener { toggleService() }
        btnSettings.setOnClickListener { openSettings() }
        btnSyncNow.setOnClickListener { syncNow() }
        btnPermissions.setOnClickListener { requestPermissions() }
        btnBattery.setOnClickListener { openBatterySettings() }

        try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            tvVersion.text = "Version ${packageInfo.versionName}"
        } catch (e: PackageManager.NameNotFoundException) {
            tvVersion.text = "Version 1.0"
        }
    }

    private fun checkPermissions(): Boolean {
        val missingPermissions = REQUIRED_PERMISSIONS.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions.toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
            false
        } else {
            true
        }
    }

    private fun hasRequiredPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            val allGranted = grantResults.all { it == PackageManager.PERMISSION_GRANTED }
            if (allGranted) {
                if (Preferences.isServiceEnabled(this)) {
                    startSmsService()
                }
            } else {
                AlertDialog.Builder(this)
                    .setTitle("Permissions Required")
                    .setMessage("This app needs SMS permissions to function. Please grant them in Settings.")
                    .setPositiveButton("Open Settings") { _, _ -> openAppSettings() }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            updateUI()
        }
    }

    private fun updateUI() {
        val serviceEnabled = Preferences.isServiceEnabled(this)
        val hasPermissions = hasRequiredPermissions()
        val lastSync = Preferences.getLastSync(this)

        if (serviceEnabled && hasPermissions) {
            tvStatus.text = "Service: Running ✅"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_green_dark))
            btnToggleService.text = "Stop Service"
        } else if (!hasPermissions) {
            tvStatus.text = "Service: Permissions Needed ⚠️"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_orange_dark))
            btnToggleService.text = "Start Service"
            btnToggleService.isEnabled = false
        } else {
            tvStatus.text = "Service: Stopped ❌"
            tvStatus.setTextColor(ContextCompat.getColor(this, android.R.color.holo_red_dark))
            btnToggleService.text = "Start Service"
            btnToggleService.isEnabled = true
        }

        if (lastSync > 0) {
            val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            tvLastSync.text = "Last Sync: ${dateFormat.format(Date(lastSync))}"
        } else {
            tvLastSync.text = "Last Sync: Never"
        }

        btnSyncNow.isEnabled = serviceEnabled && hasPermissions
        btnPermissions.isEnabled = !hasPermissions
    }

    private fun toggleService() {
        if (Preferences.isServiceEnabled(this)) {
            Preferences.setServiceEnabled(this, false)
            SmsService.stopService(this)
        } else {
            if (!hasRequiredPermissions()) {
                requestPermissions()
                return
            }
            Preferences.setServiceEnabled(this, true)
            startSmsService()
        }
        updateUI()
    }

    private fun startSmsService() {
        SmsService.startService(this)

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        // Note: Minimum periodic interval is 15 minutes (900 seconds)
        val interval = Preferences.getSyncInterval(this).toLong()
        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            if (interval < 900) 15 else interval / 60, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )
    }

    // In MainActivity.kt
    private fun syncNow() {
        if (!hasRequiredPermissions()) {
            Toast.makeText(this, "Grant SMS permissions first", Toast.LENGTH_SHORT).show()
            return
        }

        // Modern loading indicator
        val loading = AlertDialog.Builder(this)
            .setMessage("Syncing and Sending...")
            .setCancelable(false)
            .create()
        loading.show()

        lifecycleScope.launch {
            try {
                val apiService = RetrofitClient.getApiService(this@MainActivity)
                val secret = Preferences.getSecretToken(this@MainActivity)

                // 1. Get messages from BE
                val response = apiService.getOutgoingMessages(secret)

                Log.d("SMS_GATEWAY", "URL Called: ${response.raw().request.url}")
                Log.d("SMS_GATEWAY", "Response Code: ${response.code()}")

                if (response.isSuccessful) {
                    val messages = response.body()?.messages ?: emptyList()

                    if (messages.isNotEmpty()) {
                        // 2. TRIGGER HARDWARE SMS SENDING
                        val smsSender = SmsSender(this@MainActivity)
                        smsSender.sendMessages(messages)

                        Toast.makeText(this@MainActivity, "Successfully sent ${messages.size} SMS", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@MainActivity, "Server queue is empty", Toast.LENGTH_SHORT).show()
                    }
                    Preferences.setLastSync(this@MainActivity, System.currentTimeMillis())
                    updateUI()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MainActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
            } finally {
                loading.dismiss()
            }
        }
    }
    private fun checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val powerManager = getSystemService(POWER_SERVICE) as PowerManager
            if (!powerManager.isIgnoringBatteryOptimizations(packageName)) {
                AlertDialog.Builder(this)
                    .setTitle("Battery Optimization")
                    .setMessage("Please disable battery optimization for reliable background service.")
                    .setPositiveButton("Disable") { _, _ -> requestBatteryOptimizationDisable() }
                    .show()
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.M)
    private fun requestBatteryOptimizationDisable() {
        val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivityForResult(intent, BATTERY_OPTIMIZATION_REQUEST)
    }

    private fun openSettings() { startActivity(Intent(this, SettingsActivity::class.java)) }
    private fun requestPermissions() { checkPermissions() }
    private fun openBatterySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }
    private fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:$packageName")
        }
        startActivity(intent)
    }

    // In MainActivity.kt

}