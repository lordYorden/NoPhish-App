package dev.lordyorden.as_no_phish_detector.models
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val action: String,
    val timestamp: Double,
    val userId: String,
    val circleId: String = "",
    val eventId: String = "",
    val contentHash: String = "",
    val packageName: String? = null
)
