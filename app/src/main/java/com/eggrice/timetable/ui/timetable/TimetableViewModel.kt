package com.eggrice.timetable.ui.timetable

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.entity.SchemeEntity
import com.eggrice.timetable.data.entity.TimeSlotEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.di.AppContainer
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class TimetableViewModel(
    private val repository: CourseRepository,
    private val settings: AppContainer
) : ViewModel() {
    // Scheme-aware courses: switches Flow when active scheme changes
    val allCourses: StateFlow<List<CourseEntity>> = settings.activeSchemeId
        .flatMapLatest { schemeId -> repository.getCoursesByScheme(schemeId) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allTimeSlots: StateFlow<List<TimeSlotEntity>> = repository.allTimeSlots
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSchemes: StateFlow<List<SchemeEntity>> = repository.allSchemes
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeSchemeId: StateFlow<Long> = settings.activeSchemeId
    val activeSchemeName: StateFlow<String> = settings.activeSchemeName

    // Current week: auto-calculate from semester start
    private val _currentWeek = MutableStateFlow(settings.autoCurrentWeek())
    val currentWeek: StateFlow<Int> = _currentWeek

    // Re-sync current week when semester settings change (e.g. saved from SemesterSettingsPage)
    init {
        viewModelScope.launch {
            combine(settings.semesterStart, settings.semesterWeeks) { _, _ -> }
                .drop(1)
                .collect { goToToday() }
        }
    }

    val semesterTotalWeeks: StateFlow<Int> = settings.semesterWeeks

    private val _showEditor = MutableStateFlow(false)
    val showEditor: StateFlow<Boolean> = _showEditor

    private val _editingCourse = MutableStateFlow<CourseEntity?>(null)
    val editingCourse: StateFlow<CourseEntity?> = _editingCourse

    // Display settings — sourced from shared AppContainer
    val showTeacher: StateFlow<Boolean> = settings.showTeacher
    val showRoom: StateFlow<Boolean> = settings.showRoom
    val showCampus: StateFlow<Boolean> = settings.showCampus
    fun prevWeek() { if (_currentWeek.value > 1) _currentWeek.value-- }
    fun nextWeek() { if (_currentWeek.value < semesterTotalWeeks.value) _currentWeek.value++ }
    fun goToToday() {
        _currentWeek.value = settings.autoCurrentWeek()
    }
    fun goToWeek(week: Int) { _currentWeek.value = week.coerceIn(1, semesterTotalWeeks.value) }

    fun openAddEditor(day: Int? = null, slot: Int? = null) {
        _editingCourse.value = CourseEntity(
            name = "", dayOfWeek = day ?: 1, startSlot = slot ?: 1, endSlot = slot?.plus(1) ?: 2,
            schemeId = settings.activeSchemeId.value
        )
        _showEditor.value = true
    }

    fun openEditEditor(course: CourseEntity) {
        _editingCourse.value = course
        _showEditor.value = true
    }

    fun closeEditor() {
        _showEditor.value = false
        _editingCourse.value = null
    }

    fun saveCourse(course: CourseEntity) {
        viewModelScope.launch {
            if (course.id == 0L) {
                repository.insert(course)
            } else {
                repository.update(course)
            }
        }
    }

    fun deleteCourseRange(course: CourseEntity, range: DeleteRange) {
        viewModelScope.launch {
            when (range) {
                DeleteRange.THIS_INSTANCE -> removeCourseFromWeek(course)
                DeleteRange.SAME_TIME_SLOT -> repository.deleteByTimeSlot(
                    course.dayOfWeek, course.startSlot, course.endSlot, course.schemeId
                )
                DeleteRange.ALL_BY_NAME -> repository.deleteByName(course.name, course.schemeId)
            }
        }
    }

    private suspend fun removeCourseFromWeek(course: CourseEntity) {
        val week = _currentWeek.value
        val total = semesterTotalWeeks.value
        val allWeeks = when {
            course.weeks.isNotEmpty() -> course.weeks.split(",").mapNotNull { it.toIntOrNull() }
            course.weekType == "odd" -> (1..total).filter { it % 2 == 1 }
            course.weekType == "even" -> (1..total).filter { it % 2 == 0 }
            else -> (1..total).toList()
        }
        val remaining = allWeeks.filter { it != week }
        if (remaining.isEmpty()) {
            repository.deleteById(course.id)
        } else {
            repository.update(course.copy(weeks = remaining.joinToString(","), weekType = "all"))
        }
    }

    fun updateCoursePosition(course: CourseEntity, newDay: Int, newStartSlot: Int) {
        val span = course.endSlot - course.startSlot
        viewModelScope.launch {
            repository.update(course.copy(
                dayOfWeek = newDay,
                startSlot = newStartSlot,
                endSlot = newStartSlot + span
            ))
        }
    }

    // ── Scheme management ──
    fun switchScheme(scheme: SchemeEntity) {
        settings.setActiveScheme(scheme.id, scheme.name)
    }

    fun createScheme(name: String) {
        viewModelScope.launch {
            val id = repository.createScheme(name)
            settings.setActiveScheme(id, name)
        }
    }

    fun renameScheme(scheme: SchemeEntity, newName: String) {
        viewModelScope.launch {
            repository.updateScheme(scheme.copy(name = newName))
            if (scheme.id == settings.activeSchemeId.value) {
                settings.setActiveScheme(scheme.id, newName)
            }
        }
    }

    fun deleteScheme(scheme: SchemeEntity) {
        viewModelScope.launch {
            repository.deleteSchemeCascade(scheme.id)
            // Switch to default scheme if the deleted one was active
            if (scheme.id == settings.activeSchemeId.value) {
                val def = repository.getSchemeById(0L) ?: SchemeEntity(id = 0, name = "默认课表")
                settings.setActiveScheme(def.id, def.name)
            }
        }
    }

    // 按当前周过滤：odd/even周类型 + weeks列表匹配。combine确保周切换时自动重算
    val filteredCourses: StateFlow<List<CourseEntity>> = combine(
        allCourses, _currentWeek
    ) { courses, week ->
        val isOdd = week % 2 == 1
        courses.filter { c ->
            val wt = c.weekType
            if (wt == "odd" && !isOdd) return@filter false
            if (wt == "even" && isOdd) return@filter false
            if (c.weeks.isNotEmpty()) {
                val wks = c.weeks.split(",").mapNotNull { it.toIntOrNull() }
                if (wks.isNotEmpty() && week !in wks) return@filter false
            }
            true
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 非本周课程：用于半透明显示（与filteredCourses互补）
    val nonCurrentWeekCourses: StateFlow<List<CourseEntity>> = combine(
        allCourses, filteredCourses
    ) { all, filtered ->
        if (filtered.isEmpty()) all else all - filtered.toSet()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    class Factory(
        private val repository: CourseRepository,
        private val settings: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            TimetableViewModel(repository, settings) as T
    }
}

enum class DeleteRange {
    THIS_INSTANCE,
    SAME_TIME_SLOT,
    ALL_BY_NAME
}
