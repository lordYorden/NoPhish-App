package dev.lordyorden.as_no_phish_detector.models

data class CircleMember(
    val name: String,
    val role: String,
    val isConnected: Boolean,
    val avatarURL: String,
    val memberID: String
)
