//package com.maternallink.smsgateway.services
//
//import android.app.Service
//import android.content.Intent
//import android.os.IBinder
//import android.telephony.SmsManager
//import android.util.Log
//import androidx.work.*
//import com.maternallink.smsgateway.networking.ApiClient
//import kotlinx.coroutines.*
//import java.util.concurrent.TimeUnit
//
//class SmsGatewayService : Service() {
//    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
//    private val tag = "MaternalSMS"
//
//    override fun onBind(intent: Intent?): IBinder? = null
//
//    override fun onCreate() {
//        super.onCreate()
//        Log.d(tag, "🚀 SMS Gateway Service Started")
//        schedulePeriodicWork()
//    }
//
//    override fun onDestroy() {
//        super.onDestroy()
//        serviceScope.cancel()
//        Log.d(tag, "🛑 SMS Gateway Service Stopped")
//    }
//
//    private fun schedulePeriodicWork() {
//        val constraints = Constraints.Builder()
//            .setRequiredNetworkType(NetworkType.CONNECTED)
//            .build()
//
//        val periodicWorkRequest = PeriodicWorkRequestBuilder<SmsWorker>(
//            15, TimeUnit.MINUTES  // Check every 15 minutes
//        ).setConstraints(constraints)
//
//
//            .build()
//
//        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
//            "maternal_sms_worker",
//            ExistingPeriodicWorkPolicy.KEEP,
//            periodicWorkRequest
//        )
//
//        Log.d(tag, "📅 Periodic work scheduled (every 15 minutes)")
//    }
//}
//
//class SmsWorker(
//    context: android.content.Context,
//    params: WorkerParameters
//) : CoroutineWorker(context, params) {
//
//    override suspend fun doWork(): Result {
//        return try {
//            Log.d("SmsWorker", "🔍 Checking for messages...")
//            val response = ApiClient.apiService.getOutboxMessages()
//
//            Log.d("SmsWorker", "📨 Found ${response.messages.size} messages")
//
//            response.messages.forEach { message ->
//                sendSms(message)
//                delay(2000) // Wait 2 seconds between sends
//            }
//
//            Result.success()
//        } catch (e: Exception) {
//            Log.e("SmsWorker", "💥 Error: ${e.message}")
//            Result.retry()
//        }
//    }
//
//    private suspend fun sendSms(message: com.maternallink.smsgateway.models.OutboxMessage) {
//        return withContext(Dispatchers.IO) {
//            try {
//                val smsManager = SmsManager.getDefault()
//                smsManager.sendTextMessage(message.to, null, message.message, null, null)
//
//                Log.d("SmsWorker", "✅ Sent to ${message.to}: ${message.message}")
//
//                // Report delivery
//                ApiClient.apiService.reportDelivery(
//                    delivery = com.maternallink.smsgateway.models.DeliveryReport(
//                        id = message.id,
//                        status = "sent"
//                    )
//                )
//
//            } catch (e: Exception) {
//                Log.e("SmsWorker", "❌ Failed to send to ${message.to}: ${e.message}")
//            }
//        }
//    }
//}