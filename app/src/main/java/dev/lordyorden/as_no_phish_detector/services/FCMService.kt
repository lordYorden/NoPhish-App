package dev.lordyorden.as_no_phish_detector.services

import android.app.NotificationManager
import android.util.Log
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.lordyorden.as_no_phish_detector.utilities.NotificationHelper

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        message.notification?.let {
            val title = it.title ?: "none"
            val body = it.body ?: "none"

            val notif = NotificationHelper.getInstance()
                .buildNotif(title, body, CHANNEL_ID)

            val notificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, notif)

            Log.i(TAG, "Message Notification Title: $title \nbody: $body")
            // You can call a function here to show a local notification
        }

        // 2. Check if the message contains a data payload (custom key-value pairs)
        if (message.data.isNotEmpty()) {
            val customData = message.data["test"]
            Log.i(TAG,"Custom Data: $customData")
        }
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_TOPIC"
        const val NOTIFICATION_ID = 127
    }


}