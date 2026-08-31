package com.eggrice.timetable.ui.treasurebox

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.data.entity.SavedGradeEntity
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.network.ZhengfangGradeApi
import com.eggrice.timetable.network.ZfGradeItem
import com.eggrice.timetable.network.ZfTerm
import com.eggrice.timetable.ui.zhengfang.ZhengfangLoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * 教务成绩查询（试验阶段）：选学校 → 只登录（不拉课表）→ 选学期 → 查成绩。
 * 登录/选校/验证码公共流程在 ZhengfangLoginViewModel。
 */
class GradeQueryViewModel(
    client: ZhengfangClient,
    schoolRegistry: SchoolRegistry,
    appContainer: AppContainer
) : ZhengfangLoginViewModel(client, schoolRegistry, appContainer) {

    private val api = ZhengfangGradeApi(client)
    private val savedGradeDao = appContainer.savedGradeDao

    // ── 已保存成绩 key（课程|学期|总评），用于成绩卡片标记 ──
    val savedKeys: StateFlow<Set<String>> = savedGradeDao.getAll()
        .map { list ->
            list.map { "${it.courseName}|${it.termLabel}|${it.totalScore}" }.toSet()
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    // ── 学期与成绩 ──
    private val _terms = MutableStateFlow<List<ZfTerm>>(emptyList())
    val terms: StateFlow<List<ZfTerm>> = _terms

    private val _selectedTerm = MutableStateFlow<ZfTerm?>(null)
    val selectedTerm: StateFlow<ZfTerm?> = _selectedTerm

    private val _grades = MutableStateFlow<List<ZfGradeItem>>(emptyList())
    val grades: StateFlow<List<ZfGradeItem>> = _grades

    private val _gradesLoaded = MutableStateFlow(false)
    val gradesLoaded: StateFlow<Boolean> = _gradesLoaded

    /** 登录成功 → 拉学期列表并查第一个学期的成绩 */
    override suspend fun afterLogin(school: School) = loadTerms(school.baseUrl)

    override fun onBackToSchoolList() = clearResults()
    override fun onLogout() = clearResults()
    override fun onLoginStart() { _gradesLoaded.value = false }

    private fun clearResults() {
        _terms.value = emptyList()
        _selectedTerm.value = null
        _grades.value = emptyList()
        _gradesLoaded.value = false
    }

    private fun loadTerms(baseUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = "正在获取学期列表..."
            try {
                val terms = api.fetchTerms(baseUrl)
                if (terms.isEmpty()) {
                    _error.value = "未获取到学期列表，可能是教务页面结构变更"
                } else {
                    _terms.value = terms
                    val first = terms.first()
                    _selectedTerm.value = first
                    loadGrades(baseUrl, first)
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "获取学期列表失败：${e.message ?: "未知错误"}"
                _isLoading.value = false
                _progress.value = ""
            }
        }
    }

    fun selectTerm(term: ZfTerm) {
        if (_selectedTerm.value == term) return
        _selectedTerm.value = term
        val base = _selectedSchool.value?.baseUrl ?: return
        loadGrades(base, term)
    }

    private fun loadGrades(baseUrl: String, term: ZfTerm) {
        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = "正在查询 ${term.label} 成绩..."
            try {
                val grades = api.fetchGrades(baseUrl, term.xnm, term.xqm)
                _grades.value = grades
                _gradesLoaded.value = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "查询成绩失败：${e.message ?: "未知错误"}"
            } finally {
                _isLoading.value = false
                _progress.value = ""
            }
        }
    }

    // ── 保存成绩到本地（已修课程/成绩存档）──

    /** 保存单门成绩（已存在则跳过） */
    fun saveGrade(item: ZfGradeItem) {
        val school = _selectedSchool.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            item.toSavedGrade(school.name)?.let { savedGradeDao.insert(it) }
        }
    }

    /** 保存当前学期查询结果的全部成绩 */
    fun saveAllGrades() {
        val school = _selectedSchool.value ?: return
        val current = _grades.value
        if (current.isEmpty()) return
        viewModelScope.launch(Dispatchers.IO) {
            val items = current.mapNotNull { it.toSavedGrade(school.name) }
            if (items.isNotEmpty()) savedGradeDao.insertAll(items)
        }
    }

    class Factory(
        private val client: ZhengfangClient,
        private val schoolRegistry: SchoolRegistry,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            GradeQueryViewModel(client, schoolRegistry, appContainer) as T
    }
}

/** 成绩记录 → 本地存档实体 */
internal fun ZfGradeItem.toSavedGrade(schoolName: String): SavedGradeEntity? {
    val name = courseName.trim()
    if (name.isBlank()) return null
    return SavedGradeEntity(
        courseName = name,
        totalScore = totalScore,
        credits = credits,
        gpa = gpa,
        regular = regular,
        final = final,
        midterm = midterm,
        examType = examType,
        termLabel = termLabel,
        schoolName = schoolName
    )
}
