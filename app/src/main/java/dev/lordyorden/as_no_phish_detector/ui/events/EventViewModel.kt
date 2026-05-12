package dev.lordyorden.as_no_phish_detector.ui.events

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dev.convex.android.ConvexClient
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.models.PaginationResult
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.time.Instant

class EventViewModel : ViewModel() {

    private val client: ConvexClient = ConvexHelper.getInstance().convexClient

    private val _events = MutableStateFlow<List<Event>>(emptyList())
    val events: StateFlow<List<Event>> = _events

    private var currentCursor: String? = null
    private var job: Job? = null
    private var isLastPage = false
    var isLoading = false
    var startTime: Instant? = null
        set(value) {
            field = value
            resetPagination()
        }

    private fun resetPagination() {
        currentCursor = null
        _events.value = emptyList()
        isLoading = false
        isLastPage = false
        loadNextPage()
    }

    fun loadNextPage() {
        if (isLoading || isLastPage) return

        job?.cancel()

        isLoading = true

        Log.d(TAG, "cursor: $currentCursor")

        job = viewModelScope.launch {
            try {

                val timestamp = startTime?.toEpochMilliseconds()?.plus(0f) ?: run {
                    Log.e(TAG, "timestamp formating failed! start time was: ${startTime.toString()}")
                    0f
                }

                Log.d(TAG, "startTime is: $timestamp")

                val args = mutableMapOf<String, Any?>(
                    "paginationOpts" to mapOf(
                        "cursor" to currentCursor,
                        "numItems" to 4f
                    ),
                    "startTime" to timestamp
                )

                val res = client.mutation<PaginationResult<Event>>(
                    "events:get",
                    args
                )

                _events.value += res.page
                currentCursor = res.continueCursor
                isLastPage = res.isDone

            } catch (e: Exception) {
                val msg = e.message ?: "msg no found"
                Log.e(TAG, "error msg: $msg")
            }
            isLoading = false
        }
    }

    companion object {
        const val TAG = "EventViewModel"
    }
}

