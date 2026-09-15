package com.example.mybudgettree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.Category

data class CategoryTile(
    val category: Category?,
    val isMore: Boolean
)

class CategoryGardenAdapter(
    private val onCategory: (Category) -> Unit,
    private val onMore: () -> Unit
) : RecyclerView.Adapter<CategoryGardenAdapter.Holder>() {
    private val items = mutableListOf<CategoryTile>()

    fun submit(categories: List<Category>) {
        items.clear()
        items.addAll(categories.map { CategoryTile(it, isMore = false) })
        items += CategoryTile(category = null, isMore = true)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_category_tile, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val icon = view.findViewById<ImageView>(R.id.ivCategoryIcon)
        private val name = view.findViewById<TextView>(R.id.tvCategoryName)

        fun bind(tile: CategoryTile) {
            if (tile.isMore) {
                icon.setImageResource(R.drawable.ic_plus)
                name.text = itemView.context.getString(R.string.more)
                itemView.setOnClickListener { onMore() }
            } else {
                val category = tile.category ?: return
                icon.setImageResource(CategoryGarden.iconRes(category.categoryName))
                name.text = category.categoryName
                itemView.setOnClickListener { onCategory(category) }
            }
        }
    }
}
