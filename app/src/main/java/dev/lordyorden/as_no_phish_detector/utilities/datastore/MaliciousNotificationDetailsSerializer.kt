package dev.lordyorden.as_no_phish_detector.utilities.datastore

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.Serializer
import com.google.protobuf.InvalidProtocolBufferException
import dev.lordyorden.as_no_phish_detector.datastore.MaliciousNotificationDetails
import java.io.InputStream
import java.io.OutputStream

object MaliciousNotificationDetailsSerializer : Serializer<MaliciousNotificationDetails> {
    override val defaultValue: MaliciousNotificationDetails = MaliciousNotificationDetails.getDefaultInstance()

    override suspend fun readFrom(input: InputStream): MaliciousNotificationDetails {
        return try {
            MaliciousNotificationDetails.parseFrom(input)
        } catch (exception: InvalidProtocolBufferException) {
            throw CorruptionException("Unable to read malicious notification details", exception)
        }
    }

    override suspend fun writeTo(t: MaliciousNotificationDetails, output: OutputStream) {
        t.writeTo(output)
    }
}
