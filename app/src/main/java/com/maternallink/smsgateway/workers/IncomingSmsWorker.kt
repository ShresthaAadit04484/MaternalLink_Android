package com.maternallink.smsgateway.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.maternallink.smsgateway.networking.RetrofitClient

class IncomingSmsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TAG = "IncomingSmsWorker"
    }

    override suspend fun doWork(): Result {
        return try {
            val sender = inputData.getString("sender") ?: return Result.failure()
            val message = inputData.getString("message") ?: return Result.failure()
            val secret = inputData.getString("secret") ?: return Result.failure()

            val apiService = RetrofitClient.getApiService(applicationContext)
            val response = apiService.sendIncomingSMS(secret, sender, message)

            if (response.isSuccessful && response.body()?.payload?.success == true) {
                Log.d(TAG, "Incoming SMS forwarded successfully from $sender")
                Result.success()
            } else {
                Log.e(TAG, "Failed to forward incoming SMS from $sender")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error forwarding incoming SMS: ${e.message}")
            Result.retry()
        }
    }
}