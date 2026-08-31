package com.eggrice.timetable.ui.treasurebox

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.AcademicCourseItem
import com.eggrice.timetable.network.AcademicSummary
import com.eggrice.timetable.network.AcademicTypeInfo
import com.eggrice.timetable.network.ZhengfangAcademicApi
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.ui.zhengfang.ZhengfangLoginViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * 修课情况查询（试验阶段）：选学校 → 只登录（不拉课表）→ 拉取学业情况统计与已修课程列表。
 * 登录/选校/验证码公共流程在 ZhengfangLoginViewModel。
 */
class AcademicQueryViewModel(
    client: ZhengfangClient,
    schoolRegistry: SchoolRegistry,
    appContainer: AppContainer
) : ZhengfangLoginViewModel(client, schoolRegistry, appContainer) {

    private val api = ZhengfangAcademicApi(client)

    // ── 学业数据 ──
    private val _summary = MutableStateFlow<AcademicSummary?>(null)
    val summary: StateFlow<AcademicSummary?> = _summary

    private val _types = MutableStateFlow<List<AcademicTypeInfo>>(emptyList())
    val types: StateFlow<List<AcademicTypeInfo>> = _types

    private val _courses = MutableStateFlow<List<AcademicCourseItem>>(emptyList())
    val courses: StateFlow<List<AcademicCourseItem>> = _courses

    private val _loaded = MutableStateFlow(false)
    val loaded: StateFlow<Boolean> = _loaded

    /** 登录成功 → 拉取学业情况统计 + 各类型课程明细 */
    override suspend fun afterLogin(school: School) = loadData(school.baseUrl)

    override fun onBackToSchoolList() = clearResults()
    override fun onLogout() = clearResults()
    override fun onLoginStart() { _loaded.value = false }

    private fun clearResults() {
        _summary.value = null
        _types.value = emptyList()
        _courses.value = emptyList()
        _loaded.value = false
    }

    private fun loadData(baseUrl: String) {
        viewModelScope.launch {
            _isLoading.value = true
            _progress.value = "正在获取学业情况..."
            try {
                val index = api.fetchIndex(baseUrl)
                if (index.summary.studentId.isBlank() && index.types.isEmpty()) {
                    _error.value = "未获取到学业情况数据，可能是教务页面结构变更"
                    _isLoading.value = false
                    _progress.value = ""
                    return@launch
                }
                _summary.value = index.summary
                _types.value = index.types

                val all = mutableListOf<AcademicCourseItem>()
                index.types.forEachIndexed { i, type ->
                    if (index.types.size > 1) {
                        _progress.value = "正在获取「${type.name}」课程 (${i + 1}/${index.types.size})..."
                    } else {
                        _progress.value = "正在获取「${type.name}」课程..."
                    }
                    val items = api.fetchCourses(baseUrl, type.id, type.name)
                    all.addAll(items)
                }
                _courses.value = all
                _loaded.value = true
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                _error.value = "获取学业情况失败：${e.message ?: "未知错误"}"
                _isLoading.value = false
                _progress.value = ""
            } finally {
                _isLoading.value = false
                _progress.value = ""
            }
        }
    }

    class Factory(
        private val client: ZhengfangClient,
        private val schoolRegistry: SchoolRegistry,
        private val appContainer: AppContainer
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
            AcademicQueryViewModel(client, schoolRegistry, appContainer) as T
    }
}
