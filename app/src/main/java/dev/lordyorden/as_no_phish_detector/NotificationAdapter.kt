package dev.lordyorden.as_no_phish_detector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.databinding.NotificationItemBinding
import dev.lordyorden.as_no_phish_detector.models.Notification

class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.RoomViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RoomViewHolder {
        val binding = NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return RoomViewHolder(binding)
    }

    override fun getItemCount(): Int = notifications.size

    private fun getItem(position: Int) = notifications[position]

    override fun onBindViewHolder(holder: RoomViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){
                lblBody.text = body
                lblTitle.text = title
            }
        }
    }

    inner class RoomViewHolder(val binding: NotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onNotificationClick(getItem(adapterPosition))
            }
        }
    }
}
