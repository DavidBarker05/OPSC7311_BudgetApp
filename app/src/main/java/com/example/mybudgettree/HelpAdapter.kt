package com.example.mybudgettree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class HelpAdapter(
    private val onFaq: (FaqEntry) -> Unit,
    private val onContact: (ContactEntry) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<HelpListItem>()

    fun submit(rows: List<HelpListItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is HelpListItem.Faq -> VIEW_TYPE_FAQ
        is HelpListItem.Contact -> VIEW_TYPE_CONTACT
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_FAQ) {
            FaqHolder(inflater.inflate(R.layout.item_help_faq, parent, false))
        } else {
            ContactHolder(inflater.inflate(R.layout.item_help_contact, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is HelpListItem.Faq -> (holder as FaqHolder).bind(item)
            is HelpListItem.Contact -> (holder as ContactHolder).bind(item.entry)
        }
    }

    override fun getItemCount(): Int = items.size

    private inner class FaqHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val question = view.findViewById<TextView>(R.id.tvFaqQuestion)
        private val answer = view.findViewById<TextView>(R.id.tvFaqAnswer)
        private val chevron = view.findViewById<ImageView>(R.id.ivFaqChevron)

        fun bind(item: HelpListItem.Faq) {
            question.setText(item.entry.titleRes)
            answer.setText(item.entry.bodyRes)
            answer.visibility = if (item.expanded) View.VISIBLE else View.GONE
            chevron.rotation = if (item.expanded) 180f else 0f
            itemView.setOnClickListener { onFaq(item.entry) }
        }
    }

    private inner class ContactHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.ivContactIcon)
        private val name = view.findViewById<TextView>(R.id.tvContactName)

        fun bind(entry: ContactEntry) {
            icon.setImageResource(entry.iconRes)
            name.setText(entry.titleRes)
            itemView.setOnClickListener { onContact(entry) }
        }
    }

    private companion object {
        const val VIEW_TYPE_FAQ = 0
        const val VIEW_TYPE_CONTACT = 1
    }
}
