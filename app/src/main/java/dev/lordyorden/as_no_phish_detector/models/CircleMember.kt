package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

//todo unite with circle member
@Serializable
data class CircleMember(
    val name: String,
    val familyRole: String,
    val userId: String,
    val avatarUrl: String? = null,
    val isConnected: Boolean
)
