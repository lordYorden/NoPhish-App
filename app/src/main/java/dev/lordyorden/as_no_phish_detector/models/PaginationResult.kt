package dev.lordyorden.as_no_phish_detector.models
import kotlinx.serialization.Serializable

@Serializable
data class PaginationResult<T>(
    val continueCursor: String?,
    val isDone: Boolean,
    val page: List<T>
)
