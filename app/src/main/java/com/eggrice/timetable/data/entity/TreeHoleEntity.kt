package com.eggrice.timetable.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tree_holes")
data class TreeHoleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val createdAt: Long = System.currentTimeMillis(),
    val schemeId: Long = 0
)
