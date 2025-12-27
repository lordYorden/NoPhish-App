package dev.lordyorden.as_no_phish_detector.retrofit

import dev.lordyorden.as_no_phish_detector.models.CreateNotification
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.as_no_phish_detector.models.PagedList
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface NotificationRestApi {

    @POST("/notifications")
    suspend fun uploadNotification(@Body notif: CreateNotification): Response<Notification>

    @GET("/notifications")
    suspend fun getNotifications(@Query("size") size: Int = 15, @Query("page") page: Int = 1): Response<PagedList<Notification>>
}