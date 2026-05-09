package dev.lordyorden.as_no_phish_detector.utilities.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.lordyorden.as_no_phish_detector.datastore.PendingNotificationUploads
import java.io.InputStream
import java.io.OutputStream

object PendingNotificationUploadsSerializer : Serializer<PendingNotificationUploads> {
    override val defaultValue: PendingNotificationUploads = PendingNotificationUploads.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): PendingNotificationUploads {
        return try {
            PendingNotificationUploads.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read pending notification uploads", exception)
        }
    }

    override suspend fun writeTo(t: PendingNotificationUploads, output: OutputStream) {
        t.writeTo(output)
    }
}
