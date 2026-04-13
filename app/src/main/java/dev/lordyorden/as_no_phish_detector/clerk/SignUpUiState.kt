package dev.lordyorden.as_no_phish_detector.clerk

sealed interface SignUpUiState {
    data object SignedOut : SignUpUiState
    data object Success : SignUpUiState
    data object NeedsVerification : SignUpUiState
}
