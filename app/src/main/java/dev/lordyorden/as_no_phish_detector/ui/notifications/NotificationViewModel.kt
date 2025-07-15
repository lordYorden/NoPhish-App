package dev.lordyorden.as_no_phish_detector.ui.notifications

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

    private val _event = MutableLiveData<List<Notification>?>(null)
    val newNotif: LiveData<List<Notification>?> = _event

    private val _toRemove = MutableLiveData<List<Notification>?>(null)
    val removeNotif: LiveData<List<Notification>?> = _toRemove

    private val controller = NotificationController()
    //val SEARCH_TYPE = "user_details"

    //private var nurseObject: ObjectBoundary? = null

    lateinit var lifecycle: LifecycleCoroutineScope


    fun setEvent(notif: List<Notification>) {
        _event.postValue(notif)
    }

    fun setRemove(removedNotif: List<Notification>) {
        _toRemove.postValue(removedNotif)
    }

//    fun setNurse(email: String){
//        findObjectByEmail(email) { obj ->
//            nurseObject = obj
//        }
//    }

    fun checkNewNotifications(existing: List<Notification>) {
        //nurseObject ?: return

        val notificationsLeft = existing.toMutableList()
        val addedNotif = mutableListOf<Notification>()

        controller.getNotifications(object : GenericCallback<PagedList<Notification>> {
            override fun success(data: PagedList<Notification>?) {
                data?.let { obj->
                    if(obj.items.isNotEmpty()){
                        obj.items.forEach { notif ->
                            if(existing.find { n -> n.id == notif.id } == null)
                                addedNotif.add(notif)
                            else{
                                notificationsLeft.removeIf { n -> n.id == notif.id }
                            }
                        }
                    }

                    if (addedNotif.isNotEmpty())
                        setEvent(addedNotif)

                    if(notificationsLeft.isNotEmpty())
                        setRemove(notificationsLeft)

//                    notificationsLeft.forEach { removedNotif ->
//                        setRemove(removedNotif)
//                    }

                }
            }

            override fun error(error: String?) {
                error?.let { err ->  Log.e("Event Manager", err)}
            }
        }, 50, 1)
    }
}