package com.eggrice.timetable.network

import android.content.Context
import android.content.SharedPreferences
import android.webkit.CookieManager

/**
 * Cookie 持久化存储 — 从 WebView 提取 Cookie 供 OkHttp 复用。
 * 对应优化方案中的 CookieStore 设计。
 */
object CookieStore {

    private var cookie: String? = null
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences("egg_rice_cookies", Context.MODE_PRIVATE)
        cookie = prefs?.getString("saved_cookies", null)
    }

    fun save(cookieStr: String) {
        cookie = cookieStr
        prefs?.edit()?.putString("saved_cookies", cookieStr)?.apply()
    }

    fun get(): String? = cookie

    /** 从 WebView CookieManager 提取指定 URL 的 Cookie 并保存 */
    fun extractFromWebView(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val cookies = CookieManager.getInstance().getCookie(url)
        if (!cookies.isNullOrEmpty()) {
            save(cookies)
        }
        return cookies
    }

    /** 清除所有已保存的 Cookie */
    fun clear() {
        cookie = null
        prefs?.edit()?.remove("saved_cookies")?.apply()
    }

    /** 检查是否有已保存的 Cookie */
    fun hasCookies(): Boolean = !cookie.isNullOrEmpty()
}
