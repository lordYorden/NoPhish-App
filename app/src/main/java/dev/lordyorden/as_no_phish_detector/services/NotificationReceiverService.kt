package dev.lordyorden.as_no_phish_detector.services

import android.app.Notification
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import android.util.Patterns
import androidx.annotation.RequiresApi
import androidx.core.os.bundleOf
import java.util.ArrayList

@RequiresApi(Build.VERSION_CODES.Q)
class NotificationReceiverService : NotificationListenerService() {
    override fun onBind(intent: Intent): IBinder? {
        Log.i("NotificationReceiverService", "bind")
        return super.onBind(intent)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {

        val packageName = sbn.packageName

        if (packageName == "com.android.systemui" || packageName == "dev.lordyorden.as_no_phish_detector"){
            return
        }

        sbn.notification?.extras?.let { meta ->

            val notifData = mapOf(
                "title" to meta.getCharSequence(Notification.EXTRA_TITLE, "none").toString(),
                "body" to meta.getCharSequence(Notification.EXTRA_TEXT, "none").toString(),
                "extraTitle" to meta.getString(Notification.EXTRA_CONVERSATION_TITLE, "none"),
                "isGroup" to meta.getBoolean(Notification.EXTRA_IS_GROUP_CONVERSATION, false),
                "bigText" to meta.getCharSequence(Notification.EXTRA_BIG_TEXT, "none").toString(),
                "timestamp" to sbn.postTime,
                "packageName" to sbn.packageName
            )

            val flags = getNotificationSetFlags(sbn.notification.flags)


            Log.i("NotificationReceiverService", buildString {

                notifData.map { item ->
                    append(item.key)
                    append(": ")
                    append(item.value)
                    append(", ")
                }

                append("flags: ")
                append(flags)
                append(", ")
            })

            if (flags.contains("FLAG_ONGOING_EVENT")
                || flags.contains("FLAG_FOREGROUND_SERVICE")
                || flags.contains("FLAG_GROUP_SUMMARY")
                || flags.contains("FLAG_NO_CLEAR")
                || flags.contains("FLAG_INSISTENT")){
                Log.e("NotificationReceiverService","rejected notif:  ${notifData["timestamp"]}")
                return
            }

            Log.e("NotificationReceiverService","accepted notif:  ${notifData["timestamp"]}")


            val serviceIntent = Intent(this, UploadForegroundService::class.java).apply {
                action = UploadForegroundService.ACTION_START
            }

            startForegroundService(serviceIntent)

            val body = notifData["body"] as String
            val urls = extractUrlsFromBody(body)

            val notifBundle = bundleOf(*notifData.map { it.key to it.value }.toTypedArray())
            notifBundle.putBoolean("isSMS", false)
            notifBundle.putStringArrayList("urls", ArrayList(urls))
            MessageBridge.sendMessage(notifBundle)
        }

    }

    private fun extractUrlsFromBody(body: String): List<String> {
        val urls = mutableListOf<String>()

        val matcher = Patterns.WEB_URL.matcher(body)
        while (matcher.find()) {
            urls.add(matcher.group())
        }

        return urls.toList()
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
        Notification.FLAG_ONLY_ALERT_ONCE to "FLAG_ONLY_ALERT_ONCE"
        // Add more Notification.FLAG_ constants as needed:
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