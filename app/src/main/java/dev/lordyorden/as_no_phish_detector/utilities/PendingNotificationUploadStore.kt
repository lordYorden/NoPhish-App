package dev.lordyorden.as_no_phish_detector.utilities

import android.content.Context
import androidx.core.content.edit
import dev.lordyorden.as_no_phish_detector.models.PendingNotificationUpload
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class PendingNotificationUploadStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(upload: PendingNotificationUpload) {
        val uploads = loadAll()
            .filterNot { it.payload.eventId == upload.payload.eventId }
            .plus(upload)

        saveAll(uploads)
    }

    fun getUnexpired(now: Long = System.currentTimeMillis()): List<PendingNotificationUpload> {
        val uploads = loadAll()
        val unexpired = uploads.filter { now - it.createdAt <= Constants.UploadScheduler.TTL_MILLIS }

        if (unexpired.size != uploads.size) {
            saveAll(unexpired)
        }

        return unexpired
    }

    fun remove(eventIds: Set<String>) {
        if (eventIds.isEmpty()) return

        saveAll(loadAll().filterNot { eventIds.contains(it.payload.eventId) })
    }

    private fun loadAll(): List<PendingNotificationUpload> {
        val jsonString = prefs.getString(KEY_UPLOADS, null) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(PendingNotificationUpload.serializer()), jsonString)
        }.getOrDefault(emptyList())
    }

    private fun saveAll(uploads: List<PendingNotificationUpload>) {
        prefs.edit {
            putString(KEY_UPLOADS, json.encodeToString(uploads))
        }
    }

    companion object {
        private const val PREFS_NAME = "pending_notification_upload_store"
        private const val KEY_UPLOADS = "uploads"

        private val json = Json {
            encodeDefaults = true
            explicitNulls = false
        }

        @Volatile
        private var instance: PendingNotificationUploadStore? = null

        fun getInstance(context: Context): PendingNotificationUploadStore {
            return instance ?: synchronized(this) {
                instance ?: PendingNotificationUploadStore(context).also { instance = it }
            }
        }
    }
}
