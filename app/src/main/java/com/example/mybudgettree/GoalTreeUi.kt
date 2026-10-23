package com.example.mybudgettree

import android.content.Context
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Labels for the one money tree and each goal's watering can
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

    fun stageLine(context: Context, level: Int): String =
        context.getString(
            R.string.tree_stage_line,
            stageName(context, level),
            level.coerceAtLeast(1)
        )

    fun lastFilledLine(context: Context, lastFilled: LocalDate?): String =
        if (lastFilled == null) {
            context.getString(R.string.last_filled_never)
        } else {
            context.getString(R.string.last_filled, filledDateFormatter.format(lastFilled))
        }

    fun canLabel(context: Context, can: WateringCanState?): String = when {
        can == null || can.unpouredAmount <= 0.0 -> context.getString(R.string.can_empty_short)
        can.canWater -> context.getString(R.string.can_full)
        else -> context.getString(R.string.can_fill_percent, can.fillPercent)
    }

    /**
     * Explains the next step for this goal's can and the one shared tree
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
        val filling = can.unpouredAmount > 0.0 && !can.canWater
        return when {
            mature && can.canWater -> context.getString(R.string.tree_progress_mature_ready, water, goalName, nextLevel)
            mature && filling -> context.getString(R.string.tree_progress_mature_filling, water, goalName, nextLevel)
            mature -> context.getString(R.string.tree_progress_mature)
            can.canWater -> context.getString(R.string.tree_progress_ready, water, goalName, currentName, nextName)
            filling -> context.getString(R.string.tree_progress_filling, water, goalName, currentName, nextName)
            can.lastFilled == null -> context.getString(R.string.tree_progress_save_first, goalName, currentName, nextName)
            else -> context.getString(R.string.tree_progress_refill, goalName, currentName, nextName)
        }
    }

    fun grewMessage(context: Context, newLevel: Int): String {
        val previousLevel = (newLevel - 1).coerceAtLeast(1)
        val stillMature = GoalTreeGame.stageForLevel(previousLevel) == TreeStage.MATURE &&
            GoalTreeGame.stageForLevel(newLevel) == TreeStage.MATURE
        return if (stillMature) {
            context.getString(R.string.tree_grew_level, newLevel)
        } else {
            context.getString(
                R.string.tree_grew,
                stageName(context, previousLevel),
                stageName(context, newLevel)
            )
        }
    }

    fun waterFailureMessage(context: Context, errMsg: String?): String {
        val err = errMsg.orEmpty()
        return when {
            err.contains(GoalTreeGame.NOT_FULL_MESSAGE, ignoreCase = true) ->
                context.getString(R.string.can_not_full)
            err.contains(GoalTreeGame.EMPTY_CAN_MESSAGE, ignoreCase = true) || err.isBlank() ->
                context.getString(R.string.can_empty)
            else -> err
        }
    }
}
