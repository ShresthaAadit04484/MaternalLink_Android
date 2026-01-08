package com.maternallink.smsgateway.networking

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.telephony.SmsManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.maternallink.smsgateway.models.OutgoingSMS
import com.maternallink.smsgateway.utils.Preferences
import com.maternallink.smsgateway.utils.SmsDeliveryReceiver
import com.maternallink.smsgateway.workers.DeliveryReportWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
//import kotlin.runCatching
import java.util.*

class SmsSender(private val context: Context) {
    companion object {
        const val TAG = "SmsSender"
        const val SENT_SMS_ACTION = "SMS_SENT"
        const val DELIVERED_SMS_ACTION = "SMS_DELIVERED"
    }

    private val smsManager: SmsManager by lazy {
        ContextCompat.getSystemService(context, SmsManager::class.java)
            ?: throw IllegalStateException("SmsManager not available")
    }

    private val sentPendingIntents = mutableMapOf<String, PendingIntent>()
    private val deliveredPendingIntents = mutableMapOf<String, PendingIntent>()

    suspend fun sendMessages(messages: List<OutgoingSMS>): List<SendResult> {
        return withContext(Dispatchers.IO) {
            messages.map { message ->
                try {
                    sendingSingleMessage(message)
                    SendResult.Success(message.uuid)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message ${message.uuid}: ${e.message}")
                    SendResult.Failure(message.uuid, e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun sendingSingleMessage(message: OutgoingSMS) {
        Log.d(TAG, "Sending SMS to ${message.phoneNumber}: ${message.message.take(50)}...")

        val sentIntent = createSentIntent(message.uuid)
        val deliveredIntent = createDeliveredIntent(message.uuid)

        // Store for later reference
        sentPendingIntents[message.uuid] = sentIntent
        deliveredPendingIntents[message.uuid] = deliveredIntent

        // Split message if too long
        val parts = smsManager.divideMessage(message.message)

        val sentIntents = ArrayList<PendingIntent>().apply {
            for (i in parts.indices) {
                add(sentIntent)
            }
        }

        val deliveredIntents = ArrayList<PendingIntent>().apply {
            for (i in parts.indices) {
                add(deliveredIntent)
            }
        }

        smsManager.sendMultipartTextMessage(
            message.phoneNumber,
            null,
            parts,
            sentIntents,
            deliveredIntents
        )

        Log.i(TAG, "SMS ${message.uuid} sent to ${message.phoneNumber}")
    }

    private fun createSentIntent(uuid: String): PendingIntent {
        val intent = Intent(context, SmsDeliveryReceiver::class.java).apply {
            action = SENT_SMS_ACTION
            putExtra("uuid", uuid)
        }

        return PendingIntent.getBroadcast(
            context,
            uuid.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    private fun createDeliveredIntent(uuid: String): PendingIntent {
        val intent = Intent(context, SmsDeliveryReceiver::class.java).apply {
            action = DELIVERED_SMS_ACTION
            putExtra("uuid", uuid)
        }

        return PendingIntent.getBroadcast(
            context,
            uuid.hashCode() + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }


    fun reportDelivery(uuid: String, status: String, messageId: String? = null) {
        // This will be called by SmsDeliveryReceiver
        Log.d(TAG, "Delivery report for $uuid: $status")

        // Send report to server in background
        kotlin.runCatching {
            val apiService = RetrofitClient.getApiService(context)
            val secret = Preferences.getSecretToken(context)

            // We'll use WorkManager for this to handle retries
            DeliveryReportWorker.enqueueWork(
                context,
                uuid,
                status,
                messageId,
                secret
            )
        }.onFailure {
            Log.e(TAG, "Failed to enqueue delivery report: ${it.message}")
        }
    }

    sealed class SendResult {
        data class Success(val uuid: String) : SendResult()
        data class Failure(val uuid: String, val error: String) : SendResult()
    }

}