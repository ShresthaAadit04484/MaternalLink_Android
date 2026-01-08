package com.maternallink.smsgateway.networking

import com.maternallink.smsgateway.models.DeliveryReportResponse
import com.maternallink.smsgateway.models.IncomingSMSResponse
import com.maternallink.smsgateway.models.OutgoingSMSResponse
import retrofit2.Response
import retrofit2.http.*

interface ApiService {
    @GET("sms/outgoing")
    suspend fun getOutgoingMessages(
        @Query("secret") secret: String
    ): Response<OutgoingSMSResponse>

    // POST: Send delivery report
    @FormUrlEncoded
    @POST("sms/delivery-report")
    suspend fun sendDeliveryReport(
        @Field("secret") secret: String,
        @Field("uuid") uuid: String,
        @Field("status") status: String,
        @Field("message_id") messageId: String? = null
    ): Response<DeliveryReportResponse>

    // POST: Send incoming SMS
    @FormUrlEncoded
    @POST("sms/incoming")
    suspend fun sendIncomingSMS(
        @Field("secret") secret: String,
        @Field("from") from: String,
        @Field("message") message: String
    ): Response<IncomingSMSResponse>

    // GET: Health check
    @GET("health")
    suspend fun healthCheck(): Response<Map<String, Any>>
}