package dev.lordyorden.as_no_phish_detector.clerk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import dev.convex.android.AuthState
import dev.convex.android.ConvexClientWithAuth
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class UserStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)

    private val client: ConvexClientWithAuth<String> = ConvexHelper.getInstance().convexClient
    val uiState = _uiState.asStateFlow()
    init {
        combine(client.authState, Clerk.isInitialized, Clerk.sessionFlow) { authState, isInitialized, session ->
            _uiState.value =  when (authState) {
                is AuthState.Authenticated -> UserUiState.SignedIn
                is AuthState.AuthLoading -> UserUiState.Loading
                is AuthState.Unauthenticated ->
                    when {
                        !isInitialized -> UserUiState.Loading
                        session?.status == Session.SessionStatus.ACTIVE -> UserUiState.Loading
                        else -> UserUiState.SignedOut
                    }
            }
        }
        .launchIn(viewModelScope)
    }
}