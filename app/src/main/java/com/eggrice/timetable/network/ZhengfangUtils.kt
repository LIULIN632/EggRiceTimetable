package com.eggrice.timetable.network

import com.google.gson.JsonObject

/**
 * 正方教务公共工具：会话失效判断、学期标签、JSON 字段取值。
 * 供 ZhengfangGradeApi / ZhengfangAcademicApi / ZhengfangClient 共用，避免各 API 重复实现。
 */
object ZhengfangUtils {

    /** 响应为登录页 → 会话已失效（兼容不同正方版本的登录页特征） */
    fun isSessionDead(text: String): Boolean =
        text.contains("login_slogin") || text.contains("userLogin") ||
            text.contains("userPassword") || text.contains("用户登录")

    /** 学期编码 → 中文标签（3=第1学期，12=第2学期） */
    fun termLabel(xqm: String): String = when (xqm) {
        "3" -> "第1学期"
        "12" -> "第2学期"
        else -> "第${xqm}学期"
    }

    /**
     * 大小写不敏感的 JSON 字段取值：按 [names] 顺序取第一个非 null/非空字段值；
     * 数字原样转字符串；全部缺失返回空串。
     */
    fun jsonField(o: JsonObject, vararg names: String): String {
        val expected = names.mapTo(HashSet()) { it.lowercase() }
        for (key in o.keySet()) {
            if (key.lowercase() !in expected) continue
            val el = o.get(key) ?: continue
            if (el.isJsonNull) continue
            if (el.isJsonPrimitive) {
                val s = el.asString.trim()
                if (s.isNotEmpty()) return s
            }
        }
        return ""
    }
}
