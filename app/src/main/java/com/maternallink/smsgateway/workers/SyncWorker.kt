package com.maternallink.smsgateway.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maternallink.smsgateway.networking.RetrofitClient
import com.maternallink.smsgateway.networking.SmsSender
import com.maternallink.smsgateway.utils.Preferences

class SyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TAG = "SyncWorker"
        const val WORK_NAME = "sms_sync_worker"
    }

    override suspend fun doWork(): Result {
        Log.d(TAG, "Starting sync worker")

        if (!Preferences.isServiceEnabled(applicationContext)) {
            Log.d(TAG, "Service disabled, skipping sync")
            return Result.success() // Changed to success so it doesn't keep retrying if disabled
        }

        return try {
            val apiService = RetrofitClient.getApiService(applicationContext)
            val secret = Preferences.getSecretToken(applicationContext)

            val response = apiService.getOutgoingMessages(secret)

            if (response.isSuccessful) {
                val outgoingResponse = response.body()

                if (outgoingResponse?.payload?.success == true) {
                    // Warning Fix: Just use the messages directly if the model says they can't be null
                    val messages = outgoingResponse.messages ?: emptyList()

                    Log.d(TAG, "Fetched ${messages.size} messages from server")

                    if (messages.isNotEmpty()) {
                        val smsSender = SmsSender(applicationContext)

                        // FIX: Call this ONCE and store the result
                        val results = smsSender.sendMessages(messages)

                        val successCount = results.count { it is SmsSender.SendResult.Success }
                        val failureCount = results.count { it is SmsSender.SendResult.Failure }
                        Log.i(TAG, "Sent $successCount messages, $failureCount failed")
                    }

                    // Update the timestamp so MainActivity shows the new time
                    Preferences.setLastSync(applicationContext, System.currentTimeMillis())
                    Result.success()
                } else {
                    Log.e(TAG, "Server error logic: ${outgoingResponse?.payload?.error}")
                    Result.failure() // Tells MainActivity "Sync Failed"
                }
            } else {
                Log.e(TAG, "HTTP Error: ${response.code()}")
                Result.failure() // Tells MainActivity "Sync Failed"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Network Error: ${e.message}")
            Result.retry() // Tells WorkManager to try again later if Wi-Fi dropped
        }
    }
}