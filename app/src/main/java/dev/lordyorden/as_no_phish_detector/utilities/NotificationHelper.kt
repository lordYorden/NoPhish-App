package dev.lordyorden.as_no_phish_detector.utilities

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Context.NOTIFICATION_SERVICE
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.lordyorden.as_no_phish_detector.MainActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.services.UploadForegroundService.Companion.NOTIFICATION_ID
import java.lang.ref.WeakReference

class NotificationHelper private constructor(context: Context) {

    companion object {

        @Volatile
        private var instance: NotificationHelper? = null

        fun getInstance(): NotificationHelper {
            return instance
                ?: throw IllegalStateException("NotificationHelper must be initialized by calling init(context) before use")
        }

        fun init(context: Context): NotificationHelper {
            return instance ?: synchronized(this) {
                instance ?: NotificationHelper(context).also { instance = it }
            }
        }
    }

    private val contextRef = WeakReference(context)

    private fun getNotificationBuilder(
        context: Context,
        channelId: String,
        importance: Int = NotificationManagerCompat.IMPORTANCE_HIGH
    ): NotificationCompat.Builder {
        val builder: NotificationCompat.Builder
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            prepareChannel(context, channelId, importance)
            builder = NotificationCompat.Builder(context, channelId)
        } else {
            builder = NotificationCompat.Builder(context)
        }
        return builder
    }

    private fun buildNotif(title: String, body: String, channelId: String, ongoing: Boolean, notificationIntent: Intent, context: Context): Notification {
        val notificationBuilder = getNotificationBuilder(
            context,
            channelId
        )

        val pendingIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        notificationBuilder
            .setContentIntent(pendingIntent) // Open activity
            .setOngoing(ongoing)
            .setSmallIcon(R.drawable.ic_nophish_color)
            //.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_nophish_color))
            .setContentTitle(title)
            .setContentText(body)

        return notificationBuilder.build()

    }

    fun buildNotif(title: String, body: String, channelId: String, intent: Intent?, ongoing: Boolean = false): Notification? {

        contextRef.get()?.let { context ->

            val notificationIntent = intent ?: Intent(
                context,
                MainActivity::class.java
            )

            return buildNotif(title, body, channelId, ongoing, notificationIntent, context)

           /* val notificationBuilder = getNotificationBuilder(
                context,
                channelId
            )

            val pendingIntent = PendingIntent.getActivity(
                context,
                NOTIFICATION_ID,
                notificationIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )

            notificationBuilder
                .setContentIntent(pendingIntent) // Open activity
                .setOngoing(true)
                .setSmallIcon(R.drawable.ic_nophish_color)
                //.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_nophish_color))
                .setContentTitle(title)
                .setContentText(body)

            return notificationBuilder.build()*/
        }

        return null
    }

    @RequiresApi(26)
    private fun prepareChannel(context: Context, id: String, importance: Int) {
        val channelName = "Live updates"
        val channelDescription = "Recording status"
        val nm = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        var nChannel = nm.getNotificationChannel(id)

        if (nChannel == null) {
            nChannel = NotificationChannel(id, channelName, importance)
            nChannel.description = channelDescription

            // from another answer
            nChannel.enableLights(true)
            nChannel.lightColor = Color.BLUE

            nm.createNotificationChannel(nChannel)
        }
    }

}