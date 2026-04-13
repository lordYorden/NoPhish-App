package dev.lordyorden.as_no_phish_detector.clerk

sealed interface MainUiState {
    data object Loading : MainUiState
    data object SignedIn : MainUiState
    data object SignedOut : MainUiState
}