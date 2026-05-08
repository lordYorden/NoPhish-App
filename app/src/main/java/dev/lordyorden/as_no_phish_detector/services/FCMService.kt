package dev.lordyorden.as_no_phish_detector.services

import android.app.NotificationManager
import android.content.Intent
import android.util.Log
import com.clerk.api.Clerk
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import dev.lordyorden.as_no_phish_detector.ClientActivity
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import dev.lordyorden.as_no_phish_detector.utilities.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class FCMService : FirebaseMessagingService() {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val maliciousPayload = parseMaliciousInfoPayload(message.data)

        maliciousPayload?.let { payload ->
            MaliciousNotificationStore.getInstance().saveFromFcmPayload(payload)
            Log.i(TAG, "Stored malicious notification payload from FCM for eventId=${payload.eventId}")

            serviceScope.launch {
                registerMaliciousEventIfSource(payload)
            }
        }

        message.notification?.let {
            val title = it.title ?: maliciousPayload?.title ?: "Threat detected"
            val body = it.body ?: "Open NoPhish for details"

            val toDetailsScreen = Intent(this, ClientActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                action = SHOW_DETAILS_ACTION

                maliciousPayload?.let { payload ->
                    putExtra("eventId", payload.eventId)
                    putExtra("contentHash", payload.contentHash)
                }
            }

            val notif = NotificationHelper.getInstance()
                .buildNotif(title, body, CHANNEL_ID, toDetailsScreen, false)

            val notificationManager =
                this.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(NOTIFICATION_ID, notif)

            Log.i(TAG, "Message Notification Title: $title \nbody: $body")
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun parseMaliciousInfoPayload(data: Map<String, String>): RelentNotificationInfo? {
        if (data.isEmpty()) return null

        return runCatching {
            parseMaliciousInfoJson(data.requireString("data"))
        }.getOrElse { error ->
            Log.e(TAG, "Failed to parse malicious notification payload", error)
            null
        }
    }

    private fun parseMaliciousInfoJson(json: String): RelentNotificationInfo {
        val payload = JsonParser.parseString(json).asJsonObject

        return RelentNotificationInfo(
            eventId = payload.requireString("eventId"),
            sourceUserId = payload.requireString("sourceUserId"),
            circleId = payload.optionalString("circleId"),
            title = payload.optionalString("title"),
            body = payload.requireString("body"),
            packageName = payload.requireString("packageName"),
            timestamp = payload.requireLong("timestamp"),
            contentHash = payload.requireString("contentHash"),
            urls = payload.optionalStringList("urls")
        )
    }

    private suspend fun registerMaliciousEventIfSource(payload: RelentNotificationInfo) {
        val currentUserId = Clerk.activeUser?.id
        if (currentUserId.isNullOrBlank()) {
            Log.w(TAG, "Skipping Convex malicious event upload: missing active user for eventId=${payload.eventId}")
            return
        }

        if (currentUserId != payload.sourceUserId) {
            Log.i(TAG, "Skipping Convex malicious event upload on receiving device for eventId=${payload.eventId}")
            return
        }

        val circleId = payload.circleId?.takeIf { it.isNotBlank() } ?: getCurrentCircleId()
        if (circleId.isNullOrBlank() || circleId == Constants.Onboarding.ACTION_GENERATE) {
            Log.w(TAG, "Skipping Convex malicious event upload: missing circleId for eventId=${payload.eventId}")
            return
        }

        val args = mutableMapOf<String, Any?>(
            "circleId" to circleId,
            "timestamp" to payload.timestamp.toDouble(),
            "action" to "malicious notification",
            "eventId" to payload.eventId,
            "contentHash" to payload.contentHash,
            //"sourceUserId" to payload.sourceUserId
        )

        payload.packageName.takeIf { it.isNotBlank() }?.let {
            args["packageName"] = it
        }

        runCatching {
            ConvexHelper.getInstance().convexClient.mutation<String>("events:register", args)
        }.onSuccess {
            Log.i(TAG, "Registered malicious notification metadata in Convex for eventId=${payload.eventId}")
        }.onFailure { error ->
            Log.e(TAG, "Failed to register malicious notification metadata in Convex", error)
        }
    }

    private suspend fun getCurrentCircleId(): String? {
        return runCatching {
            ConvexHelper.getInstance().convexClient.mutation<String>("circles:get_my_circles")
        }.getOrElse { error ->
            Log.e(TAG, "Failed to resolve current circle for malicious event upload", error)
            null
        }
    }

    private fun Map<String, String>.requireString(fieldName: String): String {
        return this[fieldName] ?: throw IllegalArgumentException("Missing required field: $fieldName")
    }

    private fun JsonObject.requireString(fieldName: String): String {
        val element = get(fieldName)
        if (element == null || element.isJsonNull) {
            throw IllegalArgumentException("Missing required field: $fieldName")
        }

        return element.asString
    }

    private fun JsonObject.requireLong(fieldName: String): Long {
        val element = get(fieldName)
        if (element == null || element.isJsonNull) {
            throw IllegalArgumentException("Missing required field: $fieldName")
        }

        return runCatching { element.asLong }
            .getOrElse { throw IllegalArgumentException("Invalid required long field: $fieldName", it) }
    }

    private fun JsonObject.optionalString(fieldName: String): String? {
        val element = get(fieldName)
        return if (element == null || element.isJsonNull) null else element.asString
    }

    private fun JsonObject.optionalStringList(fieldName: String): List<String> {
        val element = get(fieldName)
        if (element == null || element.isJsonNull) return emptyList()

        return element.asJsonArray.map { it.asString }
    }

    companion object {
        private const val TAG = "FCMService"
        const val CHANNEL_ID = "dev.lordyorden.as_no_phish_detector.CHANNEL_ID_TOPIC"
        const val NOTIFICATION_ID = 128
        const val SHOW_DETAILS_ACTION = "dev.lordyorden.as_no_phish_detector.action.show_details"
    }
}
