package com.example.mybudgettree

import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class DashboardCategoryAdapter(
    private val onCategory: (CategorySpending) -> Unit
) : RecyclerView.Adapter<DashboardCategoryAdapter.Holder>() {
    private var items: List<CategorySpending> = emptyList()
    private var selectedId: String? = null

    fun submit(items: List<CategorySpending>, selectedId: String?) {
        this.items = items
        this.selectedId = selectedId
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_dashboard_category, parent, false))

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val row = view as LinearLayout
        private val icon = view.findViewById<ImageView>(R.id.ivDashboardIcon)
        private val name = view.findViewById<TextView>(R.id.tvDashboardName)
        private val status = view.findViewById<TextView>(R.id.tvDashboardStatus)
        private val amount = view.findViewById<TextView>(R.id.tvDashboardAmount)

        fun bind(item: CategorySpending) {
            val context = itemView.context
            val isSelected = item.category.id == selectedId
            val background = context.getColor(
                when (item.level) {
                    BudgetLevel.DANGER -> R.color.destructive_red
                    BudgetLevel.WARNING -> R.color.warning_orange
                    BudgetLevel.GOOD, BudgetLevel.NONE -> R.color.category_tile
                }
            )
            val textColor = BudgetStatusHelper.contrastingTextColor(background)

            row.background = GradientDrawable().apply {
                cornerRadius = 18f * context.resources.displayMetrics.density
                setColor(background)
                if (isSelected) setStroke((3 * context.resources.displayMetrics.density).toInt(), context.getColor(R.color.green_text))
            }
            icon.setImageResource(CategoryGarden.iconRes(item.category))
            icon.setColorFilter(textColor)
            name.text = item.category.categoryName
            name.setTextColor(textColor)
            amount.text = MoneyFormatter.format(item.total)
            amount.setTextColor(textColor)
            status.setTextColor(textColor)
            status.text = when (item.level) {
                BudgetLevel.DANGER -> context.getString(R.string.dashboard_over_max, MoneyFormatter.format(item.overBy ?: 0.0))
                BudgetLevel.WARNING -> context.getString(R.string.dashboard_over_min, MoneyFormatter.format(item.overBy ?: 0.0))
                else -> context.getString(R.string.dashboard_within_limits)
            }
            itemView.contentDescription = "${item.category.categoryName}, ${amount.text}, ${status.text}" +
                if (isSelected) ", ${context.getString(R.string.dashboard_selected)}" else ""
            itemView.setOnClickListener { onCategory(item) }
        }
    }
}
