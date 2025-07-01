package dev.lordyorden.as_no_phish_detector.retrofit

import dev.lordyorden.as_no_phish_detector.models.CreateSmsMessage
import dev.lordyorden.as_no_phish_detector.models.PagedList
import dev.lordyorden.as_no_phish_detector.models.SmsMessage
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface SmsRestApi {
    @POST("/messages")
    fun uploadSms(@Body sms: CreateSmsMessage): Call<SmsMessage>

    /*@POST("/notifications")
    fun uploadNotification(@Body sms: CreateNotification): Call<Notification>
*/
    @GET("/messages")
    fun getSmsMessages(@Query("size") size: Int = 15, @Query("page") page: Int = 1): Call<PagedList<SmsMessage>>

    @GET("/messages/byNumber/{phoneNumber}")
    fun getSmsMessagesByNumber(@Path("phoneNumber") number: String,
                               @Query("size") size: Int = 15, @Query("page") page: Int = 1): Call<PagedList<SmsMessage>>

    @GET("/messages/{messageId}")
    fun getSmsMessage(@Path("messageId") id: String): Call<SmsMessage>
}