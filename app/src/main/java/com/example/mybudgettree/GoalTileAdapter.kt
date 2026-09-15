package com.example.mybudgettree

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.Category

class GoalTileAdapter(
    private val onGoal: (Category) -> Unit
) : RecyclerView.Adapter<GoalTileAdapter.Holder>() {
    private val items = mutableListOf<Category>()

    fun submit(goals: List<Category>) {
        items.clear()
        items.addAll(goals)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_tile, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position], position == 0)
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val background = view.findViewById<FrameLayout>(R.id.goalTileBg)
        private val icon = view.findViewById<ImageView>(R.id.ivGoalIcon)
        private val name = view.findViewById<TextView>(R.id.tvGoalName)

        fun bind(goal: Category, highlighted: Boolean) {
            name.text = goal.categoryName
            icon.setImageResource(CategoryGoals.iconRes(goal.categoryName))
            if (highlighted) {
                background.setBackgroundResource(R.drawable.bg_goal_tile_dark)
                icon.setColorFilter(Color.WHITE)
            } else {
                background.setBackgroundResource(R.drawable.bg_category_tile)
                icon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.green_text))
            }
            itemView.setOnClickListener { onGoal(goal) }
        }
    }
}
