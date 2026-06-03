package com.eggrice.timetable.util

import com.eggrice.timetable.data.entity.CourseEntity

object ColorUtils {
    // 15-color macaron palette matching the existing WebView app
    val colors = listOf(
        0xFFF0D5D8.toInt() to 0xFF885A60.toInt(), // pink
        0xFFD0DCF0.toInt() to 0xFF4A6088.toInt(), // blue
        0xFFC8DDD0.toInt() to 0xFF4A7058.toInt(), // green
        0xFFD8D4E8.toInt() to 0xFF5A5080.toInt(), // purple
        0xFFF0E0C8.toInt() to 0xFF886A48.toInt(), // orange
        0xFFECD0D0.toInt() to 0xFF885050.toInt(), // red
        0xFFC8E0E8.toInt() to 0xFF487088.toInt(), // cyan
        0xFFC0DCD4.toInt() to 0xFF487060.toInt(), // teal
        0xFFFFE8C8.toInt() to 0xFF886848.toInt(), // peach
        0xFFD8D8F0.toInt() to 0xFF505080.toInt(), // lavender
        0xFFFFD8D8.toInt() to 0xFF885050.toInt(), // salmon
        0xFFD0F0D0.toInt() to 0xFF407040.toInt(), // mint
        0xFFFFF0C8.toInt() to 0xFF887040.toInt(), // lemon
        0xFFE8D0F0.toInt() to 0xFF704880.toInt(), // lilac
        0xFFD0E8F0.toInt() to 0xFF406888.toInt()  // sky
    )

    fun bgColor(index: Int) = colors[index % colors.size].first
    fun textColor(index: Int) = colors[index % colors.size].second

    fun computeColorIndex(day: Int, slot: Int): Int = (day * 3 + slot) % colors.size
}

object DateUtils {
    fun currentWeekOfSemester(startDate: String): Int {
        // startDate format: "2026-02-23" (first Monday of semester)
        return try {
            val parts = startDate.split("-")
            val start = java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val now = java.time.LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, now)
            (days / 7).toInt() + 1
        } catch (e: Exception) { 1 }
    }

    fun weekdaysZh(): List<String> = listOf("一", "二", "三", "四", "五", "六", "日")

    fun computeWeekType(weeks: List<Int>): String {
        if (weeks.isEmpty()) return "all"
        val allOdd = weeks.all { it % 2 == 1 }
        val allEven = weeks.all { it % 2 == 0 }
        return when {
            allOdd -> "odd"
            allEven -> "even"
            else -> "all"
        }
    }
}
