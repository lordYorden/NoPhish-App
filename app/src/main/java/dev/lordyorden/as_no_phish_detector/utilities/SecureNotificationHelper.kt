package dev.lordyorden.as_no_phish_detector.utilities

import dev.lordyorden.as_no_phish_detector.models.AttackDetails
import dev.lordyorden.as_no_phish_detector.models.RelentNotificationInfo
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.MessageDigest

object SecureNotificationHelper {
    private val json = Json {
        encodeDefaults = true
        explicitNulls = false
    }

    fun contentHash(
        eventId: String,
        title: String,
        body: String,
        packageName: String,
        urls: List<String>,
        notificationTimestamp: Long,
        sourceUserId: String
    ): String {
        return canonicalPayloadJson(
            eventId,
            title,
            body,
            packageName,
            urls,
            notificationTimestamp,
            sourceUserId
        ).sha256()
    }

    fun isHashValid(details: AttackDetails, expectedHash: String): Boolean {
        if (expectedHash.isBlank()) return false

        val actualHash = contentHash(
            eventId = details.eventId,
            title = details.title,
            body = details.body,
            packageName = details.packageName,
            urls = details.urls,
            notificationTimestamp = details.notificationTimestamp,
            sourceUserId = details.sourceUserId
        )

        return actualHash == expectedHash
    }

    fun toAttackDetails(payload: RelentNotificationInfo): AttackDetails {
        return AttackDetails(
            body = payload.body,
            packageName = payload.packageName,
            urls = payload.urls,
            eventId = payload.eventId,
            sourceUserId = payload.sourceUserId,
            title = payload.title.orEmpty(),
            notificationTimestamp = payload.timestamp,
            contentHash = payload.contentHash,
            receivedAt = System.currentTimeMillis()
        )
    }

    private fun canonicalPayloadJson(
        eventId: String,
        title: String,
        body: String,
        packageName: String,
        urls: List<String>,
        notificationTimestamp: Long,
        sourceUserId: String
    ): String {
        return json.encodeToString(
            CanonicalNotificationPayload(
                eventId = eventId,
                title = title,
                body = body,
                packageName = packageName,
                urls = urls.sorted(),
                timestamp = notificationTimestamp,
                sourceUserId = sourceUserId
            )
        )
    }

    private fun String.sha256(): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    @Serializable
    private data class CanonicalNotificationPayload(
        val eventId: String,
        val title: String,
        val body: String,
        val packageName: String,
        val urls: List<String>,
        val timestamp: Long,
        val sourceUserId: String
    )
}
