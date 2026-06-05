package com.eggrice.timetable.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository(private val context: Context) {

    // ── Keys ──
    private object Keys {
        val NICKNAME = stringPreferencesKey("nickname")
        val SCHOOL = stringPreferencesKey("school")
        val ACTIVE_SCHEME_ID = longPreferencesKey("active_scheme_id")
        val ACTIVE_SCHEME_NAME = stringPreferencesKey("active_scheme_name")
        val SHOW_TEACHER = booleanPreferencesKey("show_teacher")
        val SHOW_ROOM = booleanPreferencesKey("show_room")
        val SHOW_CAMPUS = booleanPreferencesKey("show_campus")
        val SHOW_SLOT_TIME = booleanPreferencesKey("show_slot_time")
        val COLOR_THEME = stringPreferencesKey("color_theme")
        val CORNER_RADIUS = intPreferencesKey("corner_radius")
        val SHOW_DASHED_BORDER = booleanPreferencesKey("show_dashed_border")
        val TEXT_CENTERED = booleanPreferencesKey("text_centered")
        val GRID_HEIGHT = intPreferencesKey("grid_height")
        val GRID_OPACITY = floatPreferencesKey("grid_opacity")
        val GRID_TEXT_SIZE = intPreferencesKey("grid_text_size")
        val CLASS_DURATION = intPreferencesKey("class_duration")
        val BREAK_DURATION = intPreferencesKey("break_duration")
        val SEMESTER_START = stringPreferencesKey("semester_start")
        val SEMESTER_WEEKS = intPreferencesKey("semester_weeks")
        val CURRENT_WEEK_OVERRIDE = intPreferencesKey("current_week_override")
        val DARK_MODE = stringPreferencesKey("dark_mode")
        val SHOW_TREASURE_BOX = booleanPreferencesKey("show_treasure_box")
        val SHOW_WIDGET = booleanPreferencesKey("show_widget")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_MINUTES = intPreferencesKey("reminder_minutes")
        val AUTO_UPDATE = booleanPreferencesKey("auto_update")
        val SHOW_ODD_EVEN = booleanPreferencesKey("show_odd_even")
        val SHOW_NON_CURRENT_WEEK = booleanPreferencesKey("show_non_current_week")
        val VIBRATION_MODE = intPreferencesKey("vibration_mode")
        val GRID_BG_COLOR = intPreferencesKey("grid_bg_color")
        val OTHER_WEEK_ALPHA = floatPreferencesKey("other_week_alpha")
        val WALLPAPER_URI = stringPreferencesKey("wallpaper_uri")
        val BORDER_STYLE = intPreferencesKey("border_style")
    }

    // ── Flow reads ──
    val nickname: Flow<String> = context.dataStore.data.map { it[Keys.NICKNAME] ?: "同学" }
    val school: Flow<String> = context.dataStore.data.map { it[Keys.SCHOOL] ?: "" }
    val activeSchemeId: Flow<Long> = context.dataStore.data.map { it[Keys.ACTIVE_SCHEME_ID] ?: 0L }
    val activeSchemeName: Flow<String> = context.dataStore.data.map { it[Keys.ACTIVE_SCHEME_NAME] ?: "默认课表" }
    val showTeacher: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TEACHER] ?: true }
    val showRoom: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_ROOM] ?: true }
    val showCampus: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_CAMPUS] ?: false }
    val showSlotTime: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_SLOT_TIME] ?: false }
    val colorTheme: Flow<String> = context.dataStore.data.map { it[Keys.COLOR_THEME] ?: "default" }
    val cornerRadius: Flow<Int> = context.dataStore.data.map { it[Keys.CORNER_RADIUS] ?: 4 }
    val showDashedBorder: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_DASHED_BORDER] ?: false }
    val textCentered: Flow<Boolean> = context.dataStore.data.map { it[Keys.TEXT_CENTERED] ?: true }
    val gridHeight: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_HEIGHT] ?: 64 }
    val gridOpacity: Flow<Float> = context.dataStore.data.map { it[Keys.GRID_OPACITY] ?: 1.0f }
    val gridTextSize: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_TEXT_SIZE] ?: 12 }
    val classDuration: Flow<Int> = context.dataStore.data.map { it[Keys.CLASS_DURATION] ?: 45 }
    val breakDuration: Flow<Int> = context.dataStore.data.map { it[Keys.BREAK_DURATION] ?: 10 }
    val semesterStart: Flow<String> = context.dataStore.data.map { it[Keys.SEMESTER_START] ?: "" }
    val semesterWeeks: Flow<Int> = context.dataStore.data.map { it[Keys.SEMESTER_WEEKS] ?: 20 }
    val currentWeekOverride: Flow<Int> = context.dataStore.data.map { it[Keys.CURRENT_WEEK_OVERRIDE] ?: 0 }
    val darkMode: Flow<String> = context.dataStore.data.map { it[Keys.DARK_MODE] ?: "light" }
    val showTreasureBox: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_TREASURE_BOX] ?: false }
    val showWidget: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_WIDGET] ?: false }
    val reminderEnabled: Flow<Boolean> = context.dataStore.data.map { it[Keys.REMINDER_ENABLED] ?: false }
    val reminderMinutes: Flow<Int> = context.dataStore.data.map { it[Keys.REMINDER_MINUTES] ?: 15 }
    val autoUpdate: Flow<Boolean> = context.dataStore.data.map { it[Keys.AUTO_UPDATE] ?: false }
    val showOddEven: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_ODD_EVEN] ?: true }
    val showNonCurrentWeek: Flow<Boolean> = context.dataStore.data.map { it[Keys.SHOW_NON_CURRENT_WEEK] ?: false }
    val vibrationMode: Flow<Int> = context.dataStore.data.map { it[Keys.VIBRATION_MODE] ?: 1 }
    val gridBgColor: Flow<Int> = context.dataStore.data.map { it[Keys.GRID_BG_COLOR] ?: -1 }
    val otherWeekAlpha: Flow<Float> = context.dataStore.data.map { it[Keys.OTHER_WEEK_ALPHA] ?: 0.50f }
    val wallpaperUri: Flow<String> = context.dataStore.data.map { it[Keys.WALLPAPER_URI] ?: "" }
    val borderStyle: Flow<Int> = context.dataStore.data.map { it[Keys.BORDER_STYLE] ?: 0 }

    // ── Suspend writes ──
    suspend fun setNickname(value: String) { context.dataStore.edit { it[Keys.NICKNAME] = value } }
    suspend fun setSchool(value: String) { context.dataStore.edit { it[Keys.SCHOOL] = value } }
    suspend fun setActiveScheme(id: Long, name: String) { context.dataStore.edit { it[Keys.ACTIVE_SCHEME_ID] = id; it[Keys.ACTIVE_SCHEME_NAME] = name } }
    suspend fun setShowTeacher(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_TEACHER] = value } }
    suspend fun setShowRoom(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_ROOM] = value } }
    suspend fun setShowCampus(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_CAMPUS] = value } }
    suspend fun setShowSlotTime(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_SLOT_TIME] = value } }
    suspend fun setColorTheme(value: String) { context.dataStore.edit { it[Keys.COLOR_THEME] = value } }
    suspend fun setCornerRadius(value: Int) { context.dataStore.edit { it[Keys.CORNER_RADIUS] = value } }
    suspend fun setShowDashedBorder(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_DASHED_BORDER] = value } }
    suspend fun setTextCentered(value: Boolean) { context.dataStore.edit { it[Keys.TEXT_CENTERED] = value } }
    suspend fun setGridHeight(value: Int) { context.dataStore.edit { it[Keys.GRID_HEIGHT] = value } }
    suspend fun setGridOpacity(value: Float) { context.dataStore.edit { it[Keys.GRID_OPACITY] = value } }
    suspend fun setGridTextSize(value: Int) { context.dataStore.edit { it[Keys.GRID_TEXT_SIZE] = value } }
    suspend fun setClassDuration(value: Int) { context.dataStore.edit { it[Keys.CLASS_DURATION] = value } }
    suspend fun setBreakDuration(value: Int) { context.dataStore.edit { it[Keys.BREAK_DURATION] = value } }
    suspend fun setSemesterStart(value: String) { context.dataStore.edit { it[Keys.SEMESTER_START] = value } }
    suspend fun setSemesterWeeks(value: Int) { context.dataStore.edit { it[Keys.SEMESTER_WEEKS] = value } }
    suspend fun setCurrentWeekOverride(value: Int) { context.dataStore.edit { it[Keys.CURRENT_WEEK_OVERRIDE] = value } }
    suspend fun setDarkMode(value: String) { context.dataStore.edit { it[Keys.DARK_MODE] = value } }
    suspend fun setShowTreasureBox(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_TREASURE_BOX] = value } }
    suspend fun setShowWidget(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_WIDGET] = value } }
    suspend fun setReminderEnabled(value: Boolean) { context.dataStore.edit { it[Keys.REMINDER_ENABLED] = value } }
    suspend fun setReminderMinutes(value: Int) { context.dataStore.edit { it[Keys.REMINDER_MINUTES] = value } }
    suspend fun setAutoUpdate(value: Boolean) { context.dataStore.edit { it[Keys.AUTO_UPDATE] = value } }
    suspend fun setShowOddEven(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_ODD_EVEN] = value } }
    suspend fun setShowNonCurrentWeek(value: Boolean) { context.dataStore.edit { it[Keys.SHOW_NON_CURRENT_WEEK] = value } }
    suspend fun setVibrationMode(value: Int) { context.dataStore.edit { it[Keys.VIBRATION_MODE] = value } }
    suspend fun setGridBgColor(value: Int) { context.dataStore.edit { it[Keys.GRID_BG_COLOR] = value } }
    suspend fun setOtherWeekAlpha(value: Float) { context.dataStore.edit { it[Keys.OTHER_WEEK_ALPHA] = value } }
    suspend fun setWallpaperUri(value: String) { context.dataStore.edit { it[Keys.WALLPAPER_URI] = value } }
    suspend fun setBorderStyle(value: Int) { context.dataStore.edit { it[Keys.BORDER_STYLE] = value } }
}
