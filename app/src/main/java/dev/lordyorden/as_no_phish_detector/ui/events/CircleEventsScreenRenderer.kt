package dev.lordyorden.as_no_phish_detector.ui.events

import android.content.Context
import androidx.core.view.isVisible
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.FragmentCircleEventsBinding

class CircleEventsScreenRenderer(
    private val binding: FragmentCircleEventsBinding,
    private val adapter: CircleEventAdapter,
    private val context: Context,
) {
    fun render(state: CircleEventsUiState) {
        val surfaceState = CircleEventsSurfaceState.from(state)

        binding.layoutCircleEventsState.isVisible = surfaceState.showStateLayout
        binding.rvCircleEvents.isVisible = !surfaceState.showStateLayout
        binding.progressCircleEvents.isVisible = surfaceState == CircleEventsSurfaceState.InitialLoading
        binding.btnCircleEventsRetry.isVisible = surfaceState is CircleEventsSurfaceState.InitialError
        binding.tvCircleEventsState.text = stateText(surfaceState)
        binding.tvResultCount.text = context.resources.getQuantityString(
            R.plurals.history_results_plural,
            state.events.size,
            state.events.size
        )

        renderAlertScope(state.alertScope)
        adapter.submitList(state.events)
    }

    fun renderContractError() {
        binding.layoutCircleEventsState.isVisible = true
        binding.rvCircleEvents.isVisible = false
        binding.progressCircleEvents.isVisible = false
        binding.btnCircleEventsRetry.isVisible = false
        binding.tvCircleEventsState.text = context.getString(R.string.circle_event_contract_error)
        binding.tvResultCount.text = context.getString(R.string.zero_result)
    }

    private fun stateText(surfaceState: CircleEventsSurfaceState): String {
        return when (surfaceState) {
            CircleEventsSurfaceState.InitialLoading -> context.getString(R.string.circle_events_loading)
            is CircleEventsSurfaceState.InitialError -> {
                val errorMessage = surfaceState.errorMessage
                if (errorMessage?.startsWith("Missing circle member") == true) {
                    context.getString(R.string.missing_for_event)
                } else {
                    context.getString(R.string.circle_events_error)
                }
            }
            CircleEventsSurfaceState.Empty -> context.getString(R.string.circle_events_empty)
            CircleEventsSurfaceState.List -> ""
        }
    }

    private fun renderAlertScope(scope: CircleAlertScope) {
        val checkedId = when (scope) {
            CircleAlertScope.ActionRequired -> R.id.btn_action_required
            CircleAlertScope.AllAlerts -> R.id.btn_all_alerts
        }
        if (binding.toggleAlertScope.checkedButtonId != checkedId) {
            binding.toggleAlertScope.check(checkedId)
        }
    }
}

private sealed class CircleEventsSurfaceState {
    data object InitialLoading : CircleEventsSurfaceState()
    data class InitialError(val error: String) : CircleEventsSurfaceState()
    data object Empty : CircleEventsSurfaceState()
    data object List : CircleEventsSurfaceState()

    val showStateLayout: Boolean
        get() = this != List

    val errorMessage: String?
        get() = (this as? InitialError)?.error

    companion object {
        fun from(state: CircleEventsUiState): CircleEventsSurfaceState {
            return when {
                state.loading == HistoryLoading.Initial && state.events.isEmpty() -> InitialLoading
                state.events.isEmpty() && state.errorMessage != null -> InitialError(state.errorMessage)
                state.loading == HistoryLoading.Idle &&
                    state.events.isEmpty() &&
                    state.errorMessage == null -> Empty
                else -> List
            }
        }
    }
}
