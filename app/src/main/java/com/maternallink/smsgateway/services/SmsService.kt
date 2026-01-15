package com.maternallink.smsgateway.services

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.*
import com.maternallink.smsgateway.MainActivity
import com.maternallink.smsgateway.workers.SyncWorker
import com.maternallink.smsgateway.utils.Preferences
import java.util.concurrent.TimeUnit

class SmsService : Service() {
    companion object {
        const val TAG = "SmsService"
        const val NOTIFICATION_ID = 101
        const val CHANNEL_ID = "sms_gateway_channel"
        const val ACTION_START = "START_SERVICE"
        const val ACTION_STOP = "STOP_SERVICE"

        fun startService(context: Context) {
            val intent = Intent(context, SmsService::class.java).apply {
                action = ACTION_START
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopService(context: Context) {
            val intent = Intent(context, SmsService::class.java).apply {
                action = ACTION_STOP
            }
            context.stopService(intent)
        }
    }

    private lateinit var workManager: WorkManager

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service created")
        workManager = WorkManager.getInstance(this)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForegroundService()
                startPeriodicSync()
                Log.i(TAG, "Service started")
            }
            ACTION_STOP -> {
                stopForegroundService()
                Log.i(TAG, "Service stopped")
            }
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = createNotification()
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun stopForegroundService() {
        stopForeground(true)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Maternal SMS Gateway")
            .setContentText("Sending reminders to mothers...")
            .setSmallIcon(android.R.drawable.ic_dialog_email)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Gateway Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Background service for sending SMS reminders"
            }

            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun startPeriodicSync() {
        val syncInterval = Preferences.getSyncInterval(this).toLong()

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()

        val syncWorkRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            syncInterval, TimeUnit.SECONDS,
            syncInterval, TimeUnit.SECONDS
        )
            .setConstraints(constraints)
            .addTag(SyncWorker.WORK_NAME)
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncWorkRequest
        )

        Log.d(TAG, "Periodic sync scheduled every $syncInterval seconds")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        Log.d(TAG, "Service destroyed")
        // Cancel all workers
        workManager.cancelAllWork()
    }
}