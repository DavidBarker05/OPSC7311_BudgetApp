package com.example.mybudgettree.database.type_converters

import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import androidx.room.TypeConverter

class DateTimeConverter {
    @TypeConverter
    fun fromLocalDate(date: LocalDate?): String? = date?.toString()

    @TypeConverter
    fun toLocalDate(value: String?): LocalDate? = value?.let { LocalDate.parse(it) }

    @TypeConverter
    fun fromLocalTime(time: LocalTime?): String? = time?.toString()

    @TypeConverter
    fun toLocalTime(value: String?): LocalTime? = value?.let { LocalTime.parse(it) }

    @TypeConverter
    fun fromYearMonth(yearMonth: YearMonth?): String? = yearMonth?.toString()

    @TypeConverter
    fun toYearMonth(value: String?): YearMonth? = value?.let { YearMonth.parse(it) }
}