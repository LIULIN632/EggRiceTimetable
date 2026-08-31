package com.eggrice.timetable.network

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jsoup.Jsoup

/** 学业情况统计（页面 div#alertBox 解析，字段缺失为空串） */
data class AcademicSummary(
    val gpa: String = "",          // 平均学分绩点 GPA
    val plannedTotal: String = "", // 计划内总课程数
    val plannedPassed: String = "",// 计划内已通过
    val plannedFailed: String = "",// 计划内未通过
    val plannedMissed: String = "",// 计划内未修
    val plannedIn: String = "",    // 计划内在读
    val unplannedPassed: String = "", // 计划外已通过
    val unplannedFailed: String = "", // 计划外未通过
    val studentId: String = ""     // 学号
)

/** 培养方案课程类型（如通识必修/专业必修），含学分要求与已获学分 */
data class AcademicTypeInfo(
    val name: String,
    val id: String,
    val requiredCredit: String = "", // 要求学分
    val earnedCredit: String = "",   // 获得学分
    val missedCredit: String = ""    // 未获得学分
)

/** 单门课程修读情况（按培养方案类型分组返回） */
data class AcademicCourseItem(
    val typeName: String = "",   // 所属课程类型
    val courseName: String = "", // 课程名称 KCMC
    val courseId: String = "",   // 课程号 KCH
    val credit: String = "",     // 学分 XF
    val nature: String = "",     // 课程性质 KCXZMC（必修/选修）
    val category: String = "",   // 课程类别 KCLBMC
    val maxGrade: String = "",   // 最高成绩 MAXCJ
    val gradePoint: String = "", // 绩点 JD
    val status: String = "",     // 修读状态 XDZT（已修/在读/未修）
    val term: String = ""        // 学年学期 JYXDXNM + JYXDXQMC
)

/** fetchIndex 返回：统计信息 + 课程类型列表 */
data class AcademicIndexResult(
    val summary: AcademicSummary,
    val types: List<AcademicTypeInfo>
)

/**
 * 正方教务 v9「学生学业情况」查询封装（基于同一 ZhengfangClient 的登录会话）。
 *
 * 接口约定（与开源 zfn_api 的 get_academia 同源验证）：
 * - 页面：GET xsxy/xsxyqk_cxXsxyqkIndex.html?gnmkdm=N105515&layout=default
 *   - input#xh_id → 学号；div#alertBox 文本 → 课程/学分统计；页面正则 → 各类型要求/获得/未获得学分
 * - 明细：POST xsxy/xsxyqk_cxJxzxjhxfyqKcxx.html?gnmkdm=N105515，body xfyqjd_id={类型id}
 *   - 返回 JSON 数组，字段名大小写不定，解析做别名容错
 */
class ZhengfangAcademicApi(private val client: ZhengfangClient) {

    /** 拉取学业情况页面：统计信息 + 课程类型列表（登录后调用） */
    suspend fun fetchIndex(baseUrl: String): AcademicIndexResult = withContext(Dispatchers.IO) {
        val url = "$baseUrl/xsxy/xsxyqk_cxXsxyqkIndex.html?gnmkdm=N105515&layout=default"
        val html = try {
            client.get(url).use { it.body?.string() ?: "" }
        } catch (e: Exception) {
            android.util.Log.w("AcademicApi", "fetchIndex failed: ${e.message}")
            ""
        }
        android.util.Log.d("AcademicApi", "fetchIndex url=$url len=${html.length} isLogin=${ZhengfangUtils.isSessionDead(html)}")
        if (html.isBlank()) return@withContext AcademicIndexResult(AcademicSummary(), emptyList())
        if (ZhengfangUtils.isSessionDead(html)) throw IllegalStateException("会话已失效，请重新登录")
        val result = AcademicIndexResult(parseSummary(html), parseTypeStatistics(html))
        android.util.Log.d("AcademicApi", "fetchIndex studentId=${result.summary.studentId} gpa=${result.summary.gpa} types=${result.types.map { it.name to it.id }}")
        result
    }

    /** 查询某类型下的全部课程修读明细（登录后调用） */
    suspend fun fetchCourses(baseUrl: String, typeId: String, typeName: String): List<AcademicCourseItem> =
        withContext(Dispatchers.IO) {
            val url = "$baseUrl/xsxy/xsxyqk_cxJxzxjhxfyqKcxx.html?gnmkdm=N105515"
            val respText = try {
                client.post(url, "xfyqjd_id=$typeId").use { it.body?.string() ?: "" }
            } catch (e: Exception) {
                android.util.Log.w("AcademicApi", "fetchCourses($typeId) failed: ${e.message}")
                ""
            }
            android.util.Log.d("AcademicApi", "fetchCourses type=$typeName id=$typeId resp=${respText.take(400)}")
            if (respText.isBlank()) return@withContext emptyList()
            if (ZhengfangUtils.isSessionDead(respText)) throw IllegalStateException("会话已失效，请重新登录")
            parseCourseItems(respText, typeName)
        }

