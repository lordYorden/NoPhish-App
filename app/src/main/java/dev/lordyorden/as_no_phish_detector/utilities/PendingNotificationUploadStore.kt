package dev.lordyorden.as_no_phish_detector.utilities

import android.content.Context
import androidx.datastore.core.DataStore
import dev.lordyorden.as_no_phish_detector.datastore.CapturedNotificationPayloadRecord
import dev.lordyorden.as_no_phish_detector.datastore.PendingNotificationUploadRecord
import dev.lordyorden.as_no_phish_detector.datastore.PendingNotificationUploads
import dev.lordyorden.as_no_phish_detector.models.CapturedNotificationPayload
import dev.lordyorden.as_no_phish_detector.models.PendingNotificationUpload
import dev.lordyorden.as_no_phish_detector.utilities.datastore.PendingNotificationUploadsSerializer
import kotlinx.coroutines.flow.first

class PendingNotificationUploadStore private constructor(context: Context) {
    private val dataStore: DataStore<PendingNotificationUploads> = EncryptedDataStoreFactory.create(
        context.applicationContext,
        DATASTORE_FILE_NAME,
        PendingNotificationUploadsSerializer
    )

    suspend fun save(upload: PendingNotificationUpload) {
        require(upload.payload.eventId.isNotBlank()) { "payload.eventId must not be blank" }
        require(upload.sourceUserId.isNotBlank()) { "sourceUserId must not be blank" }
        require(upload.circleId.isNotBlank()) { "circleId must not be blank" }

        val uploadRecord = upload.toRecord()

        updateUploads { uploads ->
            uploads
                .filterNot { it.payload.eventId == upload.payload.eventId }
                .plus(uploadRecord)
        }
    }

    suspend fun getPendingUploads(): List<PendingNotificationUpload> {
        return loadAll()
    }

    suspend fun removeExpired(now: Long = System.currentTimeMillis()) {
        updateUploads { uploads ->
            uploads.filter { now - it.createdAt <= Constants.UploadScheduler.TTL_MILLIS }
        }
    }

    suspend fun remove(eventIds: Set<String>) {
        if (eventIds.isEmpty()) return
        require(eventIds.none { it.isBlank() }) { "eventIds must not contain blank values" }

        updateUploads { uploads ->
            uploads.filterNot { eventIds.contains(it.payload.eventId) }
        }
    }

    suspend fun clearAll() {
        updateUploads { emptyList() }
    }

    private suspend fun loadAll(): List<PendingNotificationUpload> {
        return dataStore.data.first().uploadsList.map { it.toModel() }
    }

    private suspend fun updateUploads(
        transform: (List<PendingNotificationUploadRecord>) -> List<PendingNotificationUploadRecord>
    ) {
        dataStore.updateData { current ->
            current.withUploads(transform(current.uploadsList))
        }
    }

    private fun PendingNotificationUploads.withUploads(
        uploads: List<PendingNotificationUploadRecord>
    ): PendingNotificationUploads {
        return toBuilder()
            .clearUploads()
            .addAllUploads(uploads)
            .build()
    }

    private fun PendingNotificationUpload.toRecord(): PendingNotificationUploadRecord {
        return PendingNotificationUploadRecord.newBuilder()
            .setPayload(payload.toRecord())
            .setCreatedAt(createdAt)
            .setSourceUserId(sourceUserId)
            .setCircleId(circleId)
            .build()
    }

    private fun CapturedNotificationPayload.toRecord(): CapturedNotificationPayloadRecord {
        return CapturedNotificationPayloadRecord.newBuilder()
            .setEventId(eventId)
            .setTitle(title)
            .setBody(body)
            .setPackageName(packageName)
            .setTimestamp(timestamp)
            .addAllUrls(urls)
            .build()
    }

    private fun PendingNotificationUploadRecord.toModel(): PendingNotificationUpload {
        require(payload.eventId.isNotBlank()) { "payload.eventId must not be blank" }
        require(sourceUserId.isNotBlank()) { "sourceUserId must not be blank" }
        require(circleId.isNotBlank()) { "circleId must not be blank" }

        return PendingNotificationUpload(
            payload = payload.toModel(),
            createdAt = createdAt,
            sourceUserId = sourceUserId,
            circleId = circleId
        )
    }

    private fun CapturedNotificationPayloadRecord.toModel(): CapturedNotificationPayload {
        return CapturedNotificationPayload(
            eventId = eventId,
            title = title,
            body = body,
            packageName = packageName,
            timestamp = timestamp,
            urls = urlsList
        )
    }

    companion object {
        private const val DATASTORE_FILE_NAME = "pending_notification_uploads.pb"

        @Volatile
        private var instance: PendingNotificationUploadStore? = null

        fun getInstance(context: Context): PendingNotificationUploadStore {
            return instance ?: synchronized(this) {
                instance ?: PendingNotificationUploadStore(context).also { instance = it }
            }
        }
    }
}
