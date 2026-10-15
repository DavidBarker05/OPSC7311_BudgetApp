package com.example.mybudgettree

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Labels for the money tree and the watering can's last-filled date
 */
object GoalTreeUi {
    private val filledDateFormatter = DateTimeFormatter.ofPattern("d MMM yyyy", Locale.ENGLISH)

    fun stageName(context: Context, level: Int): String {
        val nameRes = when (GoalTreeGame.stageForLevel(level)) {
            TreeStage.SEED -> R.string.tree_stage_seed
            TreeStage.SPROUT -> R.string.tree_stage_sprout
            TreeStage.SAPLING -> R.string.tree_stage_sapling
            TreeStage.YOUNG -> R.string.tree_stage_young
            TreeStage.MATURE -> R.string.tree_stage_mature
        }
        return context.getString(nameRes)
    }

    fun stageLine(context: Context, goalName: String, level: Int): String =
        context.getString(
            R.string.tree_for_goal,
            goalName,
            stageName(context, level),
            level.coerceAtLeast(1)
        )

    fun lastFilledLine(context: Context, lastFilled: LocalDate?): String =
        if (lastFilled == null) {
            context.getString(R.string.last_filled_never)
        } else {
            context.getString(R.string.last_filled, filledDateFormatter.format(lastFilled))
        }

    /**
     * Explains the next growth step for this goal: save to fill the can, then water to move one stage
     */
    fun progressMessage(context: Context, goalName: String?, level: Int, can: WateringCanState?): String {
        if (goalName == null || can == null) return context.getString(R.string.tree_needs_goal)
        val safeLevel = level.coerceAtLeast(1)
        val nextLevel = GoalTreeGame.nextLevel(safeLevel)
        val currentName = stageName(context, safeLevel)
        val nextName = stageName(context, nextLevel)
        val mature = GoalTreeGame.stageForLevel(safeLevel) == TreeStage.MATURE &&
            GoalTreeGame.stageForLevel(nextLevel) == TreeStage.MATURE
        val water = MoneyFormatter.format(can.unpouredAmount)
        return when {
            mature && can.canWater -> context.getString(R.string.tree_progress_mature_ready, water, goalName, nextLevel)
            mature -> context.getString(R.string.tree_progress_mature, goalName)
            can.canWater -> context.getString(R.string.tree_progress_ready, water, goalName, currentName, nextName)
            can.lastFilled == null -> context.getString(R.string.tree_progress_save_first, goalName, currentName, nextName)
            else -> context.getString(R.string.tree_progress_refill, goalName, currentName, nextName)
        }
    }

    fun grewMessage(context: Context, goalName: String, newLevel: Int): String {
        val previousLevel = (newLevel - 1).coerceAtLeast(1)
        val stillMature = GoalTreeGame.stageForLevel(previousLevel) == TreeStage.MATURE &&
            GoalTreeGame.stageForLevel(newLevel) == TreeStage.MATURE
        return if (stillMature) {
            context.getString(R.string.tree_grew_level, goalName, newLevel)
        } else {
            context.getString(
                R.string.tree_grew,
                goalName,
                stageName(context, previousLevel),
                stageName(context, newLevel)
            )
        }
    }
}
