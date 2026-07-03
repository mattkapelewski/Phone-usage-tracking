package com.phoneusage.tracker.ui

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.phoneusage.tracker.data.AppUsageItem
import com.phoneusage.tracker.databinding.ItemAppBinding
import com.phoneusage.tracker.util.formatDuration

class AppSelectionAdapter(
    private val onToggle: (item: AppUsageItem, included: Boolean) -> Unit
) : RecyclerView.Adapter<AppSelectionAdapter.ViewHolder>() {

    private val items = mutableListOf<AppUsageItem>()

    fun submitList(newItems: List<AppUsageItem>) {
        items.clear()
        items.addAll(newItems)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemAppBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class ViewHolder(private val binding: ItemAppBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(item: AppUsageItem) {
            binding.appIcon.setImageDrawable(item.app.icon)
            binding.appLabel.text = item.app.label
            binding.appUsage.text = formatDuration(item.usageMillisToday)
            binding.appIncludedCheckbox.isChecked = item.isIncluded

            binding.root.setOnClickListener {
                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnClickListener
                val current = items[position]
                val newIncluded = !current.isIncluded
                items[position] = current.copy(isIncluded = newIncluded)
                binding.appIncludedCheckbox.isChecked = newIncluded
                onToggle(items[position], newIncluded)
            }
        }
    }
}
