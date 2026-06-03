package com.eggrice.timetable.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schemes")
data class SchemeEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val sortOrder: Int = 0
)
