package dev.lordyorden.as_no_phish_detector.retrofit

import dev.lordyorden.as_no_phish_detector.utilities.Constants
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class NotificationController  {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.RestAPI.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    val apiService: NotificationRestApi =
        retrofit.create(NotificationRestApi::class.java)
}