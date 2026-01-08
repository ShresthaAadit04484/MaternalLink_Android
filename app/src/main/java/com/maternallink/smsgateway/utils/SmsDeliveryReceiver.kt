package com.maternallink.smsgateway.utils

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.maternallink.smsgateway.networking.SmsSender

class SmsDeliveryReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "SmsDeliveryReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            SmsSender.SENT_SMS_ACTION -> {
                val uuid = intent.getStringExtra("uuid") ?: return
                val resultCode = resultCode

                val status = when (resultCode) {
                    android.app.Activity.RESULT_OK -> "sent"
                    android.telephony.SmsManager.RESULT_ERROR_GENERIC_FAILURE -> "failed"
                    android.telephony.SmsManager.RESULT_ERROR_RADIO_OFF -> "radio_off"
                    android.telephony.SmsManager.RESULT_ERROR_NULL_PDU -> "null_pdu"
                    android.telephony.SmsManager.RESULT_ERROR_NO_SERVICE -> "no_service"
                    else -> "unknown"
                }

                Log.d(TAG, "SMS sent status for $uuid: $status")

                SmsSender(context).reportDelivery(uuid, status)
            }

            SmsSender.DELIVERED_SMS_ACTION -> {
                val uuid = intent.getStringExtra("uuid") ?: return

                Log.d(TAG, "SMS delivered: $uuid")

                SmsSender(context).reportDelivery(uuid, "delivered")
            }
        }
    }
}