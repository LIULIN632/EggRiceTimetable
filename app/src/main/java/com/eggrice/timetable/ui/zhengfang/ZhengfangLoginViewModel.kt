package com.eggrice.timetable.ui.zhengfang

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.CaptchaResult
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.network.ZhengfangSchool
import com.eggrice.timetable.util.PinyinSortUtil
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * 正方教务「只登录不拉课表」流程的公共状态契约（UI 共享组件只依赖此接口）。
 * 由 ZhengfangLoginViewModel 及其子类实现。
 */
interface ZhengfangLoginState {
    val selectedSchool: StateFlow<School?>
    val searchQuery: StateFlow<String>
    val filteredSchools: StateFlow<List<School>>
    val username: StateFlow<String>
    val password: StateFlow<String>
    val isLoading: StateFlow<Boolean>
    val progress: StateFlow<String>
    val error: StateFlow<String?>
    val showCaptcha: StateFlow<Boolean>
    val captchaBase64: StateFlow<String?>

    fun updateSearch(query: String)
    fun selectSchool(school: School)
    fun updateUsername(v: String)
    fun updatePassword(v: String)
    fun backToSchoolList()
    fun startLogin()
    fun submitCaptcha(code: String)
    fun refreshCaptcha()
    fun cancelCaptcha()
}

/**
 * 正方教务「只登录不拉课表」流程的公共基类（成绩查询 / 修课情况 / 成绩存档共用）：
 * 选学校 → 登录表单 → 验证码挂起恢复 → loginOnly 建立会话。
 *
 * 子类只需实现登录成功后的数据加载（[afterLogin]），并在需要时清理各自的查询结果
 * （[onBackToSchoolList] / [onLogout] / [onLoginStart]）。
 */
