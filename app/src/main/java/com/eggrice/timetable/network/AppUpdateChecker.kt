package com.eggrice.timetable.network

import com.google.gson.JsonParser
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * 软件更新检查：查询 GitHub Releases 最新版，与当前版本号对比。
 * 仓库当前未发布 Release 时返回 UpToDate（404）；发布后即可自动检测。
 * 版本号三段式（主.次.补丁）数字比较，tag 兼容 "v" 前缀（如 v11.0.16）。
 */
class AppUpdateChecker {

    companion object {
        const val RELEASES_URL = "https://api.github.com/repos/LIULIN632/EggRiceTimetable/releases/latest"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        /** 发现新版本 */
        data class Latest(val version: String, val url: String) : Result()
        /** 已是最新（或无 Release） */
        data object UpToDate : Result()
        data class Failed(val message: String) : Result()
    }

    /** 查询最新 Release 并对比当前版本（阻塞调用，放 IO 线程） */
    fun checkLatest(currentVersion: String): Result {
        val json: String = try {
            client.newCall(Request.Builder().url(RELEASES_URL).build()).execute().use { resp ->
                if (resp.code == 404) return Result.UpToDate // 仓库未发布 Release
                if (!resp.isSuccessful) return Result.Failed("网络错误（HTTP ${resp.code}）")
                resp.body?.string() ?: return Result.Failed("响应为空")
            }
        } catch (e: Exception) {
            return Result.Failed("无法连接更新服务器")
        }
        val (tag, url) = try {
            val obj = JsonParser.parseString(json).asJsonObject
            (obj.get("tag_name")?.asString ?: "") to (obj.get("html_url")?.asString ?: "")
        } catch (e: Exception) {
            return Result.Failed("更新信息解析失败")
        }
        if (tag.isBlank()) return Result.UpToDate
        return if (isNewer(tag, currentVersion)) Result.Latest(tag.removePrefix("v"), url)
        else Result.UpToDate
    }

    /** 版本比较（纯逻辑，可单测）：tag 比 current 新才返回 true */
    internal fun isNewer(tag: String, current: String): Boolean {
        val t = parseVersion(tag) ?: return false
        val c = parseVersion(current) ?: return false
        return when {
            t.first != c.first -> t.first > c.first
            t.second != c.second -> t.second > c.second
            else -> t.third > c.third
        }
    }

    private fun parseVersion(v: String): Triple<Int, Int, Int>? {
        val nums = v.trim().removePrefix("v").split(".").mapNotNull { it.toIntOrNull() }
        return if (nums.size >= 3) Triple(nums[0], nums[1], nums[2]) else null
    }
}
