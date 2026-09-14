package com.example.mybudgettree

import java.time.LocalDate
import java.time.LocalTime

data class TransactionRow(
    val title: String,
    val categoryName: String,
    val amount: Double,
    val isIncome: Boolean,
    val date: LocalDate,
    val time: LocalTime,
    val imagePath: String?
)
