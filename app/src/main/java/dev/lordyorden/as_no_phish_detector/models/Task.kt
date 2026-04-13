package dev.lordyorden.as_no_phish_detector.models

import kotlinx.serialization.Serializable

@Serializable
data class Task(val text: String, val isCompleted: Boolean)
