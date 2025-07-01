package dev.lordyorden.as_no_phish_detector.retrofit

import dev.lordyorden.as_no_phish_detector.models.CreateSmsMessage
import dev.lordyorden.as_no_phish_detector.models.PagedList
import dev.lordyorden.as_no_phish_detector.models.SmsMessage
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class SmsController  {
    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(Constants.RestAPI.BASE_URL)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val genericService: SmsRestApi =
        retrofit.create(SmsRestApi::class.java)

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

    fun uploadSms(msg: CreateSmsMessage, objectsCallback: GenericCallback<SmsMessage>){
        val call: Call<SmsMessage> = genericService.uploadSms(msg)
        call.enqueue(getResponseCallback(objectsCallback))
    }

    fun getSmsMessage(msgID: String, objectsCallback: GenericCallback<SmsMessage>){
        val call: Call<SmsMessage> = genericService.getSmsMessage(msgID)
        call.enqueue(getResponseCallback(objectsCallback))
    }

    fun getSmsMessages(size: Int = 15, page: Int = 1, objectsCallback: GenericCallback<PagedList<SmsMessage>>){
        val call: Call<PagedList<SmsMessage>> = genericService.getSmsMessages(size, page)
        call.enqueue(getResponseCallback(objectsCallback))
    }

    fun getSmsMessagesByNumber(number: String, size: Int = 15, page: Int = 1, objectsCallback: GenericCallback<PagedList<SmsMessage>>){
        val call: Call<PagedList<SmsMessage>> = genericService.getSmsMessagesByNumber(number, size, page)
        call.enqueue(getResponseCallback(objectsCallback))
    }
}