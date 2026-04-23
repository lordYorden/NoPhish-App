package dev.lordyorden.as_no_phish_detector.models
import kotlinx.serialization.Serializable

@Serializable
data class Event(
    val memberName: String,
    val eventTimestamp: Long,
    val action: String,
    val moreDetails: String? = null
)
