package com.maternallink.smsgateway.networking

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.telephony.SmsManager
import android.util.Log
import com.maternallink.smsgateway.models.OutgoingSMS
import com.maternallink.smsgateway.utils.Preferences
import com.maternallink.smsgateway.utils.SmsDeliveryReceiver
import com.maternallink.smsgateway.workers.DeliveryReportWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

class SmsSender(private val context: Context) {
    companion object {
        const val TAG = "SmsSender"
        const val SENT_SMS_ACTION = "SMS_SENT"
        const val DELIVERED_SMS_ACTION = "SMS_DELIVERED"
    }

    // UPDATED: Better way to get SmsManager across different Android versions
    private fun getSmsManager(): SmsManager? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                // For Android 12 (API 31) and above
                context.getSystemService(SmsManager::class.java)
            } else {
                // For older versions
                @Suppress("DEPRECATION")
                SmsManager.getDefault()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error obtaining SmsManager: ${e.message}")
            null
        }
    }

    private val sentPendingIntents = mutableMapOf<String, PendingIntent>()
    private val deliveredPendingIntents = mutableMapOf<String, PendingIntent>()

    suspend fun sendMessages(messages: List<OutgoingSMS>): List<SendResult> {
        val smsManager = getSmsManager()

        if (smsManager == null) {
            Log.e(TAG, "SmsManager is NOT available on this device.")
            return messages.map { SendResult.Failure(it.uuid, "SmsManager not available") }
        }

        return withContext(Dispatchers.IO) {
            messages.map { message ->
                try {
                    // Pass the manager to the sending function
                    sendingSingleMessage(smsManager, message)
                    SendResult.Success(message.uuid)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to send message ${message.uuid}: ${e.message}")
                    SendResult.Failure(message.uuid, e.message ?: "Unknown error")
                }
            }
        }
    }

    private fun sendingSingleMessage(manager: SmsManager, message: OutgoingSMS) {
        // Use the recipient field (ensure your model matches the field name 'recipient' or 'phoneNumber')
        // Based on your previous logs, your model uses phoneNumber
        Log.d(TAG, "Sending SMS to ${message.phoneNumber}: ${message.message.take(50)}...")

        val sentIntent = createSentIntent(message.uuid)
        val deliveredIntent = createDeliveredIntent(message.uuid)

        sentPendingIntents[message.uuid] = sentIntent
        deliveredPendingIntents[message.uuid] = deliveredIntent

        // Split message if too long
        val parts = manager.divideMessage(message.message)

        val sentIntents = ArrayList<PendingIntent>().apply {
            repeat(parts.size) { add(sentIntent) }
        }

        val deliveredIntents = ArrayList<PendingIntent>().apply {
            repeat(parts.size) { add(deliveredIntent) }
        }

        manager.sendMultipartTextMessage(
            message.phoneNumber,
            null,
            parts,
            sentIntents,
            deliveredIntents
        )

        Log.i(TAG, "SMS ${message.uuid} handed to system for ${message.phoneNumber}")
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
        Log.d(TAG, "Delivery report for $uuid: $status")
        kotlin.runCatching {
            val secret = Preferences.getSecretToken(context)
            DeliveryReportWorker.enqueueWork(context, uuid, status, messageId, secret)
        }.onFailure {
            Log.e(TAG, "Failed to enqueue delivery report: ${it.message}")
        }
    }

    sealed class SendResult {
        data class Success(val uuid: String) : SendResult()
        data class Failure(val uuid: String, val error: String) : SendResult()
    }
}