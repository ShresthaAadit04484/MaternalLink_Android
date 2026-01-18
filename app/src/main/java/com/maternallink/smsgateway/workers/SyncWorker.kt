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

    // In SyncWorker.kt (Simplified logic)
    override suspend fun doWork(): Result {
        val context = applicationContext
        val apiService = RetrofitClient.getApiService(context)
        val smsSender = SmsSender(context)
        val secret = Preferences.getSecretToken(context)

        return try {
            // 1. Fetch pending messages from BE [cite: 408]
            val response = apiService.getOutgoingMessages(secret)
            if (response.isSuccessful) {
                val messages = response.body()?.messages ?: emptyList()

                // 2. Trigger the SmsSender [cite: 412]
                if (messages.isNotEmpty()) {
                    smsSender.sendMessages(messages)
                }
                Result.success()
            } else {
                Result.retry()
            }
        } catch (e: Exception) {
            Result.failure()
        }
    }
}