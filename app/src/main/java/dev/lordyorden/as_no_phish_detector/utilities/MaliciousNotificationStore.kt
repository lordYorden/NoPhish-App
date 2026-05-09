package dev.lordyorden.as_no_phish_detector.utilities

import android.content.Context
import android.util.Log
import androidx.datastore.core.DataStore
import com.google.gson.Gson
import dev.lordyorden.as_no_phish_detector.datastore.AttackDetailsRecord
import dev.lordyorden.as_no_phish_detector.datastore.MaliciousNotificationDetails
import dev.lordyorden.as_no_phish_detector.models.AttackDetails
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import dev.lordyorden.as_no_phish_detector.utilities.datastore.MaliciousNotificationDetailsSerializer
import kotlinx.coroutines.flow.first

class MaliciousNotificationStore private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val dataStore: DataStore<MaliciousNotificationDetails> = EncryptedDataStoreFactory.create(
        appContext,
        DATASTORE_FILE_NAME,
        MaliciousNotificationDetailsSerializer
    )
    private val gson = Gson()
    @Volatile
    private var legacyMigrationChecked = false

    suspend fun saveFromFcmPayload(payload: RelentNotificationInfo) {
        save(SecureNotificationHelper.toAttackDetails(payload))
    }

    suspend fun save(details: AttackDetails) {
        if (details.eventId.isBlank()) return

        ensureLegacyMigrated()
        dataStore.updateData { current ->
            current.toBuilder()
                .clearDetails()
                .addAllDetails(
                    current.detailsList
                        .filterNot { it.eventId == details.eventId }
                        .plus(details.toRecord())
                )
                .setLegacyMigrationComplete(true)
                .build()
        }
    }

    suspend fun get(eventId: String): AttackDetails? {
        ensureLegacyMigrated()
        return dataStore.data.first()
            .detailsList
            .firstOrNull { it.eventId == eventId }
            ?.toModel()
    }

    suspend fun getValidated(eventId: String, expectedHash: String): AttackDetails? {
        val details = get(eventId) ?: return null
        return if (SecureNotificationHelper.isHashValid(details, expectedHash)) details else null
    }

    private suspend fun ensureLegacyMigrated() {
        if (legacyMigrationChecked) return

        var shouldClearLegacyPrefs = false
        dataStore.updateData { current ->
            if (current.legacyMigrationComplete) {
                current
            } else {
                val migratedDetails = readLegacyDetails()
                shouldClearLegacyPrefs = true
                if (migratedDetails.isNotEmpty()) {
                    current.toBuilder()
                        .clearDetails()
                        .addAllDetails(migratedDetails.map { it.toRecord() })
                        .setLegacyMigrationComplete(true)
                        .build()
                } else {
                    current.toBuilder()
                        .setLegacyMigrationComplete(true)
                        .build()
                }
            }
        }
        if (shouldClearLegacyPrefs) {
            clearLegacyPrefs()
        }
        legacyMigrationChecked = true
    }

    private fun readLegacyDetails(): List<AttackDetails> {
        val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return prefs.all.mapNotNull { (key, value) ->
            if (!key.startsWith(EVENT_PREFIX) || value !is String) return@mapNotNull null

            runCatching {
                gson.fromJson(value, AttackDetails::class.java)
            }.getOrElse { error ->
                Log.w(TAG, "Skipping malformed legacy malicious notification detail for $key", error)
                null
            }
        }
    }

    private fun clearLegacyPrefs() {
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).edit().clear().commit()
        appContext.deleteSharedPreferences(PREFS_NAME)
    }

    private fun AttackDetails.toRecord(): AttackDetailsRecord {
        return AttackDetailsRecord.newBuilder()
            .setEventId(eventId)
            .setSourceUserId(sourceUserId)
            .setTitle(title)
            .setBody(body)
            .setPackageName(packageName)
            .setNotificationTimestamp(notificationTimestamp)
            .setContentHash(contentHash)
            .setReceivedAt(receivedAt)
            .addAllUrls(urls)
            .build()
    }

    private fun AttackDetailsRecord.toModel(): AttackDetails {
        return AttackDetails(
            body = body,
            packageName = packageName,
            urls = urlsList,
            eventId = eventId,
            sourceUserId = sourceUserId,
            title = title,
            notificationTimestamp = notificationTimestamp,
            contentHash = contentHash,
            receivedAt = receivedAt
        )
    }

    companion object {
        private const val TAG = "MaliciousNotificationStore"
        private const val PREFS_NAME = "malicious_notification_store"
        private const val EVENT_PREFIX = "event:"
        private const val DATASTORE_FILE_NAME = "malicious_notification_details.pb"

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

    }
}
