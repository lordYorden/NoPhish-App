package dev.lordyorden.as_no_phish_detector.ui.events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.convex.android.ConvexClient
import dev.convex.android.WebSocketState
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.models.PaginationResult
import dev.lordyorden.as_no_phish_detector.utilities.Constants
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.time.Instant

class EventViewModel : ViewModel() {

    private val client: ConvexClient = ConvexHelper.getInstance().convexClient

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    private var job: Job? = null
    private var webSocketJob: Job? = null
    private var requestedItemCount = Constants.HistoryPagination.PAGE_SIZE

    var startTime: Instant? = null
        set(value) {
            if (field == value) return
            field = value
            resetHistory()
        }

    fun start() {
        if (job != null) return
        observeReconnects()
        subscribeToHistory(HistoryLoading.Initial)
    }

    fun loadNextPage() {
        val state = _uiState.value
        if (state.loading != HistoryLoading.Idle || state.endReached || state.errorMessage != null) return

        requestedItemCount += Constants.HistoryPagination.PAGE_SIZE
        subscribeToHistory(HistoryLoading.Append)
    }

    fun retry() {
        val loading = if (_uiState.value.events.isEmpty()) {
            HistoryLoading.Initial
        } else {
            HistoryLoading.Append
        }
        subscribeToHistory(loading)
    }

    private fun resetHistory() {
        requestedItemCount = Constants.HistoryPagination.PAGE_SIZE
        job?.cancel()
        job = null
        _uiState.value = HistoryUiState(loading = HistoryLoading.Initial)
        subscribeToHistory(HistoryLoading.Initial)
    }

    private fun subscribeToHistory(loading: HistoryLoading) {
        job?.cancel()

        _uiState.update {
            it.copy(
                loading = loading,
                errorMessage = null,
                endReached = if (loading == HistoryLoading.Initial) false else it.endReached
            )
        }

        val args = historyArgs()
        Log.d(TAG, "subscribing to history with args: $args")

        job = viewModelScope.launch {
            try {
                client.subscribe<PaginationResult<Event>>("events:get", args).collect { result ->
                    result.onSuccess { page ->
                        Log.d(TAG, "received ${page.page.size} events; isDone=${page.isDone}")
                        _uiState.value = HistoryUiState(
                            events = page.page,
                            loading = HistoryLoading.Idle,
                            endReached = page.isDone,
                        )
                    }.onFailure { error ->
                        publishError(error)
                    }
                }
            } catch (error: Exception) {
                publishError(error)
            }
        }
    }

    private fun observeReconnects() {
        if (webSocketJob != null) return

        webSocketJob = viewModelScope.launch {
            client.webSocketStateFlow
                .drop(1)
                .distinctUntilChanged()
                .collect { state ->
                    if (state == WebSocketState.CONNECTED && _uiState.value.errorMessage != null) {
                        retry()
                    }
                }
        }
    }

    private fun publishError(error: Throwable) {
        val message = error.message ?: error::class.java.simpleName
        Log.e(TAG, "history subscription failed", error)
        _uiState.update {
            it.copy(
                loading = HistoryLoading.Idle,
                errorMessage = message
            )
        }
    }

    private fun historyArgs(): Map<String, Any?> {
        val args = mutableMapOf<String, Any?>(
            "paginationOpts" to mapOf(
                "cursor" to null,
                "numItems" to requestedItemCount.toFloat()
            )
        )

        val startTimestamp = startTime?.toEpochMilliseconds()
        if (startTimestamp != null) {
            args["startTime"] = startTimestamp.toDouble()
        }

        return args
    }

    companion object {
        const val TAG = "EventViewModel"
    }
}
