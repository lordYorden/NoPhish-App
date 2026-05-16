package dev.lordyorden.as_no_phish_detector.clerk

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.clerk.api.Clerk
import com.clerk.api.session.Session
import dev.convex.android.AuthState
import dev.convex.android.ConvexClientWithAuth
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn

class UserStateViewModel : ViewModel() {
    private val _uiState = MutableStateFlow<UserUiState>(UserUiState.Loading)

    private val client: ConvexClientWithAuth<String> = ConvexHelper.getInstance().convexClient
    private val networkMonitor = NetworkMonitor.getInstance()
    private var hasSeenAuthenticatedSession = Clerk.activeUser != null
    private var isInOfflineGrace = false

    val uiState = _uiState.asStateFlow()

    init {
        combine(
            client.authState,
            Clerk.isInitialized,
            Clerk.sessionFlow,
            Clerk.userFlow,
            networkMonitor.isOnline
        ) { authState, isInitialized, session, user, isOnline ->
            if (isInitialized && user == null) {
                hasSeenAuthenticatedSession = false
            }

            _uiState.value = when (authState) {
                is AuthState.Authenticated -> {
                    hasSeenAuthenticatedSession = true
                    isInOfflineGrace = false
                    UserUiState.SignedIn
                }

                is AuthState.AuthLoading ->
                    when {
                        !isOnline && hasSeenAuthenticatedSession -> {
                            isInOfflineGrace = true
                            UserUiState.SignedIn
                        }
                        !isOnline -> UserUiState.ConnectionFailure
                        else -> UserUiState.Loading
                    }

                is AuthState.Unauthenticated ->
                    when {
                        !isInitialized -> UserUiState.Loading
                        session?.status == Session.SessionStatus.ACTIVE -> UserUiState.Loading
                        !isOnline && hasSeenAuthenticatedSession -> {
                            isInOfflineGrace = true
                            UserUiState.SignedIn
                        }
                        !isOnline -> UserUiState.ConnectionFailure
                        isInOfflineGrace -> {
                            isInOfflineGrace = false
                            UserUiState.SignedOut
                        }
                        else -> UserUiState.SignedOut
                    }
            }
        }
            .launchIn(viewModelScope)
    }
}
