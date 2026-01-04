package com.maternallink.smsgateway.models

import com.google.gson.annotations.SerializedName


data class OutgoingSMS(
    @SerializedName("to")
    val phoneNumber: String,

    @SerializedName("message")
    val message: String,

    @SerializedName("uuid")
    val uuid: String
)

data class OutgoingSMSResponse(
    @SerializedName("payload")
    val payload: Payload,

    @SerializedName("messages")
    val messages: List<OutgoingSMS>
){
    data class Payload(
        @SerializedName("success")
        val success: Boolean,

        @SerializedName("error")
        val error: String?
    )
}