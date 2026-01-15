//package com.maternallink.smsgateway.networking
//
//import com.maternallink.smsgateway.models.DeliveryReport
//import com.maternallink.smsgateway.models.OutboxResponse
//import retrofit2.Response
//import retrofit2.Retrofit
//import retrofit2.converter.gson.GsonConverterFactory
//import retrofit2.http.GET
//import retrofit2.http.POST
//import retrofit2.http.Body
//import retrofit2.http.Query
//
//
//interface MaternalLinkApi {
//    @GET("sms/outbox")
//    suspend fun getOutboxMessages(@Query("secret") secret: String = "changeme"): OutboxResponse
//
//    @POST("sms/delivery")
//    suspend fun reportDelivery(
//        @Query("secret") secret: String = "changeme",
//        @Body delivery: DeliveryReport
//    ): Response<Unit>
//}
//
//object ApiClient {
//    private const val BASE_URL = "http://192.168.1.66:8000/"
//
//    val apiService: MaternalLinkApi by lazy {
//        Retrofit.Builder()
//            .baseUrl(BASE_URL)
//            .addConverterFactory(GsonConverterFactory.create())
//            .build()
//            .create(MaternalLinkApi::class.java)
//    }
//}