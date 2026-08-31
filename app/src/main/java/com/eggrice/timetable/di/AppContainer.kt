package com.eggrice.timetable.di

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.data.dao.SavedGradeDao
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.network.QiangZhiClient
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.util.currentWeekFrom
import com.eggrice.timetable.util.parseSemesterStart
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class AppContainer(context: Context) {
    private val app = context.applicationContext as TimetableApplication
    val repository: CourseRepository get() = app.repository
    val zhengfangClient by lazy { ZhengfangClient() }
    val qiangzhiClient by lazy { QiangZhiClient() }
    val schoolRegistry by lazy { SchoolRegistry(context) }
    val savedGradeDao: SavedGradeDao by lazy { app.database.savedGradeDao() }

    private val prefs = context.getSharedPreferences("egg_rice_prefs", Context.MODE_PRIVATE)

    // ── Favorite schools ──
    private val gson = Gson()
    private val _favoriteSchoolIds = MutableStateFlow(loadFavoriteIds())
    val favoriteSchoolIds: StateFlow<Set<String>> = _favoriteSchoolIds

    private fun loadFavoriteIds(): Set<String> {
        val raw = prefs.getString("favorite_school_ids", null) ?: return emptySet()
        return try {
            gson.fromJson(raw, object : TypeToken<Set<String>>() {}.type) ?: emptySet()
        } catch (_: Exception) { emptySet() }
    }

    fun toggleFavoriteSchool(schoolId: String) {
        val current = _favoriteSchoolIds.value.toMutableSet()
        if (schoolId in current) current.remove(schoolId) else current.add(schoolId)
        _favoriteSchoolIds.value = current
        prefs.edit().putString("favorite_school_ids", gson.toJson(current)).apply()
    }

    // ── Custom user-defined schools ──
    private val _customSchools = MutableStateFlow(loadCustomSchools())
    val customSchools: StateFlow<List<School>> = _customSchools

    private fun loadCustomSchools(): List<School> {
        val raw = prefs.getString("custom_schools", null) ?: return emptyList()
        return try {
            gson.fromJson(raw, object : TypeToken<List<School>>() {}.type) ?: emptyList()
        } catch (_: Exception) { emptyList() }
    }

    fun addCustomSchool(school: School) {
        val current = _customSchools.value.toMutableList()
        current.removeAll { it.baseUrl.equals(school.baseUrl, ignoreCase = true) }
        current.add(0, school)
        _customSchools.value = current
        prefs.edit().putString("custom_schools", gson.toJson(current)).apply()
    }

    /** 更新自定义学校（编辑保存，按 id 定位） */
    fun updateCustomSchool(old: School, new: School) {
        val current = _customSchools.value.toMutableList()
        val idx = current.indexOfFirst { it.id == old.id }
        if (idx >= 0) current[idx] = new
        _customSchools.value = current
        prefs.edit().putString("custom_schools", gson.toJson(current)).apply()
    }

    /** 删除自定义学校 */
    fun removeCustomSchool(school: School) {
        val current = _customSchools.value.toMutableList()
        current.removeAll { it.id == school.id }
        _customSchools.value = current
        prefs.edit().putString("custom_schools", gson.toJson(current)).apply()
    }

    // ── Credential memory (EncryptedSharedPreferences) ──
    private val credPrefs: SharedPreferences by lazy {
        val ctx = context.applicationContext
        val masterKey = MasterKey.Builder(ctx)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        val encrypted = try {
            EncryptedSharedPreferences.create(
                ctx,
                "edu_credentials_encrypted",
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.w("AppContainer", "EncryptedSharedPreferences unavailable, falling back to plain", e)
            ctx.getSharedPreferences("edu_credentials_encrypted", Context.MODE_PRIVATE)
        }
        migrateOldCredentials(ctx, encrypted)
        encrypted
    }

    private fun migrateOldCredentials(ctx: Context, target: SharedPreferences) {
        val marker = ctx.getSharedPreferences("_migration_marker", Context.MODE_PRIVATE)
        if (marker.getBoolean("creds_migrated", false)) return

        val old = ctx.getSharedPreferences("egg_rice_prefs", Context.MODE_PRIVATE)
        val keysToRemove = mutableListOf<String>()
        old.all.forEach { (key, value) ->
            if (key.startsWith("cred_user_") || key.startsWith("cred_pass_")) {
                target.edit().putString(key, value.toString()).apply()
                keysToRemove.add(key)
            }
        }
        if (keysToRemove.isNotEmpty()) {
            val edit = old.edit()
            keysToRemove.forEach { edit.remove(it) }
            edit.apply()
        }
        marker.edit().putBoolean("creds_migrated", true).apply()
    }

    fun saveCredential(baseUrl: String, username: String, password: String) {
        val key = baseUrl.trimEnd('/')
        credPrefs.edit()
            .putString("cred_user_$key", username)
            .putString("cred_pass_$key", password)
            .apply()
    }

    fun loadCredential(baseUrl: String): Pair<String, String>? {
        val key = baseUrl.trimEnd('/')
        val user = credPrefs.getString("cred_user_$key", null) ?: return null
        val pass = credPrefs.getString("cred_pass_$key", null) ?: return null
        return Pair(user, pass)
    }

    fun deleteCredential(baseUrl: String) {
        val key = baseUrl.trimEnd('/')
        credPrefs.edit()
            .remove("cred_user_$key")
            .remove("cred_pass_$key")
            .apply()
    }

    // ── User profile ──
    private val _nickname = MutableStateFlow(prefs.getString("nickname", "同学") ?: "同学")
    val nickname: StateFlow<String> = _nickname
    private val _school = MutableStateFlow(prefs.getString("school", "") ?: "")
    val school: StateFlow<String> = _school

    fun setNickname(value: String) {
        _nickname.value = value; prefs.edit().putString("nickname", value).apply()
    }
    fun setSchool(value: String) {
        _school.value = value; prefs.edit().putString("school", value).apply()
    }

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

    // 值类型 setter（个性化配置页使用，避免先 toggle 再比较的竞态）
    fun setShowTeacher(value: Boolean) { setBool("show_teacher", value, _showTeacher) }
    fun setShowRoom(value: Boolean) { setBool("show_room", value, _showRoom) }
    fun setShowCampus(value: Boolean) { setBool("show_campus", value, _showCampus) }
    fun setShowSlotTime(value: Boolean) { setBool("show_slot_time", value, _showSlotTime) }

    // ── Theme switching (7 themes) ──
    private val _colorTheme = MutableStateFlow(prefs.getString("color_theme", "default") ?: "default")
    val colorTheme: StateFlow<String> = _colorTheme

    // ── Card appearance ──
    private val _cornerRadius = MutableStateFlow(prefs.getInt("corner_radius", 4))
    val cornerRadius: StateFlow<Int> = _cornerRadius

    fun setCornerRadius(value: Int) { setInt("corner_radius", value, _cornerRadius) }
    fun setColorTheme(value: String) { setStr("color_theme", value, _colorTheme) }

    // ── Grid appearance ──
    private val _textCentered = MutableStateFlow(prefs.getBoolean("text_centered", true))
    val textCentered: StateFlow<Boolean> = _textCentered
    private val _gridHeight = MutableStateFlow(prefs.getInt("grid_height", 64))
    val gridHeight: StateFlow<Int> = _gridHeight
    private val _gridOpacity = MutableStateFlow(prefs.getFloat("grid_opacity", 1.0f))
    val gridOpacity: StateFlow<Float> = _gridOpacity
    private val _gridTextSize = MutableStateFlow(prefs.getInt("grid_text_size", 12))
    val gridTextSize: StateFlow<Int> = _gridTextSize

    fun setTextCentered(value: Boolean) { setBool("text_centered", value, _textCentered) }
    fun setGridHeight(value: Int) { setInt("grid_height", value, _gridHeight) }
    fun setGridOpacity(value: Float) {
        _gridOpacity.value = value; prefs.edit().putFloat("grid_opacity", value).apply()
    }
    fun setGridTextSize(value: Int) { setInt("grid_text_size", value, _gridTextSize) }
    fun setShowNonCurrentWeek(value: Boolean) { setBool("show_non_current_week", value, _showNonCurrentWeek) }
    fun setShowOddEven(value: Boolean) { setBool("show_odd_even", value, _showOddEven) }

    private val _verticalLayout = MutableStateFlow(prefs.getBoolean("vertical_layout", true))
    val verticalLayout: StateFlow<Boolean> = _verticalLayout
    fun setVerticalLayout(value: Boolean) { setBool("vertical_layout", value, _verticalLayout) }

    // ── Time slot defaults ──
    private val _defaultClassDuration = MutableStateFlow(prefs.getInt("class_duration", 45))
    val defaultClassDuration: StateFlow<Int> = _defaultClassDuration
    fun setDefaultClassDuration(value: Int) { setInt("class_duration", value, _defaultClassDuration) }

    private val _defaultBreakDuration = MutableStateFlow(prefs.getInt("break_duration", 10))
    val defaultBreakDuration: StateFlow<Int> = _defaultBreakDuration
    fun setDefaultBreakDuration(value: Int) { setInt("break_duration", value, _defaultBreakDuration) }

    // ── Semester / week settings ──
    private val _semesterStart = MutableStateFlow(prefs.getString("semester_start", "") ?: "")
    val semesterStart: StateFlow<String> = _semesterStart
    fun setSemesterStart(value: String) { setStr("semester_start", value, _semesterStart) }

    private val _semesterWeeks = MutableStateFlow(prefs.getInt("semester_weeks", 20))
    val semesterWeeks: StateFlow<Int> = _semesterWeeks
    fun setSemesterWeeks(value: Int) { setInt("semester_weeks", value, _semesterWeeks) }

    // ── Cross-tab navigation signal: Homework ──
    private val _pendingShowHomework = MutableStateFlow(false)
    val pendingShowHomework: StateFlow<Boolean> = _pendingShowHomework

    fun requestShowHomework() { _pendingShowHomework.value = true }
    fun consumeShowHomework() { _pendingShowHomework.value = false }

    fun autoCurrentWeek(): Int {
        val start = parseSemesterStart(_semesterStart.value) ?: return 1
        return currentWeekFrom(start, _semesterWeeks.value)
    }

    // ── General settings ──
    private val _darkMode = MutableStateFlow(prefs.getString("dark_mode", "light") ?: "light")
    val darkMode: StateFlow<String> = _darkMode
    fun setDarkMode(value: String) { setStr("dark_mode", value, _darkMode) }

    // ── Feature toggles ──
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
    private val _otherWeekAlpha = MutableStateFlow(prefs.getFloat("other_week_alpha", 0.50f))
    val otherWeekAlpha: StateFlow<Float> = _otherWeekAlpha
    fun setOtherWeekAlpha(value: Float) {
        _otherWeekAlpha.value = value; prefs.edit().putFloat("other_week_alpha", value).apply()
    }

    // ── Wallpaper URI (empty = none) ──
    private val _wallpaperUri = MutableStateFlow(prefs.getString("wallpaper_uri", "") ?: "")
    val wallpaperUri: StateFlow<String> = _wallpaperUri
    fun setWallpaperUri(value: String) { setStr("wallpaper_uri", value, _wallpaperUri) }

    // ── Skin: "wangcai" bundles sea-blue theme + 旺财 pet, "fried_rice" bundles golden theme ──
    private val _skin = MutableStateFlow(prefs.getString("app_skin", "wangcai") ?: "wangcai")
    val skin: StateFlow<String> = _skin

    fun setSkin(value: String) {
        _skin.value = value; prefs.edit().putString("app_skin", value).apply()
        when (value) {
            "wangcai" -> {
                setColorTheme("default")
                setPetIndex(1)
            }
            "macaron_blue" -> {
                setColorTheme("macaron_blue")
                setPetIndex(1)
            }
            "macaron_pink" -> {
                setColorTheme("macaron_pink")
                setPetIndex(0)
            }
            "matcha" -> {
                setColorTheme("matcha")
                setPetIndex(3)
            }
            "sakura" -> {
                setColorTheme("sakura")
                setPetIndex(0)
            }
            "wisteria" -> {
                setColorTheme("wisteria")
                setPetIndex(2)
            }
            "fried_rice" -> {
                setColorTheme("fried_rice")
                setPetIndex(1)
            }
            // Unknown skin values: apply as color theme directly
            else -> {
                setColorTheme(value)
            }
        }
    }

    // ── Pet: 0=小咪, 1=旺财, 2=跳跳, 3=滚滚 ──
    private val _petIndex = MutableStateFlow(prefs.getInt("pet_index", 1))
    val petIndex: StateFlow<Int> = _petIndex
    fun setPetIndex(value: Int) { setInt("pet_index", value, _petIndex) }

    private val _petName = MutableStateFlow(prefs.getString("pet_name", "旺财") ?: "旺财")
    val petName: StateFlow<String> = _petName
    fun setPetName(value: String) {
        _petName.value = value; prefs.edit().putString("pet_name", value).apply()
    }
    private val _borderStyle = MutableStateFlow(prefs.getInt("border_style", 0))
    val borderStyle: StateFlow<Int> = _borderStyle
    fun setBorderStyle(value: Int) { setInt("border_style", value, _borderStyle) }

    // ── helpers（单一事实来源：SharedPreferences + StateFlow）──
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
