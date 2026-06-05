package com.eggrice.timetable.ui.import_

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.webkit.JavascriptInterface
import android.webkit.WebView
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.School
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.data.repository.CourseRepository
import com.eggrice.timetable.network.CookieStore
import com.eggrice.timetable.network.CourseFields
import com.eggrice.timetable.network.parseSlotRange
import com.eggrice.timetable.network.parseDayHeader
import com.eggrice.timetable.network.isValidCourseName
import com.eggrice.timetable.network.extractCourseFields
import com.eggrice.timetable.network.splitCourseBlocks
import com.eggrice.timetable.network.parseWeeks
import com.eggrice.timetable.network.parseWeeksToString
import com.eggrice.timetable.network.computeWeekType
import com.eggrice.timetable.network.parseBrDelimitedElements
import com.eggrice.timetable.network.parseBrOrNewlineElements
import com.eggrice.timetable.network.detectVerticalLayout
import com.eggrice.timetable.network.buildDayMap
import com.eggrice.timetable.network.filterBodyRows
import com.eggrice.timetable.network.parseHorizontalTable
import com.eggrice.timetable.network.parseVerticalTable
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import java.util.concurrent.TimeUnit

private const val TAG = "WebImport"

/** JSON model matching shiguangschedule's ImportCourseJsonModel from JS adapters. */
data class ImportCourseJson(
    val name: String? = "",
    val teacher: String? = "",
    val position: String? = "",
    val day: Int? = 1,
    val startSection: Int? = null,
    val endSection: Int? = null,
    val weeks: List<Int>? = emptyList(),
    val isCustomTime: Boolean = false,
    val customStartTime: String? = null,
    val customEndTime: String? = null,
    val color: String? = null,
    val remark: String? = null
)

/** JS polyfill injected into WebView before any adapter script runs.
 *  Mirrors shiguangschedule's WebViewUtils.injectAllJavaScript(). */
val ANDROID_BRIDGE_PROMISE_JS = """
(function() {
    window._androidPromiseResolvers = {};
    window._androidPromiseRejectors = {};

    window._resolveAndroidPromise = function(promiseId, result) {
        if (window._androidPromiseResolvers[promiseId]) {
            window._androidPromiseResolvers[promiseId](result);
            delete window._androidPromiseResolvers[promiseId];
            delete window._androidPromiseRejectors[promiseId];
        }
    };

    window._rejectAndroidPromise = function(promiseId, error) {
        if (window._androidPromiseRejectors[promiseId]) {
            window._androidPromiseRejectors[promiseId](new Error(error));
            delete window._androidPromiseResolvers[promiseId];
            delete window._androidPromiseRejectors[promiseId];
        }
    };

    window.AndroidBridgePromise = {
        showAlert: function(title, content, confirmText) {
            return new Promise((resolve, reject) => {
                const promiseId = 'alert_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.showAlert(title, content, confirmText, promiseId);
            });
        },
        showPrompt: function(title, tip, defaultText, validatorJsFunction) {
            return new Promise((resolve, reject) => {
                const promiseId = 'prompt_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.showPrompt(title, tip, defaultText, validatorJsFunction, promiseId);
            });
        },
        showSingleSelection: function(title, itemsJsonString, defaultSelectedIndex) {
            return new Promise((resolve, reject) => {
                const promiseId = 'singleSelect_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.showSingleSelection(title, itemsJsonString, defaultSelectedIndex, promiseId);
            });
        },
        saveImportedCourses: function(coursesJsonString) {
            return new Promise((resolve, reject) => {
                const promiseId = 'saveCourses_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.saveImportedCourses(coursesJsonString, promiseId);
            });
        },
        saveCourseConfig: function(configJsonString) {
            return new Promise((resolve, reject) => {
                const promiseId = 'saveConfig_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.saveCourseConfig(configJsonString, promiseId);
            });
        },
        savePresetTimeSlots: function(timeSlotsJsonString) {
            return new Promise((resolve, reject) => {
                const promiseId = 'saveTimeSlots_' + Date.now() + Math.random().toString(36).substring(2);
                window._androidPromiseResolvers[promiseId] = resolve;
                window._androidPromiseRejectors[promiseId] = reject;
                AndroidBridge.savePresetTimeSlots(timeSlotsJsonString, promiseId);
            });
        }
    };
})();
""".trimIndent()

