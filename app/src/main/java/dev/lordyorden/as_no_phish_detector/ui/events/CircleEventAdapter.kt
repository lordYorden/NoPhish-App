package dev.lordyorden.as_no_phish_detector.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.ItemCircleEventBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class CircleEventAdapter(
    private val onDetailsClick: (Event) -> Unit,
) : RecyclerView.Adapter<CircleEventAdapter.CircleEventViewHolder>() {

    private var events = emptyList<Event>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    fun submitList(nextEvents: List<Event>) {
        val diff = DiffUtil.calculateDiff(EventDiffCallback(events, nextEvents))
        events = nextEvents
        diff.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleEventViewHolder {
        val binding = ItemCircleEventBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return CircleEventViewHolder(binding)
    }

    override fun getItemCount() = events.size

    override fun onBindViewHolder(holder: CircleEventViewHolder, position: Int) {
        holder.bind(events[position])
    }

    private fun getEvent(position: Int): Event? = events.getOrNull(position)

    private fun formatTimestamp(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    inner class CircleEventViewHolder(private val binding: ItemCircleEventBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDetails.setOnClickListener {
                getEvent(bindingAdapterPosition)?.let(onDetailsClick)
            }
        }

        fun bind(event: Event) {
            val context = binding.root.context
            binding.tvTitle.text = event.action
            binding.tvDesc.text = "description here"
            binding.tvTime.text = formatTimestamp(event.timestamp.toLong())

            event.packageName?.let {
                ImageLoader.getInstance().loadAppIcon(it, binding.ivAppSource, R.drawable.ic_phone)
            } ?: run {
                binding.ivAppSource.setImageResource(R.drawable.ic_phone)
            }
        }
    }
}

private class EventDiffCallback(
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
