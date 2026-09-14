package com.example.mybudgettree

import android.graphics.BitmapFactory
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import java.time.format.DateTimeFormatter
import java.util.Locale

class TransactionAdapter : RecyclerView.Adapter<TransactionAdapter.Holder>() {
    private val items = mutableListOf<TransactionRow>()
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    private val dateFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)

    fun submit(rows: List<TransactionRow>) {
        items.clear()
        items.addAll(rows)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val image = view.findViewById<ImageView>(R.id.ivTransactionImage)
        private val name = view.findViewById<TextView>(R.id.tvTransactionName)
        private val detail = view.findViewById<TextView>(R.id.tvTransactionDetail)
        private val category = view.findViewById<TextView>(R.id.tvTransactionCategory)
        private val amount = view.findViewById<TextView>(R.id.tvTransactionAmount)

        fun bind(row: TransactionRow) {
            name.text = row.title
            detail.text = itemView.context.getString(
                R.string.transaction_detail,
                row.time.format(timeFormatter),
                dateFormatter.format(row.date)
            )
            category.text = row.categoryName
            amount.text = MoneyFormatter.formatSigned(row.amount, row.isIncome)
            val path = row.imagePath
            if (!path.isNullOrBlank()) {
                val bitmap = BitmapFactory.decodeFile(path)
                if (bitmap != null) {
                    image.setImageBitmap(bitmap)
                    image.clearColorFilter()
                    image.scaleType = ImageView.ScaleType.CENTER_CROP
                    image.setPadding(0, 0, 0, 0)
                } else {
                    showPlaceholder()
                }
            } else {
                showPlaceholder()
            }
        }

        private fun showPlaceholder() {
            image.setImageResource(R.drawable.ic_transaction_placeholder)
            image.scaleType = ImageView.ScaleType.CENTER_INSIDE
            val padding = (10 * itemView.resources.displayMetrics.density).toInt()
            image.setPadding(padding, padding, padding, padding)
        }
    }
}
