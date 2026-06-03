package com.eggrice.timetable.network

import com.eggrice.timetable.data.entity.CourseEntity
import com.eggrice.timetable.util.RsaUtil
import com.google.gson.Gson
import com.google.gson.JsonParser
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLEncoder
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class ZhengfangSchool(
    val name: String,
    val url: String,
    val isV8: Boolean = true
)

data class LoginResult(
    val success: Boolean,
    val error: String? = null,
    val cookies: String? = null,
    val courses: List<CourseEntity>? = null,
    val needsCaptcha: Boolean = false,
    val captchaBase64: String? = null
)

data class CaptchaResult(
    val base64: String,
    val refresh: suspend () -> CaptchaResult
)

class ZhengfangClient {
    private val gson = Gson()

    // ── Cookie store — replicates Python requests.Session() auto cookie management ──
    private val cookieStore = ConcurrentHashMap<String, ConcurrentHashMap<String, Cookie>>()

    private val cookieJar = object : CookieJar {
        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            val result = mutableListOf<Cookie>()
            val now = System.currentTimeMillis()
            for ((host, hostCookies) in cookieStore) {
                if (url.host == host || url.host.endsWith(".$host") ||
                    (host.startsWith(".") && url.host.endsWith(host))) {
                    for (cookie in hostCookies.values) {
                        if (cookie.expiresAt == 0L || cookie.expiresAt > now) {
                            result.add(cookie)
                        }
                    }
                }
            }
            return result
        }

