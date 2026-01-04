package com.maternallink.smsgateway.models

import kotlin.uuid.Uuid

data class DeliveryReport(
    val uuid: String,
    val status: String,
    val messageId: String? = null,
    val secret: String
)

data class DeliveryReportResponse(
    val payload: Payload
){
    data class Payload(
        val success: Boolean,
        val error: String?
    )
}