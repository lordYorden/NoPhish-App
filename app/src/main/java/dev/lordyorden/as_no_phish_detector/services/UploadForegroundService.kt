package dev.lordyorden.as_no_phish_detector.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import dev.lordyorden.as_no_phish_detector.MainActivity
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.models.CreateNotification
import dev.lordyorden.as_no_phish_detector.models.CreateSmsMessage
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.as_no_phish_detector.models.SmsMessage
import dev.lordyorden.as_no_phish_detector.retrofit.GenericCallback
import dev.lordyorden.as_no_phish_detector.retrofit.NotificationController
import dev.lordyorden.as_no_phish_detector.retrofit.SmsController


class UploadForegroundService : Service() {

    private var isServiceRunningRightNow = false
    private lateinit var wakeLock: WakeLock
    private lateinit var powerManager: PowerManager
    private var lastShownNotificationId = -1
    private val smsController: SmsController = SmsController()
    private val notificationController: NotificationController = NotificationController()

//    override fun onCreate() {
//        super.onCreate()
//        Log.i(TAG, "onCreate: created()")
//
////        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
////            .setContentTitle("SMS Receiver")
////            .setContentText("SMS Receiver is running")
////            .setSmallIcon(R.drawable.ic_fish)
////            .build()
////        startForeground(NOTIFICATION_ID, notification)
//    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.i(TAG, "onStartCommand: Intent Data: ${intent?.action}")
        //makeApiCall()

        val action = intent?.action

        if (action.equals(ACTION_START)) {

            intent?.extras?.let { msg ->
                handleNewMessage(msg)
            }

            if (isServiceRunningRightNow) {
                return START_STICKY
            }

            isServiceRunningRightNow = true
            notifyToUserForForegroundService()

        } else if (action.equals(ACTION_STOP)) {
            stopHandlingMassages()
            isServiceRunningRightNow = false
        }

        return START_STICKY
    }

    private fun handleNewMessage(message: Bundle){
        Log.d(TAG, "startRecording() + " + Thread.currentThread().name)

        val isSMS = message.getBoolean("isSMS", true)

        if(isSMS){
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
        }else{

            val title = message.getString("title", "none")
            val body = message.getString("body", "none")
            val timestamp = message.getLong("timestamp", 0L)

            Log.i(TAG, "$timestamp new notification, title: $title: $body")

            notificationController.uploadNotification(CreateNotification(title, body, timestamp), object : GenericCallback<Notification>{
                override fun success(data: Notification?) {
                    Log.i(TAG, "uploaded notification $data")
                }

                override fun error(error: String?) {
                    Log.e(TAG, "upload failed: $error")
                }
            })
        }


        /*smsController.uploadSms(CreateSmsMessage(number, body, timestamp), object : GenericCallback<SmsMessage>{
            override fun success(data: SmsMessage?) {
                Log.i(TAG, "uploaded sms message $data")
            }

            override fun error(error: String?) {
                Log.e(TAG, "upload failed: $error")
            }
        })*/


//        // Keep CPU working
//        val powerManager: PowerManager = getSystemService(POWER_SERVICE) as PowerManager
//        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NoPhishDetector:tag");
//        wakeLock.acquire(10*60*1000L /*10 minutes*/)
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
            notificationManager.cancel(lastShownNotificationId);
        }
        lastShownNotificationId = NOTIFICATION_ID;
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

    override fun onBind(p0: Intent?): IBinder? {
        Log.i(TAG, "onBind: called!")
        return null
    }

//    override fun stopService(name: Intent?): Boolean {
//        Log.i(TAG, "stopService: stopping.........")
//        return super.stopService(name)
//    }

    fun isMyServiceRunning(context: Context): Boolean {
        val manager = context.getSystemService(ACTIVITY_SERVICE) as ActivityManager
        //val runs = manager.getRunningServices(Int.MAX_VALUE)
        for (service in manager.getRunningServices(Int.MAX_VALUE)) {
            if (UploadForegroundService::class.java.name == service.service.className) {
                return true
            }
        }
        return false
    }

    companion object {
        private const val TAG = "SmsForegroundService"

        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_FOREGROUND"
        const val NOTIFICATION_ID = 127

        const val MAIN_ACTION: String = "dev.lordyorden.as_no_phish_detector.Services.SmsForegroundService.action.main"
        const val ACTION_START: String = "ACTION_START"
        const val ACTION_STOP: String = "ACTION_STOP"
    }
}