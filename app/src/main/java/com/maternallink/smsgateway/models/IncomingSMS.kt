package com.maternallink.smsgateway.models

data class IncomingSMS(
    val from: String,
    val message: String,
    val secret: String
)

data class IncomingSMSResponse(
    val payload: Payload
){
    data class Payload(
        val success: Boolean,
        val error: String?
    )
}
