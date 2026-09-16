package com.example.mybudgettree

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale

class TransactionHistoryAdapter(
    private val onCalendarClick: () -> Unit,
    private val entryLayout: Int = R.layout.item_transaction,
    private val onEntryClick: ((TransactionRow) -> Unit)? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val items = mutableListOf<TransactionListItem>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

    fun submit(rows: List<TransactionListItem>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int = when (items[position]) {
        is TransactionListItem.Header -> VIEW_TYPE_HEADER
        is TransactionListItem.Entry -> VIEW_TYPE_ENTRY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return if (viewType == VIEW_TYPE_HEADER) {
            HeaderHolder(inflater.inflate(R.layout.item_transaction_month, parent, false))
        } else {
            EntryHolder(inflater.inflate(entryLayout, parent, false))
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = items[position]) {
            is TransactionListItem.Header -> (holder as HeaderHolder).bind(item)
            is TransactionListItem.Entry -> (holder as EntryHolder).bind(item.row)
        }
    }

    override fun getItemCount(): Int = items.size

    private inner class HeaderHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val title = view.findViewById<TextView>(R.id.tvTransactionMonth)
        private val calendar = view.findViewById<ImageButton>(R.id.btnMonthCalendar)

        fun bind(item: TransactionListItem.Header) {
            title.text = item.title
            calendar.visibility = if (item.showCalendar) View.VISIBLE else View.GONE
            calendar.setOnClickListener { onCalendarClick() }
        }
    }

    private inner class EntryHolder(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.ivTransactionImage)
        private val name = view.findViewById<TextView>(R.id.tvTransactionName)
        private val detail = view.findViewById<TextView>(R.id.tvTransactionDetail)
        private val category = view.findViewById<TextView?>(R.id.tvTransactionCategory)
        private val amount = view.findViewById<TextView>(R.id.tvTransactionAmount)

        fun bind(row: TransactionRow) {
            itemView.setOnClickListener { onEntryClick?.invoke(row) }
            name.text = row.title
            detail.text = itemView.context.getString(
                R.string.transaction_detail,
                row.time.format(timeFormatter),
                dateFormatter.format(row.date)
            )
            category?.text = row.categoryName
            amount.text = MoneyFormatter.formatSigned(row.amount, row.isIncome)
            val path = row.imagePath
            if (!path.isNullOrBlank()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.clearColorFilter()
                    image.imageTintList = null
                    image.scaleType = ImageView.ScaleType.CENTER_CROP
                    image.setPadding(0, 0, 0, 0)
                } else {
                    showPlaceholder(row)
                }
            } else {
                showPlaceholder(row)
            }
        }

        private fun showPlaceholder(row: TransactionRow) {
            image.setImageResource(row.iconRes ?: R.drawable.ic_transaction_placeholder)
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = (10 * itemView.resources.displayMetrics.density).toInt()
            image.setPadding(padding, padding, padding, padding)
            image.setColorFilter(ContextCompat.getColor(itemView.context, R.color.green_text))
        }
    }

    private companion object {
        const val VIEW_TYPE_HEADER = 0
        const val VIEW_TYPE_ENTRY = 1
    }
}
