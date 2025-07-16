package dev.lordyorden.as_no_phish_detector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.databinding.SmsItemBinding
import dev.lordyorden.as_no_phish_detector.models.SmsMessage


class SmsAdapter(
    private val sms: List<SmsMessage>,
    private val onSmsClick: (SmsMessage) -> Unit
) : RecyclerView.Adapter<SmsAdapter.SmsViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SmsViewHolder {
        val binding = SmsItemBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = SmsViewHolder(binding)

        return holder
    }

    override fun getItemCount(): Int = sms.size

    private fun getItem(position: Int) = sms[position]

    override fun onBindViewHolder(holder: SmsViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){
                lblBody.text = body
                lblTitle.text = phone_number
            }
        }
    }

    inner class SmsViewHolder(val binding: SmsItemBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onSmsClick(getItem(adapterPosition))
            }
        }
    }
}
