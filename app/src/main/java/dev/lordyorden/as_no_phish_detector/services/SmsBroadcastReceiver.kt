package dev.lordyorden.as_no_phish_detector.services

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Telephony
import android.util.Log
import androidx.annotation.RequiresApi

class SmsBroadcastReceiver : BroadcastReceiver() {


    @RequiresApi(Build.VERSION_CODES.O)
    override fun onReceive(context: Context?, intent: Intent?) {
        Log.i(TAG, "onReceive: ${intent?.action}")

        if (intent?.action.equals(Telephony.Sms.Intents.SMS_RECEIVED_ACTION)) {
            val msgFromIntent = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            msgFromIntent.forEachIndexed { index, smsMessage ->
                Log.i(TAG, "onReceive: $index")

                Log.i(TAG, "Address: ${smsMessage.originatingAddress}")
                Log.i(TAG, "Body: ${smsMessage.displayMessageBody}")
                Log.i(TAG, "user data: ${smsMessage.userData}")

                val serviceIntent = Intent(context, UploadForegroundService::class.java)
                serviceIntent.let {
                    it.putExtra("address", smsMessage.originatingAddress)
                    it.putExtra("body", smsMessage.displayMessageBody)
                    it.putExtra("timestamp", smsMessage.timestampMillis)
                    it.putExtra("isSMS", true)

                    it.action = UploadForegroundService.ACTION_START
                }
                context?.startForegroundService(serviceIntent)
            }
        }
    }

    companion object {
        private const val TAG = "SmsBroadcastReceiver"
    }
}