class WebImportViewModel(
    private val repository: CourseRepository,
    private val schoolRegistry: SchoolRegistry,
    private val schemeIdProvider: () -> Long = { 0L }
) : ViewModel() {

    private val gson = Gson()
    private val handler = Handler(Looper.getMainLooper())

    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool

    /** Custom URL override — when non-empty, this URL is used instead of school.baseUrl. */
    private val _customUrl = MutableStateFlow("")
    val customUrl: StateFlow<String> = _customUrl

    /** Free URL mode — WebView active without a selected school. */
    private val _freeUrlActive = MutableStateFlow(false)
    val freeUrlActive: StateFlow<Boolean> = _freeUrlActive

    /** URL history for free URL mode (max 10 entries). */
    private val _urlHistory = MutableStateFlow<List<String>>(emptyList())
    val urlHistory: StateFlow<List<String>> = _urlHistory

    private var urlHistoryPrefs: SharedPreferences? = null

    val filteredSchools: StateFlow<List<School>> = combine(_searchQuery, _selectedSchool) { query, _ ->
        if (query.isBlank()) schoolRegistry.allSchools
        else schoolRegistry.allSchools.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.city.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _importedCount = MutableStateFlow(0)
    val importedCount: StateFlow<Int> = _importedCount

    @JvmField val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage

    @JvmField val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting

    private val _parseLogs = MutableStateFlow<List<String>>(emptyList())
    val parseLogs: StateFlow<List<String>> = _parseLogs

    private val _detectedCount = MutableStateFlow(0)
    val detectedCount: StateFlow<Int> = _detectedCount

    // Tracks whether HTML capture should auto-save (manual import) or only detect
    private var manualImportMode = false

    /** WebView reference set from UI layer. Used by bridge methods. */
    var webView: WebView? = null

    fun updateSearch(query: String) { _searchQuery.value = query }
    fun selectSchool(school: School) { _selectedSchool.value = school; _freeUrlActive.value = false; _importedCount.value = 0 }
    fun clearSchool() { _selectedSchool.value = null; _freeUrlActive.value = false; _importedCount.value = 0; _detectedCount.value = 0 }
    fun setCustomUrl(url: String) { _customUrl.value = url }
    fun toastShown() { _toastMessage.value = null }

    /** Initialize URL history from SharedPreferences (call once with app context). */
    fun initUrlHistory(context: Context) {
        urlHistoryPrefs = context.getSharedPreferences("web_import_urls", Context.MODE_PRIVATE)
        val json = urlHistoryPrefs?.getString("history", null)
        if (!json.isNullOrEmpty()) {
            try {
                val type = object : TypeToken<List<String>>() {}.type
                _urlHistory.value = Gson().fromJson(json, type)
            } catch (_: Exception) {
                _urlHistory.value = emptyList()
            }
        }
    }

    /** Enter free URL mode — no school selected, load arbitrary教务 URL directly. */
    fun enterFreeUrlMode(url: String) {
        val trimmed = url.trim()
        if (trimmed.isBlank()) return
        _selectedSchool.value = null
        _freeUrlActive.value = true
        _customUrl.value = trimmed
        _importedCount.value = 0
        saveUrlToHistory(trimmed)
    }

    /** Exit free URL mode and return to school list. */
    fun exitFreeUrlMode() {
        _freeUrlActive.value = false
        _customUrl.value = ""
        _selectedSchool.value = null
        _importedCount.value = 0
        _detectedCount.value = 0
    }

    private fun saveUrlToHistory(url: String) {
        val history = _urlHistory.value.toMutableList()
        history.removeAll { it.equals(url, ignoreCase = true) }
        history.add(0, url)
        if (history.size > 10) history.subList(10, history.size).clear()
        _urlHistory.value = history
        urlHistoryPrefs?.edit()?.putString("history", Gson().toJson(history))?.apply()
    }

    /** Remove a URL from history. */
    fun removeUrlFromHistory(url: String) {
        val history = _urlHistory.value.toMutableList()
        history.removeAll { it.equals(url, ignoreCase = true) }
        _urlHistory.value = history
        urlHistoryPrefs?.edit()?.putString("history", Gson().toJson(history))?.apply()
    }

    /** The effective URL to load: customUrl > school.baseUrl. */
    fun getEffectiveUrl(): String? {
        val custom = _customUrl.value.trim()
        if (custom.isNotEmpty()) return custom
        return _selectedSchool.value?.baseUrl?.trimEnd('/')
    }

    private fun addLog(msg: String) {
        Log.d(TAG, msg)
        _parseLogs.value = _parseLogs.value + msg
    }

    // ═══════════════════════════════════════════
    //  Route 1: JS adapter script execution (拾光课表 pattern)
    // ═══════════════════════════════════════════

    /** Inject the AndroidBridgePromise polyfill into the WebView. Call once after page load. */
    fun injectBridgePromise() {
        webView?.evaluateJavascript(ANDROID_BRIDGE_PROMISE_JS, null)
        addLog("已注入 AndroidBridgePromise JS 桥接层")
    }

    /** Load and execute the JS adapter script for the selected school's jwType.
     *  This is the "一键导入" pattern from shiguangschedule. */
    fun executeJsAdapter() {
        viewModelScope.launch {
            if (_isImporting.value) return@launch
            _isImporting.value = true
            _parseLogs.value = emptyList()
            addLog("正在执行 JS 适配器脚本导入...")

            try {
                val jwType = _selectedSchool.value?.jwType
                val scriptName = if (jwType != null) {
                    when (jwType) {
                        JwSystemType.ZHENGFANG -> "zhengfang_01.js"
                        JwSystemType.QIANGZHI -> "zhengfang_01.js"
                        JwSystemType.QINGGUO -> "qingguo_jiaowu_qingguo_01.js"
                        JwSystemType.CHAOXING -> "chaoxing_jiaowu_chaoxing.js"
                        JwSystemType.URP -> "urp_jiaowu_urp_01.js"
                    }
                } else {
                    // Free URL mode — try all adapters, starting with zhengfang (most common)
                    addLog("通用导入模式: 未指定教务类型，使用正方适配器尝试")
                    "zhengfang_01.js"
                }
                val scriptPath = "adapters/scripts/$scriptName"
                addLog("加载适配器脚本: $scriptPath")

                val scriptContent = withContext(Dispatchers.IO) {
                    try {
                        val ctx = webView?.context ?: throw Exception("WebView context unavailable")
                        ctx.assets.open(scriptPath).bufferedReader().use { it.readText() }
                    } catch (e: Exception) {
                        addLog("JS适配器脚本加载失败: ${e.message}，回退到 HTML 解析")
                        null
                    }
                }

                if (scriptContent != null) {
                    addLog("适配器脚本已加载 (${scriptContent.length} 字符)，开始执行")
                    // Execute on main thread via WebView
                    withContext(Dispatchers.Main) {
                        webView?.evaluateJavascript(scriptContent) { result ->
                            addLog("适配器脚本执行完成: ${result ?: "无返回值"}")
                        }
                    }
                } else {
                    // Fallback: capture HTML and parse with Jsoup
                    withContext(Dispatchers.Main) {
                        webView?.evaluateJavascript(
                            "(function(){AndroidBridge.captureHtml(document.documentElement.outerHTML);})();",
                            null
                        )
                    }
                }
            } catch (e: Exception) {
                addLog("JS适配器执行失败: ${e.message}")
                _toastMessage.value = "导入失败：${e.message}"
                _isImporting.value = false
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Route 1b: OkHttp 直接抓取课表页面 (使用 WebView 提取的 Cookie)
    //  对应优化方案中的"WebView 登录 → Cookie → OkHttp 抓取"混合方案
    // ═══════════════════════════════════════════

    /** 使用 WebView 中提取的 Cookie，通过 OkHttp 直接抓取当前页面 HTML 并解析。
     *  比 WebView 内 JS 抓取更快更可靠，适合 JS 适配器失败时的回退方案。 */
    fun fetchCourseViaOkHttp(currentPageUrl: String) {
        viewModelScope.launch {
            if (_isImporting.value) return@launch
            _isImporting.value = true
            _parseLogs.value = emptyList()
            addLog("OkHttp: 尝试直接抓取课表页面...")

            try {
                val cookies = CookieStore.get()
                if (cookies.isNullOrEmpty()) {
                    addLog("OkHttp: 未找到已保存的 Cookie，请先在 WebView 中登录")
                    _toastMessage.value = "请先在教务页面中登录"
                    _isImporting.value = false
                    return@launch
                }
                addLog("OkHttp: 使用已保存的 Cookie (${cookies.length} 字符)")

                val url = currentPageUrl.ifBlank {
                    _selectedSchool.value?.baseUrl?.trimEnd('/') ?: ""
                }
                if (url.isBlank()) {
                    addLog("OkHttp: 无有效 URL")
                    _isImporting.value = false
                    return@launch
                }
                addLog("OkHttp: 请求 $url")

                val request = Request.Builder()
                    .url(url)
                    .addHeader("Cookie", cookies)
                    .addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .build()

                val html = withContext(Dispatchers.IO) {
                    okHttpClient.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            throw Exception("HTTP ${response.code}: ${response.message}")
                        }
                        response.body?.string() ?: throw Exception("响应体为空")
                    }
                }
                addLog("OkHttp: 成功获取页面 (${html.length} 字符)")

                val jwType = _selectedSchool.value?.jwType
                addLog("OkHttp: 教务系统类型: ${jwType?.label ?: "未知"}")
                val courses = withContext(Dispatchers.IO) { parseHtmlToCourses(html, jwType) }
                addLog("OkHttp: 解析完成，共识别 ${courses.size} 门课程")

                if (courses.isNotEmpty()) {
                    courses.forEachIndexed { i, c ->
                        addLog("  ${i + 1}. ${c.name} | ${c.teacher} | ${c.room} | 周${c.dayOfWeek} 第${c.startSlot}-${c.endSlot}节 | 周次=${c.weeks}")
                    }
                    val schemeId = schemeIdProvider()
                    val coursesWithScheme = courses.map { it.copy(schemeId = schemeId) }
                    val inserted = withContext(Dispatchers.IO) { insertCourses(coursesWithScheme) }
                    _importedCount.value = inserted
                    _detectedCount.value = 0
                    addLog("OkHttp: 成功导入 $inserted 门课程")
                } else {
                    addLog("OkHttp: 未检测到课表数据")
                    _toastMessage.value = "未检测到课表，请确认已进入课表页面"
                }
            } catch (e: Exception) {
                addLog("OkHttp 抓取失败: ${e.message}")
                _toastMessage.value = "抓取失败：${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    // ═══════════════════════════════════════════
    //  Route 2: HTML capture + Jsoup parsing (fallback / "识别" flow)
    // ═══════════════════════════════════════════

    /** Called from JS bridge when HTML is captured. Routes to detect or manual-import based on mode. */
    fun onHtmlCaptured(html: String) {
        if (manualImportMode) {
            manualImportMode = false
            manualImportFromHtml(html)
        } else {
            detectFromHtml(html)
        }
    }

    /** Parse HTML and save directly (manual import flow). */
    private fun manualImportFromHtml(html: String) {
        viewModelScope.launch {
            try {
                addLog("手动导入: 开始解析 HTML (${html.length} 字符)")
                val jwType = _selectedSchool.value?.jwType
                addLog("手动导入: 教务系统类型: ${jwType?.label ?: "未知"}")
                val courses = withContext(Dispatchers.IO) { parseHtmlToCourses(html, jwType) }
                addLog("手动导入: 解析完成，共识别 ${courses.size} 门课程")
                if (courses.isNotEmpty()) {
                    courses.forEachIndexed { i, c ->
                        addLog("  ${i + 1}. ${c.name} | ${c.teacher} | ${c.room} | 周${c.dayOfWeek} 第${c.startSlot}-${c.endSlot}节 | 周次=${c.weeks}")
                    }
                    val schemeId = schemeIdProvider()
                    val coursesWithScheme = courses.map { it.copy(schemeId = schemeId) }
                    val inserted = withContext(Dispatchers.IO) { insertCourses(coursesWithScheme) }
                    _importedCount.value = inserted
                    _detectedCount.value = 0
                    addLog("手动导入: 成功导入 $inserted 门课程")
                } else {
                    _toastMessage.value = "未检测到课表，请确认已进入课表页面"
                }
            } catch (e: Exception) {
                addLog("手动导入失败: ${e.message}")
                _toastMessage.value = "导入失败：${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Start manual import: capture HTML then parse + save. */
    fun startManualImport() {
        if (_isImporting.value) return
        _isImporting.value = true
        _importedCount.value = 0
        _parseLogs.value = emptyList()
        manualImportMode = true
        addLog("手动导入: 捕获页面HTML...")
    }

    /** Parse-only detection (used as fallback when JS adapter fails). */
    private fun detectFromHtml(html: String) {
        viewModelScope.launch {
            _parseLogs.value = emptyList()
            _detectedCount.value = 0
            try {
                addLog("识别: 开始解析 HTML (${html.length} 字符)")
                val jwType = _selectedSchool.value?.jwType
                addLog("识别: 教务系统类型: ${jwType?.label ?: "未知"}")
                val courses = withContext(Dispatchers.IO) { parseHtmlToCourses(html, jwType) }
                addLog("识别: 解析完成，共识别 ${courses.size} 门课程")
                _detectedCount.value = courses.size
                if (courses.isNotEmpty()) {
                    courses.forEachIndexed { i, c ->
                        addLog("  ${i + 1}. ${c.name} | ${c.teacher} | ${c.room} | 周${c.dayOfWeek} 第${c.startSlot}-${c.endSlot}节 | 周次=${c.weeks}")
                    }
                    _toastMessage.value = "识别到 ${courses.size} 门课程，点击手动导入课表保存"
                } else {
                    _toastMessage.value = "未检测到课表，请确认已进入课表页面"
                }
            } catch (e: Exception) {
                addLog("识别失败: ${e.message}")
                _toastMessage.value = "识别失败：${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Import courses from JSON (called by JS bridge saveImportedCourses). */
    fun importFromJson(coursesJson: String) {
        viewModelScope.launch {
            try {
                addLog("JS桥接: 收到课程JSON (${coursesJson.length} 字符)")
                val type = object : TypeToken<List<ImportCourseJson>>() {}.type
                val importedCourses: List<ImportCourseJson> = gson.fromJson(coursesJson, type)
                addLog("JS桥接: 解析到 ${importedCourses.size} 门课程")

                val courses = importedCourses.mapNotNull { convertImportCourse(it) }
                if (courses.isEmpty()) {
                    addLog("JS桥接: 转换后无有效课程 (原始=${importedCourses.size}门)")
                    _toastMessage.value = "未识别到有效课程数据"
                    _isImporting.value = false
                    return@launch
                }

                addLog("JS桥接: 转换成功 ${courses.size} 门课程，写入数据库")
                val inserted = withContext(Dispatchers.IO) { insertCourses(courses) }
                _importedCount.value = inserted
                _detectedCount.value = 0
                addLog("JS桥接: 成功导入 $inserted 门课程")
            } catch (e: Exception) {
                val detail = e.stackTraceToString()
                addLog("JS桥接: 导入异常: $detail")
                _toastMessage.value = "导入失败：${e.message ?: e.javaClass.simpleName}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Convert shiguangschedule-format ImportCourseJson to our CourseEntity.
     *  All field accesses are null-guarded because Gson can bypass Kotlin null-safety. */
    private fun convertImportCourse(json: ImportCourseJson): CourseEntity? {
        val name = json.name?.trim() ?: return null
        if (name.isEmpty() || name.length < 2) return null
        val day = json.day.takeIf { it in 1..7 } ?: return null

        val startSlot = json.startSection ?: 1
        val endSlot = json.endSection ?: startSlot
        val weeksList = json.weeks ?: emptyList()
        val weeks = weeksList.sorted().joinToString(",") { it.toString() }
        val weekType = computeWeekType(parseWeeks(weeks))

        return CourseEntity(
            name = name,
            teacher = (json.teacher ?: "").trim(),
            room = (json.position ?: "").trim(),
            dayOfWeek = day,
            startSlot = startSlot,
            endSlot = endSlot,
            weeks = weeks,
            weekType = weekType,
            colorIndex = (day * 3 + startSlot) % 15,
            schemeId = schemeIdProvider()
        )
    }

    // 直接插入（不去重）
    private suspend fun insertCourses(courses: List<CourseEntity>): Int {
        if (courses.isNotEmpty()) {
            repository.insertAll(courses)
        }
        return courses.size
    }

    // ═══════════════════════════════════════════
    //  HTML → Jsoup parsers (fallback when JS adapters unavailable)
    // ═══════════════════════════════════════════

    private fun parseHtmlToCourses(html: String, jwType: JwSystemType?): List<CourseEntity> {
        val doc: Document = Jsoup.parse(html)
        return when (jwType) {
            JwSystemType.ZHENGFANG -> {
                addLog("使用正方教务解析策略")
                parseZhengfang(doc)
            }
            JwSystemType.QIANGZHI -> {
                addLog("使用强智教务解析策略")
                parseQiangzhi(doc)
            }
            JwSystemType.QINGGUO -> {
                addLog("使用青果教务解析策略")
                parseQingguo(doc)
            }
            JwSystemType.CHAOXING -> {
                addLog("使用超星教务解析策略")
                parseChaoxing(doc)
            }
            JwSystemType.URP -> {
                addLog("使用URP教务解析策略")
                parseURP(doc)
            }
            null -> {
                addLog("未指定教务类型，尝试自动检测")
                autoDetectAndParse(doc)
            }
        }
    }

    private fun autoDetectAndParse(doc: Document): List<CourseEntity> {
        val strategies = listOf(
            "正方教务" to { parseZhengfang(doc) },
            "强智教务" to { parseQiangzhi(doc) },
            "青果教务" to { parseQingguo(doc) },
            "超星教务" to { parseChaoxing(doc) },
            "URP教务" to { parseURP(doc) }
        )
        var best = emptyList<CourseEntity>()
        for ((name, parser) in strategies) {
            val result = parser()
            addLog("  $name 检测到 ${result.size} 门课程")
            if (result.size > best.size) best = result
        }
        if (best.isEmpty()) {
            addLog("  尝试通用解析...")
            best = parseGeneric(doc)
            addLog("  通用解析检测到 ${best.size} 门课程")
        }
        return best
    }

    // ── Zhengfang parser ──

    private fun parseZhengfang(doc: Document): List<CourseEntity> {
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#Table1").firstOrNull()
            ?: doc.select("table[id*=Table1]").firstOrNull()
            ?: doc.select("table[id*=kbcx]").firstOrNull()
            ?: doc.select("table[id*=kb]").firstOrNull()
            ?: doc.select("table.datelist").firstOrNull()
            ?: doc.select("table.table").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return emptyList()

        addLog("正方: 找到课表表格 (class=${table.className()})")

        val allRows = table.select("tr")
        if (allRows.size < 2) return emptyList()

        val (dayMap, headerRowIdx) = buildDayMap(allRows.take(4))
        addLog("正方: dayMap cols=$dayMap, headerRow=$headerRowIdx")
        if (dayMap.size < 3) addLog("正方: 星期头不足，使用默认列映射 col1=周一..col7=周日")

        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        val (bodyRows, rowSlotRanges) = filterBodyRows(allRows, headerRowIdx)
        if (bodyRows.isEmpty()) return emptyList()

        val isVertical = detectVerticalLayout(bodyRows)
        addLog("正方: ${if (isVertical) "竖排" else "横排"}布局, bodyRows=${bodyRows.size}")

        val courses = if (isVertical) parseVerticalTable(bodyRows, dayMap, colors)
        else parseHorizontalTable(bodyRows, dayMap, colOffset, colors, rowSlotRanges)

        addLog("正方: 解析到 ${courses.size} 门课程")
        return courses
    }

    private fun parseQiangzhi(doc: Document): List<CourseEntity> {
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#kbtable").firstOrNull()
            ?: doc.select("table[id*=kbtable]").firstOrNull()
            ?: doc.select("table#gridview").firstOrNull()
            ?: doc.select("table.grid").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return emptyList()

        addLog("强智: 找到课表表格")

        val allRows = table.select("tr")
        if (allRows.size < 2) return emptyList()

        val dayMap = buildDayMap(listOf(allRows[0])).first.toMutableMap()
        if (dayMap.isEmpty()) { for (i in 1..7) dayMap[i] = i; addLog("强智: 未找到星期头，使用默认列映射") }

        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        val rowsMinusHeader = allRows.drop(1)
        val (bodyRows, rowSlotRanges) = filterBodyRows(rowsMinusHeader, -1)

        val courses = parseHorizontalTable(bodyRows, dayMap, colOffset, colors, rowSlotRanges)
        addLog("强智: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── Qingguo parser ──

    private fun parseQingguo(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val tables = doc.select("table")
        for (table in tables) {
            if (table.select("tr").size < 3) continue
            for (cell in table.select("td")) {
                val text = cell.text().trim()
                if (text.length < 4) continue
                if (Regex("""^[\d\s.,;:：.，、；：]+$""").matches(text)) continue

                val textLines = parseBrOrNewlineElements(cell)
                if (textLines.size < 2) continue
                parseGenericCourse(textLines, text, colors, courses)
            }
            if (courses.isNotEmpty()) break
        }
        addLog("青果: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── URP parser ──

    private fun parseURP(doc: Document): List<CourseEntity> {
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#Table1").firstOrNull()
            ?: doc.select("table#ctl00_ContentPlaceHolder1_Table1").firstOrNull()
            ?: doc.select("table#ctl00_ContentPlaceHolder1_gridView").firstOrNull()
            ?: doc.select("table.datelist").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return emptyList()

        addLog("URP: 找到课表表格")

        val allRows = table.select("tr")
        if (allRows.size < 2) return emptyList()

        val dayMap = buildDayMap(listOf(allRows[0])).first.toMutableMap()
        if (dayMap.isEmpty()) for (i in 1..7) dayMap[i] = i

        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        val rowsMinusHeader = allRows.drop(1)
        val (bodyRows, rowSlotRanges) = filterBodyRows(rowsMinusHeader, -1)

        val courses = parseHorizontalTable(bodyRows, dayMap, colOffset, colors, rowSlotRanges)
        addLog("URP: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── Chaoxing parser ──

    private fun parseChaoxing(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val courseCards = doc.select("[class*=course], [class*=kc], [class*=schedule]")
        for (card in courseCards) {
            val text = card.text().trim()
            if (text.length < 4) continue
            val textLines = parseBrOrNewlineElements(card)
            if (textLines.size >= 2) parseGenericCourse(textLines, text, colors, courses)
        }

        if (courses.isEmpty()) courses.addAll(parseGeneric(doc))

        addLog("超星: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── Generic fallback ──

    private fun parseGeneric(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        for (td in doc.select("td")) {
            val text = td.text().trim()
            if (text.length < 4) continue
            if (Regex("""^[\d\s.,;:：.\-/]+$""").matches(text)) continue

            val textLines = parseBrOrNewlineElements(td)
            if (textLines.size >= 2) parseGenericCourse(textLines, text, colors, courses)
        }
        return courses
    }

    private fun parseGenericCourse(
        lines: List<String>, fullText: String, colors: List<Int>, courses: MutableList<CourseEntity>
    ) {
        val (name, teacher, room, weeksRaw) = extractCourseFields(lines)
        if (name.isEmpty() || name.length < 2) return

        val wm = Regex("""(\d+)\s*[-–]\s*(\d+)\s*周""").find(fullText)
        val dm = Regex("""周([一二三四五六日])""").find(fullText)
        val sm = Regex("""第(\d+)[\s-]*(\d+)?节""").find(fullText)

        val day = when (dm?.groupValues?.get(1)) {
            "一" -> 1; "二" -> 2; "三" -> 3; "四" -> 4; "五" -> 5; "六" -> 6; "日" -> 7; else -> return
        }
        val startSlot = sm?.groupValues?.get(1)?.toIntOrNull() ?: return
        val endSlot = sm?.groupValues?.get(2)?.toIntOrNull() ?: startSlot
        val weeks = if (wm != null) {
            val ws = wm.groupValues[1].toIntOrNull() ?: 1
            val we = wm.groupValues[2].toIntOrNull() ?: 16
            (ws..we).joinToString(",")
        } else {
            parseWeeksToString(weeksRaw)
        }

        courses.add(CourseEntity(
            name = name, teacher = teacher, room = room,
            dayOfWeek = day, startSlot = startSlot, endSlot = endSlot,
            weeks = weeks, colorIndex = colors[courses.size % colors.size]
        ))
    }

    class Factory(
        private val repository: CourseRepository,
        private val schoolRegistry: SchoolRegistry,
        private val schemeIdProvider: () -> Long = { 0L }
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            WebImportViewModel(repository, schoolRegistry, schemeIdProvider) as T
    }
}

// ═══════════════════════════════════════════
//  Enhanced ScheduleBridge — full JS-native bridge
//  Implements the shiguangschedule AndroidBridge pattern for two-way communication
// ═══════════════════════════════════════════

/** Bridge injected as "AndroidBridge" in WebView.
 *  Supports both simple HTML capture AND the full AndroidBridgePromise pattern. */
class ScheduleBridge(
    private val viewModel: WebImportViewModel? = null,
    private val onHtmlCaptured: ((String) -> Unit)? = null
) {
    private val handler = Handler(Looper.getMainLooper())

    /** Simple HTML capture (legacy "识别" flow). */
    @JavascriptInterface
    fun captureHtml(html: String) {
        handler.post {
            onHtmlCaptured?.invoke(html)
        }
    }

    /** Show a toast from JS. */
    @JavascriptInterface
    fun showToast(message: String) {
        handler.post {
            viewModel?._toastMessage?.value = message
        }
    }

    /** Show alert dialog — resolves via promise. */
    @JavascriptInterface
    fun showAlert(titleText: String, contentText: String, confirmText: String, promiseId: String) {
        handler.post {
            // Auto-confirm in import context — user already chose to import
            resolveJsPromise(promiseId, "true")
        }
    }

    /** Show prompt dialog — auto-resolves with empty (cancel). */
    @JavascriptInterface
    fun showPrompt(
        titleText: String, tipText: String, defaultText: String,
        validatorJsFunction: String, promiseId: String
    ) {
        handler.post {
            resolveJsPromise(promiseId, "null")
        }
    }

    /** Show single selection — auto-resolves with -1 (cancel). */
    @JavascriptInterface
    fun showSingleSelection(
        titleText: String, itemsJsonString: String,
        defaultSelectedIndex: Int, promiseId: String
    ) {
        handler.post {
            resolveJsPromise(promiseId, "null")
        }
    }

    /** Save courses from JS adapter — the key bridge method. */
    @JavascriptInterface
    fun saveImportedCourses(coursesJsonString: String, promiseId: String) {
        Log.d(TAG, "Bridge: 收到 JS 课程数据 (${coursesJsonString.length} 字符)")
        handler.post {
            viewModel?.let { vm ->
                vm.importFromJson(coursesJsonString)
                // mark importing done after importFromJson processes
            }
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun saveCourseConfig(configJsonString: String, promiseId: String) {
        handler.post {
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun savePresetTimeSlots(timeSlotsJsonString: String, promiseId: String) {
        handler.post {
            resolveJsPromise(promiseId, "true")
        }
    }

    @JavascriptInterface
    fun notifyTaskCompletion() {
        handler.post {
            viewModel?._isImporting?.value = false
        }
    }

    private fun resolveJsPromise(promiseId: String, result: String) {
        viewModel?.webView?.evaluateJavascript(
            "window._resolveAndroidPromise('$promiseId', $result);", null
        )
    }
}
