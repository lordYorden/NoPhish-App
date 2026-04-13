package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class Member(
    val name: String,
    val familyRole: String,
    val userId: String,
//    val avatarUrl: String?
)
