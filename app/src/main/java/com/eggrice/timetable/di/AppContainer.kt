package com.eggrice.timetable.di

import android.content.Context
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.network.ZhengfangClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppContainer(context: Context) {
    private val app = context.applicationContext as TimetableApplication
    val repository: CourseRepository get() = app.repository
    val zhengfangClient by lazy { ZhengfangClient() }
    val schoolRegistry by lazy { SchoolRegistry(context) }

    private val prefs = context.getSharedPreferences("egg_rice_prefs", Context.MODE_PRIVATE)

    // ── User profile ──
    private val _nickname = MutableStateFlow(prefs.getString("nickname", "同学") ?: "同学")
    val nickname: StateFlow<String> = _nickname
    private val _school = MutableStateFlow(prefs.getString("school", "") ?: "")
    val school: StateFlow<String> = _school

    fun setNickname(value: String) { _nickname.value = value; prefs.edit().putString("nickname", value).apply() }
    fun setSchool(value: String) { _school.value = value; prefs.edit().putString("school", value).apply() }

    // ── Scheme management ──
    private val _activeSchemeId = MutableStateFlow(prefs.getLong("active_scheme_id", 0L))
    val activeSchemeId: StateFlow<Long> = _activeSchemeId
    private val _activeSchemeName = MutableStateFlow(prefs.getString("active_scheme_name", "默认课表") ?: "默认课表")
    val activeSchemeName: StateFlow<String> = _activeSchemeName

    fun setActiveScheme(id: Long, name: String) {
        _activeSchemeId.value = id
        _activeSchemeName.value = name
        prefs.edit().putLong("active_scheme_id", id).putString("active_scheme_name", name).apply()
    }

    // ── Display toggles ──
    private val _showTeacher = MutableStateFlow(prefs.getBoolean("show_teacher", true))
    val showTeacher: StateFlow<Boolean> = _showTeacher
    private val _showRoom = MutableStateFlow(prefs.getBoolean("show_room", true))
    val showRoom: StateFlow<Boolean> = _showRoom
    private val _showCampus = MutableStateFlow(prefs.getBoolean("show_campus", false))
    val showCampus: StateFlow<Boolean> = _showCampus
    private val _showSlotTime = MutableStateFlow(prefs.getBoolean("show_slot_time", false))
    val showSlotTime: StateFlow<Boolean> = _showSlotTime

    fun toggleShowTeacher() { setBool("show_teacher", !_showTeacher.value, _showTeacher) }
    fun toggleShowRoom() { setBool("show_room", !_showRoom.value, _showRoom) }
    fun toggleShowCampus() { setBool("show_campus", !_showCampus.value, _showCampus) }
    fun toggleShowSlotTime() { setBool("show_slot_time", !_showSlotTime.value, _showSlotTime) }

    // ── Theme switching (4 themes) ──
    private val _colorTheme = MutableStateFlow(prefs.getString("color_theme", "default") ?: "default")
    val colorTheme: StateFlow<String> = _colorTheme

    // ── Card appearance ──
    private val _cornerRadius = MutableStateFlow(prefs.getInt("corner_radius", 12))
    val cornerRadius: StateFlow<Int> = _cornerRadius

    fun setCornerRadius(value: Int) { setInt("corner_radius", value, _cornerRadius) }
    fun setColorTheme(value: String) { setStr("color_theme", value, _colorTheme) }

    // ── Grid appearance ──
    private val _showDashedBorder = MutableStateFlow(prefs.getBoolean("show_dashed_border", false))
    val showDashedBorder: StateFlow<Boolean> = _showDashedBorder
    private val _textCentered = MutableStateFlow(prefs.getBoolean("text_centered", true))
    val textCentered: StateFlow<Boolean> = _textCentered
    private val _gridHeight = MutableStateFlow(prefs.getInt("grid_height", 72))
    val gridHeight: StateFlow<Int> = _gridHeight
    private val _gridOpacity = MutableStateFlow(prefs.getFloat("grid_opacity", 1.0f))
    val gridOpacity: StateFlow<Float> = _gridOpacity
    private val _gridTextSize = MutableStateFlow(prefs.getInt("grid_text_size", 13))
    val gridTextSize: StateFlow<Int> = _gridTextSize

    fun toggleDashedBorder() { setBool("show_dashed_border", !_showDashedBorder.value, _showDashedBorder) }
    fun toggleTextCentered() { setBool("text_centered", !_textCentered.value, _textCentered) }
    fun setGridHeight(value: Int) { setInt("grid_height", value, _gridHeight) }
    fun setGridOpacity(value: Float) { _gridOpacity.value = value; prefs.edit().putFloat("grid_opacity", value).apply() }
    fun setGridTextSize(value: Int) { setInt("grid_text_size", value, _gridTextSize) }

    // ── Time slot defaults ──
    private val _defaultClassDuration = MutableStateFlow(prefs.getInt("class_duration", 45))
    val defaultClassDuration: StateFlow<Int> = _defaultClassDuration
    fun setDefaultClassDuration(value: Int) { setInt("class_duration", value, _defaultClassDuration) }

    private val _defaultBreakDuration = MutableStateFlow(prefs.getInt("break_duration", 10))
    val defaultBreakDuration: StateFlow<Int> = _defaultBreakDuration
    fun setDefaultBreakDuration(value: Int) { setInt("break_duration", value, _defaultBreakDuration) }

    // ── Semester / week settings ──
    // First Monday of semester, e.g. "2026-02-23". Empty = not configured.
    private val _semesterStart = MutableStateFlow(prefs.getString("semester_start", "") ?: "")
    val semesterStart: StateFlow<String> = _semesterStart
    fun setSemesterStart(value: String) { setStr("semester_start", value, _semesterStart) }

    // Total teaching weeks in the semester (default 20)
    private val _semesterWeeks = MutableStateFlow(prefs.getInt("semester_weeks", 20))
    val semesterWeeks: StateFlow<Int> = _semesterWeeks
    fun setSemesterWeeks(value: Int) { setInt("semester_weeks", value, _semesterWeeks) }

    // Manual week override. 0 = auto-calculate from semester start date.
    private val _currentWeekOverride = MutableStateFlow(prefs.getInt("current_week_override", 0))
    val currentWeekOverride: StateFlow<Int> = _currentWeekOverride
    fun setCurrentWeekOverride(value: Int) { setInt("current_week_override", value, _currentWeekOverride) }

    /** Auto-calculate current week from semester start date. Returns 1 if not configured. */
    fun autoCurrentWeek(): Int {
        val startStr = _semesterStart.value
        if (startStr.isBlank()) return 1
        return try {
            val parts = startStr.split("-")
            val start = java.time.LocalDate.of(parts[0].toInt(), parts[1].toInt(), parts[2].toInt())
            val now = java.time.LocalDate.now()
            val days = java.time.temporal.ChronoUnit.DAYS.between(start, now)
            val week = Math.floorDiv(days, 7).toInt() + 1
            week.coerceIn(1, _semesterWeeks.value)
        } catch (_: Exception) { 1 }
    }

    // ── General settings ──
    private val _darkMode = MutableStateFlow(prefs.getString("dark_mode", "light") ?: "light")
    val darkMode: StateFlow<String> = _darkMode
    fun setDarkMode(value: String) { setStr("dark_mode", value, _darkMode) }

    // ── Feature toggles (user-controlled, non-core features default OFF) ──
    private val _showTreasureBox = MutableStateFlow(prefs.getBoolean("show_treasure_box", false))
    val showTreasureBox: StateFlow<Boolean> = _showTreasureBox
    fun toggleTreasureBox() { setBool("show_treasure_box", !_showTreasureBox.value, _showTreasureBox) }

    private val _showWidget = MutableStateFlow(prefs.getBoolean("show_widget", false))
    val showWidget: StateFlow<Boolean> = _showWidget
    fun toggleWidget() { setBool("show_widget", !_showWidget.value, _showWidget) }

    private val _reminderEnabled = MutableStateFlow(prefs.getBoolean("reminder_enabled", false))
    val reminderEnabled: StateFlow<Boolean> = _reminderEnabled
    private val _reminderMinutes = MutableStateFlow(prefs.getInt("reminder_minutes", 15))
    val reminderMinutes: StateFlow<Int> = _reminderMinutes

    private val _autoUpdate = MutableStateFlow(prefs.getBoolean("auto_update", false))
    val autoUpdate: StateFlow<Boolean> = _autoUpdate

    private val _showOddEven = MutableStateFlow(prefs.getBoolean("show_odd_even", true))
    val showOddEven: StateFlow<Boolean> = _showOddEven
    private val _showNonCurrentWeek = MutableStateFlow(prefs.getBoolean("show_non_current_week", false))
    val showNonCurrentWeek: StateFlow<Boolean> = _showNonCurrentWeek

    fun toggleReminder() { setBool("reminder_enabled", !_reminderEnabled.value, _reminderEnabled) }
    fun setReminderMinutes(value: Int) { setInt("reminder_minutes", value, _reminderMinutes) }
    fun toggleShowOddEven() { setBool("show_odd_even", !_showOddEven.value, _showOddEven) }
    fun toggleAutoUpdate() { setBool("auto_update", !_autoUpdate.value, _autoUpdate) }
    fun toggleShowNonCurrentWeek() { setBool("show_non_current_week", !_showNonCurrentWeek.value, _showNonCurrentWeek) }

    // ── Vibration mode: 0=off, 1=light, 2=medium, 3=strong ──
    private val _vibrationMode = MutableStateFlow(prefs.getInt("vibration_mode", 1))
    val vibrationMode: StateFlow<Int> = _vibrationMode
    fun setVibrationMode(value: Int) { setInt("vibration_mode", value, _vibrationMode) }

    // ── Grid background color: -1=default(theme), 0..N=palette index ──
    private val _gridBgColor = MutableStateFlow(prefs.getInt("grid_bg_color", -1))
    val gridBgColor: StateFlow<Int> = _gridBgColor
    fun setGridBgColor(value: Int) { setInt("grid_bg_color", value, _gridBgColor) }

    // ── Non-current week opacity (0.05 .. 0.5) ──
    private val _otherWeekAlpha = MutableStateFlow(prefs.getFloat("other_week_alpha", 0.18f))
    val otherWeekAlpha: StateFlow<Float> = _otherWeekAlpha
    fun setOtherWeekAlpha(value: Float) { _otherWeekAlpha.value = value; prefs.edit().putFloat("other_week_alpha", value).apply() }

    // ── Border style: 0=none, 1=solid, 2=dashed ──
    private val _borderStyle = MutableStateFlow(prefs.getInt("border_style", 0))
    val borderStyle: StateFlow<Int> = _borderStyle
    fun setBorderStyle(value: Int) { setInt("border_style", value, _borderStyle) }

    // ── helpers ──
    private fun setBool(key: String, value: Boolean, flow: MutableStateFlow<Boolean>) {
        flow.value = value; prefs.edit().putBoolean(key, value).apply()
    }
    private fun setInt(key: String, value: Int, flow: MutableStateFlow<Int>) {
        flow.value = value; prefs.edit().putInt(key, value).apply()
    }
    private fun setStr(key: String, value: String, flow: MutableStateFlow<String>) {
        flow.value = value; prefs.edit().putString(key, value).apply()
    }
}
