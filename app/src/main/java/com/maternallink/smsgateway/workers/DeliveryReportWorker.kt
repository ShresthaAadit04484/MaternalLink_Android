package com.maternallink.smsgateway.workers

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.WorkerParameters
import com.maternallink.smsgateway.networking.RetrofitClient

class DeliveryReportWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    companion object {
        const val TAG = "DeliveryReportWorker"

        fun enqueueWork(
            context: Context,
            uuid: String,
            status: String,
            messageId: String?,
            secret: String
        ) {
            val inputData = Data.Builder()
                .putString("uuid", uuid)
                .putString("status", status)
                .putString("messageId", messageId)
                .putString("secret", secret)
                .build()

            val workRequest = androidx.work.OneTimeWorkRequestBuilder<DeliveryReportWorker>()
                .setInputData(inputData)
                .setConstraints(
                    androidx.work.Constraints.Builder()
                        .setRequiredNetworkType(androidx.work.NetworkType.CONNECTED)
                        .build()
                )
                .addTag("delivery_report")
                .build()

            androidx.work.WorkManager.getInstance(context).enqueue(workRequest)
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val uuid = inputData.getString("uuid") ?: return Result.failure()
            val status = inputData.getString("status") ?: return Result.failure()
            val messageId = inputData.getString("messageId")
            val secret = inputData.getString("secret") ?: return Result.failure()

            val apiService = RetrofitClient.getApiService(applicationContext)
            val response = apiService.sendDeliveryReport(secret, uuid, status, messageId)

            if (response.isSuccessful && response.body()?.payload?.success == true) {
                Log.d(TAG, "Delivery report sent successfully for $uuid")
                Result.success()
            } else {
                Log.e(TAG, "Failed to send delivery report for $uuid")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending delivery report: ${e.message}")
            Result.retry()
        }
    }
}