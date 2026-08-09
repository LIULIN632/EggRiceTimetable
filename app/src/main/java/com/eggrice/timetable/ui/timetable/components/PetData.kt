package com.eggrice.timetable.ui.timetable.components

import com.eggrice.timetable.R

data class PetInfo(val emoji: String, val defaultName: String)

val PetList = listOf(
    PetInfo("🐱", "小咪"),
    PetInfo("🐶", "旺财"),
    PetInfo("🐰", "跳跳"),
    PetInfo("🐼", "滚滚"),
)

fun petEmoji(index: Int): String = PetList.getOrElse(index) { PetList[0] }.emoji

val PetDrawables = listOf(
    R.drawable.pet_xiaomi,
    R.drawable.pet_wangcai_new,
    R.drawable.pet_tiaotiao,
    R.drawable.pet_gungun,
)

fun petDrawable(index: Int): Int = PetDrawables.getOrElse(index) { PetDrawables[0] }
