package com.example.mybudgettree

import android.content.Context
import com.example.mybudgettree.database.entries.Category
import com.example.mybudgettree.database.entries.Expense
import com.example.mybudgettree.database.entries.Income
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.TemporalAdjusters

object NotificationFeed {
    private const val MAX_TRANSACTION_ITEMS = 15
    private const val RECENT_DAYS = 14L

    fun build(
        context: Context,
        categories: List<Category>,
        expenses: List<Expense>,
        incomes: List<Income>,
        today: LocalDate = LocalDate.now()
    ): List<NotificationListItem> {
        val categoryNames = categories.associate { it.id to it.categoryName }
        val cutoff = today.minusDays(RECENT_DAYS)
        val now = LocalTime.now().withSecond(0).withNano(0)
        val updateTime = if (now.toSecondOfDay() < 10 * 60) LocalTime.MIN else now.minusMinutes(10)

        val items = mutableListOf<WalletNotification>()
        items += WalletNotification(
            id = "reminder-today",
            type = WalletNotification.Type.REMINDER,
            title = context.getString(R.string.notification_reminder_title),
            body = context.getString(R.string.notification_reminder_body),
            date = today,
            time = now,
            iconRes = R.drawable.ic_notif_bell
        )
        items += WalletNotification(
            id = "update-today",
            type = WalletNotification.Type.UPDATE,
            title = context.getString(R.string.notification_update_title),
            body = context.getString(R.string.notification_update_body),
            date = today,
            time = updateTime,
            iconRes = R.drawable.ic_notif_star
        )

        val recentExpenses = expenses.filter { !it.date.isBefore(cutoff) }
        val recentIncomes = incomes.filter { !it.date.isBefore(cutoff) }
        val transactions = (
            recentIncomes.map { income ->
                transactionNotification(
                    context = context,
                    id = "tx-i-${income.id}",
                    categoryName = categoryNames[income.categoryId].orEmpty(),
                    description = income.description,
                    amount = income.amount,
                    isIncome = true,
                    date = income.date,
                    time = income.startTime
                )
            } + recentExpenses.map { expense ->
                transactionNotification(
                    context = context,
                    id = "tx-e-${expense.id}",
                    categoryName = categoryNames[expense.categoryId].orEmpty(),
                    description = expense.description,
                    amount = expense.amount,
                    isIncome = false,
                    date = expense.date,
                    time = expense.startTime
                )
            }
            ).sortedWith(compareByDescending<WalletNotification> { it.date }.thenByDescending { it.time })
            .take(MAX_TRANSACTION_ITEMS)
        items += transactions

        if (recentExpenses.isNotEmpty()) {
            val promptDate = budgetPromptDate(today)
            items += WalletNotification(
                id = "budget-prompt",
                type = WalletNotification.Type.BUDGET,
                title = context.getString(R.string.notification_expense_title),
                body = context.getString(R.string.notification_expense_body),
                date = promptDate,
                time = LocalTime.of(17, 0),
                iconRes = R.drawable.ic_notif_expense
            )
        }

        val grouped = items
            .distinctBy { it.id }
            .sortedWith(compareByDescending<WalletNotification> { it.date }.thenByDescending { it.time })
            .groupBy { groupFor(it.date, today) }
            .toSortedMap(compareBy { it.ordinal })

        val rows = mutableListOf<NotificationListItem>()
        grouped.forEach { (group, notifications) ->
            rows += NotificationListItem.Header(context.getString(group.titleRes))
            notifications.forEachIndexed { index, notification ->
                rows += NotificationListItem.Entry(
                    notification = notification,
                    showDivider = index != notifications.lastIndex
                )
            }
        }
        return rows
    }

    private fun transactionNotification(
        context: Context,
        id: String,
        categoryName: String,
        description: String,
        amount: Double,
        isIncome: Boolean,
        date: LocalDate,
        time: LocalTime
    ): WalletNotification {
        val signedAmount = MoneyFormatter.formatSigned(amount, isIncome)
        val highlight = when {
            categoryName.isBlank() && description.isBlank() -> signedAmount
            categoryName.isBlank() -> "$description | $signedAmount"
            description.isBlank() -> "$categoryName | $signedAmount"
            else -> context.getString(
                R.string.notification_transaction_highlight,
                categoryName,
                description,
                signedAmount
            )
        }
        return WalletNotification(
            id = id,
            type = WalletNotification.Type.TRANSACTION,
            title = context.getString(R.string.notification_transaction_title),
            body = context.getString(R.string.notification_transaction_body),
            highlight = highlight,
            date = date,
            time = time,
            iconRes = R.drawable.ic_notif_dollar
        )
    }

    private fun budgetPromptDate(today: LocalDate): LocalDate {
        val lastSaturday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)).let { saturday ->
            if (!saturday.isBefore(today)) saturday.minusWeeks(1) else saturday
        }
        return lastSaturday
    }

    private fun groupFor(date: LocalDate, today: LocalDate): Group {
        if (date == today) return Group.TODAY
        if (date == today.minusDays(1)) return Group.YESTERDAY
        val lastSaturday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.SATURDAY)).let { saturday ->
            if (!saturday.isBefore(today)) saturday.minusWeeks(1) else saturday
        }
        val lastSunday = lastSaturday.plusDays(1)
        return if (!date.isBefore(lastSaturday) && !date.isAfter(lastSunday)) {
            Group.THIS_WEEKEND
        } else {
            Group.EARLIER
        }
    }

    private enum class Group(val titleRes: Int) {
        TODAY(R.string.notification_group_today),
        YESTERDAY(R.string.notification_group_yesterday),
        THIS_WEEKEND(R.string.notification_group_weekend),
        EARLIER(R.string.notification_group_earlier)
    }
}
