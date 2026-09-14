package com.example.mybudgettree

sealed class TransactionListItem {
    data class Header(val title: String, val showCalendar: Boolean) : TransactionListItem()
    data class Entry(val row: TransactionRow) : TransactionListItem()
}
