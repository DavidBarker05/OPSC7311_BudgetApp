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
}
