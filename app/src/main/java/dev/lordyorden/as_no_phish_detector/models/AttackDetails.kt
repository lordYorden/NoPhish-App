package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class AttackDetails(
    val body: String,
    val packageName: String,
    val urls: List<String>,
    val eventId: String,
    val sourceUserId: String,
    val title: String = "",
    val notificationTimestamp: Long,
    val contentHash: String,
    val receivedAt: Long
)
