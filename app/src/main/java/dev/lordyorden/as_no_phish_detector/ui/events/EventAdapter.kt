package dev.lordyorden.as_no_phish_detector.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.ItemAttackHistoryBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventAdapter(
    private val onMemberClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {
    private var items = listOf<Event>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    fun updateData(newList: List<Event>) {
        val oldSize = items.size
        val newItemsCount = newList.size - oldSize
        this.items = newList

        if (newItemsCount > 0) {
            // Efficiently notify only the appended items
            notifyItemRangeChanged(oldSize, newItemsCount)
        }
        else{
            notifyDataSetChanged()
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding =
            ItemAttackHistoryBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = EventViewHolder(binding)

        return holder
    }

    private fun getItem(position: Int) = items[position]

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        with(holder.binding) {
            with(getItem(position)) {
                tvName.text = action
                tvDate.text = formatTimestamp(timestamp.toLong())

                moreDetails?.let {
                    btnDetails.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                    btnDetails.isEnabled = true

                } ?: run {
                    btnDetails.isEnabled = false
                    btnDetails.setTextColor(ContextCompat.getColor(root.context, R.color.surface_text))
                }
            }
        }
    }

    private fun formatTimestamp(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    override fun getItemCount() = items.size

    inner class EventViewHolder(val binding: ItemAttackHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {
        init {
            binding.btnDetails.setOnClickListener {
                onMemberClick(getItem(bindingAdapterPosition))
            }
        }
    }
}
