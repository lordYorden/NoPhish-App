package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class CapturedNotificationPayload(
    val eventId: String,
    val title: String,
    val body: String,
    val packageName: String,
    val timestamp: Long,
    val urls: List<String>
)
