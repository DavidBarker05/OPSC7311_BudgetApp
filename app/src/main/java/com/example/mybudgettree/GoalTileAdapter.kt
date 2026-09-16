package com.example.mybudgettree

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.mybudgettree.database.entries.SavingsGoal

data class GoalTile(
    val goal: SavingsGoal?,
    val isMore: Boolean
)

class GoalTileAdapter(
    private val onGoal: (SavingsGoal) -> Unit,
    private val onMore: () -> Unit
) : RecyclerView.Adapter<GoalTileAdapter.Holder>() {
    private val items = mutableListOf<GoalTile>()

    fun submit(goals: List<SavingsGoal>) {
        items.clear()
        items.addAll(goals.map { GoalTile(it, isMore = false) })
        items += GoalTile(goal = null, isMore = true)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_goal_tile, parent, false)
        return Holder(view)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
        private val background = view.findViewById<FrameLayout>(R.id.goalTileBg)
        private val icon = view.findViewById<ImageView>(R.id.ivGoalIcon)
        private val name = view.findViewById<TextView>(R.id.tvGoalName)

        fun bind(tile: GoalTile) {
            if (tile.isMore) {
                background.setBackgroundResource(R.drawable.bg_category_tile)
                icon.setImageResource(R.drawable.ic_plus)
                icon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.green_text))
                name.text = itemView.context.getString(R.string.more)
                itemView.setOnClickListener { onMore() }
            } else {
                val goal = tile.goal ?: return
                name.text = goal.goalName
                icon.setImageResource(IconCatalog.resFor(goal.iconKey))
                background.setBackgroundResource(R.drawable.bg_category_tile)
                icon.setColorFilter(ContextCompat.getColor(itemView.context, R.color.green_text))
                itemView.setOnClickListener { onGoal(goal) }
            }
        }
    }
}
