package com.eggrice.timetable.ui.import_

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.data.isJwSystemAvailable
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.network.LoginResult
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.network.ZhengfangSchool
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class ImportViewModel(
    private val repository: CourseRepository,
    private val client: ZhengfangClient,
    private val schoolRegistry: SchoolRegistry
) : ViewModel() {
    private val _selectedSystem = MutableStateFlow<JwSystemType?>(null)
    val selectedSystem: StateFlow<JwSystemType?> = _selectedSystem

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    val filteredSchools: StateFlow<List<School>> = combine(_searchQuery, _selectedSystem) { query, system ->
        if (system != null) schoolRegistry.filter(system, query) else emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _progress = MutableStateFlow("")
    val progress: StateFlow<String> = _progress

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _result = MutableStateFlow<LoginResult?>(null)
    val result: StateFlow<LoginResult?> = _result

    private val _captchaBase64 = MutableStateFlow<String?>(null)
    val captchaBase64: StateFlow<String?> = _captchaBase64

    private val _showCaptcha = MutableStateFlow(false)
    val showCaptcha: StateFlow<Boolean> = _showCaptcha

    // ── Coroutine lifecycle management (matches Dawn-Course pattern) ──
    private var loginJob: Job? = null
    private var captchaContinuation: ((String) -> Unit)? = null
    private var captchaRefresher: (suspend () -> com.eggrice.timetable.network.CaptchaResult)? = null

    fun selectSystem(type: JwSystemType) {
        if (isJwSystemAvailable(type)) {
            _selectedSystem.value = type
            _selectedSchool.value = null
            _searchQuery.value = ""
        }
    }

    fun selectSchool(school: School?) {
        _selectedSchool.value = school
    }

    fun updateSearch(query: String) {
        _searchQuery.value = query
    }

    fun goBack() {
        if (_selectedSchool.value != null) {
            _selectedSchool.value = null
        } else if (_selectedSystem.value != null) {
            _selectedSystem.value = null
            _searchQuery.value = ""
        }
    }

    fun startLogin(username: String, password: String) {
        val school = _selectedSchool.value ?: return

        // Cancel any previous login attempt
        loginJob?.cancel()
        captchaContinuation = null
        captchaRefresher = null

        _isLoading.value = true
        _progress.value = ""
        _result.value = null
        _showCaptcha.value = false

        val zfSchool = ZhengfangSchool(school.name, school.baseUrl.trimEnd('/'), school.isV8)

        loginJob = viewModelScope.launch {
            try {
                val res = client.login(
                    school = zfSchool,
                    username = username,
                    password = password,
                    onProgress = { msg -> _progress.value = msg },
                    onCaptcha = { captchaResult ->
                        captchaRefresher = captchaResult.refresh
                        withContext(Dispatchers.Main) {
                            _captchaBase64.value = captchaResult.base64
                            _showCaptcha.value = true
                        }
                        suspendCancellableCoroutine<String> { cont ->
                            captchaContinuation = { code ->
                                if (cont.isActive) cont.resume(code)
                            }
                            cont.invokeOnCancellation {
                                captchaContinuation = null
                            }
                        }
                    }
                )
                _isLoading.value = false
                _showCaptcha.value = false
                _result.value = res

                if (res.success && res.courses != null) {
                    val existing = repository.allCourses.first()
                    val newCourses = res.courses.filter { new ->
                        existing.none { existingCourse ->
                            existingCourse.name == new.name &&
                            existingCourse.dayOfWeek == new.dayOfWeek &&
                            existingCourse.startSlot == new.startSlot
                        }
                    }
                    repository.insertAll(newCourses)
                }
            } catch (e: CancellationException) {
                // ViewModel scope cancelled — ignore silently, this is normal
                _isLoading.value = false
                _showCaptcha.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _showCaptcha.value = false
                _result.value = LoginResult(false, "登录失败：${e.message ?: "未知错误"}")
            }
        }
    }

    fun submitCaptcha(code: String) {
        _showCaptcha.value = false
        captchaContinuation?.invoke(code)
        captchaContinuation = null
    }

    fun refreshCaptcha() {
        val refresher = captchaRefresher ?: return
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _progress.value = "正在刷新验证码..."
                val result = refresher()
                withContext(Dispatchers.Main) {
                    _captchaBase64.value = result.base64
                }
                captchaRefresher = result.refresh
                _progress.value = ""
            } catch (e: CancellationException) {
                // Normal cancellation, ignore
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _progress.value = ""
                }
            }
        }
    }

    fun cancelCaptcha() {
        _showCaptcha.value = false
        captchaContinuation = null
        captchaRefresher = null
        loginJob?.cancel()
        loginJob = null
        _isLoading.value = false
    }

    fun reset() {
        _result.value = null
        _progress.value = ""
        _showCaptcha.value = false
    }

    override fun onCleared() {
        super.onCleared()
        captchaContinuation = null
        captchaRefresher = null
        loginJob?.cancel()
        loginJob = null
    }

    class Factory(
        private val repository: CourseRepository,
        private val client: ZhengfangClient,
        private val schoolRegistry: SchoolRegistry
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            ImportViewModel(repository, client, schoolRegistry) as T
    }
}
