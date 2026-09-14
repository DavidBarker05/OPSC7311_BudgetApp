package com.example.mybudgettree

import java.time.LocalDate
import java.time.LocalTime

data class WalletNotification(
    val id: String,
    val type: Type,
    val title: String,
    val body: String,
    val highlight: String? = null,
    val date: LocalDate,
    val time: LocalTime,
    val iconRes: Int
) {
    enum class Type { REMINDER, UPDATE, TRANSACTION, BUDGET }
}

sealed class NotificationListItem {
    data class Header(val title: String) : NotificationListItem()
    data class Entry(
        val notification: WalletNotification,
        val showDivider: Boolean
    ) : NotificationListItem()
}