    // ── 页面解析 ──

    internal fun parseSummary(html: String): AcademicSummary {
        if (html.isBlank()) return AcademicSummary()
        return try {
            val doc = Jsoup.parse(html)
            val studentId = doc.select("input#xh_id").attr("value").trim()
                .ifBlank { Regex("""(?:id|name)=["']xh_id["'][^>]*value=["']([^"']*)["']""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1)?.trim() ?: "" }
            val alertText = doc.select("div#alertBox").text()
                .ifBlank { Regex("""<div[^>]*id=["']alertBox["'][^>]*>([\s\S]*?)</div>""", RegexOption.IGNORE_CASE).find(html)?.groupValues?.get(1) ?: "" }
                .replace(Regex("""<[^>]*>"""), "").replace("&nbsp;", " ").replace(" ", "")
                .trim()
            parseAlertStatistics(alertText, studentId)
        } catch (_: Exception) {
            AcademicSummary()
        }
    }

    /** 解析 alertBox 文本中的统计数字（对齐 zfn_api 正则） */
    internal fun parseAlertStatistics(alertText: String, studentId: String = ""): AcademicSummary {
        if (alertText.isBlank()) return AcademicSummary(studentId = studentId)
        val summary = AcademicSummary(studentId = studentId)
        return try {
            val gpa = Regex("""[0-9]{1,}[.][0-9]+""").find(alertText)?.value ?: ""
            val plan = Regex(
                """计划总课程(\d+)门通过(\d+)门?.*?未通过(\d+)门?.*?未修(\d+)?.*?在读(\d+)门?.*?计划外?.*?通过(\d+)门?.*?未通过(\d+)门"""
            ).find(alertText)
            if (plan == null) {
                summary.copy(gpa = gpa)
            } else {
                val g = plan.groupValues
                summary.copy(
                    gpa = gpa,
                    plannedTotal = g.getOrNull(1) ?: "",
                    plannedPassed = g.getOrNull(2) ?: "",
                    plannedFailed = g.getOrNull(3) ?: "",
                    plannedMissed = g.getOrNull(4) ?: "",
                    plannedIn = g.getOrNull(5) ?: "",
                    unplannedPassed = g.getOrNull(6) ?: "",
                    unplannedFailed = g.getOrNull(7) ?: ""
                )
            }
        } catch (_: Exception) {
            summary
        }
    }

    /** 解析页面中的课程类型学分块：类型名/要求学分/获得学分/未获得学分 + showKc{id} */
    internal fun parseTypeStatistics(html: String): List<AcademicTypeInfo> {
        if (html.isBlank()) return emptyList()
        val seen = LinkedHashMap<String, AcademicTypeInfo>()
        try {
            // 模式 A：对齐 zfn_api 原版（双引号 + &nbsp;，类型名长度不限）
            val patternA = Regex(
                """"([^"]{1,40})&nbsp;.*?要求学分.*?[:：]([0-9]+[.][0-9]+|0|&nbsp;).*?获得学分.*?[:：]([0-9]+[.][0-9]+|0|&nbsp;).*?未获得学分.*?[:：]([0-9]+[.][0-9]+|0|&nbsp;)[\s\S]*?showKc([0-9]+)"""
            )
            for (m in patternA.findAll(html)) {
                val g = m.groupValues
                val name = g[1].trim()
                val id = g[4].trim()
                if (name.isBlank() || id.isBlank()) continue
                if (name.contains("span") || name.length > 20) continue
                if (seen.containsKey(name)) continue
                seen[name] = AcademicTypeInfo(
                    name = name,
                    id = id,
                    requiredCredit = cleanCredit(g[2]),
                    earnedCredit = cleanCredit(g[3]),
                    missedCredit = cleanCredit(g[5])
                )
            }

            // 模式 B：Jsoup 定位 showKc span，向上找含「要求学分」的块反向兜底
            if (seen.isEmpty()) {
                val doc = Jsoup.parse(html)
                for (span in doc.select("[id^=showKc]")) {
                    val id = span.id().removePrefix("showKc")
                    if (id.isBlank() || id.any { !it.isDigit() }) continue
                    var node: org.jsoup.nodes.Element? = span.parent()
                    var block: org.jsoup.nodes.Element? = null
                    while (node != null) {
                        if (node.text().contains("要求学分") || node.text().contains("获得学分")) {
                            block = node
                            break
                        }
                        node = node.parent()
                    }
                    if (block == null) continue
                    val blockText = block.text()
                    val name = Regex("""([\u4e00-\u9fa5A-Za-z]{2,20})\s*要求学分""")
                        .find(blockText)?.groupValues?.get(1)?.trim()
                        ?: block.select("td, th, span, b, strong").firstOrNull()?.text()?.trim()
                        ?: continue
                    if (name.isBlank() || name.length > 20 || name.contains("span") || name.any { it.isDigit() }) continue
                    val req = Regex("""要求学分\s*[:：]?\s*([0-9]+[.][0-9]+|0)""").find(blockText)?.groupValues?.get(1)
                    val earn = Regex("""获得学分\s*[:：]?\s*([0-9]+[.][0-9]+|0)""").find(blockText)?.groupValues?.get(1)
                    val miss = Regex("""未获得学分\s*[:：]?\s*([0-9]+[.][0-9]+|0)""").find(blockText)?.groupValues?.get(1)
                    if (seen.containsKey(name)) continue
                    seen[name] = AcademicTypeInfo(
                        name = name,
                        id = id,
                        requiredCredit = cleanCredit(req ?: ""),
                        earnedCredit = cleanCredit(earn ?: ""),
                        missedCredit = cleanCredit(miss ?: "")
                    )
                }
            }
        } catch (_: Exception) { }
        android.util.Log.d("AcademicApi", "parseTypeStatistics matched=${seen.size}")
        return seen.values.toList()
    }

    private fun cleanCredit(raw: String): String =
        raw.trim().takeUnless { it.isBlank() || it == "&nbsp;" || it == "0" } ?: ""

    // ── 明细 JSON 解析 ──

    internal fun parseCourseItems(jsonText: String, typeName: String = ""): List<AcademicCourseItem> {
        return try {
            val arr = extractJsonArray(jsonText) ?: return emptyList()
            arr.mapNotNull { el ->
                if (!el.isJsonObject) return@mapNotNull null
                val o = el.asJsonObject
                val courseName = ZhengfangUtils.jsonField(o, "KCMC", "kcmc").trim()
                if (courseName.isBlank()) return@mapNotNull null
                AcademicCourseItem(
                    typeName = typeName,
                    courseName = courseName,
                    courseId = ZhengfangUtils.jsonField(o, "KCH", "kch"),
                    credit = ZhengfangUtils.jsonField(o, "XF", "xf"),
                    nature = ZhengfangUtils.jsonField(o, "KCXZMC", "kcxzmc"),
                    category = ZhengfangUtils.jsonField(o, "KCLBMC", "kclbmc"),
                    maxGrade = ZhengfangUtils.jsonField(o, "MAXCJ", "maxcj", "CJ", "cj"),
                    gradePoint = ZhengfangUtils.jsonField(o, "JD", "jd"),
                    status = ZhengfangUtils.jsonField(o, "XDZT", "xdzt"),
                    term = buildTerm(o)
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    /** 兼容 [ {...} ] 与 { "data": [...] } / { "items": [...] } 等多种包装 */
    private fun extractJsonArray(text: String): JsonArray? {
        val trimmed = text.trimStart()
        if (trimmed.startsWith("[")) {
            return runCatching { JsonParser.parseString(text).asJsonArray }.getOrNull()
        }
        return runCatching {
            val root = JsonParser.parseString(text).asJsonObject
            for (key in listOf("data", "items", "list", "rows", "result")) {
                val el = root.get(key) ?: continue
                when (el) {
                    is JsonArray -> return@runCatching el
                    is JsonObject -> {
                        listOf("items", "list", "rows").forEach { inner ->
                            el.getAsJsonArray(inner)?.let { return@runCatching it }
                        }
                    }
                }
            }
            null
        }.getOrNull()
    }

    /** 学年 + 学期 → "2024-2025 第1学期"；学期编码转中文 */
    private fun buildTerm(o: JsonObject): String {
        val year = ZhengfangUtils.jsonField(o, "JYXDXNM", "jyxdxnm", "XNMC", "xnmc")
        val rawTerm = ZhengfangUtils.jsonField(o, "JYXDXQMC", "jyxdxqmc", "XQMC", "xqmc")
        if (year.isBlank() && rawTerm.isBlank()) return ""
        val termLabel = if (rawTerm.isBlank()) "" else ZhengfangUtils.termLabel(rawTerm)
        return listOf(year, termLabel).filter { it.isNotBlank() }.joinToString(" ")
    }
}
