package dev.lordyorden.as_no_phish_detector

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.databinding.NotificationItemBinding
import dev.lordyorden.as_no_phish_detector.models.Notification
import dev.lordyorden.tradely.utilities.ImageLoader


class NotificationAdapter(
    private val notifications: List<Notification>,
    private val onNotificationClick: (Notification) -> Unit
) : RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NotificationViewHolder {
        val binding = NotificationItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = NotificationViewHolder(binding)

        return holder
    }

    override fun getItemCount(): Int = notifications.size

    private fun getItem(position: Int) = notifications[position]

    override fun onBindViewHolder(holder: NotificationViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){
                lblBody.text = body
                lblTitle.text = title

                if (packageName != null && packageName != "none")
                    ImageLoader.getInstance().loadAppIcon(packageName!!, appIcon)
            }
        }
    }

    inner class NotificationViewHolder(val binding: NotificationItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onNotificationClick(getItem(adapterPosition))
            }
        }
    }
}
