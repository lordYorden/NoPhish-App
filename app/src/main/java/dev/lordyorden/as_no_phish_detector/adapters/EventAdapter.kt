package dev.lordyorden.as_no_phish_detector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.ItemRecentActivityBinding
import dev.lordyorden.as_no_phish_detector.models.Event

class EventAdapter(
    private val members: List<Event>,
    private val onMemberClick: (Event) -> Unit
) : RecyclerView.Adapter<EventAdapter.EventViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = ItemRecentActivityBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = EventViewHolder(binding)

        return holder
    }

    override fun getItemCount(): Int = members.size

    private fun getItem(position: Int) = members[position]

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){

                //TODO format real data
                tvName.text = memberName
                tvAction.text = action
                tvTime.text = root.context.getString(R.string.timestemp_temp)
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