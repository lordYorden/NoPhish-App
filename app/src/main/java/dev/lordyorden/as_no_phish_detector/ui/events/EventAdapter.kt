package dev.lordyorden.as_no_phish_detector.ui.events

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.ItemAttackHistoryBinding
import dev.lordyorden.as_no_phish_detector.databinding.ItemHistoryStatusBinding
import dev.lordyorden.as_no_phish_detector.models.Event
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

class EventAdapter(
    private val onMemberClick: (Event) -> Unit,
    private val onRetryClick: () -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private var rows = listOf<HistoryRow>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.US)

    val eventCount: Int
        get() = rows.count { it is HistoryRow.EventItem }

    fun updateData(
        events: List<Event>,
        showAppendLoading: Boolean,
        showEnd: Boolean,
        showAppendError: Boolean,
    ) {
        val nextRows = buildList {
            events.forEach { event -> add(HistoryRow.EventItem(event)) }
            when {
                showAppendLoading -> add(HistoryRow.StatusItem(HistoryStatus.Loading))
                showAppendError -> add(HistoryRow.StatusItem(HistoryStatus.Error))
                showEnd -> add(HistoryRow.StatusItem(HistoryStatus.End))
            }
        }

        val diff = DiffUtil.calculateDiff(HistoryDiffCallback(rows, nextRows))
        rows = nextRows
        diff.dispatchUpdatesTo(this)
    }

    override fun getItemViewType(position: Int): Int {
        return when (rows[position]) {
            is HistoryRow.EventItem -> VIEW_TYPE_EVENT
            is HistoryRow.StatusItem -> VIEW_TYPE_STATUS
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            VIEW_TYPE_EVENT -> {
                val binding = ItemAttackHistoryBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                EventViewHolder(binding)
            }

            VIEW_TYPE_STATUS -> {
                val binding = ItemHistoryStatusBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                StatusViewHolder(binding)
            }

            else -> throw IllegalArgumentException("Unsupported history row view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val row = rows[position]) {
            is HistoryRow.EventItem -> (holder as EventViewHolder).bind(row.event)
            is HistoryRow.StatusItem -> (holder as StatusViewHolder).bind(row.status)
        }
    }

    override fun getItemCount() = rows.size

    private fun getEvent(position: Int): Event? {
        return (rows.getOrNull(position) as? HistoryRow.EventItem)?.event
    }

    private fun formatTimestamp(timestampMillis: Long): String {
        return Instant.ofEpochMilli(timestampMillis)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    inner class EventViewHolder(private val binding: ItemAttackHistoryBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnDetails.setOnClickListener {
                getEvent(bindingAdapterPosition)?.let(onMemberClick)
            }
        }

        fun bind(event: Event) {
            with(binding) {
                tvName.text = event.action
                tvDate.text = formatTimestamp(event.timestamp.toLong())

                event.packageName?.let {
                    ImageLoader.getInstance().loadAppIcon(it, ivAppSource,  R.drawable.ic_phone)
                } ?: run {
                    ivAppSource.setImageResource(R.drawable.ic_phone)
                }

                btnDetails.setTextColor(ContextCompat.getColor(root.context, R.color.primary))
                btnDetails.isEnabled = true
            }
        }
    }

    private inner class StatusViewHolder(private val binding: ItemHistoryStatusBinding) :
        RecyclerView.ViewHolder(binding.root) {

        init {
            binding.btnRetry.setOnClickListener {
                onRetryClick()
            }
        }

        fun bind(status: HistoryStatus) {
            val context = binding.root.context
            binding.loading.isVisible = status == HistoryStatus.Loading
            binding.btnRetry.isVisible = status == HistoryStatus.Error
            binding.tvStatus.text = when (status) {
                HistoryStatus.Loading -> context.getString(R.string.history_loading_more)
                HistoryStatus.End -> context.getString(R.string.history_end)
                HistoryStatus.Error -> context.getString(R.string.history_error)
            }
        }
    }

    companion object {
        private const val VIEW_TYPE_EVENT = 1
        private const val VIEW_TYPE_STATUS = 2
    }
}

private sealed class HistoryRow {
    data class EventItem(val event: Event) : HistoryRow()
    data class StatusItem(val status: HistoryStatus) : HistoryRow()
}

private enum class HistoryStatus {
    Loading,
    End,
    Error,
}

private class HistoryDiffCallback(
    private val oldRows: List<HistoryRow>,
    private val newRows: List<HistoryRow>,
) : DiffUtil.Callback() {
    override fun getOldListSize() = oldRows.size

    override fun getNewListSize() = newRows.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldRows[oldItemPosition]
        val new = newRows[newItemPosition]

        return when {
            old is HistoryRow.EventItem && new is HistoryRow.EventItem -> {
                old.event.eventId == new.event.eventId
            }

            old is HistoryRow.StatusItem && new is HistoryRow.StatusItem -> {
                old.status == new.status
            }

            else -> false
        }
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldRows[oldItemPosition] == newRows[newItemPosition]
    }
}
