package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class AttackDetails(
    val body: String,
    val packageName: String,
    val urls: List<String>
)
