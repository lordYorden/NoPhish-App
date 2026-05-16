package dev.lordyorden.as_no_phish_detector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.ItemRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventPreviewAdapter(
    private val onMemberClick: (Event) -> Unit
) : RecyclerView.Adapter<EventPreviewAdapter.EventViewHolder>() {

    private var events = emptyList<Event>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    fun submitList(nextEvents: List<Event>) {
        val diff = DiffUtil.calculateDiff(EventPreviewDiffCallback(events, nextEvents))
        events = nextEvents
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemRecentActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = EventViewHolder(binding)

        return holder
    }

    override fun getItemCount(): Int = events.size

    private fun getItem(position: Int) = events[position]

    private fun formatTimestamp(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){

                tvName.text = "name"
                tvAction.text = action
                tvTime.text = formatTimestamp(timestamp.toLong())

                packageName?.let {
                    ImageLoader.getInstance().loadAppIcon(it, ivIcon, R.drawable.ic_phone)
                } ?: run {
                    ivIcon.setImageResource(R.drawable.ic_phone)
                }
            }
        }
    }

    inner class EventViewHolder(val binding: ItemRecentActivityBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onMemberClick(getItem(bindingAdapterPosition))
            }
        }
    }
}

private class EventPreviewDiffCallback(
    private val oldEvents: List<Event>,
    private val newEvents: List<Event>,
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldEvents.size

    override fun getNewListSize() = newEvents.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldEvents[oldItemPosition].eventId == newEvents[newItemPosition].eventId
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldEvents[oldItemPosition] == newEvents[newItemPosition]
    }
}
