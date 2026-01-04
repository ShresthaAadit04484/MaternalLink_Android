package com.maternallink.smsgateway.networking

import com.maternallink.smsgateway.models.DeliveryReportResponse
import com.maternallink.smsgateway.models.OutgoingSMSResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("sms/outgoin")
    suspend fun getOutgoingMessages(
        @Query("secret") secret: String
    ): Response<OutgoingSMSResponse>

    @FormUrlEncoded
    @POST("sms/delivery-report")
    suspend fun sendDeliveryReport(
        @Field("secret") secret: String,
        @Field("uuid") uuid: String,
        @Field("status") status: String,
        @Field("message_id") messageId: String? = null
    ): Response<DeliveryReportResponse>

    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}