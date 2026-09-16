package com.example.mybudgettree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.SavingsContribution
import java.time.format.DateTimeFormatter
import java.util.Locale

class SavingsContributionAdapter(
    private val onRemove: (SavingsContribution) -> Unit
) : RecyclerView.Adapter<SavingsContributionAdapter.Holder>() {
    private val items = mutableListOf<SavingsContribution>()
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d, yyyy", Locale.ENGLISH)

    fun submit(contributions: List<SavingsContribution>) {
        items.clear()
        items.addAll(contributions)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_savings_contribution, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val date = view.findViewById<TextView>(R.id.tvContributionDate)
        private val amount = view.findViewById<TextView>(R.id.tvContributionAmount)
        private val remove = view.findViewById<ImageButton>(R.id.btnRemoveContribution)

        fun bind(contribution: SavingsContribution) {
            date.text = dateFormatter.format(contribution.date)
            amount.text = MoneyFormatter.format(contribution.amount)
            remove.setOnClickListener { onRemove(contribution) }
        }
    }
}