abstract class ZhengfangLoginViewModel(
    protected val client: ZhengfangClient,
    protected val schoolRegistry: SchoolRegistry,
    protected val appContainer: AppContainer
) : ViewModel(), ZhengfangLoginState {

    // ── 学校选择 ──
    protected val _selectedSchool = MutableStateFlow<School?>(null)
    override val selectedSchool: StateFlow<School?> = _selectedSchool

    protected val _searchQuery = MutableStateFlow("")
    override val searchQuery: StateFlow<String> = _searchQuery

    override val filteredSchools: StateFlow<List<School>> = combine(
        _searchQuery, appContainer.favoriteSchoolIds, appContainer.customSchools
    ) { query, favIds, customs ->
        val registrySchools = schoolRegistry.filter(JwSystemType.ZHENGFANG, query)
        val matchingCustoms = customs.filter {
            it.jwType == JwSystemType.ZHENGFANG &&
                (query.isBlank() || it.name.contains(query, ignoreCase = true))
        }
        (matchingCustoms + registrySchools).sortedWith(
            compareByDescending<School> { it.id in favIds }
                .thenBy { PinyinSortUtil.sortKey(it.name) }
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // ── 登录表单 ──
    protected val _username = MutableStateFlow("")
    override val username: StateFlow<String> = _username
    protected val _password = MutableStateFlow("")
    override val password: StateFlow<String> = _password
    protected val _isLoading = MutableStateFlow(false)
    override val isLoading: StateFlow<Boolean> = _isLoading
    protected val _progress = MutableStateFlow("")
    override val progress: StateFlow<String> = _progress
    protected val _error = MutableStateFlow<String?>(null)
    override val error: StateFlow<String?> = _error

    // ── 登录态 ──
    protected val _loggedIn = MutableStateFlow(false)
    val loggedIn: StateFlow<Boolean> = _loggedIn

    // ── 验证码（挂起恢复模式，与 ImportViewModel 同款）──
    protected val _captchaBase64 = MutableStateFlow<String?>(null)
    override val captchaBase64: StateFlow<String?> = _captchaBase64
    protected val _showCaptcha = MutableStateFlow(false)
    override val showCaptcha: StateFlow<Boolean> = _showCaptcha
    private var captchaContinuation: ((String) -> Unit)? = null
    private var captchaRefresher: (suspend () -> CaptchaResult)? = null
    private var loginJob: Job? = null

    override fun updateSearch(query: String) { _searchQuery.value = query }

    override fun selectSchool(school: School) {
        _selectedSchool.value = school
        // 自动回填已记忆的账号密码
        val cred = appContainer.loadCredential(school.baseUrl)
        _username.value = cred?.first ?: ""
        _password.value = cred?.second ?: ""
        _error.value = null
    }

    override fun updateUsername(v: String) { _username.value = v }
    override fun updatePassword(v: String) { _password.value = v }

    /** 返回学校列表（换学校/返回），并清理登录与查询状态 */
    override fun backToSchoolList() {
        loginJob?.cancel()
        _selectedSchool.value = null
        _loggedIn.value = false
        _error.value = null
        _progress.value = ""
        _showCaptcha.value = false
        onBackToSchoolList()
    }

    /** 退出登录（保留选中的学校，回到登录表单） */
    fun logout() {
        loginJob?.cancel()
        _loggedIn.value = false
        _error.value = null
        _progress.value = ""
        onLogout()
    }

    /** 只登录建立会话，成功后调用 [afterLogin] 加载各自数据 */
    override fun startLogin() {
        val school = _selectedSchool.value ?: return
        val u = _username.value.trim()
        val p = _password.value
        if (u.isBlank() || p.isBlank()) {
            _error.value = "请输入学号和密码"
            return
        }

        loginJob?.cancel()
        captchaContinuation = null
        captchaRefresher = null

        _isLoading.value = true
        _progress.value = ""
        _error.value = null
        _showCaptcha.value = false
        onLoginStart()

        val zfSchool = ZhengfangSchool(school.name, school.baseUrl.trimEnd('/'), school.isV8)
        loginJob = viewModelScope.launch {
            try {
                val res = client.loginOnly(
                    school = zfSchool,
                    username = u,
                    password = p,
                    onProgress = { msg -> _progress.value = msg },
                    onCaptcha = { captchaResult ->
                        captchaRefresher = captchaResult.refresh
                        withContext(Dispatchers.Main) {
                            _captchaBase64.value = captchaResult.base64
                            _showCaptcha.value = true
                        }
                        suspendCancellableCoroutine<String> { cont ->
                            if (cont.isActive) {
                                captchaContinuation = { code ->
                                    if (cont.isActive && !code.isNullOrBlank()) {
                                        cont.resume(code)
                                    }
                                }
                                cont.invokeOnCancellation { captchaContinuation = null }
                            }
                        }
                    }
                )
                _isLoading.value = false
                _showCaptcha.value = false
                if (res.success) {
                    appContainer.saveCredential(school.baseUrl, u, p)
                    _loggedIn.value = true
                    afterLogin(school)
                } else {
                    _error.value = res.error ?: "登录失败"
                }
            } catch (e: CancellationException) {
                // 正常的协程取消，静默处理
                _isLoading.value = false
                _showCaptcha.value = false
            } catch (e: Exception) {
                _isLoading.value = false
                _showCaptcha.value = false
                _error.value = "登录失败：${e.message ?: "未知错误"}"
            }
        }
    }

    // ── 子类扩展点 ──

    /** 登录成功后的数据加载（在 viewModelScope 中调用） */
    protected open suspend fun afterLogin(school: School) {}

    /** 开始登录时清理各自的旧数据 */
    protected open fun onLoginStart() {}

    /** 返回学校列表时清理各自的查询结果 */
    protected open fun onBackToSchoolList() {}

    /** 退出登录时清理各自的查询结果 */
    protected open fun onLogout() {}

    // ── 验证码操作 ──

    override fun submitCaptcha(code: String) {
        _showCaptcha.value = false
        captchaContinuation?.invoke(code)
        captchaContinuation = null
    }

    override fun refreshCaptcha() {
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
                // 正常取消，忽略
            } catch (e: Exception) {
                withContext(Dispatchers.Main) { _progress.value = "" }
            }
        }
    }

    override fun cancelCaptcha() {
        _showCaptcha.value = false
        captchaContinuation = null
        captchaRefresher = null
        loginJob?.cancel()
        loginJob = null
        _isLoading.value = false
    }

    fun clearError() { _error.value = null }

    override fun onCleared() {
        super.onCleared()
        captchaContinuation = null
        captchaRefresher = null
        loginJob?.cancel()
        loginJob = null
    }
}
