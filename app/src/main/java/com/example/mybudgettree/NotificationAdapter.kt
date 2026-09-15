package com.example.mybudgettree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale

class NotificationAdapter(
    private val onItemClick: (WalletNotification) -> Unit
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<NotificationListItem>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

    fun submit(rows: List<NotificationListItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is NotificationListItem.Header -> VIEW_TYPE_HEADER
        is NotificationListItem.Entry -> VIEW_TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_notification_header, parent, false))
        } else {
            EntryHolder(inflater.inflate(R.layout.item_notification, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is NotificationListItem.Header -> (holder as HeaderHolder).bind(item)
            is NotificationListItem.Entry -> (holder as EntryHolder).bind(item)
        }
    }

    override fun getItemCount(): Int = items.size

    private class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvNotificationGroup)

        fun bind(item: NotificationListItem.Header) {
            title.text = item.title
        }
    }

    private inner class EntryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.ivNotificationIcon)
        private val title = view.findViewById<TextView>(R.id.tvNotificationTitle)
        private val body = view.findViewById<TextView>(R.id.tvNotificationBody)
        private val highlight = view.findViewById<TextView>(R.id.tvNotificationHighlight)
        private val time = view.findViewById<TextView>(R.id.tvNotificationTime)
        private val divider = view.findViewById<View>(R.id.notificationDivider)

        fun bind(item: NotificationListItem.Entry) {
            val notification = item.notification
            icon.setImageResource(notification.iconRes)
            title.text = notification.title
            body.text = notification.body
            if (notification.highlight.isNullOrBlank()) {
                highlight.visibility = View.GONE
                highlight.text = ""
            } else {
                highlight.visibility = View.VISIBLE
                highlight.text = notification.highlight
            }
            time.text = itemView.context.getString(
                R.string.transaction_detail,
                notification.time.format(timeFormatter),
                dateFormatter.format(notification.date)
            )
            divider.visibility = if (item.showDivider) View.VISIBLE else View.INVISIBLE
            itemView.setOnClickListener { onItemClick(notification) }
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ENTRY = 1
    }
}
