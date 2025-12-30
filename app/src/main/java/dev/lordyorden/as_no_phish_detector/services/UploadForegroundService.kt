package dev.lordyorden.as_no_phish_detector.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.lifecycleScope
import dev.lordyorden.as_no_phish_detector.MainActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.retrofit.NotificationController
import dev.lordyorden.as_no_phish_detector.retrofit.SmsController
import kotlinx.coroutines.launch
import java.security.MessageDigest


class UploadForegroundService : LifecycleService() {

    private var isServiceRunningRightNow = false
    private lateinit var wakeLock: WakeLock
    private lateinit var powerManager: PowerManager
    private var lastShownNotificationId = -1
    private val smsController: SmsController = SmsController()
    private val notificationController: NotificationController = NotificationController()

    override fun onCreate() {
        super.onCreate()
        Log.i(TAG, "onCreate: created()")

        lifecycleScope.launch {
            MessageBridge.messageFlow.collect { bundle ->
                handleNewMessage(bundle)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        Log.i(TAG, "onStartCommand: Intent Data: ${intent?.action}")

        val action = intent?.action

        if (action == ACTION_START) {

            if (!isServiceRunningRightNow) {
                isServiceRunningRightNow = true
                notifyToUserForForegroundService()
            }

        } else if (action == ACTION_STOP) {
            stopHandlingMassages()
            isServiceRunningRightNow = false
            stopSelf()
        }

        return START_STICKY
    }

    private suspend fun handleNewMessage(message: Bundle){
        //Log.d(TAG, "startRecording() + " + Thread.currentThread().name)

        val isSMS = message.getBoolean("isSMS", true)

/*        if(isSMS){
            val number = message.getString("address", "none")
            val body = message.getString("body", "none")
            val timestamp = message.getLong("timestamp", 0L)

            Log.i(TAG, "$timestamp new message from $number: $body")

            smsController.uploadSms(CreateSmsMessage(number, body, timestamp), object : GenericCallback<SmsMessage>{
                override fun success(data: SmsMessage?) {
                    Log.i(TAG, "uploaded sms message $data")
                }

                override fun error(error: String?) {
                    Log.e(TAG, "upload failed: $error")
                }
            })
        }else{*/
        if(!isSMS){

            val title = message.getString("title", "none")
            val extraTitle = message.getString("extraTitle", "none")
            val isGroup = message.getBoolean("isGroup", false)
            val body = message.getString("body", "none")
            val packageName = message.getString("packageName", "none")
            val timestamp = message.getLong("timestamp", 0L)
            val urls = message.getStringArrayList("urls")?.toList() ?: listOf<String>()

/*            val notif = CreateNotification(
                title,
                extraTitle,
                isGroup,
                body,
                packageName,
                timestamp)*/

            val hash = buildString {
                append(body)
                append(packageName) }.toSha256()

            val rel = RelentNotificationInfo(
                body,
                packageName,
                hash,
                urls
            )

            try {
                //notificationController.apiService.uploadNotification(notif)
                notificationController.apiService.uploadRelInfo(rel)

                Log.i(TAG, "uploaded notification $rel")
            } catch (e: Exception) {
                Log.e(TAG, "upload failed: $e")
            }
        }


/*        // Keep CPU working
        val powerManager: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoPhishDetector:tag");
        wakeLock.acquire(10*60*1000L)s*/
    }

    private fun String.toSha256(): String {
        val bytes = this.toByteArray()
        val md = MessageDigest.getInstance("SHA-256")
        val digest = md.digest(bytes)

        // Convert bytes to a hex string
        return digest.joinToString("") { "%02x".format(it) }
    }

    override fun onDestroy() {
        super.onDestroy()
    }

    private fun stopHandlingMassages(){
        //wakeLock.release();
        stopForeground(STOP_FOREGROUND_REMOVE)
    }

    /*@OptIn(DelicateCoroutinesApi::class)
    fun makeApiCall() {
        Log.i(TAG, "makeApiCall: Called!")
        GlobalScope.launch(Dispatchers.IO) {
            try {
                val response = RetrofitBuilder.getApiService().getPosts()
                Log.i(TAG, "rawJSON: ${response.body()}")
            } catch (e: Exception) {
               Log.e(TAG, "rawJSON: ", e)
            }

            //stop the service running foreground.
            stopSelf()
        }
        Log.i(TAG, "makeApiCall: Released!")
    }*/

    private fun notifyToUserForForegroundService(){

        val notificationIntent = Intent(
            this,
            MainActivity::class.java
        )
        notificationIntent.setAction(MAIN_ACTION)
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = PendingIntent.getActivity(
            this,
            NOTIFICATION_ID,
            notificationIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = getNotificationBuilder(this,
            CHANNEL_ID,
            NotificationManagerCompat.IMPORTANCE_HIGH)

        notificationBuilder
            .setContentIntent(pendingIntent) // Open activity
            .setOngoing(true)
            .setSmallIcon(R.drawable.ic_nophish_color)
            //.setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.ic_nophish_color))
            .setContentTitle("Keeping you protected")
            .setContentText("Scanning incoming messages and notifications")

        val notification = notificationBuilder.build()

        startForeground(NOTIFICATION_ID, notification)

        if (NOTIFICATION_ID != lastShownNotificationId) {
            // Cancel previous notification
            val notificationManager: NotificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.cancel(lastShownNotificationId)
        }
        lastShownNotificationId = NOTIFICATION_ID
    }

    private fun getNotificationBuilder(
        context: Context,
        channelId: String,
        importance: Int
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

/*    override fun onBind(p0: Intent?): IBinder? {
        Log.i(TAG, "onBind: called!")
        return null
    }

    override fun stopService(name: Intent?): Boolean {
        Log.i(TAG, "stopService: stopping.........")
        return super.stopService(name)
    }

    fun isMyServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        //val runs = manager.getRunningServices(Int.MAX_VALUE)
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }*/

    companion object {
        private const val TAG = "UploadForegroundService"
        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_FOREGROUND"
        const val NOTIFICATION_ID = 127
        const val MAIN_ACTION: String = "dev.lordyorden.as_no_phish_detector.Services.UploadForegroundService.action.main"
        const val ACTION_START: String = "ACTION_START"
        const val ACTION_STOP: String = "ACTION_STOP"
    }
}