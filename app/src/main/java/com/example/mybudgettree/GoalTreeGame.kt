package com.example.mybudgettree

import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * How far the user's one money tree has grown. Level 1 is a seed. From level 5 the picture stays a
 * mature tree while the level number can keep rising
 */
enum class TreeStage { SEED, SPROUT, SAPLING, YOUNG, MATURE }

/**
 * The water currently waiting in one goal's can
 *
 * @property fillPercent How full the can looks, from 0 to 100
 * @property unpouredAmount Savings added since this can was last poured
 * @property lastFilled The savings date on the deposit that most recently added water, including water that has already been poured
 * @property lastFilledAt When that deposit was recorded, used to decide which can was filled most recently
 * @property canWater True when the can is full and can be poured onto the shared tree
 */
data class WateringCanState(
    val fillPercent: Int,
    val unpouredAmount: Double,
    val lastFilled: LocalDate?,
    val lastFilledAt: LocalDateTime?,
    val canWater: Boolean
)

/**
 * One goal together with the water currently in its can
 */
data class GoalCanSnapshot(
    val goal: SavingsGoal,
    val can: WateringCanState
)

/**
 * Rules for the one money tree and the can that belongs to each savings goal.
 * Saving toward a goal fills that goal's can. The can is full when the savings added since it was
 * last poured reach the goal's target. Pouring a full can grows the shared tree by one level.
 * The can remembers the date it was last filled even after the water has been poured
 */
object GoalTreeGame {
    const val EMPTY_CAN_MESSAGE = "The watering can is empty"
    const val NOT_FULL_MESSAGE = "The watering can is not full"

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
     * the can in an empty state. The can can water the shared tree once that water reaches the
     * goal's target. A goal with no target counts as full whenever any water is waiting
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
            canWater = isFilled(unpoured, goal.targetAmount)
        )
    }

    /**
     * Picks the can shown on the watering-can screen, which is the can that will be poured.
     * A full can wins over one that is still filling. Among those, the one filled most recently wins.
     * A goal that has never been filled loses to one that has. Remaining ties use the goal name,
     * so the choice stays stable
     */
    fun featured(entries: List<GoalCanSnapshot>): GoalCanSnapshot? {
        if (entries.isEmpty()) return null
        return entries.maxWithOrNull(
            compareBy<GoalCanSnapshot> { it.can.canWater }
                .thenBy { it.can.lastFilledAt ?: LocalDateTime.MIN }
                .thenBy { it.goal.goalName }
        )
    }

    private fun isUnpoured(contribution: SavingsContribution, lastWatered: LocalDateTime?): Boolean {
        if (lastWatered == null) return true
        val createdAt = contributionCreatedAt(contribution) ?: return true
        return createdAt.isAfter(lastWatered)
    }

    private fun isFilled(unpoured: Double, target: Double?): Boolean {
        if (unpoured <= 0.0) return false
        if (target == null || target <= 0.0) return true
        return unpoured >= target
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
