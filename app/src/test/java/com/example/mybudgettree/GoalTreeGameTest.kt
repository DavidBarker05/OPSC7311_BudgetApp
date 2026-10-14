package com.example.mybudgettree

import com.example.mybudgettree.database.entries.SavingsContribution
import com.example.mybudgettree.database.entries.SavingsGoal
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class GoalTreeGameTest {

    private val goal = SavingsGoal(
        id = "goal-1",
        goalName = "Wedding",
        targetAmount = 1000.0,
        treeLevel = 1
    )

    @Test
    fun emptyCan_hasNoWaterAndNoLastFilledDate() {
        val can = GoalTreeGame.canState(goal, emptyList())
        assertEquals(0, can.fillPercent)
        assertEquals(0.0, can.unpouredAmount, 0.0)
        assertNull(can.lastFilled)
        assertFalse(can.canWater)
    }

    @Test
    fun saving_fillsTheCanAndRecordsTheLastFilledDate() {
        val can = GoalTreeGame.canState(
            goal,
            listOf(deposit(amount = 250.0, on = LocalDate.of(2026, 10, 14), at = LocalDateTime.of(2026, 10, 14, 9, 0)))
        )
        assertEquals(25, can.fillPercent)
        assertEquals(250.0, can.unpouredAmount, 0.0)
        assertEquals(LocalDate.of(2026, 10, 14), can.lastFilled)
        assertTrue(can.canWater)
    }

    @Test
    fun lastFilled_usesTheMostRecentlyRecordedDeposit() {
        val can = GoalTreeGame.canState(
            goal,
            listOf(
                deposit(amount = 100.0, on = LocalDate.of(2026, 10, 1), at = LocalDateTime.of(2026, 10, 1, 8, 0)),
                deposit(amount = 50.0, on = LocalDate.of(2026, 9, 1), at = LocalDateTime.of(2026, 10, 14, 18, 30))
            )
        )
        assertEquals(LocalDate.of(2026, 9, 1), can.lastFilled)
        assertEquals(150.0, can.unpouredAmount, 0.0)
    }

    @Test
    fun watering_emptiesTheCanAndKeepsTheLastFilledDate() {
        val watered = goal.copy(lastWateredAt = LocalDateTime.of(2026, 10, 14, 12, 0).toString())
        val can = GoalTreeGame.canState(
            watered,
            listOf(deposit(amount = 250.0, on = LocalDate.of(2026, 10, 14), at = LocalDateTime.of(2026, 10, 14, 9, 0)))
        )
        assertEquals(0, can.fillPercent)
        assertFalse(can.canWater)
        assertEquals(LocalDate.of(2026, 10, 14), can.lastFilled)
    }

    @Test
    fun savingAfterWatering_refillsOnlyTheNewDeposit() {
        val watered = goal.copy(lastWateredAt = LocalDateTime.of(2026, 10, 14, 12, 0).toString())
        val can = GoalTreeGame.canState(
            watered,
            listOf(
                deposit(amount = 250.0, on = LocalDate.of(2026, 10, 14), at = LocalDateTime.of(2026, 10, 14, 9, 0)),
                deposit(amount = 100.0, on = LocalDate.of(2026, 10, 15), at = LocalDateTime.of(2026, 10, 15, 9, 0))
            )
        )
        assertEquals(10, can.fillPercent)
        assertEquals(100.0, can.unpouredAmount, 0.0)
        assertEquals(LocalDate.of(2026, 10, 15), can.lastFilled)
        assertTrue(can.canWater)
    }

    @Test
    fun goalWithoutATarget_showsAFullCanWhenAnyWaterIsWaiting() {
        val openGoal = goal.copy(targetAmount = null)
        val can = GoalTreeGame.canState(
            openGoal,
            listOf(deposit(amount = 20.0, on = LocalDate.of(2026, 10, 14), at = LocalDateTime.of(2026, 10, 14, 9, 0)))
        )
        assertEquals(100, can.fillPercent)
        assertTrue(can.canWater)
    }

    @Test
    fun tinyDeposit_stillShowsSomeWaterInTheCan() {
        val can = GoalTreeGame.canState(
            goal,
            listOf(deposit(amount = 1.0, on = LocalDate.of(2026, 10, 14), at = LocalDateTime.of(2026, 10, 14, 9, 0)))
        )
        assertEquals(1, can.fillPercent)
        assertTrue(can.canWater)
    }

    @Test
    fun stages_followTheTreeLevel() {
        assertEquals(TreeStage.SEED, GoalTreeGame.stageForLevel(1))
        assertEquals(TreeStage.SPROUT, GoalTreeGame.stageForLevel(2))
        assertEquals(TreeStage.SAPLING, GoalTreeGame.stageForLevel(3))
        assertEquals(TreeStage.YOUNG, GoalTreeGame.stageForLevel(4))
        assertEquals(TreeStage.MATURE, GoalTreeGame.stageForLevel(5))
        assertEquals(TreeStage.MATURE, GoalTreeGame.stageForLevel(9))
        assertEquals(2, GoalTreeGame.nextLevel(1))
    }

    @Test
    fun featuredGoal_isTheOneFilledMostRecently() {
        val older = snapshot("Car", filledAt = LocalDateTime.of(2026, 10, 1, 8, 0), level = 4)
        val newer = snapshot("Wedding", filledAt = LocalDateTime.of(2026, 10, 14, 8, 0), level = 1)
        assertEquals("Wedding", GoalTreeGame.featured(listOf(older, newer))?.goal?.goalName)
    }

    @Test
    fun featuredGoal_whenNothingHasBeenFilled_prefersTheTallerTree() {
        val seed = snapshot("Wedding", filledAt = null, level = 1)
        val sapling = snapshot("Car", filledAt = null, level = 3)
        assertEquals("Car", GoalTreeGame.featured(listOf(seed, sapling))?.goal?.goalName)
    }

    private fun deposit(amount: Double, on: LocalDate, at: LocalDateTime) = SavingsContribution(
        id = "c-$amount",
        goalId = goal.id,
        amount = amount,
        date = on.toString(),
        createdAt = at.toString()
    )

    private fun snapshot(name: String, filledAt: LocalDateTime?, level: Int): GoalCanSnapshot {
        val savingsGoal = SavingsGoal(id = name, goalName = name, targetAmount = 100.0, treeLevel = level)
        val can = if (filledAt == null) {
            GoalTreeGame.canState(savingsGoal, emptyList())
        } else {
            GoalTreeGame.canState(
                savingsGoal,
                listOf(
                    SavingsContribution(
                        id = name,
                        goalId = name,
                        amount = 10.0,
                        date = filledAt.toLocalDate().toString(),
                        createdAt = filledAt.toString()
                    )
                )
            )
        }
        return GoalCanSnapshot(savingsGoal, can)
    }
}
