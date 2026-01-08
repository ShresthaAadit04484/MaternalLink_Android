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
            return Result.retry()
        }

        return try {
            val apiService = RetrofitClient.getApiService(applicationContext)
            val secret = Preferences.getSecretToken(applicationContext)

            // Fetch messages from server
            val response = apiService.getOutgoingMessages(secret)

            if (response.isSuccessful) {
                val outgoingResponse = response.body()

                if (outgoingResponse?.payload?.success == true) {
                    val messages = outgoingResponse.messages

                    Log.d(TAG, "Fetched ${messages.size} messages from server")

                    if (messages.isNotEmpty()) {
                        // Send SMS messages
                        val smsSender = SmsSender(applicationContext)
                        val results = smsSender.sendMessages(messages)

                        val successCount = results.count { it is SmsSender.SendResult.Success }
                        val failureCount = results.count { it is SmsSender.SendResult.Failure}

                        Log.i(TAG, "Sent $successCount messages, $failureCount failed")

                        Preferences.setLastSync(applicationContext, System.currentTimeMillis())
                    }
                } else {
                    Log.e(TAG, "Server returned error: ${outgoingResponse?.payload?.error}")
                }
            } else {
                Log.e(TAG, "Failed to fetch messages: ${response.code()} ${response.message()}")
            }

            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Error in sync worker: ${e.message}")
            Result.retry()
        }
    }
}