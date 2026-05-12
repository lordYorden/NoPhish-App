package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class PendingNotificationUpload(
    val payload: CapturedNotificationPayload,
    val createdAt: Long
)
