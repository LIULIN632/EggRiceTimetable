package com.eggrice.timetable.data

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

enum class JwSystemType(val label: String, val description: String) {
    ZHENGFANG("正方教务", "支持大多数高校"),
    QIANGZHI("强智教务", "支持走班制课程"),
    QINGGUO("青果教务", "支持新高考排课"),
    CHAOXING("超星学习通", "支持学习通同步"),
    URP("URP教务", "支持URP教务系统")
}

data class School(
    val id: String,
    val name: String,
    val city: String,
    val jwType: JwSystemType,
    val baseUrl: String,
    val isV8: Boolean = true
)

fun isJwSystemAvailable(type: JwSystemType): Boolean = true

private data class SchoolJson(
    val id: String,
    val name: String,
    val city: String,
    val jwType: String,
    val baseUrl: String,
    val isV8: Boolean = true
)

class SchoolRegistry(context: Context) {
    // 从assets/schools.json加载学校列表，解析失败时用内置fallbackSchools兜底
    val allSchools: List<School> by lazy {
        try {
            val input = context.assets.open("schools.json")
            val reader = InputStreamReader(input, Charsets.UTF_8)
            val rawList = Gson().fromJson(reader, Array<SchoolJson>::class.java)
            reader.close()
            rawList.map { raw ->
                School(
                    id = raw.id,
                    name = raw.name,
                    city = raw.city,
                    jwType = parseJwType(raw.jwType),
                    baseUrl = raw.baseUrl,
                    isV8 = raw.isV8
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            fallbackSchools
        }
    }

    fun filter(type: JwSystemType, query: String): List<School> {
        val q = query.trim()
        return allSchools.filter {
            it.jwType == type &&
            (q.isEmpty() || it.name.contains(q, ignoreCase = true) || it.city.contains(q, ignoreCase = true))
        }
    }

    private fun parseJwType(raw: String): JwSystemType = when (raw.uppercase()) {
        "QIANGZHI" -> JwSystemType.QIANGZHI
        "QINGGUO" -> JwSystemType.QINGGUO
        "CHAOXING" -> JwSystemType.CHAOXING
        "URP" -> JwSystemType.URP
        else -> JwSystemType.ZHENGFANG
    }

    companion object {
        val fallbackSchools = listOf(
            School("yitsd", "烟台理工学院", "烟台", JwSystemType.ZHENGFANG, "https://jwxt.yitsd.edu.cn/jwglxt", isV8 = true),
            School("ytu", "烟台大学", "烟台", JwSystemType.ZHENGFANG, "http://210.47.245.4/", isV8 = false),
            School("cust", "长春理工大学", "长春", JwSystemType.ZHENGFANG, "https://jw.cust.edu.cn/jwglxt"),
            School("cquit", "重庆理工大学", "重庆", JwSystemType.ZHENGFANG, "https://jw.cqut.edu.cn/jwglxt"),
            School("cqu", "重庆大学", "重庆", JwSystemType.ZHENGFANG, "https://jw.cqu.edu.cn/jwglxt"),
            School("scut", "华南理工大学", "广州", JwSystemType.ZHENGFANG, "https://jw.scut.edu.cn/jwglxt"),
            School("tongji", "同济大学", "上海", JwSystemType.ZHENGFANG, "https://jw.tongji.edu.cn/jwglxt"),
            School("jnu", "暨南大学", "广州", JwSystemType.ZHENGFANG, "https://jw.jnu.edu.cn/jwglxt"),
            School("suda", "苏州大学", "苏州", JwSystemType.ZHENGFANG, "https://jw.suda.edu.cn/jwglxt"),
        )
    }
}
