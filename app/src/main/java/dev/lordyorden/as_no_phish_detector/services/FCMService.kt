package dev.lordyorden.as_no_phish_detector.services

import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.Gson
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.utilities.NotificationHelper

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            val title = it.title ?: "none"
            val body = it.body ?: "none"

            val toDetailsScreen = Intent(this, ClientActivity::class.java).apply {
                // This is the magic flag to trigger onNewIntent
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)

                // Required if starting from a Service/Background context
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

                action = SHOW_DETAILS_ACTION

                if (message.data.isNotEmpty()) {
                    Log.i(TAG,"Custom Data: ${message.data}")
                    val data = message.data.getOrDefault("data", "{}")

                    val dataJson = Gson().fromJson(data, RelentNotificationInfo::class.java)

                    putExtra("packageName", dataJson.packageName)
                    putExtra("body", dataJson.body)
                    putStringArrayListExtra("urls", ArrayList(dataJson.urls.toList()))
                }
            }

            Log.d(TAG, "preparing notif ${toDetailsScreen.action}")

            val notif = NotificationHelper.getInstance()
                .buildNotif(title, body, CHANNEL_ID, toDetailsScreen, false)

            val notificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, notif)

            Log.i(TAG, "Message Notification Title: $title \nbody: $body")
        }


    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_TOPIC"
        const val NOTIFICATION_ID = 127
        const val SHOW_DETAILS_ACTION = "dev.lordyorden.as_no_phish_detector.action.show_details"
    }


}