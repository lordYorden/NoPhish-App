package dev.lordyorden.as_no_phish_detector.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import dev.lordyorden.as_no_phish_detector.R
import dev.lordyorden.as_no_phish_detector.databinding.BadgeActiveBinding
import dev.lordyorden.as_no_phish_detector.databinding.BadgePendingBinding
import dev.lordyorden.as_no_phish_detector.databinding.ItemCircleMemberBinding
import dev.lordyorden.as_no_phish_detector.models.CircleMember
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader

class CircleAdapter(
    private val members: List<CircleMember>,
    private val onMemberClick: (CircleMember) -> Unit
) : RecyclerView.Adapter<CircleAdapter.CircleViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CircleViewHolder {
        val binding = ItemCircleMemberBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        val holder = CircleViewHolder(binding)

        return holder
    }

    override fun getItemCount(): Int = members.size

    private fun getItem(position: Int) = members[position]

    override fun onBindViewHolder(holder: CircleViewHolder, position: Int) {
        with(holder.binding){
            with(getItem(position)){
                tvName.text = buildString {
                    append(name)
                }

                tvRole.text = buildString {
                    append("(")
                    append(familyRole)
                    append(")")
                }

                ImageLoader.getInstance().loadImage(avatarUrl ?: "", ivIcon, R.drawable.bg_circle_avatar_gray)

                frameBadgeStatus.removeAllViews()
                when(isConnected){
                    true -> {
                        root.alpha = 1f
                        tvStatus.text = root.resources.getString(R.string.watching_over)
                        val activeBadge = BadgeActiveBinding.inflate(LayoutInflater.from(root.context))
                        frameBadgeStatus.addView(activeBadge.root)
                    }
                    false -> {
                        root.alpha = 0.65f
                        tvStatus.text = root.resources.getString(R.string.invitation_sent)
                        val pendingBadge = BadgePendingBinding.inflate(LayoutInflater.from(root.context))
                        frameBadgeStatus.addView(pendingBadge.root)
                    }
                }
            }
        }
    }

    inner class CircleViewHolder(val binding: ItemCircleMemberBinding) : RecyclerView.ViewHolder(binding.root) {
        init {
            binding.root.setOnClickListener {
                onMemberClick(getItem(bindingAdapterPosition))
            }
        }
    }
}