package dev.lordyorden.as_no_phish_detector.ui.events

import android.content.Context
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentAttackHistoryBinding
import dev.lordyorden.as_no_phish_detector.models.Event

class HistoryScreenRenderer(
    private val binding: FragmentAttackHistoryBinding,
    private val adapter: EventAdapter,
    private val context: Context,
) {
    private var previousEvents: List<Event> = emptyList()

    fun render(state: HistoryUiState) {
        val surfaceState = HistorySurfaceState.from(state)

        renderSurfaceState(surfaceState)
        renderRows(state)
        preserveAnchorIfNeeded(state)
        renderResultCount(state.events.size)

        previousEvents = state.events
    }

    private fun renderSurfaceState(surfaceState: HistorySurfaceState) {
        binding.layoutHistoryState.isVisible = surfaceState.showStateLayout
        binding.rvSearchResults.isVisible = !surfaceState.showStateLayout
        binding.progressHistory.isVisible = surfaceState == HistorySurfaceState.InitialLoading
        binding.btnHistoryRetry.isVisible = surfaceState == HistorySurfaceState.InitialError

        binding.tvHistoryState.text = when (surfaceState) {
            HistorySurfaceState.InitialLoading -> context.getString(R.string.history_loading)
            HistorySurfaceState.InitialError -> context.getString(R.string.history_error)
            HistorySurfaceState.Empty -> context.getString(R.string.history_empty)
            HistorySurfaceState.List -> ""
        }
    }

    private fun renderRows(state: HistoryUiState) {
        adapter.updateData(
            events = state.events,
            showAppendLoading = state.loading == HistoryLoading.Append && state.events.isNotEmpty(),
            showEnd = state.endReached &&
                state.events.isNotEmpty() &&
                state.loading == HistoryLoading.Idle &&
                state.errorMessage == null,
            showAppendError = state.errorMessage != null && state.events.isNotEmpty()
        )
    }

    private fun preserveAnchorIfNeeded(state: HistoryUiState) {
        val layoutManager = binding.rvSearchResults.layoutManager as LinearLayoutManager
        val anchor = currentAnchor(layoutManager) ?: return

        if (state.loading != HistoryLoading.Idle || anchor.position == 0) return

        val newPosition = state.events.indexOfFirst { it.eventId == anchor.eventId }
        if (newPosition < 0 || newPosition == anchor.position) return

        binding.rvSearchResults.post {
            layoutManager.scrollToPositionWithOffset(newPosition, anchor.top)
        }
    }

    private fun currentAnchor(layoutManager: LinearLayoutManager): ScrollAnchor? {
        val position = layoutManager.findFirstVisibleItemPosition()
        if (position < 0) return null

        val eventId = previousEvents.getOrNull(position)?.eventId ?: return null
        val top = binding.rvSearchResults
            .findViewHolderForAdapterPosition(position)
            ?.itemView
            ?.top ?: 0

        return ScrollAnchor(position, eventId, top)
    }

    private fun renderResultCount(count: Int) {
        binding.tvResultCount.text = buildString {
            append(count)
            append(if (count == 1) " result" else " results")
        }
    }
}

private enum class HistorySurfaceState {
    InitialLoading,
    InitialError,
    Empty,
    List;

    val showStateLayout: Boolean
        get() = this != List

    companion object {
        fun from(state: HistoryUiState): HistorySurfaceState {
            return when {
                state.loading == HistoryLoading.Initial && state.events.isEmpty() -> InitialLoading
                state.events.isEmpty() && state.errorMessage != null -> InitialError
                state.loading == HistoryLoading.Idle &&
                    state.events.isEmpty() &&
                    state.errorMessage == null -> Empty
                else -> List
            }
        }
    }
}

private data class ScrollAnchor(
    val position: Int,
    val eventId: String,
    val top: Int,
)
