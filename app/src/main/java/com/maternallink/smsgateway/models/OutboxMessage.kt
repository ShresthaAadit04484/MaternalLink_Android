//package com.maternallink.smsgateway.models
//
//import com.google.gson.annotations.SerializedName
//
//data class OutboxMessage(
//    @SerializedName("id") val id: String,
//    @SerializedName("to") val to: String,
//    @SerializedName("message") val message: String
//)
//
//data class OutboxResponse(
//    @SerializedName("messages") val messages: List<OutboxMessage>
//)
//
//data class DeliveryReport(
//    @SerializedName("id") val id: String,
//    @SerializedName("status") val status: String
//)