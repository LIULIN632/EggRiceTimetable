package com.eggrice.timetable.data.entity

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.PrimaryKey

@Immutable
@Entity(tableName = "courses")
data class CourseEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val credits: String = "",
    val teacher: String = "",
    val room: String = "",
    val dayOfWeek: Int = 1,       // 1-7 (Mon-Sun)
    val startSlot: Int = 1,       // 1-12
    val endSlot: Int = 2,         // 1-12
    val weekType: String = "all", // all, odd, even
    val weeks: String = "",       // comma-separated week numbers, empty = all
    val colorIndex: Int = 0,      // 0-14 palette index
    val schemeId: Long = 0        // 0=default scheme
)

@Immutable
@Entity(tableName = "time_slots")
data class TimeSlotEntity(
    @PrimaryKey val slot: Int,    // 1-12
    val startTime: String,        // "08:00"
    val endTime: String           // "08:45"
)
