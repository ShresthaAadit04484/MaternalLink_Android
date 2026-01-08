package com.maternallink.smsgateway.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import android.util.Log
import androidx.work.*
import com.maternallink.smsgateway.workers.IncomingSmsWorker
import java.util.concurrent.TimeUnit

class SmsReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "SMS received")

        if (!Preferences.isServiceEnabled(context)) {
            Log.d(TAG, "Service is disabled, ignoring SMS")
            return
        }

        if (intent.action == Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            processSmsIntent(intent, context)
        }
    }

    private fun processSmsIntent(intent: Intent, context: Context) {
        try {
            // Use the recommended API that handles all PDU parsing internally
            val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)

            messages?.forEach { message ->
                val sender = message.displayOriginatingAddress
                val body = message.displayMessageBody

                if (sender != null && body != null) {
                    Log.i(TAG, "SMS from $sender: $body")
                    forwardSmsToServer(context, sender, body)
                } else {
                    Log.w(TAG, "SMS message has null sender or body")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing SMS intent: ${e.message}", e)
        }
    }

    private fun forwardSmsToServer(context: Context, sender: String, body: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val inputData = Data.Builder()
            .putString("sender", sender)
            .putString("message", body)
            .putString("secret", Preferences.getSecretToken(context))
            .build()

        val workRequest = OneTimeWorkRequestBuilder<IncomingSmsWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 30, TimeUnit.SECONDS)
            .addTag("incoming_sms")
            .setInputData(inputData)
            .build()

        WorkManager.getInstance(context).enqueue(workRequest)
        Log.d(TAG, "Enqueued incoming SMS for forwarding")
    }
}