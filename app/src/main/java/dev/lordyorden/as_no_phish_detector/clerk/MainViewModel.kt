package dev.lordyorden.as_no_phish_detector.clerk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<MainUiState>(MainUiState.Loading)
    val uiState = _uiState.asStateFlow()
    init {
        combine(Clerk.isInitialized, Clerk.userFlow) { isInitialized, user ->
            _uiState.value = when {
                !isInitialized -> MainUiState.Loading
                user != null -> MainUiState.SignedIn
                else -> MainUiState.SignedOut
            }
        }
            .launchIn(viewModelScope)
    }
}