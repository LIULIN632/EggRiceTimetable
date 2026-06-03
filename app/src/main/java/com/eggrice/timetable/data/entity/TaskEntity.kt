package com.eggrice.timetable.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val completed: Boolean = false,
    val sortOrder: Int = 0,
    val schemeId: Long = 0
)
