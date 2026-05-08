package dev.lordyorden.as_no_phish_detector.utilities

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import dev.lordyorden.as_no_phish_detector.models.AttackDetails
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo

class MaliciousNotificationStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val gson = Gson()

    fun saveFromFcmPayload(payload: RelentNotificationInfo) {
        save(SecureNotificationHelper.toAttackDetails(payload))
    }

    fun save(details: AttackDetails) {
        if (details.eventId.isBlank()) return

        prefs.edit {
            putString(key(details.eventId), gson.toJson(details))
        }
    }

    fun get(eventId: String): AttackDetails? {
        val json = prefs.getString(key(eventId), null) ?: return null
        return runCatching { gson.fromJson(json, AttackDetails::class.java) }.getOrNull()
    }

    fun getValidated(eventId: String, expectedHash: String): AttackDetails? {
        val details = get(eventId) ?: return null
        return if (SecureNotificationHelper.isHashValid(details, expectedHash)) details else null
    }

    companion object {
        private const val PREFS_NAME = "malicious_notification_store"
        private const val EVENT_PREFIX = "event:"

        @Volatile
        private var instance: MaliciousNotificationStore? = null

        fun getInstance(): MaliciousNotificationStore {
            return instance
                ?: throw IllegalStateException("MaliciousNotificationStore must be initialized by calling init(context) before use")
        }

        fun init(context: Context): MaliciousNotificationStore {
            return instance ?: synchronized(this) {
                instance ?: MaliciousNotificationStore(context).also { instance = it }
            }
        }

        private fun key(eventId: String): String = EVENT_PREFIX + eventId
    }
}
