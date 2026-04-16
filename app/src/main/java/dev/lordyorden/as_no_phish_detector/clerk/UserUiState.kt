package dev.lordyorden.as_no_phish_detector.clerk

sealed interface UserUiState {
    data object Loading : UserUiState
    data object SignedIn : UserUiState
    data object SignedOut : UserUiState
}