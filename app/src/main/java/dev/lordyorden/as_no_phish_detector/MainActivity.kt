package dev.lordyorden.as_no_phish_detector

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.databinding.ActivityMainBinding
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.as_no_phish_detector.services.UploadForegroundService
import dev.lordyorden.as_no_phish_detector.ui.notifications.NotificationViewModel


@RequiresApi(Build.VERSION_CODES.O)
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var adapter: NotificationAdapter
    private val notificationViewModel: NotificationViewModel by viewModels()
    private val notificationList: MutableList<Notification> = mutableListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        enableEdgeToEdge()
        setContentView(binding.root)

        initViews()
        notificationViewModel.lifecycle = lifecycleScope
        startActivity(Intent(ACTION_NOTIFICATION_LISTENER_SETTINGS))
    }


    private fun initViews() {
        binding.btnStart.setOnClickListener {
            commandToService(UploadForegroundService.ACTION_START)
        }

        binding.btnStop.setOnClickListener {
            commandToService(UploadForegroundService.ACTION_STOP)
            //notificationViewModel.checkNewNotifications(notificationList)
        }

//        notificationViewModel.newNotif.observe(this) { notification->
//            if(notification == null)
//                return@observe
//
//            addNotification(notification)
//        }
//
//        notificationViewModel.removeNotif.observe(this) { notification->
//            if(notification == null)
//                return@observe
//
//            removeNotification(notification)
//        }


        binding.rvNotification.layoutManager = LinearLayoutManager(this)

        adapter = NotificationAdapter(notificationList) { notif ->

            Toast.makeText(this, "title: ${notif.title}", Toast.LENGTH_SHORT).show()
        }

        binding.rvNotification.adapter = adapter
    }

    private fun commandToService(action: String) {
        val intent = Intent(this, UploadForegroundService::class.java)
        intent.setAction(action)
        startForegroundService(intent)
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