        override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
            val hostCookies = cookieStore.getOrPut(url.host) { ConcurrentHashMap() }
            for (cookie in cookies) {
                if (cookie.name.isNotEmpty()) {
                    hostCookies[cookie.name] = cookie
                }
            }
        }
    }

    private val trustAllManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) {}
    }

    // User-Agent matches Python school-api reference exactly
    private val USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; WOW64) " +
            "AppleWebKit/537.36 (KHTML, like Gecko) Chrome/62.0.3202.89 Safari/537.36"

    private val client: OkHttpClient by lazy {
        val sslContext = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustAllManager), SecureRandom())
        }
        OkHttpClient.Builder()
            .sslSocketFactory(sslContext.socketFactory, trustAllManager)
            .hostnameVerifier { _, _ -> true }
            .cookieJar(cookieJar)
            .followRedirects(true)
            .followSslRedirects(true)
            .connectTimeout(java.time.Duration.ofSeconds(15))
            .readTimeout(java.time.Duration.ofSeconds(20))
            .addInterceptor { chain ->
                val req = chain.request()
                val url = req.url
                val path = url.encodedPath

                val builder = req.newBuilder()
                    .header("User-Agent", USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .header("Cache-Control", "no-cache, no-store")
                    .header("Pragma", "no-cache")

                // Referer matching Python school-api session header pattern
                if (path.contains("/xtgl/")) {
                    val origin = "${url.scheme}://${url.host}" +
                            (if (url.port != 80 && url.port != 443) ":${url.port}" else "")
                    val prefix = path.substring(0, path.indexOf("/xtgl/"))
                    builder.header("Referer", "$origin$prefix/xtgl/login_slogin.html")
                }

                chain.proceed(builder.build())
            }
            .build()
    }

    private val FORM_TYPE = "application/x-www-form-urlencoded".toMediaType()

    // ── Retrieve cookies as a string for a given URL (for LoginResult) ──
    fun getCookiesForUrl(baseUrl: String): String {
        val url = baseUrl.toHttpUrl()
        return cookieJar.loadForRequest(url).joinToString("; ") { "${it.name}=${it.value}" }
    }

    private fun get(url: String): Response {
        return client.newCall(Request.Builder().url(url).get().build()).execute()
    }

    private fun post(url: String, body: String): Response {
        return client.newCall(
            Request.Builder()
                .url(url)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .post(body.toRequestBody(FORM_TYPE))
                .build()
        ).execute()
    }

    private fun extractHiddenField(html: String, fieldName: String): String {
        val patterns = listOf(
            Regex("""(?:id|name)=["']${Regex.escape(fieldName)}["'][^>]*value=["']([^"']*)["']""", RegexOption.IGNORE_CASE),
            Regex("""value=["']([^"']*)["'][^>]*(?:id|name)=["']${Regex.escape(fieldName)}["']""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1]
        }
        return ""
    }

    private fun extractCsrftoken(html: String): String {
        val patterns = listOf(
            Regex("""(?:id|name)=["']csrftoken["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE),
            Regex("""value=["']([^"']+)["'][^>]*(?:id|name)=["']csrftoken["']""", RegexOption.IGNORE_CASE),
            Regex("""(?:id|name)=["']_csrf["'][^>]*value=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
        )
        for (p in patterns) {
            val m = p.find(html)
            if (m != null) return m.groupValues[1]
        }
        return ""
    }

    private fun checkLoginError(body: String, finalUrl: String): String? {
        if (body.isEmpty()) return null
        try {
            val json = JsonParser.parseString(body).asJsonObject
            if (json.has("result") && json.get("result").asString == "error")
                return json.get("message")?.asString ?: "登录失败"
            if (json.has("status") && json.get("status").asString == "error")
                return json.get("message")?.asString ?: "登录失败"
            if (json.has("result") && json.get("result").asString == "success") return null
            if (json.has("status") && json.get("status").asString == "success") return null
        } catch (_: Exception) {}
        if (Regex("""密码.*错误|用户名.*错误|密码.*不正确|用户名.*不正确|帐号.*错误|帐号.*不存在|账号.*错误|账号.*不存在|用户不存在|密码错误""").containsMatchIn(body))
            return "用户名或密码错误，请检查后重试"
        if (Regex("""验证码.*错误|验证码.*不正确|校验码.*错误|yzm.*错误""").containsMatchIn(body))
            return "验证码错误，请重新输入"
        if (Regex("""系统.*维护|暂停.*服务""").containsMatchIn(body))
            return "教务系统正在维护中，请稍后再试"
        if ("xs_main" in body || "xsgr" in body || "欢迎" in body || "wdyy_szbtn" in body || "退出" in body)
            return null
        if ("TextBox1" in body || "yhm" in body)
            return "登录失败，请检查学号和密码是否正确"
        return null
    }

    private fun isCampusPortal(html: String): Boolean {
        // Detect campus network authentication pages (校园网认证 / 深澜 / 锐捷 / 华为等)
        val markers = listOf(
            "校园网", "上网认证", "Portal", "portal", "深澜", "srun",
            "锐捷", "Ruijie", "华为", "Huawei", "认证登录", "上网登录",
            "ePortal", "Dr.COM", "inode", "iNode", "城市热点"
        )
        val markerCount = markers.count { it in html }
        // Campus portals typically lack 教务-specific markers
        val isNotJw = "xtgl" !in html && "yhm" !in html && "csrftoken" !in html &&
                "TextBox1" !in html && "login_slogin" !in html
        return markerCount >= 2 || (markerCount >= 1 && isNotJw)
    }

    private fun isRedirectedToOtherSystem(html: String, finalUrl: String, baseUrl: String): Boolean {
        // Check if we were redirected to a completely different domain
        if (finalUrl.isNotEmpty() && baseUrl.isNotEmpty()) {
            try {
                val finalHost = finalUrl.toHttpUrl().host
                val baseHost = baseUrl.toHttpUrl().host
                if (finalHost != baseHost &&
                    !finalHost.endsWith(baseHost) && !baseHost.endsWith(finalHost))
                    return true
            } catch (_: Exception) {}
        }
        return false
    }

    private fun urlEncode(s: String): String = URLEncoder.encode(s, "UTF-8")

    suspend fun login(
        school: ZhengfangSchool,
        username: String,
        password: String,
        onProgress: (String) -> Unit = {},
        onCaptcha: (suspend (CaptchaResult) -> String)? = null
    ): LoginResult {
        var lastResult: LoginResult? = null
        for (attempt in 1..2) {
            if (attempt > 1) onProgress("网络不稳定，正在重试...")
            val result = tryLogin(school, username, password, onProgress, onCaptcha)
            if (result.success || result.error == null) return result
            val isNetworkError = result.error.contains("无法连接") ||
                    result.error.contains("超时") ||
                    result.error.contains("网络") ||
                    result.error.contains("中断") ||
                    result.error.contains("SSL") ||
                    result.error.contains("地址无法解析")
            if (!isNetworkError) return result
            lastResult = result
        }
        return lastResult ?: LoginResult(false, "未知错误")
    }

    private suspend fun tryLogin(
        school: ZhengfangSchool,
        username: String,
        password: String,
        onProgress: (String) -> Unit,
        onCaptcha: (suspend (CaptchaResult) -> String)?
    ): LoginResult = withContext(Dispatchers.IO) {
        // Clear any stale cookies before starting
        cookieStore.clear()

        val baseUrl = school.url.trimEnd('/')

        try {
            // Step 1: GET login page — cookies auto-saved by CookieJar
            onProgress("正在连接教务系统...")
            // Try V8 login page first; fall back to legacy ASPX for non-V8 schools
            val loginPageUrls = if (school.isV8) {
                listOf("$baseUrl/xtgl/login_slogin.html")
            } else {
                listOf(
                    "$baseUrl/xtgl/login_slogin.html",  // Try V8 first (may have been upgraded)
                    "$baseUrl/default2.aspx",             // Legacy ASPX login
                    "$baseUrl/"                           // Root fallback
                )
            }
            var loginHtml = ""
            var loginFinalUrl = ""
            for (loginUrl in loginPageUrls) {
                val loginPageRes = get(loginUrl)
                val body = loginPageRes.body?.string() ?: ""
                val finalUrl = loginPageRes.request.url.toString()
                loginPageRes.close()
                if (body.length > 100) {
                    loginHtml = body; loginFinalUrl = finalUrl; break
                }
            }

            if (!currentCoroutineContext().isActive) throw CancellationException()

            if (loginHtml.length < 100)
                return@withContext LoginResult(false, "无法连接教务系统，请检查网络和地址是否正确")

            // Detect redirect to campus network portal (校园网认证)
            if (isCampusPortal(loginHtml))
                return@withContext LoginResult(false, "检测到校园网认证页面，\n请先连接校园网络或VPN后重试")

            // Detect other redirects (e.g. to different login system)
            if (isRedirectedToOtherSystem(loginHtml, loginFinalUrl, baseUrl))
                return@withContext LoginResult(false, "教务系统地址可能已变更，\n请联系学校确认教务系统网址")

            val csrftoken = extractCsrftoken(loginHtml)
            // For non-V8 ASPX systems, extract ASP.NET form fields
            val viewState = extractHiddenField(loginHtml, "__VIEWSTATE")
            val viewStateGenerator = extractHiddenField(loginHtml, "__VIEWSTATEGENERATOR")
            val eventValidation = extractHiddenField(loginHtml, "__EVENTVALIDATION")

            if (!school.isV8) {
                // Legacy ASPX system — CSRF token not required
                if (loginHtml.contains("TextBox") || loginHtml.contains("yhm") || loginHtml.contains("xh") ||
                    viewState.isNotEmpty()) {
                    // Recognizable ASPX login form — proceed
                } else if (csrftoken.isEmpty() && viewState.isEmpty()) {
                    return@withContext LoginResult(false, "无法识别教务登录页面，\n该学校可能使用了不同的教务系统版本")
                }
            } else {
                if (csrftoken.isEmpty() && !loginHtml.contains("xtgl"))
                    return@withContext LoginResult(false, "无法识别教务登录页面，\n该学校可能使用了不同的教务系统版本")
            }

            // Step 2: GET public key (may fail for non-V8 schools — proceed without)
            onProgress("正在获取加密公钥...")
            var modulus = ""
            var exponent = ""
            try {
                val pkeyRes = get("$baseUrl/xtgl/login_getPublicKey.html?time=${System.currentTimeMillis()}")
                val pkeyBody = pkeyRes.body?.string() ?: ""
                pkeyRes.close()
                var json = JsonParser.parseString(pkeyBody).asJsonObject
                if (!json.has("modulus") && json.has("data")) {
                    json = json.getAsJsonObject("data")
                }
                modulus = json.get("modulus")?.asString ?: ""
                exponent = json.get("exponent")?.asString ?: "10001"
            } catch (_: Exception) {
                // Non-V8 schools may not expose public key API — continue without RSA
            }

            if (!currentCoroutineContext().isActive) throw CancellationException()

            // Step 3: GET captcha with retry — robust image validation
            // Non-V8 schools may use different captcha URLs
            val captchaUrls = if (school.isV8) {
                listOf("$baseUrl/xtgl/login_getCaptcha.html")
            } else {
                listOf(
                    "$baseUrl/xtgl/login_getCaptcha.html",  // Try V8 first
                    "$baseUrl/CheckCode.aspx",
                    "$baseUrl/sys/ValidateCode.aspx",
                    "$baseUrl/validateCode.jsp"
                )
            }
            onProgress("正在检测验证码...")
            var captchaBytes = ByteArray(0)
            var captchaSuccess = false
            for (captchaAttempt in 1..4) {
                if (!currentCoroutineContext().isActive) throw CancellationException()
                if (captchaAttempt > 1) onProgress("验证码加载失败，正在重试(${captchaAttempt - 1}/3)...")
                try {
                    for (captchaUrl in captchaUrls) {
                        val captchaRes = get("$captchaUrl?time=${System.currentTimeMillis()}")
                        val responseBytes = captchaRes.body?.bytes() ?: ByteArray(0)
                        val contentType = captchaRes.header("Content-Type") ?: ""
                        val contentLength = captchaRes.header("Content-Length")?.toIntOrNull() ?: 0
                        captchaRes.close()
                        captchaBytes = responseBytes
                        val looksLikeImage = captchaBytes.isNotEmpty() && captchaBytes.size > 80 &&
                            ((contentType.isNotEmpty() && contentType.contains("image", ignoreCase = true)) ||
                             (contentType.isEmpty() && captchaBytes[0] != 0x3C.toByte() && captchaBytes[0] != 0x7B.toByte()) ||
                             (contentLength > 80))
                        if (looksLikeImage) { captchaSuccess = true; break }
                    }
                    if (captchaSuccess) break
                } catch (e: Exception) {
                    if (captchaAttempt >= 4) throw e
                }
                if (captchaAttempt < 4) delay(800)
            }

            val hasCaptcha = captchaSuccess && captchaBytes.size > 80

            // Step 4: RSA encrypt password
            val encryptedPwd = if (modulus.isNotEmpty()) {
                RsaUtil.encrypt(password, modulus, exponent)
            } else {
                password
            }

            // Step 5: Handle captcha
            var captchaCode = ""
            if (hasCaptcha) {
                if (onCaptcha == null) return@withContext LoginResult(false, "该教务系统需要输入验证码")
                val captchaB64 = Base64.getEncoder().encodeToString(captchaBytes)

                // Captcha refresh URLs matching the primary captcha URL list
                val refreshCaptchaUrls = captchaUrls
                fun makeCaptchaResult(b64: String): CaptchaResult = CaptchaResult(b64) {
                    withContext(Dispatchers.IO) {
                        var refreshBytes = ByteArray(0)
                        var refreshSuccess = false
                        for (retry in 1..4) {
                            if (!currentCoroutineContext().isActive) throw CancellationException()
                            try {
                                for (refreshUrl in refreshCaptchaUrls) {
                                    val r = get("$refreshUrl?time=${System.currentTimeMillis()}")
                                    refreshBytes = r.body?.bytes() ?: ByteArray(0)
                                    val refreshContentType = r.header("Content-Type") ?: ""
                                    r.close()
                                    val looksLikeImage = refreshBytes.isNotEmpty() && refreshBytes.size > 80 &&
                                        ((refreshContentType.isNotEmpty() && refreshContentType.contains("image", ignoreCase = true)) ||
                                         (refreshContentType.isEmpty() && refreshBytes[0] != 0x3C.toByte() && refreshBytes[0] != 0x7B.toByte()))
                                    if (looksLikeImage) { refreshSuccess = true; break }
                                }
                                if (refreshSuccess) break
                            } catch (_: Exception) {
                                if (retry >= 4) throw CancellationException("验证码刷新失败")
                            }
                            if (retry < 4) delay(600)
                        }
                        makeCaptchaResult(Base64.getEncoder().encodeToString(refreshBytes))
                    }
                }
                val captchaResult = makeCaptchaResult(captchaB64)
                captchaCode = onCaptcha(captchaResult)
            }

            // Step 6: POST login
            onProgress("正在登录...")
            // Build login body according to system version
            val (loginBody, loginPostUrl) = if (school.isV8 || csrftoken.isNotEmpty()) {
                // V8-style login
                buildString {
                    if (csrftoken.isNotEmpty()) {
                        append("csrftoken=").append(urlEncode(csrftoken))
                        append("&")
                    }
                    append("yhm=").append(urlEncode(username))
                    append("&mm=").append(urlEncode(encryptedPwd))
                    append("&language=zh_CN")
                    if (captchaCode.isNotEmpty()) {
                        append("&yzm=").append(urlEncode(captchaCode))
                    }
                } to "$baseUrl/xtgl/login_slogin.html?time=${System.currentTimeMillis()}"
            } else {
                // Legacy ASPX-style login
                buildString {
                    if (viewState.isNotEmpty()) {
                        append("__VIEWSTATE=").append(urlEncode(viewState)).append("&")
                    }
                    if (viewStateGenerator.isNotEmpty()) {
                        append("__VIEWSTATEGENERATOR=").append(urlEncode(viewStateGenerator)).append("&")
                    }
                    if (eventValidation.isNotEmpty()) {
                        append("__EVENTVALIDATION=").append(urlEncode(eventValidation)).append("&")
                    }
                    append("txtUserName=").append(urlEncode(username)).append("&")
                    append("TextBox2=").append(urlEncode(encryptedPwd))  // May be plaintext for non-V8
                    append("&RadioButtonList1=%D1%A7%C9%FA")  // Student role
                    append("&Button1=")
                    if (captchaCode.isNotEmpty()) {
                        append("&txtSecretCode=").append(urlEncode(captchaCode))
                    }
                } to "$baseUrl/default2.aspx"
            }

            val loginRes = post(loginPostUrl, loginBody)
            val loginBody2 = loginRes.body?.string() ?: ""
            val finalUrl = loginRes.request.url.toString()
            loginRes.close()

            if (!currentCoroutineContext().isActive) throw CancellationException()

            val errorMsg = checkLoginError(loginBody2, finalUrl)
            if (errorMsg != null) return@withContext LoginResult(false, errorMsg)

            // Step 7: Fetch schedule — branch by system version
            onProgress("登录成功，正在获取课表...")
            val courses = if (school.isV8) {
                fetchScheduleV8(baseUrl, onProgress)
            } else {
                fetchScheduleLegacy(baseUrl, onProgress)
            }
            if (courses.isEmpty()) return@withContext LoginResult(false, "未能获取课表数据，可能本学期未选课或课表为空")

            LoginResult(true, cookies = getCookiesForUrl(baseUrl), courses = courses)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            val msg = e.message ?: "未知错误"
            LoginResult(false, translateError(msg))
        }
    }

    private suspend fun fetchScheduleV8(baseUrl: String, onProgress: (String) -> Unit): List<CourseEntity> {
        onProgress("正在通过 API 获取课表...")
        val now = java.time.LocalDate.now()
        val (xnm, xqm) = if (now.monthValue >= 8 || now.monthValue <= 1) {
            now.year.toString() to "3"
        } else {
            (now.year - 1).toString() to "12"
        }

        val apiUrl = "$baseUrl/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151"
        val apiBody = "xnm=$xnm&xqm=$xqm&kzlx=ck&xsdm=&kclbdm="

        val res = post(apiUrl, apiBody)
        val body = res.body?.string() ?: ""
        res.close()

        val courses = parseJsonSchedule(body)
        if (courses.isEmpty()) {
            val altXqm = if (xqm == "3") "12" else "3"
            val altXnm = if (altXqm == "12") (xnm.toInt() - 1).toString() else xnm
            val altBody = "xnm=$altXnm&xqm=$altXqm&kzlx=ck&xsdm=&kclbdm="
            val altRes = post(apiUrl, altBody)
            val altResp = altRes.body?.string() ?: ""
            altRes.close()
            return parseJsonSchedule(altResp)
        }
        return courses
    }

    private fun parseJsonSchedule(json: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        try {
            val root = JsonParser.parseString(json).asJsonObject
            val kbList = root.getAsJsonArray("kbList")
                ?: root.getAsJsonArray("kblist")
                ?: return courses

            for (item in kbList) {
                val obj = item.asJsonObject
                val name = (obj.get("kcmc")?.asString ?: "").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
                val teacher = obj.get("xm")?.asString ?: obj.get("jsxm")?.asString ?: ""
                val room = obj.get("cdmc")?.asString ?: obj.get("jsmc")?.asString ?: ""
                val day = (obj.get("xqj")?.asInt ?: obj.get("xq")?.asInt ?: 0)
                val secStr = obj.get("jcs")?.asString ?: obj.get("jc")?.asString ?: ""
                val weekStr = obj.get("zcd")?.asString ?: obj.get("zhous")?.asString ?: ""

                if (name.isEmpty() || day < 1 || day > 7 || secStr.isEmpty()) continue

                val (startSlot, endSlot) = parseSections(secStr)
                val weeks = parseWeeks(weekStr)
                val weekType = computeWeekType(weeks)

                courses.add(CourseEntity(
                    name = name, teacher = teacher, room = room,
                    dayOfWeek = day, startSlot = startSlot, endSlot = endSlot,
                    weekType = weekType, weeks = weeks.joinToString(",") { it.toString() },
                    colorIndex = (day * 3 + startSlot) % 15
                ))
            }
        } catch (_: Exception) {}
        return courses
    }

    // ── Legacy (non-V8) schedule: HTML page scrape ──
    private suspend fun fetchScheduleLegacy(baseUrl: String, onProgress: (String) -> Unit): List<CourseEntity> {
        // Try JSON API first — some IP-based deployments still support V8 API
        onProgress("正在尝试 API 获取课表...")
        val v8Courses = fetchScheduleV8(baseUrl) { /* silent */ }
        if (v8Courses.isNotEmpty()) return v8Courses

        // Fall back to HTML schedule page
        onProgress("正在通过页面解析课表...")
        val scheduleUrls = listOf(
            "$baseUrl/xsgrkbcx.aspx",
            "$baseUrl/xskbcx.aspx",
            "$baseUrl/xsdjkbcx.aspx",
            "$baseUrl/wsxk/xskbcx.aspx"
        )
        for (scheduleUrl in scheduleUrls) {
            try {
                val res = get(scheduleUrl)
                val html = res.body?.string() ?: ""
                res.close()
                if (html.length > 500 && ("课程" in html || "课表" in html || "Table1" in html || "table" in html.lowercase())) {
                    val courses = parseHtmlScheduleTable(html)
                    if (courses.isNotEmpty()) return courses
                }
            } catch (_: Exception) { continue }
        }

        // Last resort: try navigating through the main page menu
        try {
            val mainRes = get("$baseUrl/xs_main.aspx")
            val mainHtml = mainRes.body?.string() ?: ""
            mainRes.close()
            // Extract schedule page link from navigation
            val linkMatch = Regex("""(?:xskbcx|xsdjkbcx|xsgrkbcx)\.aspx[^"']*""").find(mainHtml)
            if (linkMatch != null) {
                val link = linkMatch.value
                val fullUrl = if (link.startsWith("http")) link else "$baseUrl/$link"
                val schedRes = get(fullUrl)
                val schedHtml = schedRes.body?.string() ?: ""
                schedRes.close()
                val courses = parseHtmlScheduleTable(schedHtml)
                if (courses.isNotEmpty()) return courses
            }
        } catch (_: Exception) {}

        return emptyList()
    }

    // Parse old-style ASPX schedule HTML table into CourseEntity list
    private fun parseHtmlScheduleTable(html: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()

        // Method 1: Parse <table id="Table1"> — classic 正方 grid table
        // Also support V8 正方 kbgrid_table_0 format
        val tableRegex = Regex("""<table[^>]*id=["'](?:Table1|kbgrid_table_0)["'][^>]*>(.*?)</table>""", RegexOption.DOT_MATCHES_ALL.let { setOf(it, RegexOption.IGNORE_CASE) })
        val tableMatch = tableRegex.find(html)
        val tableHtml = tableMatch?.groupValues?.get(1) ?: html

        // Extract rows
        val rowRegex = Regex("""<tr[^>]*>(.*?)</tr>""", RegexOption.DOT_MATCHES_ALL.let { setOf(it, RegexOption.IGNORE_CASE) })
        val rows = rowRegex.findAll(tableHtml).toList()

        if (rows.size < 3) return courses // Need at least header + content rows

        // Parse header row for day-of-week mapping
        val headerCells = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""", RegexOption.DOT_MATCHES_ALL.let { setOf(it, RegexOption.IGNORE_CASE) })
            .findAll(rows[0].value).toList()

        // Map column index → dayOfWeek
        val dayColumnMap = mutableMapOf<Int, Int>()
        for ((colIdx, cell) in headerCells.withIndex()) {
            val text = cell.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            when {
                text.contains("一") || text.contains("周一") -> dayColumnMap[colIdx] = 1
                text.contains("二") || text.contains("周二") -> dayColumnMap[colIdx] = 2
                text.contains("三") || text.contains("周三") -> dayColumnMap[colIdx] = 3
                text.contains("四") || text.contains("周四") -> dayColumnMap[colIdx] = 4
                text.contains("五") || text.contains("周五") -> dayColumnMap[colIdx] = 5
                text.contains("六") || text.contains("周六") -> dayColumnMap[colIdx] = 6
                text.contains("日") || text.contains("周日") -> dayColumnMap[colIdx] = 7
            }
        }

        // Parse data rows
        var currentSlot = 0
        for (row in rows.drop(1)) {
            val cells = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""", RegexOption.DOT_MATCHES_ALL.let { setOf(it, RegexOption.IGNORE_CASE) })
                .findAll(row.value).toList()

            if (cells.isEmpty()) continue

            // Check if this is a section header row (上午/下午/晚上 → slot number)
            val firstCellText = cells.getOrNull(0)?.groupValues?.get(1)?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            val slotMatch = Regex("""第\s*(\d+)\s*节""").find(firstCellText)
            if (slotMatch != null) {
                currentSlot = slotMatch.groupValues[1].toIntOrNull() ?: (currentSlot + 1)
            } else if (firstCellText.isNotEmpty() && !firstCellText.contains("上午") && !firstCellText.contains("下午") && !firstCellText.contains("晚上")) {
                currentSlot++
            }

            // Parse course info from each day column
            for ((colIdx, cell) in cells.withIndex()) {
                val day = dayColumnMap[colIdx] ?: continue
                val cellHtml = cell.groupValues[1]
                val cellText = cellHtml.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<[^>]*>"), "")
                    .trim()

                if (cellText.isBlank() || cellText.length < 2) continue
                if (Regex("""^[\d\s.]*$""").matches(cellText)) continue // skip number-only cells

                // Parse course details from cell text
                val lines = cellText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) continue

                // Line 0 = course name, Line 1 = teacher, Line 2 = room, Line 3 = weeks (common pattern)
                val name = lines.getOrElse(0) { "" }.replace("&nbsp;", "").trim()
                val teacher = lines.getOrElse(1) { "" }.replace("&nbsp;", "").trim()
                val room = lines.getOrElse(2) { "" }.replace("&nbsp;", "").trim()
                val weeksRaw = lines.getOrElse(3) { "" }.replace("&nbsp;", "").trim()

                if (name.isEmpty()) continue

                // Extract weeks from the raw text
                val weeks = parseWeeks(weeksRaw.ifEmpty {
                    // Try to find week numbers in the full cell text
                    val weekMatch = Regex("""(\d+)\s*[-–]\s*(\d+)\s*周""").find(cellText)
                    if (weekMatch != null) {
                        val start = weekMatch.groupValues[1].toIntOrNull()
                        if (start != null) {
                            val end = weekMatch.groupValues[2].toIntOrNull() ?: start
                            (start..end).joinToString(",") { it.toString() }
                        } else ""
                    } else ""
                })
                val weekType = computeWeekType(weeks)

                // Determine if this cell spans multiple rows (course duration)
                val rowSpanMatch = Regex("""row[sS]pan\s*=\s*["'](\d+)["']""").find(cellHtml)
                val rowSpan = rowSpanMatch?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val endSlot = currentSlot + rowSpan - 1

                courses.add(CourseEntity(
                    name = name,
                    teacher = if (teacher.length < 10 && !teacher.contains("周") && !teacher.contains("第")) teacher else "",
                    room = if (room.length < 10 && (room.contains("楼") || room.contains("教") || room.contains("室") || room.contains("区") || room.matches(Regex(".*\\d+.*")))) room else "",
                    dayOfWeek = day,
                    startSlot = currentSlot,
                    endSlot = endSlot,
                    weekType = weekType,
                    weeks = weeks.joinToString(",") { it.toString() },
                    colorIndex = (day * 3 + currentSlot) % 15
                ))
            }
        }

        // Method 2: If Method 1 found nothing, try regex-based extraction from the full HTML
        if (courses.isEmpty()) {
            // Look for common course data patterns in old 正方 HTML
            val allCellContent = Regex("""<td[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
                .findAll(html)
                .map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }
                .filter { it.length > 5 && Regex("""[一-鿿]""").containsMatchIn(it) }
                .toList()

            // Try to map sequential cells to course entries
            // Pattern: course name, teacher, room, weeks repeating
            var i = 0
            var inferredDay = 1
            var inferredSlot = 1
            while (i + 2 < allCellContent.size) {
                val potentialName = allCellContent[i]
                val potentialTeacher = allCellContent.getOrElse(i + 1) { "" }
                val potentialRoom = allCellContent.getOrElse(i + 2) { "" }

                if (potentialName.length in 2..30 && Regex("""[一-鿿]""").containsMatchIn(potentialName)) {
                    val weeks = parseWeeks(allCellContent.getOrElse(i + 3) { "" })
                    courses.add(CourseEntity(
                        name = potentialName,
                        teacher = if (potentialTeacher.length < 15) potentialTeacher else "",
                        room = if (potentialRoom.length < 15) potentialRoom else "",
                        dayOfWeek = inferredDay,
                        startSlot = inferredSlot,
                        endSlot = inferredSlot + 1,
                        weekType = computeWeekType(weeks),
                        weeks = weeks.joinToString(",") { it.toString() },
                        colorIndex = (inferredDay * 3 + inferredSlot) % 15
                    ))
                    inferredSlot++
                    if (inferredSlot > 12) { inferredSlot = 1; inferredDay++ }
                    if (inferredDay > 7) break
                    i += 4
                } else {
                    i++
                }
            }
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}" }
    }

    private fun parseSections(str: String): Pair<Int, Int> {
        val parts = str.replace("节", "").split("-")
        val start = parts[0].toIntOrNull() ?: 1
        val end = parts.getOrNull(1)?.toIntOrNull() ?: start
        return Pair(start, if (end < start) start else end)
    }

    private fun parseWeeks(str: String): List<Int> {
        if (str.isEmpty()) return emptyList()
        val weeks = mutableListOf<Int>()
        val segments = str.replace("周", "").split(",")
        for (seg in segments) {
            val match = Regex("""(\d+)(?:-(\d+))?[\(（]([单雙双])[\)）]?""").find(seg.trim())
            if (match != null) {
                val start = match.groupValues[1].toIntOrNull() ?: continue
                val end = match.groupValues[2].toIntOrNull() ?: start
                val flag = match.groupValues[3]
                for (w in start..end) {
                    if (flag == "单" && w % 2 != 1) continue
                    if (flag == "双" && w % 2 != 0) continue
                    if (w !in weeks) weeks.add(w)
                }
            } else {
                val simple = Regex("""(\d+)(?:-(\d+))?""").find(seg.trim())
                if (simple != null) {
                    val start = simple.groupValues[1].toIntOrNull() ?: continue
                    val end = simple.groupValues[2].toIntOrNull() ?: start
                    for (w in start..end) { if (w !in weeks) weeks.add(w) }
                }
            }
        }
        return weeks.sorted()
    }

    private fun computeWeekType(weeks: List<Int>): String {
        if (weeks.isEmpty()) return "all"
        val allOdd = weeks.all { it % 2 == 1 }
        val allEven = weeks.all { it % 2 == 0 }
        return when {
            allOdd -> "odd"
            allEven -> "even"
            else -> "all"
        }
    }

    private fun translateError(msg: String): String {
        val lower = msg.lowercase()
        return when {
            lower.contains("ssl") || lower.contains("certificate") || lower.contains("pkix") ->
                "SSL证书验证失败，正在自动处理中，请重试"
            lower.contains("connect") && (lower.contains("refused") || lower.contains("timeout")) ->
                "无法连接到教务服务器，请检查网络连接或确认学校教务地址"
            lower.contains("unknownhost") || lower.contains("unable to resolve host") ->
                "无法解析教务系统地址，请确认学校教务网址正确"
            lower.contains("timeout") || lower.contains("timed out") ->
                "连接教务系统超时，请检查网络或稍后重试"
            lower.contains("eof") || lower.contains("unexpected end") ->
                "教务服务器连接中断，请重试或检查网络稳定性"
            lower.contains("reset") || lower.contains("connection reset") ->
                "教务服务器重置了连接，可能是触发了防火墙保护，请稍后重试"
            lower.contains("404") -> "教务系统页面不存在，该学校可能更换了教务系统地址"
            lower.contains("500") -> "教务系统服务器内部错误，请稍后重试"
            lower.contains("502") || lower.contains("503") ->
                "教务系统暂时不可用，可能是服务器繁忙或正在维护"
            lower.contains("403") -> "教务系统拒绝访问，可能需要校园网环境或VPN"
            lower.contains("401") -> "认证失败，学号或密码错误"
            lower.contains("redirect") || lower.contains("too many") ->
                "教务系统重定向次数过多，请检查地址是否正确"
            lower.contains("broken pipe") || lower.contains("ioexception") ->
                "网络连接中断，请检查网络稳定性后重试"
            else -> "网络连接失败，请检查网络和教务地址后重试"
        }
    }
}
