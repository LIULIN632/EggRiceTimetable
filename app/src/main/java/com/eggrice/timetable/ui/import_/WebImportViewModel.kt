package com.eggrice.timetable.ui.import_

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
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element

private const val TAG = "WebImport"

/** JSON model matching shiguangschedule's ImportCourseJsonModel from JS adapters. */
data class ImportCourseJson(
    val name: String = "",
    val teacher: String = "",
    val position: String = "",
    val day: Int = 1,
    val startSection: Int? = null,
    val endSection: Int? = null,
    val weeks: List<Int> = emptyList(),
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

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery

    private val _selectedSchool = MutableStateFlow<School?>(null)
    val selectedSchool: StateFlow<School?> = _selectedSchool

    /** Custom URL override — when non-empty, this URL is used instead of school.baseUrl. */
    private val _customUrl = MutableStateFlow("")
    val customUrl: StateFlow<String> = _customUrl

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
    fun selectSchool(school: School) { _selectedSchool.value = school; _importedCount.value = 0 }
    fun clearSchool() { _selectedSchool.value = null; _importedCount.value = 0; _detectedCount.value = 0 }
    fun setCustomUrl(url: String) { _customUrl.value = url }
    fun toastShown() { _toastMessage.value = null }

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
                val jwType = _selectedSchool.value?.jwType ?: JwSystemType.ZHENGFANG
                val scriptName = when (jwType) {
                    JwSystemType.ZHENGFANG -> "zhengfang_01.js"
                    JwSystemType.QIANGZHI -> "zhengfang_01.js" // shares patterns
                    JwSystemType.QINGGUO -> "qingguo_jiaowu_qingguo_01.js"
                    JwSystemType.CHAOXING -> "chaoxing_jiaowu_chaoxing.js"
                    JwSystemType.URP -> "urp_jiaowu_urp_01.js"
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
                    addLog("JS桥接: 转换后无有效课程")
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
                addLog("JS桥接: JSON解析失败: ${e.message}")
                _toastMessage.value = "导入失败：${e.message}"
            } finally {
                _isImporting.value = false
            }
        }
    }

    /** Convert shiguangschedule-format ImportCourseJson to our CourseEntity. */
    private fun convertImportCourse(json: ImportCourseJson): CourseEntity? {
        val name = json.name.trim()
        if (name.isEmpty() || name.length < 2) return null
        if (json.day < 1 || json.day > 7) return null

        val startSlot = json.startSection ?: 1
        val endSlot = json.endSection ?: startSlot
        val weeks = json.weeks.sorted().joinToString(",") { it.toString() }
        val weekType = detectWeekType(weeks)

        return CourseEntity(
            name = name,
            teacher = json.teacher.trim(),
            room = json.position.trim(),
            dayOfWeek = json.day,
            startSlot = startSlot,
            endSlot = endSlot,
            weeks = weeks,
            weekType = weekType,
            colorIndex = (json.day * 3 + startSlot) % 15,
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
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#Table1").firstOrNull()
            ?: doc.select("table[id*=Table1]").firstOrNull()
            ?: doc.select("table[id*=kbcx]").firstOrNull()
            ?: doc.select("table[id*=kb]").firstOrNull()
            ?: doc.select("table.datelist").firstOrNull()
            ?: doc.select("table.table").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return courses

        addLog("正方: 找到课表表格 (class=${table.className()})")

        val allRows = table.select("tr")
        if (allRows.size < 2) return courses

        // Find day-header row
        var headerRowIdx = -1
        val dayMap = mutableMapOf<Int, Int>()
        for (ri in 0 until minOf(allRows.size, 4)) {
            val cells = allRows[ri].select("td, th")
            cells.forEachIndexed { col, cell ->
                val text = cell.text().trim()
                val day = when {
                    text.matches(Regex("星期\\s*一|周一|^一$")) -> 1
                    text.matches(Regex("星期\\s*二|周二|^二$")) -> 2
                    text.matches(Regex("星期\\s*三|周三|^三$")) -> 3
                    text.matches(Regex("星期\\s*四|周四|^四$")) -> 4
                    text.matches(Regex("星期\\s*五|周五|^五$")) -> 5
                    text.matches(Regex("星期\\s*六|周六|^六$")) -> 6
                    text.matches(Regex("星期\\s*日|周日|星期\\s*天|周天|^日$")) -> 7
                    else -> null
                }
                if (day != null && day !in dayMap.values) dayMap[col] = day
            }
            if (dayMap.size >= 5) { headerRowIdx = ri; break }
        }

        addLog("正方: dayMap cols=$dayMap, headerRow=$headerRowIdx")

        if (dayMap.size < 3) {
            addLog("正方: 星期头不足(${dayMap.size}<3)，使用默认列映射 col1=周一..col7=周日")
            dayMap.clear()
            for (i in 1..7) dayMap[i] = i
        }

        val bodyStart = maxOf(headerRowIdx + 1, 1)
        val bodyRows = allRows.drop(bodyStart).filter { row ->
            val cells = row.select("td, th")
            if (cells.isEmpty()) return@filter false
            val allText = cells.map { it.text().trim() }.filter { it.isNotBlank() }
            if (allText.isEmpty()) return@filter false
            if (isMonthOrDateRow(cells)) return@filter false
            true
        }

        if (bodyRows.isEmpty()) return courses

        val isVertical = detectVertical(bodyRows)
        addLog("正方: ${if (isVertical) "竖排" else "横排"}布局, bodyRows=${bodyRows.size}")

        if (isVertical) parseZhengfangVertical(bodyRows, dayMap, colors, courses)
        else parseZhengfangHorizontal(bodyRows, dayMap, colors, courses)

        addLog("正方: 解析到 ${courses.size} 门课程")
        return courses
    }

    private fun isMonthOrDateRow(cells: List<Element>): Boolean {
        val texts = cells.map { it.text().trim() }.filter { it.isNotBlank() }
        if (texts.isEmpty()) return true
        val firstText = cells.firstOrNull()?.text()?.trim() ?: ""
        if (parseSlotLabel(firstText) != null) return false
        val monthDateCount = texts.count { isMonthOrDateCell(it) }
        return monthDateCount > texts.size / 2 && texts.size >= 3
    }

    private fun isMonthOrDateCell(text: String): Boolean {
        if (text.isBlank()) return true
        if (Regex("""^\d{1,2}\s*月$""").matches(text)) return true
        if (Regex("""^\d{1,2}/\d{1,2}$""").matches(text)) return true
        if (Regex("""^第\s*\d+\s*周$""").matches(text)) return true
        if (Regex("""^\d{4}$""").matches(text)) return true
        if (text in listOf("上午", "下午", "晚上", "早晨", "中午")) return true
        return false
    }

    private fun detectVertical(bodyRows: List<Element>): Boolean {
        var slotCount = 0
        for (i in 0 until minOf(6, bodyRows.size)) {
            val first = bodyRows[i].select("td, th").firstOrNull()?.text()?.trim() ?: continue
            if (parseSlotLabel(first) != null) {
                slotCount++
            } else if (Regex("""^\s*\d+\s*$""").matches(first) && first.toIntOrNull() in 1..12) {
                slotCount++
            } else if (Regex("""节|课""").containsMatchIn(first)) {
                slotCount++
            }
        }
        return slotCount >= 2
    }

    private fun isValidCourseName(name: String): Boolean {
        if (name.length < 2 || name.length > 25) return false
        if (Regex("""^[\d\s./\-月日周星期]+$""").matches(name)) return false
        if (name in listOf("无", "备注", "节次", "时间", "课程", "教师", "教室")) return false
        if (!Regex("""[一-龥A-Za-z]""").containsMatchIn(name)) return false
        return true
    }

    private fun parseZhengfangVertical(
        bodyRows: List<Element>,
        dayMap: Map<Int, Int>,
        colors: List<Int>,
        courses: MutableList<CourseEntity>
    ) {
        addLog("正方竖排: 开始按列解析 ${bodyRows.size} 行, dayMap=$dayMap")

        // Build clean column→day mapping with position-based fallback
        // Column 0 = slot labels, columns 1+ = Mon-Sun (unless overridden by dayMap)
        val maxCols = bodyRows.maxOfOrNull { row -> row.select("td, th").size } ?: return
        val colToDay = mutableMapOf<Int, Int>()
        for (col in 1 until maxCols) {
            colToDay[col] = dayMap[col] ?: col.coerceIn(1, 7)
        }
        addLog("正方竖排: maxCols=$maxCols, colToDay=$colToDay")

        // Pre-scan: collect slot ranges for each row (for rowSpan lookahead)
        val rowSlotRanges = mutableListOf<Pair<Int, Int>>()
        for (row in bodyRows) {
            val firstText = row.select("td, th").firstOrNull()?.text()?.trim() ?: ""
            val slotRange = parseSlotLabel(firstText)
            rowSlotRanges.add(slotRange ?: (-1 to -1))
        }

        // Process each day column independently
        for ((dayCol, day) in colToDay) {
            if (day !in 1..7) continue
            var coursesInCol = 0
            for (rowIdx in bodyRows.indices) {
                val row = bodyRows[rowIdx]
                val cells = row.select("td, th")
                if (dayCol >= cells.size) continue

                // Get slot from first column
                val slotRange = rowSlotRanges.getOrNull(rowIdx)
                if (slotRange == null || slotRange.first < 0) continue
                val (startSlot, endSlot) = slotRange

                val cell = cells[dayCol]
                val text = cell.text().trim()
                if (text.isBlank() || text.length < 2) continue

                // Parse cell content
                val brLines = cell.html().split(Regex("(?i)<br\\s*/?>")).map {
                    Jsoup.parse(it).text().trim()
                }.filter { it.isNotBlank() }

                val contentLines = if (brLines.size >= 2) brLines
                else text.split("\n", "\r\n").map { it.trim() }.filter { it.isNotBlank() }

                if (contentLines.isEmpty()) continue

                // Handle rowSpan
                val rowSpan = cell.attr("rowspan").toIntOrNull() ?: 1
                val cellEndSlot = if (rowSpan > 1) {
                    val lookAheadIdx = rowIdx + rowSpan - 1
                    if (lookAheadIdx < rowSlotRanges.size) {
                        val lastRange = rowSlotRanges[lookAheadIdx]
                        if (lastRange.first > 0) lastRange.second else endSlot + rowSpan - 1
                    } else endSlot + rowSpan - 1
                } else endSlot

                val blocks = splitMultipleCourses(contentLines)
                for (block in blocks) {
                    if (block.isEmpty()) continue
                    val (name, teacher, room, weeksRaw) = extractCourseFields(block)
                    if (name.isEmpty() || name.length < 2) continue

                    val weeks = parseWeekRange(weeksRaw)
                    val weekType = detectWeekType(weeks, weeksRaw)

                    courses.add(CourseEntity(
                        name = name, teacher = teacher, room = room,
                        dayOfWeek = day, startSlot = startSlot, endSlot = cellEndSlot,
                        weeks = weeks, colorIndex = colors[courses.size % colors.size],
                        weekType = weekType
                    ))
                    coursesInCol++
                }
            }
            if (coursesInCol > 0) addLog("正方竖排: 星期$day 解析到 $coursesInCol 门课程")
        }
    }

    /** Extract (name, teacher, room, weeks) from course text lines.
     *  FIXED: widened teacher regex to 2-10 chars, supports mixed charset names. */
    private fun extractCourseFields(lines: List<String>): CourseFields {
        if (lines.isEmpty()) return CourseFields("", "", "", "")
        val name = lines[0]

        var teacher = ""
        var room = ""
        var weeksRaw = ""

        for (i in 1 until lines.size) {
            val line = lines[i].trim()
            if (line.isEmpty()) continue

            // Skip credit/score lines
            if (Regex("""学分|绩点|考核""").containsMatchIn(line)) continue
            if (Regex("""^\d+\.?\d*\s*(学分)?$""").matches(line)) continue

            // Weeks
            if (Regex("""周""").containsMatchIn(line)) {
                if (weeksRaw.isEmpty()) weeksRaw = line
                continue
            }
            // Room: has digits + building suffix
            if (Regex("""\d""").containsMatchIn(line) && room.isEmpty()) {
                room = line.removePrefix("@")
                continue
            }
            // Teacher: widened to 2-10 chars, supports mixed charset (e.g. "欧阳修", "王John")
            if (Regex("""^[一-鿿A-Za-z·]{2,10}$""").matches(line) && teacher.isEmpty()) {
                teacher = line
            }
        }

        return CourseFields(name, teacher, room, weeksRaw)
    }

    private data class CourseFields(val name: String, val teacher: String, val room: String, val weeks: String)

    private fun splitMultipleCourses(lines: List<String>): List<List<String>> {
        if (lines.isEmpty()) return emptyList()
        if (lines.size <= 6) return listOf(lines)

        val blocks = mutableListOf<List<String>>()
        val current = mutableListOf<String>()

        for (line in lines) {
            val looksLikeName = line.length in 2..18 &&
                !line.contains("周") &&
                !Regex("""^\d|^@|^第|^星期|^[\d\s./\-]+$""").containsMatchIn(line) &&
                Regex("""[一-龥A-Za-z]""").containsMatchIn(line)

            if (looksLikeName && current.isNotEmpty() && current.size >= 2) {
                blocks.add(current.toList())
                current.clear()
            }
            current.add(line)
        }
        if (current.isNotEmpty()) blocks.add(current.toList())
        return if (blocks.size > 1) blocks else listOf(lines)
    }

    /** Parse slot labels including bare digits (YIT vertical timetable uses "1", "2", etc.). */
    private fun parseSlotLabel(text: String): Pair<Int, Int>? {
        Regex("""第\s*(\d+)\s*[-–~至]\s*(\d+)\s*节""").find(text)?.let {
            return Pair(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }
        Regex("""第\s*(\d+)\s*节""").find(text)?.let {
            val s = it.groupValues[1].toInt()
            return Pair(s, s)
        }
        Regex("""^(\d+)\s*[-–~]\s*(\d+)\s*节?$""").find(text)?.let {
            return Pair(it.groupValues[1].toInt(), it.groupValues[2].toInt())
        }
        // Bare digit — key for YIT vertical timetable
        val bareDigit = text.trim()
        val num = bareDigit.toIntOrNull()
        if (num != null && num in 1..12) {
            return Pair(num, num)
        }
        val bigSlots = mapOf(
            "一" to (1 to 2), "二" to (3 to 4), "三" to (5 to 6),
            "四" to (7 to 8), "五" to (9 to 10), "六" to (11 to 12)
        )
        Regex("""第([一二三四五六])大节""").find(text)?.let {
            return bigSlots[it.groupValues[1]]
        }
        return null
    }

    private fun parseZhengfangHorizontal(
        bodyRows: List<Element>,
        dayMap: Map<Int, Int>,
        colors: List<Int>,
        courses: MutableList<CourseEntity>
    ) {
        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        var slotIndex = 0
        // Pre-scan rows to build slot-range index for rowSpan lookahead
        val rowSlotRanges = mutableListOf<Pair<Int, Int>>()
        var runningSlot = 0
        for (row in bodyRows) {
            val firstText = row.select("td, th").firstOrNull()?.text()?.trim() ?: ""
            val parsed = parseSlotLabel(firstText)
            if (parsed != null) {
                runningSlot = parsed.first
                rowSlotRanges.add(parsed)
            } else {
                val isHeader = firstText.isNotEmpty() &&
                    (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                     firstText.contains("早晨") || firstText.contains("中午"))
                if (!isHeader) runningSlot++
                rowSlotRanges.add(if (isHeader) -1 to -1 else runningSlot to runningSlot)
            }
        }

        var dataRowIdx = 0
        for (row in bodyRows) {
            val cells = row.select("td, th")
            if (cells.isEmpty()) { dataRowIdx++; continue }

            val firstText = cells[0].text().trim()
            val slotFromLabel = parseSlotLabel(firstText)
            val isSectionHeader = firstText.isNotEmpty() &&
                (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                 firstText.contains("早晨") || firstText.contains("中午"))

            if (slotFromLabel != null) {
                slotIndex = slotFromLabel.first
            } else if (!isSectionHeader) {
                slotIndex++
            }

            cells.forEachIndexed { ci, cell ->
                if (ci == 0) return@forEachIndexed
                if (isSectionHeader) return@forEachIndexed
                val day = dayMap[ci - colOffset]
                    ?: (ci - colOffset).takeIf { it in 1..7 }
                    ?: return@forEachIndexed
                val text = cell.text().trim()
                if (text.isBlank() || text.length < 1) return@forEachIndexed
                if (text.matches(Regex("""^\d{1,2}\s*月$""")) || text.matches(Regex("""^\d{1,2}/\d{1,2}$"""))) return@forEachIndexed

                val rowSpan = cell.attr("rowspan").toIntOrNull() ?: 1

                // Use slot range from label for accurate endSlot; rowSpan extends further
                val labelEnd = slotFromLabel?.second ?: slotIndex
                val endSlot = if (rowSpan > 1) {
                    val lookAheadIdx = dataRowIdx + rowSpan - 1
                    if (lookAheadIdx < rowSlotRanges.size) {
                        val lastRange = rowSlotRanges[lookAheadIdx]
                        if (lastRange.first > 0) lastRange.second else labelEnd + rowSpan - 1
                    } else labelEnd + rowSpan - 1
                } else {
                    maxOf(labelEnd, slotIndex)
                }

                val brLines = cell.html().split(Regex("(?i)<br\\s*/?>")).map {
                    Jsoup.parse(it).text().trim()
                }.filter { it.isNotBlank() }

                if (brLines.isEmpty()) return@forEachIndexed

                val (name, teacher, room, weeksRaw) = extractCourseFields(brLines)
                if (name.isEmpty() || name.length < 2) return@forEachIndexed

                val weeks = parseWeekRange(weeksRaw)
                val weekType = detectWeekType(weeks, weeksRaw)

                courses.add(CourseEntity(
                    name = name, teacher = teacher, room = room,
                    dayOfWeek = day, startSlot = slotIndex, endSlot = endSlot,
                    weeks = weeks, colorIndex = colors[courses.size % colors.size],
                    weekType = weekType
                ))
            }
            dataRowIdx++
        }
    }

    // ── Qiangzhi parser ──

    private fun parseQiangzhi(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#kbtable").firstOrNull()
            ?: doc.select("table[id*=kbtable]").firstOrNull()
            ?: doc.select("table#gridview").firstOrNull()
            ?: doc.select("table.grid").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return courses

        addLog("强智: 找到课表表格")

        val rows = table.select("tr")
        if (rows.size < 2) return courses

        val headerCells = rows[0].select("td, th")
        val dayMap = mutableMapOf<Int, Int>()
        headerCells.forEachIndexed { col, cell ->
            val text = cell.text().trim()
            when {
                text.contains("一") -> dayMap[col] = 1
                text.contains("二") -> dayMap[col] = 2
                text.contains("三") -> dayMap[col] = 3
                text.contains("四") -> dayMap[col] = 4
                text.contains("五") -> dayMap[col] = 5
                text.contains("六") -> dayMap[col] = 6
                text.contains("日") -> dayMap[col] = 7
            }
        }

        if (dayMap.isEmpty()) {
            for (i in 1..7) dayMap[i] = i
            addLog("强智: 未找到星期头，使用默认列映射")
        }

        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        var slotIndex = 0
        // Pre-scan rows for rowSpan lookahead
        val rowSlotRanges = mutableListOf<Pair<Int, Int>>()
        var runningSlot = 0
        for (ri in 1 until rows.size) {
            val cells = rows[ri].select("td, th")
            val firstText = cells.firstOrNull()?.text()?.trim() ?: ""
            val parsed = parseSlotLabel(firstText)
            if (parsed != null) {
                runningSlot = parsed.first
                rowSlotRanges.add(parsed)
            } else {
                val isHeader = firstText.isNotEmpty() &&
                    (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                     firstText.contains("早晨") || firstText.contains("中午"))
                if (!isHeader) runningSlot++
                rowSlotRanges.add(if (isHeader) -1 to -1 else runningSlot to runningSlot)
            }
        }

        var dataRowIdx = 0
        for (ri in 1 until rows.size) {
            val cells = rows[ri].select("td, th")
            if (cells.isEmpty()) { dataRowIdx++; continue }

            val firstText = cells[0].text().trim()
            val slotFromLabel = parseSlotLabel(firstText)
            val isSectionHeader = firstText.isNotEmpty() &&
                (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                 firstText.contains("早晨") || firstText.contains("中午"))

            if (slotFromLabel != null) {
                slotIndex = slotFromLabel.first
            } else if (!isSectionHeader) {
                slotIndex++
            }

            cells.forEachIndexed { ci, cell ->
                if (ci == 0) return@forEachIndexed
                if (isSectionHeader) return@forEachIndexed
                val day = dayMap[ci - colOffset]
                    ?: (ci - colOffset).takeIf { it in 1..7 }
                    ?: return@forEachIndexed
                val text = cell.text().trim()
                if (text.isBlank() || text.length < 2) return@forEachIndexed

                val rowSpan = cell.attr("rowspan").toIntOrNull() ?: 1
                val labelEnd = slotFromLabel?.second ?: slotIndex
                val endSlot = if (rowSpan > 1) {
                    val lookAheadIdx = dataRowIdx + rowSpan - 1
                    if (lookAheadIdx < rowSlotRanges.size) {
                        val lastRange = rowSlotRanges[lookAheadIdx]
                        if (lastRange.first > 0) lastRange.second else labelEnd + rowSpan - 1
                    } else labelEnd + rowSpan - 1
                } else {
                    maxOf(labelEnd, slotIndex)
                }

                val lines = cell.html().split(Regex("(?i)<br\\s*/?>")).map {
                    Jsoup.parse(it).text().trim()
                }.filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    val textLines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    if (textLines.size < 2) return@forEachIndexed
                    parseGenericCourse(textLines, text, colors, courses)
                    return@forEachIndexed
                }

                val (name, teacher, room, weeksRaw) = extractCourseFields(lines)
                if (name.isEmpty()) return@forEachIndexed

                val weeks = parseWeekRange(weeksRaw)
                val weekType = detectWeekType(weeks, weeksRaw)

                courses.add(CourseEntity(
                    name = name, teacher = teacher, room = room,
                    dayOfWeek = day, startSlot = slotIndex, endSlot = endSlot,
                    weeks = weeks, colorIndex = colors[courses.size % colors.size],
                    weekType = weekType
                ))
            }
            dataRowIdx++
        }
        addLog("强智: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── Qingguo parser ──

    private fun parseQingguo(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val tables = doc.select("table")
        for (table in tables) {
            val rows = table.select("tr")
            if (rows.size < 3) continue

            val cells = table.select("td")
            for (cell in cells) {
                val text = cell.text().trim()
                if (text.length < 4) continue
                if (Regex("""^[\d\s.,;:：.，、；：]+$""").matches(text)) continue

                val lines = cell.html().split(Regex("(?i)<br\\s*/?>")).map {
                    Jsoup.parse(it).text().trim()
                }.filter { it.isNotBlank() }

                val textLines = if (lines.size >= 2) lines
                else text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

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
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        val table = doc.select("table#Table1").firstOrNull()
            ?: doc.select("table#ctl00_ContentPlaceHolder1_Table1").firstOrNull()
            ?: doc.select("table#ctl00_ContentPlaceHolder1_gridView").firstOrNull()
            ?: doc.select("table.datelist").firstOrNull()
            ?: doc.select("table").firstOrNull()
            ?: return courses

        addLog("URP: 找到课表表格")

        val rows = table.select("tr")
        if (rows.size < 2) return courses

        val headerCells = rows[0].select("td, th")
        val dayMap = mutableMapOf<Int, Int>()
        headerCells.forEachIndexed { col, cell ->
            val text = cell.text().trim()
            when {
                text.contains("一") -> dayMap[col] = 1
                text.contains("二") -> dayMap[col] = 2
                text.contains("三") -> dayMap[col] = 3
                text.contains("四") -> dayMap[col] = 4
                text.contains("五") -> dayMap[col] = 5
                text.contains("六") -> dayMap[col] = 6
                text.contains("日") -> dayMap[col] = 7
            }
        }

        if (dayMap.isEmpty()) {
            for (i in 1..7) dayMap[i] = i
        }

        val colOffset = if (dayMap.containsKey(0)) 1 else 0
        var slotIndex = 0
        // Pre-scan rows for rowSpan lookahead
        val rowSlotRanges = mutableListOf<Pair<Int, Int>>()
        var runningSlot = 0
        for (ri in 1 until rows.size) {
            val cells = rows[ri].select("td, th")
            val firstText = cells.firstOrNull()?.text()?.trim() ?: ""
            val parsed = parseSlotLabel(firstText)
            if (parsed != null) {
                runningSlot = parsed.first
                rowSlotRanges.add(parsed)
            } else {
                val isHeader = firstText.isNotEmpty() &&
                    (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                     firstText.contains("早晨") || firstText.contains("中午"))
                if (!isHeader) runningSlot++
                rowSlotRanges.add(if (isHeader) -1 to -1 else runningSlot to runningSlot)
            }
        }

        var dataRowIdx = 0
        for (ri in 1 until rows.size) {
            val cells = rows[ri].select("td, th")
            if (cells.isEmpty()) { dataRowIdx++; continue }

            val firstText = cells[0].text().trim()
            val slotFromLabel = parseSlotLabel(firstText)
            val isSectionHeader = firstText.isNotEmpty() &&
                (firstText.contains("上") || firstText.contains("下") || firstText.contains("晚") ||
                 firstText.contains("早晨") || firstText.contains("中午"))

            if (slotFromLabel != null) {
                slotIndex = slotFromLabel.first
            } else if (!isSectionHeader) {
                slotIndex++
            }

            cells.forEachIndexed { ci, cell ->
                if (ci == 0) return@forEachIndexed
                if (isSectionHeader) return@forEachIndexed
                val day = dayMap[ci - colOffset]
                    ?: (ci - colOffset).takeIf { it in 1..7 }
                    ?: return@forEachIndexed
                val text = cell.text().trim()
                if (text.isBlank() || text.length < 2) return@forEachIndexed

                val rowSpan = cell.attr("rowspan").toIntOrNull() ?: 1
                val labelEnd = slotFromLabel?.second ?: slotIndex
                val endSlot = if (rowSpan > 1) {
                    val lookAheadIdx = dataRowIdx + rowSpan - 1
                    if (lookAheadIdx < rowSlotRanges.size) {
                        val lastRange = rowSlotRanges[lookAheadIdx]
                        if (lastRange.first > 0) lastRange.second else labelEnd + rowSpan - 1
                    } else labelEnd + rowSpan - 1
                } else {
                    maxOf(labelEnd, slotIndex)
                }

                val lines = cell.html().split(Regex("(?i)<br\\s*/?>")).map {
                    Jsoup.parse(it).text().trim()
                }.filter { it.isNotBlank() }

                if (lines.isEmpty()) {
                    val textLines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                    if (textLines.size >= 2) {
                        parseGenericCourse(textLines, text, colors, courses)
                    }
                    return@forEachIndexed
                }

                val (name, teacher, room, weeksRaw) = extractCourseFields(lines)
                if (name.isEmpty()) return@forEachIndexed

                val weeks = parseWeekRange(weeksRaw)
                val weekType = detectWeekType(weeks, weeksRaw)

                courses.add(CourseEntity(
                    name = name, teacher = teacher, room = room,
                    dayOfWeek = day, startSlot = slotIndex, endSlot = endSlot,
                    weeks = weeks, colorIndex = colors[courses.size % colors.size],
                    weekType = weekType
                ))
            }
            dataRowIdx++
        }
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

            val lines = card.html().split(Regex("(?i)<br\\s*/?>")).map {
                Jsoup.parse(it).text().trim()
            }.filter { it.isNotBlank() }

            val textLines = if (lines.size >= 2) lines
            else text.split("\n").map { it.trim() }.filter { it.isNotBlank() }

            if (textLines.size >= 2) {
                parseGenericCourse(textLines, text, colors, courses)
            }
        }

        if (courses.isEmpty()) {
            parseGeneric(doc).let { courses.addAll(it) }
        }

        addLog("超星: 解析到 ${courses.size} 门课程")
        return courses
    }

    // ── Generic fallback ──

    private fun parseGeneric(doc: Document): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)
        val allTds = doc.select("td")

        for (td in allTds) {
            val text = td.text().trim()
            if (text.length < 4) continue
            if (Regex("""^[\d\s.,;:：.\-/]+$""").matches(text)) continue

            val lines = td.html().split(Regex("(?i)<br\\s*/?>")).map {
                Jsoup.parse(it).text().trim()
            }.filter { it.isNotBlank() }

            if (lines.size < 2) {
                val textLines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (textLines.size < 2) continue
                parseGenericCourse(textLines, text, colors, courses)
            } else {
                parseGenericCourse(lines, text, colors, courses)
            }
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
            parseWeekRange(weeksRaw)
        }

        courses.add(CourseEntity(
            name = name, teacher = teacher, room = room,
            dayOfWeek = day, startSlot = startSlot, endSlot = endSlot,
            weeks = weeks, colorIndex = colors[courses.size % colors.size]
        ))
    }

    // ── Utilities ──

    private fun parseWeekRange(raw: String): String {
        if (raw.isBlank()) return ""
        // Detect 单/双 flag from raw text
        val isOddOnly = raw.contains("单周") || raw.contains("(单)") || raw.contains("（单）")
        val isEvenOnly = raw.contains("双周") || raw.contains("(双)") || raw.contains("（双）")
        // "1-16周" or "1-8,10-16周" or "第1-16周" — strip non-numeric first
        val cleaned = raw.replace(Regex("""[第周\(（\)）单双]"""), "").trim()
        val ranges = Regex("""(\d+)\s*[-–~]\s*(\d+)""").findAll(cleaned).toList()
        if (ranges.isNotEmpty()) {
            val weeks = mutableSetOf<Int>()
            for (m in ranges) {
                val s = m.groupValues[1].toIntOrNull() ?: continue
                val e = m.groupValues[2].toIntOrNull() ?: continue
                for (w in s..e) {
                    if (isOddOnly && w % 2 != 1) continue
                    if (isEvenOnly && w % 2 != 0) continue
                    weeks.add(w)
                }
            }
            return weeks.sorted().joinToString(",")
        }
        // Comma-separated: "1,3,5,7,9,11,13,15"
        val singles = Regex("""\d+""").findAll(cleaned)
            .map { it.value.toIntOrNull() ?: 0 }
            .filter { it in 1..30 }
            .filter {
                if (isOddOnly) it % 2 == 1
                else if (isEvenOnly) it % 2 == 0
                else true
            }
            .toSet()
        return if (singles.isNotEmpty()) singles.sorted().joinToString(",") else ""
    }

    private fun detectWeekType(weeks: String, raw: String = ""): String {
        // Check raw text for explicit 单/双 indicators first
        if (raw.contains("单周") || raw.contains("(单)") || raw.contains("（单）")) return "odd"
        if (raw.contains("双周") || raw.contains("(双)") || raw.contains("（双）")) return "even"
        if (weeks.isBlank()) return "all"
        val nums = weeks.split(",").mapNotNull { it.toIntOrNull() }
        if (nums.isEmpty()) return "all"
        return when {
            nums.all { it % 2 == 1 } -> "odd"
            nums.all { it % 2 == 0 } -> "even"
            else -> "all"
        }
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
