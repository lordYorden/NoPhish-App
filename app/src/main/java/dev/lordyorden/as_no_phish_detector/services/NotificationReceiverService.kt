package dev.lordyorden.as_no_phish_detector.services

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.annotation.RequiresApi

@RequiresApi(Build.VERSION_CODES.Q)
class NotificationReceiverService : NotificationListenerService() {
    override fun onBind(intent: Intent): IBinder? {
        Log.i("NotificationReceiverService", "bind")
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        if (packageName == "com.android.systemui"){
            return
        }

        sbn.notification?.extras?.let { meta ->
            val title = meta.getCharSequence(Notification.EXTRA_TITLE, "none")
            val body = meta.getCharSequence(Notification.EXTRA_TEXT, "none")
            val flags = getNotificationSetFlags(sbn.notification.flags)

            val titleStr = title?.toString() ?: "none"
            val bigText: CharSequence = meta.getCharSequence(Notification.EXTRA_BIG_TEXT, "none")
            /*val subText: String = meta.getString(Notification.EXTRA_SUB_TEXT, "none")
            val infoText: String = meta.getString(Notification.EXTRA_INFO_TEXT, "none")*/
            val timestamp = sbn.postTime
            Log.i("NotificationReceiverService", buildString {
                append("title: ")
                append(titleStr)
                append(" content: ")
                append(body)
                append(" bigText: ")
                append(bigText)
                append(" timestamp: ")
                append(timestamp)
                append(" flagID: ")
                append(sbn.notification.flags)
                append(" flags: ")
                append(flags)
                append(" package: ")
                append(packageName)
            })

            if (flags.contains("FLAG_ONGOING_EVENT") || flags.contains("FLAG_FOREGROUND_SERVICE") || flags.contains("FLAG_GROUP_SUMMARY") || flags.contains("FLAG_NO_CLEAR")){
                return
            }

            val serviceIntent = Intent(this, UploadForegroundService::class.java)
            serviceIntent.let {
                it.putExtra("title", titleStr)
                it.putExtra("body", body)
                it.putExtra("timestamp", timestamp)
                it.putExtra("isSMS", false)
                it.action = UploadForegroundService.ACTION_START
            }
            startForegroundService(serviceIntent)
        }

    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        // Implement what you want here
    }

    private val notificationFlagNames = mapOf(
        Notification.FLAG_AUTO_CANCEL to "FLAG_AUTO_CANCEL",
        Notification.FLAG_ONGOING_EVENT to "FLAG_ONGOING_EVENT",
        Notification.FLAG_INSISTENT to "FLAG_INSISTENT",
        Notification.FLAG_NO_CLEAR to "FLAG_NO_CLEAR",
        Notification.FLAG_FOREGROUND_SERVICE to "FLAG_FOREGROUND_SERVICE",
        Notification.FLAG_GROUP_SUMMARY to "FLAG_GROUP_SUMMARY",
        Notification.FLAG_LOCAL_ONLY to "FLAG_LOCAL_ONLY",
        Notification.FLAG_BUBBLE to "FLAG_BUBBLE",
        Notification.FLAG_SHOW_LIGHTS to "FLAG_SHOW_LIGHTS",
        Notification.FLAG_HIGH_PRIORITY to "FLAG_HIGH_PRIORITY",
        Notification.FLAG_ONLY_ALERT_ONCE to "FLAG_ONLY_ALERT_ONCE"
        // Add more Notification.FLAG_ constants as needed:
        // Notification.FLAG_ONLY_ALERT_ONCE to "FLAG_ONLY_ALERT_ONCE",
        // Notification.FLAG_HIGH_PRIORITY to "FLAG_HIGH_PRIORITY", // Deprecated in API 26+
        // Notification.FLAG_SHOW_LIGHTS to "FLAG_SHOW_LIGHTS",
        // Notification.FLAG_VIBRATE to "FLAG_VIBRATE",
        // Notification.FLAG_SOUND to "FLAG_SOUND"
    )

    private fun getNotificationSetFlags(flags: Int): List<String> {
        return notificationFlagNames.filter { (flagValue, _) ->
            // Use bitwise AND to check if the specific flagValue bit is set within 'flags'
            (flags and flagValue) != 0
        }.map { (_, flagName) ->
            flagName
        }.toList()
    }
}