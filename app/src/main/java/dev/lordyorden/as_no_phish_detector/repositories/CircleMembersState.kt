package dev.lordyorden.as_no_phish_detector.repositories

import dev.lordyorden.as_no_phish_detector.models.CircleMember

data class CircleMembersState(
    val circleId: String,
    val members: List<CircleMember> = emptyList(),
    val membersByUserId: Map<String, CircleMember> = emptyMap(),
    val loaded: Boolean = false,
    val errorMessage: String? = null,
)
