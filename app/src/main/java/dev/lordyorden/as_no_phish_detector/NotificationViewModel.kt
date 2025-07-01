package dev.lordyorden.as_no_phish_detector

import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.as_no_phish_detector.models.PagedList
import dev.lordyorden.as_no_phish_detector.retrofit.GenericCallback
import dev.lordyorden.as_no_phish_detector.retrofit.NotificationController

class NotificationViewModel : ViewModel() {

    private val _event = MutableLiveData<Notification?>(null)
    val newNotif: LiveData<Notification?> = _event

    private val _toRemove = MutableLiveData<Notification?>(null)
    val removeNotif: LiveData<Notification?> = _toRemove

    private val controller = NotificationController()
    //val SEARCH_TYPE = "user_details"

    //private var nurseObject: ObjectBoundary? = null

    lateinit var lifecycle: LifecycleCoroutineScope


    fun setEvent(obj: Notification) {
        _event.postValue(obj)
    }

    fun setRemove(obj: Notification) {
        _toRemove.postValue(obj)
    }

//    fun setNurse(email: String){
//        findObjectByEmail(email) { obj ->
//            nurseObject = obj
//        }
//    }

    fun checkNewNotifications(existing: List<Notification>) {
        //nurseObject ?: return

        val notificationsLeft = existing.toMutableList()

        controller.getNotifications(object : GenericCallback<PagedList<Notification>> {
            override fun success(data: PagedList<Notification>?) {
                data?.let { obj->
                    if(obj.items.isNotEmpty()){
                        obj.items.forEach { notif ->

                            if(existing.find { n -> n.id == notif.id } == null)
                                _event.value = notif
                            else{
                                notificationsLeft.removeIf { n -> n.id == notif.id }
                            }



                        }
                    }

                    notificationsLeft.forEach { removedNotif ->
                        setRemove(removedNotif)
                    }

                }
            }

            override fun error(error: String?) {
                error?.let { err ->  Log.e("Event Manager", err)}
            }
        }, 50, 1)
    }
}