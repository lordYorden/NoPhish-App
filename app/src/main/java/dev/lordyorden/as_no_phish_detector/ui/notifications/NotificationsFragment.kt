package dev.lordyorden.as_no_phish_detector.ui.notifications

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.adapters.NotificationAdapter
import dev.lordyorden.as_no_phish_detector.databinding.FragmentNotificationsBinding
import dev.lordyorden.as_no_phish_detector.models.Notification

class NotificationsFragment : Fragment() {

    private lateinit var binding: FragmentNotificationsBinding
    val notificationViewModel: NotificationViewModel by activityViewModels()
    private lateinit var adapter: NotificationAdapter
    private val notificationList: MutableList<Notification> = mutableListOf()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false)
        val root: View = binding.root

        notificationViewModel.lifecycle = lifecycleScope
        initViews()
        return root
    }

    private fun initViews() {

        binding.btnFetch.setOnClickListener {
            notificationViewModel.checkNewNotifications(notificationList)
        }

        notificationViewModel.newNotif.observe(viewLifecycleOwner) { notifications->
            if(notifications == null)
                return@observe

            notifications.forEach { notif ->
                addNotification(notif)
            }
        }

        notificationViewModel.removeNotif.observe(viewLifecycleOwner) { notifications->
            if(notifications == null)
                return@observe

            notifications.forEach { notif ->
                removeNotification(notif)
            }
        }


        binding.rvNotification.layoutManager = LinearLayoutManager(requireActivity())

        adapter = NotificationAdapter(notificationList) { notif ->

            //Toast.makeText(this, "title: ${notif.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvNotification.adapter = adapter
    }

    private fun removeNotification(notif: Notification) {
        val notifIdx = notificationList.indexOfFirst { n -> (n.id == notif.id) }
        if(notifIdx != -1) {
            notificationList.remove(notif)
            adapter.notifyItemRemoved(notifIdx)
        }
    }

    private fun addNotification(notif: Notification) {
        val notifIdx = notificationList.size
        notificationList.add(notif)
        adapter.notifyItemChanged(notifIdx)
    }
}