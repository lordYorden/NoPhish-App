package dev.lordyorden.as_no_phish_detector.ui.events

import android.content.Context
import android.util.Log
import androidx.core.view.isVisible
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.EventPreviewAdapter
import dev.lordyorden.as_no_phish_detector.databinding.SectionRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.repositories.CircleMembersState

class CircleRecentActivityRenderer(
    private val binding: SectionRecentActivityBinding,
    private val adapter: EventPreviewAdapter,
    private val context: Context,
) {
    fun render(
        events: List<Event>,
        membersState: CircleMembersState,
    ) {
        if (events.isEmpty()) {
            renderMessage(context.getString(R.string.circle_events_empty))
            return
        }

        if (membersState.errorMessage != null) {
            renderError()
            return
        }

        if (!membersState.loaded) {
            adapter.submitList(emptyList())
            binding.rvEvents.isVisible = false
            binding.tvRecentEmpty.isVisible = false
            return
        }

        val previewItems = events.map { event ->
            val member = membersState.membersByUserId[event.userId]
            if (member == null) {
                Log.e(TAG, "Missing circle member for eventId=${event.eventId}")
                renderMessage(context.getString(R.string.missing_for_event))
                return
            }
            CircleEventUiItem(event, member)
        }

        adapter.submitList(previewItems)
        binding.rvEvents.isVisible = true
        binding.tvRecentEmpty.isVisible = false
    }

    fun renderError() {
        renderMessage(context.getString(R.string.circle_events_error))
    }

    private fun renderMessage(message: String) {
        adapter.submitList(emptyList())
        binding.rvEvents.isVisible = false
        binding.tvRecentEmpty.isVisible = true
        binding.tvRecentEmpty.text = message
    }

    companion object {
        private const val TAG = "CircleRecentActivityRenderer"
    }
}
