package com.example.mybudgettree

import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * How far a goal's money tree has grown. Level 1 is a seed. From level 5 the picture stays a
 * mature tree while the level number can keep rising
 */
enum class TreeStage { SEED, SPROUT, SAPLING, YOUNG, MATURE }

/**
 * The water currently waiting in one goal's can
 *
 * @property fillPercent How full the can looks, from 0 to 100
 * @property unpouredAmount Savings added since the tree was last watered
 * @property lastFilled The savings date on the deposit that most recently filled the can, including water that has already been poured
 * @property lastFilledAt When that deposit was recorded, used to decide which goal was filled most recently
 * @property canWater True when there is savings waiting to be poured onto the tree
 */
data class WateringCanState(
    val fillPercent: Int,
    val unpouredAmount: Double,
    val lastFilled: LocalDate?,
    val lastFilledAt: LocalDateTime?,
    val canWater: Boolean
)

/**
 * One goal together with the water currently in its can, used to choose which tree to show
 */
data class GoalCanSnapshot(
    val goal: SavingsGoal,
    val can: WateringCanState
)

/**
 * Rules for the savings-goal tree. Saving money fills that goal's watering can. Pouring the can
 * grows the tree that represents the goal. The can remembers the date it was last filled even
 * after the water has been poured
 */
object GoalTreeGame {
    const val EMPTY_CAN_MESSAGE = "The watering can is empty"

    fun stageForLevel(level: Int): TreeStage = when {
        level <= 1 -> TreeStage.SEED
        level == 2 -> TreeStage.SPROUT
        level == 3 -> TreeStage.SAPLING
        level == 4 -> TreeStage.YOUNG
        else -> TreeStage.MATURE
    }

    fun nextLevel(currentLevel: Int): Int = currentLevel.coerceAtLeast(1) + 1

    /**
     * Describes the can for [goal] from its [contributions]
     *
     * Water still in the can is every deposit recorded after [SavingsGoal.lastWateredAt].
     * Deposits with no parseable timestamp still count as water, so a bad value cannot trap
     * the can in an empty state. A goal with no target shows a full can whenever any water is waiting
     */
    fun canState(goal: SavingsGoal, contributions: List<SavingsContribution>): WateringCanState {
        val lastWatered = goal.lastWateredAtAsLocalDateTime()
        val unpoured = contributions
            .filter { isUnpoured(it, lastWatered) }
            .sumOf { it.amount }
        val latestFill = contributions
            .mapNotNull { contribution ->
                val filledOn = contributionDate(contribution) ?: return@mapNotNull null
                val recordedAt = contributionCreatedAt(contribution) ?: LocalDateTime.MIN
                recordedAt to filledOn
            }
            .maxByOrNull { it.first }
        return WateringCanState(
            fillPercent = fillPercent(unpoured, goal.targetAmount),
            unpouredAmount = unpoured,
            lastFilled = latestFill?.second,
            lastFilledAt = latestFill?.first?.takeUnless { it == LocalDateTime.MIN },
            canWater = unpoured > 0.0
        )
    }

    /**
     * Picks the goal whose tree and can should be shown on the shared watering-can screen.
     * The goal filled most recently wins. A goal that has never been filled loses to one that has.
     * Ties prefer the taller tree, then the name, so the choice stays stable
     */
    fun featured(entries: List<GoalCanSnapshot>): GoalCanSnapshot? {
        if (entries.isEmpty()) return null
        return entries.maxWithOrNull(
            compareBy<GoalCanSnapshot> { it.can.lastFilledAt ?: LocalDateTime.MIN }
                .thenBy { it.goal.treeLevel }
                .thenBy { it.goal.goalName }
        )
    }

    private fun isUnpoured(contribution: SavingsContribution, lastWatered: LocalDateTime?): Boolean {
        if (lastWatered == null) return true
        val createdAt = contributionCreatedAt(contribution) ?: return true
        return createdAt.isAfter(lastWatered)
    }

    private fun fillPercent(unpoured: Double, target: Double?): Int {
        if (unpoured <= 0.0) return 0
        if (target == null || target <= 0.0) return 100
        return ((unpoured / target) * 100.0).toInt().coerceIn(1, 100)
    }

    private fun contributionDate(contribution: SavingsContribution): LocalDate? =
        runCatching { contribution.dateAsLocalDate() }.getOrNull()

    private fun contributionCreatedAt(contribution: SavingsContribution): LocalDateTime? =
        runCatching { contribution.createdAtAsLocalDateTime() }.getOrNull()
}
