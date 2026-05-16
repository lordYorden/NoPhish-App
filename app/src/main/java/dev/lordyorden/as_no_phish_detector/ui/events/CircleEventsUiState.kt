package dev.lordyorden.as_no_phish_detector.ui.events

data class CircleEventsUiState(
    val events: List<CircleEventUiItem> = emptyList(),
    val loading: HistoryLoading = HistoryLoading.Initial,
    val endReached: Boolean = false,
    val errorMessage: String? = null,
    val alertScope: CircleAlertScope = CircleAlertScope.ActionRequired,
)
