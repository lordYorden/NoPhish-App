package dev.lordyorden.as_no_phish_detector.retrofit

import dev.lordyorden.as_no_phish_detector.models.CreateNotification
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.as_no_phish_detector.models.PagedList
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class NotificationController  {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.RestAPI.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val genericService: NotificationRestApi =
        retrofit.create(NotificationRestApi::class.java)

    private fun <T> getResponseCallback(genericCallback: GenericCallback<T>): Callback<T> {
        return object : Callback<T> {
            override fun onResponse(call: Call<T>, response: Response<T>) {
                if (response.isSuccessful) {
                    try {
                        //val data = response.body()!!.string()
                        val res = response.body()
                        genericCallback.success(res)
                    } catch (e: IOException) {
                        genericCallback.error(response.errorBody()?.string())
                        throw RuntimeException(e)
                    }
                } else {
                    genericCallback.error(response.errorBody()?.string())
                }
            }

            override fun onFailure(call: Call<T>, throwable: Throwable) {
                genericCallback.error(throwable.message)
            }
        }
    }

    fun uploadNotification(notif: CreateNotification, objectsCallback: GenericCallback<Notification>){
        val call: Call<Notification> = genericService.uploadNotification(notif)
        call.enqueue(getResponseCallback(objectsCallback))
    }

    fun getNotifications(objectsCallback: GenericCallback<PagedList<Notification>>, size: Int = 15, page: Int = 1){
        val call: Call<PagedList<Notification>> = genericService.getNotifications(size, page)
        call.enqueue(getResponseCallback(objectsCallback))
    }

    /*fun getSmsMessage(msgID: String, objectsCallback: GenericCallback<SmsMessage>){
        val call: Call<SmsMessage> = genericService.getSmsMessage(msgID)
        call.enqueue(getResponseCallback(objectsCallback))
    }



    fun getSmsMessagesByNumber(number: String, size: Int = 15, page: Int = 1, objectsCallback: GenericCallback<PagedList<SmsMessage>>){
        val call: Call<PagedList<SmsMessage>> = genericService.getSmsMessagesByNumber(number, size, page)
        call.enqueue(getResponseCallback(objectsCallback))
    }*/
}