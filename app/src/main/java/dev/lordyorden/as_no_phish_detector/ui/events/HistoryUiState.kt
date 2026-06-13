package dev.lordyorden.as_no_phish_detector.ui.events

import dev.lordyorden.as_no_phish_detector.models.Event

data class HistoryUiState(
    val events: List<Event> = emptyList(),
    val loading: HistoryLoading = HistoryLoading.Initial,
    val endReached: Boolean = false,
    val errorMessage: String? = null,
)
