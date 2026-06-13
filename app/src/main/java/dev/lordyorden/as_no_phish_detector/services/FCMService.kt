package dev.lordyorden.as_no_phish_detector.services

import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationPayloadParser
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import dev.lordyorden.as_no_phish_detector.utilities.NotificationHelper
import dev.lordyorden.as_no_phish_detector.utilities.SecureNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class FCMService : FirebaseMessagingService() {
    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)
        Log.i(TAG, "Received FCM message with data keys=${message.data.keys} notification=${message.notification != null}")

        val maliciousPayloadJson = getMaliciousPayloadJson(message.data) ?: run {
            Log.e(TAG, "No payload was found")
            return
        }

        val maliciousPayload = parseMaliciousInfoPayload(maliciousPayloadJson)

        maliciousPayload?.let { payload ->
            if (!isPayloadHashValid(payload)) return

            showNotification(payload)
            enqueueMaliciousEventRegistration(maliciousPayloadJson, payload.eventId)

            runBlocking(Dispatchers.IO) {
                runCatching {
                    MaliciousNotificationStore.getInstance().saveFromFcmPayload(payload)
                }.onSuccess {
                    Log.i(TAG, "Stored malicious notification payload from FCM for eventId=${payload.eventId}")
                }.onFailure { error ->
                    Log.e(TAG, "Failed to store malicious notification payload from FCM for eventId=${payload.eventId}", error)
                }
            }
        }
    }

    private fun isPayloadHashValid(payload: RelentNotificationInfo): Boolean {
        return runCatching {
            SecureNotificationHelper.requireValidPayloadHash(payload)
        }.onFailure { error ->
            Log.e(TAG, "Rejected tampered malicious notification payload for eventId=${payload.eventId}", error)
        }.isSuccess
    }

    private fun showNotification(maliciousPayload: RelentNotificationInfo) {
        val title = "Threat detected"
        val body = "Click here for more details"

        val toDetailsScreen = Intent(this, ClientActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            action = SHOW_DETAILS_ACTION

            putExtra("eventId", maliciousPayload.eventId)
            putExtra("contentHash", maliciousPayload.contentHash)
        }

        val notif = NotificationHelper.getInstance()
            .buildNotif(title, body, CHANNEL_ID, toDetailsScreen, false)

        val notificationManager =
            this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        notificationManager.notify(NOTIFICATION_ID, notif)

        Log.i(TAG, "Displayed malicious notification for eventId=${maliciousPayload.eventId}")
    }

    private fun parseMaliciousInfoPayload(json: String): RelentNotificationInfo? {
        return runCatching {
            MaliciousNotificationPayloadParser.parse(json)
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse malicious notification payload", error)
            null
        }
    }

    private fun getMaliciousPayloadJson(data: Map<String, String>): String? {
        if (data.isEmpty()) return null

        return data["data"]?.takeIf { it.isNotBlank() }
    }

    private fun enqueueMaliciousEventRegistration(payloadJson: String, eventId: String) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<RegisterMaliciousEventWorker>()
            .setConstraints(constraints)
            .setInputData(workDataOf(RegisterMaliciousEventWorker.KEY_PAYLOAD_JSON to payloadJson))
            .build()

        WorkManager.getInstance(applicationContext).enqueue(request)
        Log.i(TAG, "Enqueued malicious event registration worker for eventId=$eventId")
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_TOPIC"
        const val NOTIFICATION_ID = 128
        const val SHOW_DETAILS_ACTION = "dev.lordyorden.as_no_phish_detector.action.show_details"
    }
}
