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
import kotlinx.coroutines.launch
import retrofit2.Response

class NotificationViewModel : ViewModel() {

    private val _event = MutableLiveData<List<Notification>?>(null)
    val newNotif: LiveData<List<Notification>?> = _event

    private val _toRemove = MutableLiveData<List<Notification>?>(null)
    val removeNotif: LiveData<List<Notification>?> = _toRemove

    private val controller = NotificationController()

    lateinit var lifecycle: LifecycleCoroutineScope


    fun setEvent(notif: List<Notification>) {
        _event.postValue(notif)
    }

    fun setRemove(removedNotif: List<Notification>) {
        _toRemove.postValue(removedNotif)
    }

    fun checkNewNotifications(existing: List<Notification>) {

        val notificationsLeft = existing.toMutableList()
        val addedNotif = mutableListOf<Notification>()

        lifecycle.launch {

            var res: Response<PagedList<Notification>>? = null

            try {
                res = controller.apiService.getNotifications(15, 1)

            }catch (e: Exception){
                Log.e("NotificationViewModel", "fetch failed: $e")

            }finally {
                val data: List<Notification> = res?.body()?.items ?: listOf()
                data.let { items ->
                    if (items.isNotEmpty()){
                        items.forEach { notif ->
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



        }
    }
}