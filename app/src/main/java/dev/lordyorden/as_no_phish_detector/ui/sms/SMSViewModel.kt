package dev.lordyorden.as_no_phish_detector.ui.sms

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.lordyorden.as_no_phish_detector.models.PagedList
import dev.lordyorden.as_no_phish_detector.models.SmsMessage
import dev.lordyorden.as_no_phish_detector.retrofit.GenericCallback
import dev.lordyorden.as_no_phish_detector.retrofit.SmsController

class SMSViewModel : ViewModel() {

    private val _event = MutableLiveData<List<SmsMessage>?>(null)
    val newNotif: LiveData<List<SmsMessage>?> = _event

    private val _toRemove = MutableLiveData<List<SmsMessage>?>(null)
    val removeNotif: LiveData<List<SmsMessage>?> = _toRemove

    private val controller = SmsController()

    lateinit var lifecycle: LifecycleCoroutineScope


    fun setEvent(sms: List<SmsMessage>) {
        _event.postValue(sms)
    }

    fun setRemove(removedSms: List<SmsMessage>) {
        _toRemove.postValue(removedSms)
    }

    fun checkNewNotifications(existing: List<SmsMessage>) {
        //nurseObject ?: return

        val smsLeft = existing.toMutableList()
        val addedSms = mutableListOf<SmsMessage>()

        controller.getSmsMessages(object : GenericCallback<PagedList<SmsMessage>> {
            override fun success(data: PagedList<SmsMessage>?) {
                data?.let { obj->
                    if(obj.items.isNotEmpty()){
                        obj.items.forEach { sms ->
                            if(existing.find { n -> n.id == sms.id } == null)
                                addedSms.add(sms)
                            else{
                                smsLeft.removeIf { n -> n.id == sms.id }
                            }
                        }
                    }

                    if (addedSms.isNotEmpty())
                        setEvent(addedSms)

                    if(smsLeft.isNotEmpty())
                        setRemove(smsLeft)
                }
            }

            override fun error(error: String?) {
                error?.let { err ->  Log.e("Event Manager", err)}
            }
        }, 50, 1)
    }
}