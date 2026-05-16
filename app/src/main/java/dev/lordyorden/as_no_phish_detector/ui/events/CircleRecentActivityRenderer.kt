package dev.lordyorden.as_no_phish_detector.ui.events

import android.content.Context
import android.util.Log
import androidx.core.view.isVisible
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.adapters.EventPreviewAdapter
import dev.lordyorden.as_no_phish_detector.databinding.SectionRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.models.Event

class CircleRecentActivityRenderer(
    private val binding: SectionRecentActivityBinding,
    private val adapter: EventPreviewAdapter,
    private val context: Context,
) {
    fun render(
        events: List<Event>,
        members: List<CircleMember>,
        membersLoaded: Boolean,
    ) {
        if (events.isEmpty()) {
            renderMessage(context.getString(R.string.circle_events_empty))
            return
        }

        if (!membersLoaded) {
            adapter.submitList(emptyList())
            binding.rvEvents.isVisible = false
            binding.tvRecentEmpty.isVisible = false
            return
        }

        val memberNamesById = members.associate { it.userId to it.name }
        val previewItems = events.map { event ->
            val memberName = memberNamesById[event.userId]
            if (memberName.isNullOrBlank()) {
                Log.e(TAG, "Missing circle member for eventId=${event.eventId}, userId=${event.userId}")
                renderMessage(context.getString(R.string.missing_for_event))
                return
            }
            CircleEventUiItem(event, memberName)
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
