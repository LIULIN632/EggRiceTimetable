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
                // For schedule API/HTML pages, set Referer to the schedule query page itself
                if (path.contains("/kbcx/") || path.contains("/xskbcx") || path.contains("/xsgrkbcx")) {
                    val origin = "${url.scheme}://${url.host}" +
                            (if (url.port != 80 && url.port != 443) ":${url.port}" else "")
                    val prefix = path.substringBefore("/kbcx/").ifEmpty { path.substringBefore("/xskbcx") }
                        .ifEmpty { path.substringBefore("/xsgrkbcx") }
                    // Use the page's own URL as Referer, matching the AJAX request origin
                    val refererPath = if (path.contains("gnmkdm")) path else "$path?gnmkdm=N2151"
                    builder.header("Referer", "$origin$refererPath")
                    builder.header("X-Requested-With", "XMLHttpRequest")
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
                fetchScheduleV8(baseUrl, username, onProgress)
            } else {
                fetchScheduleLegacy(baseUrl, username, onProgress)
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

    private suspend fun fetchScheduleV8(baseUrl: String, username: String, onProgress: (String) -> Unit): List<CourseEntity> {
        onProgress("正在通过 API 获取课表...")
        val now = java.time.LocalDate.now()
        val (xnm, xqm) = if (now.monthValue >= 8 || now.monthValue <= 1) {
            now.year.toString() to "3"
        } else {
            (now.year - 1).toString() to "12"
        }

        // Try both semester combinations
        val semesterPairs = listOf(
            xnm to xqm,
            (if (xqm == "3") xnm else (xnm.toInt() - 1).toString()) to (if (xqm == "3") "12" else "3")
        ).distinct()

        // Try different API URL patterns (with/without gnmkdm, different codes)
        val apiUrlPatterns = listOf(
            "$baseUrl/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N2151",
            "$baseUrl/kbcx/xskbcx_cxXsgrkb.html?gnmkdm=N121603",
            "$baseUrl/kbcx/xskbcx_cxXsgrkb.html"
        )
        val bodyTemplates = listOf(
            "xnm={xnm}&xqm={xqm}&kzlx=ck&xsdm={xsdm}&kclbdm=",
            "xnm={xnm}&xqm={xqm}&xsdm={xsdm}",
            "xnm={xnm}&xqm={xqm}&xh={xh}",
            "xnm={xnm}&xqm={xqm}",
            "xn={xnm}&xq={xqm}"
        )

        var lastBody = ""
        for (apiPattern in apiUrlPatterns) {
            for (bodyTmpl in bodyTemplates) {
                for ((sn, sq) in semesterPairs) {
                    if (!currentCoroutineContext().isActive) break
                    val url = apiPattern
                    val body = bodyTmpl.replace("{xnm}", sn).replace("{xqm}", sq).replace("{xsdm}", username).replace("{xh}", username)
                    try {
                        val res = post(url, body)
                        val respBody = res.body?.string() ?: ""
                        res.close()
                        lastBody = respBody

                        // Try JSON parsing first
                        var courses = parseJsonSchedule(respBody)
                        if (courses.isNotEmpty()) return courses

                        // If JSON parsing found an array but couldn't parse courses, log details
                        val looksLikeJson = respBody.trimStart().startsWith("{") || respBody.trimStart().startsWith("[")
                        if (looksLikeJson && respBody.length > 100) {
                            android.util.Log.d("ZhengfangClient",
                                "JSON response (${respBody.length} chars) but parseJsonSchedule returned 0 courses. Preview: ${respBody.take(300)}")
                        }

                        // If the response looks like HTML, try HTML parsing
                        if (respBody.contains("<table", ignoreCase = true) ||
                            respBody.contains("kbgrid_table") || respBody.contains("kblist_table") ||
                            respBody.contains("timetable_con")) {
                            courses = parseScheduleHtmlV8Grid(respBody)
                            if (courses.isEmpty()) courses = parseScheduleHtmlV8List(respBody)
                            // Also try classic Table1 parsing (vertical timetables)
                            if (courses.isEmpty()) courses = parseHtmlScheduleTable(respBody)
                            if (courses.isNotEmpty()) return courses
                        }
                    } catch (_: Exception) { continue }
                }
            }
        }

        // Fallback: try the HTML schedule page directly
        onProgress("API 未返回数据，尝试页面解析...")
        val v8Html = fetchScheduleV8HtmlPage(baseUrl, xnm, xqm, username)
        if (v8Html.isNotEmpty()) {
            var courses = parseScheduleHtmlV8Grid(v8Html)
            if (courses.isEmpty()) courses = parseScheduleHtmlV8List(v8Html)
            // Try classic Table1 parsing (handles vertical timetables like YIT)
            if (courses.isEmpty()) courses = parseHtmlScheduleTable(v8Html)
            // Also try parsing the last JSON API response as HTML
            if (courses.isEmpty() && lastBody.isNotEmpty() &&
                (lastBody.contains("<table", ignoreCase = true) || lastBody.contains("kbgrid") || lastBody.contains("timetable_con"))) {
                courses = parseScheduleHtmlV8Grid(lastBody)
                if (courses.isEmpty()) courses = parseScheduleHtmlV8List(lastBody)
                if (courses.isEmpty()) courses = parseHtmlScheduleTable(lastBody)
            }
            return courses
        }

        // Last resort: try to parse the JSON API response body as HTML
        if (lastBody.isNotEmpty()) {
            var courses = parseScheduleHtmlV8Grid(lastBody)
            if (courses.isNotEmpty()) return courses
            courses = parseScheduleHtmlV8List(lastBody)
            if (courses.isNotEmpty()) return courses
            return parseHtmlScheduleTable(lastBody)
        }

        return emptyList()
    }

    // Fetch the V8 HTML schedule page (grid/list view)
    private fun fetchScheduleV8HtmlPage(baseUrl: String, xnm: String, xqm: String, username: String): String {
        // Try multiple URL patterns for the V8 schedule HTML page
        val gnCodes = listOf("N2151", "N121603", "N121601")
        val urlPatterns = listOf(
            "/kbcx/xskbcx_cxXskbcx.html",
            "/kbcx/xskbcx_cxKbcx.html",
            "/xskbcx.aspx",
            "/xsgrkbcx.aspx",
            "/xsdjkbcx.aspx"
        )
        val bodyParams = listOf(
            "xnm=$xnm&xqm=$xqm&kzlx=ck&xsdm=$username&kclbdm=",
            "xnm=$xnm&xqm=$xqm&xsdm=$username",
            "xnm=$xnm&xqm=$xqm&xh=$username",
            "xnm=$xnm&xqm=$xqm",
            "xn=$xnm&xq=$xqm"
        )

        for (pattern in urlPatterns) {
            for (gnCode in gnCodes) {
                val url = "$baseUrl$pattern?gnmkdm=$gnCode"
                for (body in bodyParams) {
                    try {
                        val res = post(url, body)
                        val html = res.body?.string() ?: ""
                        res.close()
                        if (html.length > 500 &&
                            (html.contains("table", ignoreCase = true) || html.contains("课程") ||
                             html.contains("课表") || html.contains("kbgrid") || html.contains("timetable_con"))) {
                            return html
                        }
                    } catch (_: Exception) { continue }
                }
            }
        }
        // Also try GET on common schedule page URLs
        for (pattern in urlPatterns.take(2)) {
            for (gnCode in gnCodes.take(1)) {
                try {
                    val res = get("$baseUrl$pattern?gnmkdm=$gnCode&xnm=$xnm&xqm=$xqm")
                    val html = res.body?.string() ?: ""
                    res.close()
                    if (html.length > 500 &&
                        (html.contains("table", ignoreCase = true) || html.contains("课程") ||
                         html.contains("课表") || html.contains("kbgrid") || html.contains("timetable_con"))) {
                        return html
                    }
                } catch (_: Exception) { continue }
            }
        }
        return ""
    }

    private fun parseJsonSchedule(json: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        try {
            val rootEl = JsonParser.parseString(json)

            // Handle root-is-array directly
            if (rootEl.isJsonArray) {
                android.util.Log.d("ZhengfangClient", "JSON root is array, size=${rootEl.asJsonArray.size()}")
                for (item in rootEl.asJsonArray) {
                    val obj = item.asJsonObject ?: continue
                    addCourseFromJsonObj(obj, courses)
                }
                android.util.Log.d("ZhengfangClient", "JSON root array: parsed ${courses.size} courses")
                return courses
            }

            if (!rootEl.isJsonObject) {
                android.util.Log.w("ZhengfangClient", "JSON root is not object or array")
                return courses
            }
            val root = rootEl.asJsonObject

            // Recursively find ALL arrays in the JSON — don't rely on known key names
            val allArrays = mutableListOf<com.google.gson.JsonArray>()
            findJsonArrays(root, allArrays)

            // Pick the best array: one that has course-like objects (largest with matching fields)
            var kbList: com.google.gson.JsonArray? = null
            var bestScore = 0
            for (arr in allArrays) {
                if (arr.size() == 0) continue
                val first = arr.get(0)
                if (!first.isJsonObject) continue
                val score = scoreCourseArray(arr)
                if (score > bestScore) {
                    bestScore = score
                    kbList = arr
                }
            }
            android.util.Log.d("ZhengfangClient", "Found ${allArrays.size} arrays, best score=$bestScore, size=${kbList?.size() ?: 0}")

            if (kbList == null || kbList.size() == 0) {
                android.util.Log.w("ZhengfangClient", "No course-like array found in JSON. Top-level keys: ${root.keySet()}")
                // Log first 500 chars of JSON for debugging
                android.util.Log.d("ZhengfangClient", "JSON preview: ${json.take(500)}")
                return courses
            }

            // Log first item keys for debugging
            if (kbList.size() > 0 && kbList.get(0).isJsonObject) {
                android.util.Log.d("ZhengfangClient", "First course keys: ${kbList.get(0).asJsonObject.keySet()}")
            }

            var rejectedName = 0; var rejectedDay = 0; var rejectedSection = 0
            for (item in kbList) {
                val obj = item.asJsonObject ?: continue
                val reason = addCourseFromJsonObj(obj, courses)
                when (reason) {
                    "name" -> rejectedName++
                    "day" -> rejectedDay++
                    "section" -> rejectedSection++
                }
            }
            android.util.Log.d("ZhengfangClient",
                "JSON parsed: ${courses.size} courses, rejected: name=$rejectedName day=$rejectedDay section=$rejectedSection")
        } catch (e: Exception) {
            android.util.Log.e("ZhengfangClient", "JSON schedule parse error: ${e.message}", e)
        }
        return courses
    }

    /** Recursively find all JSON arrays in a JsonObject tree. */
    private fun findJsonArrays(node: com.google.gson.JsonElement, result: MutableList<com.google.gson.JsonArray>) {
        when {
            node.isJsonArray -> {
                result.add(node.asJsonArray)
                for (item in node.asJsonArray) findJsonArrays(item, result)
            }
            node.isJsonObject -> {
                for ((_, value) in node.asJsonObject.entrySet()) findJsonArrays(value, result)
            }
        }
    }

    /** Score an array: how many items look like course data. */
    private fun scoreCourseArray(arr: com.google.gson.JsonArray): Int {
        var score = 0
        val sample = minOf(arr.size(), 3)
        for (i in 0 until sample) {
            val obj = arr.get(i)?.asJsonObject ?: continue
            val keys = obj.keySet()
            // Name-like
            if (keys.any { it.let { k -> hasNameField(k, obj.get(k)?.asString) } }) score += 3
            // Day-like
            if (keys.any { it.let { k -> hasDayField(k, obj.get(k)) } }) score += 2
            // Section-like
            if (keys.any { it.let { k -> hasSectionField(k, obj.get(k)) } }) score += 2
            // Weeks-like
            if (keys.any { it.let { k -> hasWeekField(k, obj.get(k)?.asString) } }) score += 1
            // Teacher-like
            if (keys.any { it.let { k -> hasTeacherField(k, obj.get(k)?.asString) } }) score += 1
        }
        return score
    }

    /** Extract a single course from a JSON object. Returns rejection reason or "ok". */
    private fun addCourseFromJsonObj(obj: com.google.gson.JsonObject, courses: MutableList<CourseEntity>): String {
        val keys = obj.keySet()
        val keySetStr = keys.joinToString(",")

        // ── Dynamic field matching: scan ALL keys ──
        var name = ""
        var teacher = ""
        var room = ""
        var day = 0
        var startSecInt = 0
        var endSecInt = 0
        var weekStr = ""
        var secStr = ""
        var kcsj = ""

        for (key in keys) {
            val value = obj.get(key) ?: continue
            val strVal = if (value.isJsonPrimitive) value.asString else ""

            // Name
            if (name.isEmpty() && hasNameField(key, strVal)) {
                name = strVal.replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            }
            // Day
            if (day == 0 && hasDayField(key, value)) {
                day = value.asInt.coerceIn(1, 7)
            }
            // Sections
            if (startSecInt == 0 && hasSectionField(key, value)) {
                if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                    startSecInt = value.asInt
                } else if (strVal.isNotEmpty()) {
                    val num = strVal.toIntOrNull()
                    if (num != null) startSecInt = num
                    else secStr = strVal // e.g. "1-2"
                }
            }
            // End section (separate field)
            if (endSecInt == 0 && hasEndSectionField(key, value)) {
                if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                    endSecInt = value.asInt
                } else {
                    val num = strVal.toIntOrNull()
                    if (num != null) endSecInt = num
                }
            }
            // Weeks
            if (weekStr.isEmpty() && hasWeekField(key, strVal)) {
                weekStr = strVal
            }
            // Teacher
            if (teacher.isEmpty() && hasTeacherField(key, strVal)) {
                teacher = strVal
            }
            // Room
            if (room.isEmpty() && hasRoomField(key, strVal)) {
                room = strVal
            }
            // KCSJ
            if (kcsj.isEmpty() && hasKcsjField(key)) {
                kcsj = strVal
            }
        }

        // ── Hardcoded fallback for known field names ──
        if (name.isEmpty()) name = oneOf(obj, "kcmc", "courseName", "name", "kcm", "coursename", "course_name", "kcmcView")
            .replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
        if (teacher.isEmpty()) teacher = oneOf(obj, "xm", "jsxm", "teacher", "jsxx", "teacherName", "skjs", "teacher_name", "jsm")
        if (room.isEmpty()) room = oneOf(obj, "cdmc", "jsmc", "room", "classroom", "location", "skdd", "place", "classRoom")
        if (day == 0) day = oneOfInt(obj, "xqj", "xq", "day", "weekDay", "dayOfWeek", "xingqi", "xinqi")
        if (day !in 1..7) {
            val dayName = oneOf(obj, "xqjmc", "dayName")
            day = when {
                dayName.contains("一") -> 1; dayName.contains("二") -> 2
                dayName.contains("三") -> 3; dayName.contains("四") -> 4
                dayName.contains("五") -> 5; dayName.contains("六") -> 6
                dayName.contains("日") || dayName.contains("天") -> 7
                else -> day
            }
        }
        if (startSecInt == 0) startSecInt = oneOfInt(obj, "ksjc", "djj", "startSection", "startSec", "qsz", "qjz")
        if (endSecInt == 0) endSecInt = oneOfInt(obj, "jsjc", "endSection", "endSec", "jsz")
        if (secStr.isEmpty()) secStr = oneOf(obj, "jcs", "jc", "sections", "jcdm", "sectionNo", "pkjc", "section", "jcsj")
        if (kcsj.isEmpty()) kcsj = oneOf(obj, "kcsj", "timeCode")
        if (weekStr.isEmpty()) weekStr = oneOf(obj, "zcd", "zhous", "weeks", "week", "weekRange", "skzc", "zcz", "kkzc")

        // Try kcsj for day+sections
        if (kcsj.length >= 5 && day !in 1..7) {
            day = kcsj.substring(0, 1).toIntOrNull() ?: 0
        }
        if (kcsj.length >= 5 && startSecInt == 0) {
            startSecInt = kcsj.substring(1, 3).toIntOrNull() ?: 0
            endSecInt = kcsj.substring(3, 5).toIntOrNull() ?: 0
            if (endSecInt == 0) endSecInt = startSecInt
        }

        // ── Validation ──
        if (name.isEmpty() || name.length < 2) {
            if (name.isNotEmpty()) android.util.Log.d("ZhengfangClient", "Rejected (name<2): '$name' keys=$keySetStr")
            return "name"
        }
        if (day !in 1..7 && kcsj.isEmpty()) {
            android.util.Log.d("ZhengfangClient", "Rejected (day=$day not in 1..7): name='$name' keys=$keySetStr")
            return "day"
        }
        if (secStr.isEmpty() && startSecInt == 0 && endSecInt == 0) {
            android.util.Log.d("ZhengfangClient", "Rejected (no section): name='$name' keys=$keySetStr")
            return "section"
        }

        val (startSlot, endSlot) = if (startSecInt > 0) {
            Pair(startSecInt, if (endSecInt >= startSecInt) endSecInt else startSecInt)
        } else {
            parseSections(secStr)
        }

        val weeks = parseWeeks(weekStr)
        val weekType = computeWeekType(weeks)

        courses.add(CourseEntity(
            name = name, teacher = teacher, room = room,
            dayOfWeek = day.coerceIn(1, 7), startSlot = startSlot, endSlot = endSlot,
            weekType = weekType, weeks = weeks.joinToString(",") { it.toString() },
            colorIndex = (day * 3 + startSlot) % 15
        ))
        return "ok"
    }

    // ── Dynamic field matchers ──

    private fun hasNameField(key: String, value: String?): Boolean {
        if (value.isNullOrBlank() || value.length < 2) return false
        val kl = key.lowercase()
        return kl.contains("kcmc") || kl.contains("coursename") || kl == "name" ||
            kl.contains("course") && kl.contains("name") ||
            kl == "kcm" || kl.contains("kcmcview") || kl.contains("title") ||
            kl == "cn" || kl.contains("classname")
    }

    private fun hasDayField(key: String, value: com.google.gson.JsonElement?): Boolean {
        if (value == null) return false
        val kl = key.lowercase()
        if (kl in listOf("xqj", "xq", "day", "weekday", "dayofweek", "xingqi", "xinqi", "week")) {
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                val n = value.asInt; return n in 1..7
            }
            if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                val s = value.asString
                return s.toIntOrNull()?.let { it in 1..7 } == true ||
                    s.contains("一") || s.contains("二") || s.contains("三") || s.contains("四") ||
                    s.contains("五") || s.contains("六") || s.contains("日")
            }
        }
        return false
    }

    private fun hasSectionField(key: String, value: com.google.gson.JsonElement?): Boolean {
        if (value == null) return false
        val kl = key.lowercase()
        if (kl in listOf("jcs", "jc", "sections", "jcdm", "sectionno", "pkjc", "section", "jcsj",
                "ksjc", "djj", "startsection", "startsec", "qsz", "qjz", "start", "begin")) {
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                val n = value.asInt; return n in 1..12
            }
            if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                val s = value.asString
                return s.toIntOrNull()?.let { it in 1..12 } == true || Regex("""\d+-\d+""").matches(s)
            }
        }
        return false
    }

    private fun hasEndSectionField(key: String, value: com.google.gson.JsonElement?): Boolean {
        if (value == null) return false
        val kl = key.lowercase()
        if (kl in listOf("jsjc", "endsection", "endsec", "jsz", "end")) {
            if (value.isJsonPrimitive && value.asJsonPrimitive.isNumber) {
                val n = value.asInt; return n in 1..12
            }
            if (value.isJsonPrimitive && value.asJsonPrimitive.isString) {
                val s = value.asString
                return s.toIntOrNull()?.let { it in 1..12 } == true
            }
        }
        return false
    }

    private fun hasWeekField(key: String, value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val kl = key.lowercase()
        return kl.contains("zcd") || kl.contains("zhous") || kl == "weeks" || kl == "week" ||
            kl.contains("weekrange") || kl.contains("skzc") || kl.contains("zcz") ||
            kl.contains("kkzc") || kl.contains("weekstr") ||
            (kl.contains("zc") && value.contains("周"))
    }

    private fun hasTeacherField(key: String, value: String?): Boolean {
        if (value.isNullOrBlank() || value.length < 2) return false
        val kl = key.lowercase()
        return kl == "xm" || kl.contains("jsxm") || kl == "teacher" || kl.contains("jsxx") ||
            kl.contains("teachername") || kl.contains("skjs") || kl.contains("teacher_name") ||
            kl == "jsm" || kl.contains("instructor")
    }

    private fun hasRoomField(key: String, value: String?): Boolean {
        if (value.isNullOrBlank()) return false
        val kl = key.lowercase()
        return kl.contains("cdmc") || kl.contains("jsmc") || kl == "room" || kl.contains("classroom") ||
            kl == "location" || kl.contains("skdd") || kl.contains("place") ||
            kl.contains("classroom") || kl.contains("address")
    }

    private fun hasKcsjField(key: String): Boolean {
        val kl = key.lowercase()
        return kl == "kcsj" || kl == "timecode" || kl == "time"
    }

    // ── V8 HTML grid parser — ported from Dawn-Course zhengfang.js parseNewZhengfang() ──
    // Handles the kbgrid_table_0 format: <td id="X-Y"> containing <div class="timetable_con"> blocks
    private fun parseScheduleHtmlV8Grid(html: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val tdRegex = Regex("""<td[^>]*\bid\s*=\s*["']?(\d+)-(\d+)["']?[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
        val blockSplitter = Regex("""<div\s+class=["']?timetable_con""", RegexOption.IGNORE_CASE)

        for (tdMatch in tdRegex.findAll(html)) {
            val day = tdMatch.groupValues[1].toIntOrNull() ?: continue
            if (day !in 1..7) continue
            val cellContent = tdMatch.groupValues[2] // the content inside the td

            // Split into individual course blocks
            val blocks = cellContent.split(blockSplitter)
            for (i in 1 until blocks.size) {
                val blockHtml = "<div class=\"timetable_con" + blocks[i]

                // Extract course name from .title element
                var name = extractNameFromBlock(blockHtml)

                // Extract teacher from title="教师" or title="老师"
                var teacher = extractTextByTitle(blockHtml, "教师")
                if (teacher.isEmpty()) teacher = extractTextByTitle(blockHtml, "老师")
                if (teacher.isNotEmpty()) teacher = cleanTeacherNameRegex(teacher)

                // Extract location from title="上课地点" or title="教室"
                var location = extractTextByTitle(blockHtml, "上课地点")
                if (location.isEmpty()) location = extractTextByTitle(blockHtml, "教室")
                if (location.isEmpty()) location = extractTextByTitle(blockHtml, "校区/上课地点")

                // Extract time info from title="节/周" or pattern (X-X节) X周
                var weeksStr = ""
                var sectionsStr = ""
                val timeText = extractTextByTitle(blockHtml, "节/周")
                if (timeText.isNotEmpty()) {
                    sectionsStr = extractSectionsStr(timeText)
                    weeksStr = extractWeeksStr(timeText)
                }

                // Fallback: pattern (X-X节) X周  in raw HTML
                val timeMatch = Regex("""[\(（](\d+(?:-\d+)?节)[\)）]\s*([^<]*周[^<]*)""").find(blockHtml)
                if (timeMatch != null) {
                    sectionsStr = timeMatch.groupValues[1]
                    weeksStr = timeMatch.groupValues[2]
                }

                // If still missing, try extracting from normalized text
                if (teacher.isEmpty() || location.isEmpty() || weeksStr.isEmpty() || sectionsStr.isEmpty()) {
                    val text = normalizeText(blockHtml)
                    if (teacher.isEmpty()) {
                        val tm = Regex("""教师\s*[:：]?\s*([^\s/，,;；]+)""").find(text)
                        if (tm != null) teacher = cleanTeacherNameRegex(tm.groupValues[1].trim())
                    }
                    if (location.isEmpty()) {
                        val lm = Regex("""上课地点\s*[:：]?\s*([^教师周数节次校区]+)""").find(text)
                        if (lm != null) location = lm.groupValues[1].trim()
                    }
                    if (weeksStr.isEmpty()) weeksStr = extractWeeksStr(text)
                    if (sectionsStr.isEmpty()) sectionsStr = extractSectionsStr(text)
                }

                if (name.isEmpty() || weeksStr.isEmpty() || sectionsStr.isEmpty()) continue

                val weeks = parseWeeks(weeksStr)
                val sections = parseSectionList(sectionsStr)
                if (weeks.isEmpty() || sections.isEmpty()) continue

                courses.add(CourseEntity(
                    name = name, teacher = teacher, room = location,
                    dayOfWeek = day,
                    startSlot = sections.first(), endSlot = sections.last(),
                    weekType = computeWeekType(weeks),
                    weeks = weeks.joinToString(",") { it.toString() },
                    colorIndex = (day * 3 + sections.first()) % 15
                ))
            }
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}" }
    }

    // ── V8 HTML list parser — ported from Dawn-Course zhengfang.js parserList() ──
    // Handles the kblist_table format with rows like <td id="jc_X-Y-Z">
    private fun parseScheduleHtmlV8List(html: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        // Match list rows: <td id="jc_DAY-START-END"> ... </td> followed by course info td
        val rowRegex = Regex(
            """<tr[^>]*>\s*<td[^>]*id=["']?jc_(\d+)-(\d+)-(\d+)["']?[^>]*>\s*</td>\s*<td[^>]*>([\s\S]*?)</td>\s*</tr>""",
            RegexOption.IGNORE_CASE
        )

        for (match in rowRegex.findAll(html)) {
            val day = match.groupValues[1].toIntOrNull() ?: continue
            val sectionStart = match.groupValues[2].toIntOrNull() ?: continue
            val sectionEnd = match.groupValues[3].toIntOrNull() ?: sectionStart
            val blockHtml = match.groupValues[4]

            var name = extractNameFromBlock(blockHtml)
            val text = normalizeText(blockHtml)

            var teacher = extractTextByTitle(blockHtml, "教师")
            if (teacher.isEmpty()) teacher = extractTextByTitle(blockHtml, "老师")
            if (teacher.isNotEmpty()) teacher = cleanTeacherNameRegex(teacher)
            if (teacher.isEmpty()) {
                val tm = Regex("""教师\s*[:：]?\s*([^\s/，,;；]+)""").find(text)
                if (tm != null) teacher = cleanTeacherNameRegex(tm.groupValues[1].trim())
            }

            var location = extractTextByTitle(blockHtml, "上课地点")
            if (location.isEmpty()) location = extractTextByTitle(blockHtml, "教室")
            if (location.isEmpty()) location = extractTextByTitle(blockHtml, "校区/上课地点")

            var weeksStr = extractWeeksStr(text)
            var sectionsStr = "$sectionStart-${sectionEnd}节"

            // Try to get better week info from title="节/周"
            val timeText = extractTextByTitle(blockHtml, "节/周")
            if (timeText.isNotEmpty() && !weeksStr.isNotEmpty()) {
                weeksStr = extractWeeksStr(timeText)
            }

            if (name.isEmpty() || weeksStr.isEmpty()) continue

            val weeks = parseWeeks(weeksStr)
            val sections = (sectionStart..sectionEnd).toList()
            if (weeks.isEmpty()) continue

            courses.add(CourseEntity(
                name = name, teacher = teacher, room = location,
                dayOfWeek = day,
                startSlot = sectionStart, endSlot = sectionEnd,
                weekType = computeWeekType(weeks),
                weeks = weeks.joinToString(",") { it.toString() },
                colorIndex = (day * 3 + sectionStart) % 15
            ))
        }

        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}" }
    }

    // ── Helper functions for HTML regex parsing (ported from zhengfang.js / common_parser_utils.js) ──

    private fun stripTags(html: String): String {
        var result = html
        var previous: String
        do {
            previous = result
            result = result.replace(Regex("<[^>]*>"), "")
        } while (result != previous)
        return result.replace(Regex("[<>]"), "")
    }

    private fun normalizeText(html: String): String {
        return stripTags(html).replace(Regex("\\s+"), " ").replace("：", ":").trim()
    }

    private fun extractNameFromBlock(blockHtml: String): String {
        // Match <div class="title"> or <u class="title"> or <span class="title">
        val titleMatch = Regex("""<([a-zA-Z]+)[^>]*class=["']?title[^>]*>([\s\S]*?)</\1>""", RegexOption.IGNORE_CASE)
            .find(blockHtml)
        if (titleMatch != null) {
            return stripTags(titleMatch.groupValues[2]).trim()
        }
        val altMatch = Regex("""<u[^>]*class=["']?title[^>]*>([\s\S]*?)</u>""", RegexOption.IGNORE_CASE)
            .find(blockHtml)
        if (altMatch != null) {
            return stripTags(altMatch.groupValues[1]).trim()
        }
        return ""
    }

    // Extract text from <span title="XXX"> or <font> after title span
    private fun extractTextByTitle(blockHtml: String, titleText: String): String {
        // Pattern 1: <span title="XXX">content</span>
        val patternInside = Regex(
            """<span[^>]*title\s*=\s*["']?\s*${Regex.escape(titleText)}\s*["']?[^>]*>([\s\S]*?)</span>""",
            RegexOption.IGNORE_CASE
        )
        val matchInside = patternInside.find(blockHtml)
        if (matchInside != null) {
            val content = stripTags(matchInside.groupValues[1]).trim()
            if (content.isNotEmpty()) return content
        }

        // Pattern 2: ...title="XXX"...</span><font>content</font>
        val patternAfter = Regex(
            """title\s*=\s*["']?\s*${Regex.escape(titleText)}\s*["']?[^>]*>[\s\S]*?</span>\s*<font[^>]*>([\s\S]*?)</font>""",
            RegexOption.IGNORE_CASE
        )
        val matchAfter = patternAfter.find(blockHtml)
        if (matchAfter != null) {
            return stripTags(matchAfter.groupValues[1]).trim()
        }
        return ""
    }

    private fun cleanTeacherNameRegex(raw: String): String {
        var text = stripTags(raw)
        text = text.replace(Regex("""教师\s*[:：]?\s*"""), "").trim()
        val keywordRegex = Regex("""(教学班组成|教学班|选课备注|考核方式|课程学时组成|总学时|学时|学分|班级|课程性质|课程类别)\s*[:：]?""")
        val match = keywordRegex.find(text)
        if (match != null) {
            text = text.substring(0, match.range.first).trim()
        }
        text = text.replace(Regex("""[，,;；]\s*$"""), "").trim()
        return text
    }

    private fun extractWeeksStr(text: String): String {
        val weeksMatch = Regex("""周数\s*[:：]?\s*([^教师节次校区]+?周[^教师节次校区]*)""").find(text)
        if (weeksMatch != null) return weeksMatch.groupValues[1].trim()
        val rangeMatch = Regex("""(\d+\s*[-至~～—－]\s*\d+\s*周[^\s]*)""").find(text)
        if (rangeMatch != null) return rangeMatch.groupValues[1].trim()
        val singleMatch = Regex("""(\d+\s*周[^\s]*)""").find(text)
        if (singleMatch != null) return singleMatch.groupValues[1].trim()
        return ""
    }

    private fun extractSectionsStr(text: String): String {
        val sectionMatch = Regex("""节次\s*[:：]?\s*(\d+)\s*[-至~～—－]\s*(\d+)""").find(text)
        if (sectionMatch != null) return sectionMatch.groupValues[1] + "-" + sectionMatch.groupValues[2] + "节"
        val rangeMatch = Regex("""第?\s*(\d+)\s*[-至~～—－]\s*(\d+)\s*节""").find(text)
        if (rangeMatch != null) return rangeMatch.groupValues[1] + "-" + rangeMatch.groupValues[2] + "节"
        val singleMatch = Regex("""第?\s*(\d+)\s*节""").find(text)
        if (singleMatch != null) return singleMatch.groupValues[1] + "节"
        return ""
    }

    // Parse section list from string like "1-2节" or "1-2"
    private fun parseSectionList(sectionsString: String): List<Int> {
        val sections = mutableListOf<Int>()
        var str = sectionsString
            .replace("第", "").replace("节次:", "").replace("节次：", "")
            .replace("节", "").replace("(", "").replace(")", "")
            .replace("（", "").replace("）", "")
            .replace(Regex("[至~～—－]"), "-")
        val parts = str.split("-")
        val start = parts[0].trim().toIntOrNull()
        val end = parts.getOrNull(1)?.trim()?.toIntOrNull() ?: start
        if (start != null && end != null) {
            for (s in start..end) sections.add(s)
        }
        return sections
    }

    private fun oneOf(obj: com.google.gson.JsonObject, vararg keys: String): String {
        for (k in keys) {
            val v = obj.get(k)?.asString
            if (!v.isNullOrEmpty()) return v
        }
        return ""
    }

    private fun oneOfInt(obj: com.google.gson.JsonObject, vararg keys: String): Int {
        for (k in keys) {
            val el = obj.get(k) ?: continue
            if (el.isJsonPrimitive) {
                val v = el.asJsonPrimitive
                if (v.isNumber()) return v.asInt
                if (v.isString) { val n = v.asString.toIntOrNull(); if (n != null) return n }
            }
        }
        return 0
    }

    // ── Legacy (non-V8) schedule: HTML page scrape ──
    private suspend fun fetchScheduleLegacy(baseUrl: String, username: String, onProgress: (String) -> Unit): List<CourseEntity> {
        // Try JSON API first — some IP-based deployments still support V8 API
        onProgress("正在尝试 API 获取课表...")
        val v8Courses = fetchScheduleV8(baseUrl, username) { /* silent */ }
        if (v8Courses.isNotEmpty()) return v8Courses

        // Try V8 HTML schedule page (some schools upgraded to V8 but labeled as non-V8)
        onProgress("正在尝试 V8 页面解析...")
        val now = java.time.LocalDate.now()
        val (xnm, xqm) = if (now.monthValue >= 8 || now.monthValue <= 1) {
            now.year.toString() to "3"
        } else {
            (now.year - 1).toString() to "12"
        }
        val v8Html = fetchScheduleV8HtmlPage(baseUrl, xnm, xqm, username)
        if (v8Html.isNotEmpty()) {
            val v8GridCourses = parseScheduleHtmlV8Grid(v8Html)
            if (v8GridCourses.isNotEmpty()) return v8GridCourses
            val v8ListCourses = parseScheduleHtmlV8List(v8Html)
            if (v8ListCourses.isNotEmpty()) return v8ListCourses
        }

        // Fall back to old-style HTML schedule page
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

    // Parse old-style ASPX schedule HTML table into CourseEntity list.
    // Handles BOTH horizontal (横排) and vertical (竖排) table layouts.
    private fun parseHtmlScheduleTable(html: String): List<CourseEntity> {
        // Try V8 grid format first (kbgrid_table_0 with timetable_con divs)
        val v8Grid = parseScheduleHtmlV8Grid(html)
        if (v8Grid.isNotEmpty()) return v8Grid

        // Try V8 list format (kblist_table with jc_X-Y-Z ids)
        val v8List = parseScheduleHtmlV8List(html)
        if (v8List.isNotEmpty()) return v8List

        // ── Classic Table1 parsing ──
        val tableRegex = Regex("""<table[^>]*id=["'](?:Table1|kbgrid_table_0)["'][^>]*>(.*?)</table>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val tableMatch = tableRegex.find(html)
        val tableHtml = tableMatch?.groupValues?.get(1) ?: html

        val rowRegex = Regex("""<tr[^>]*>(.*?)</tr>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
        val rows = rowRegex.findAll(tableHtml).toList()
        if (rows.size < 3) return emptyList()

        // ── Detect vertical layout ──
        val isVertical = detectVerticalTable(rows, html)
        if (isVertical) {
            return parseVerticalTable(rows, html)
        }

        // ── Horizontal layout parsing (existing logic) ──
        return parseHorizontalTable(rows, html)
    }

    /** Detect whether the timetable uses vertical (竖排) layout:
     *  - First column cells are slot labels (bare digits or "第X节" patterns)
     *  - YIT (烟台理工) domain is a strong signal */
    private fun detectVerticalTable(rows: List<MatchResult>, fullHtml: String): Boolean {
        // YIT domain auto-detection
        if (Regex("""yitsd\.edu\.cn""", RegexOption.IGNORE_CASE).containsMatchIn(fullHtml)) return true

        var slotLabelCount = 0
        val totalRows = minOf(rows.size, 8)
        for (i in 0 until totalRows) {
            val cells = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .findAll(rows[i].value).toList()
            if (cells.isEmpty()) continue
            val firstText = cells[0].groupValues[1].replace(Regex("<[^>]*>"), "").trim()
            if (isSlotLabel(firstText)) slotLabelCount++
        }
        // If first column has 2+ slot labels in first 8 rows, it's vertical
        return slotLabelCount >= 2
    }

    /** Parse vertical (竖排) timetable: rows=slots, columns=days. */
    private fun parseVerticalTable(rows: List<MatchResult>, fullHtml: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()
        val allRows = rows.map { row ->
            Regex("""<t[dh][^>]*>(.*?)</t[dh]>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .findAll(row.value).toList()
        }.filter { it.isNotEmpty() }

        if (allRows.isEmpty()) return courses

        val colors = listOf(0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11)

        // ── Build day→column mapping from header row ──
        val dayMap = mutableMapOf<Int, Int>()
        // Check first few rows for day headers
        for (ri in 0 until minOf(allRows.size, 4)) {
            allRows[ri].forEachIndexed { col, cell ->
                val text = cell.groupValues[1].replace(Regex("<[^>]*>"), "").trim()
                val day = parseDayHeader(text)
                if (day != null && day !in dayMap.values) dayMap[col] = day
            }
            if (dayMap.size >= 5) break // Enough days found
        }
        // Fallback: column 1+ = Mon-Sun
        if (dayMap.size < 3) {
            dayMap.clear()
            val maxCols = allRows.maxOfOrNull { it.size } ?: 0
            for (c in 1 until maxCols) dayMap[c] = c.coerceIn(1, 7)
        }

        android.util.Log.d("ZhengfangClient", "Vertical: dayMap=$dayMap, rows=${allRows.size}")

        // ── Pre-scan slot ranges for rowSpan lookahead ──
        val rowSlotRanges = mutableListOf<Pair<Int, Int>>()
        for (row in allRows) {
            val firstText = row.getOrNull(0)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            rowSlotRanges.add(parseSlotRange(firstText) ?: Pair(-1, -1))
        }

        // ── Parse each data row ──
        for (rowIdx in allRows.indices) {
            val row = allRows[rowIdx]
            val (startSlot, endSlot) = rowSlotRanges.getOrNull(rowIdx) ?: Pair(-1, -1)
            if (startSlot < 1) continue

            for (colIdx in row.indices) {
                if (colIdx == 0) continue // Skip slot label column
                val day = dayMap[colIdx] ?: continue
                if (day !in 1..7) continue

                val cellHtml = row[colIdx].groupValues[1]
                val text = cellHtml.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<[^>]*>"), "").trim()
                if (text.isBlank() || text.length < 2) continue
                if (Regex("""^[\d\s.\-/]+$""").matches(text)) continue

                // Handle rowSpan
                val rowSpan = Regex("""row[sS]pan\s*=\s*["'](\d+)["']""").find(cellHtml)
                    ?.groupValues?.get(1)?.toIntOrNull() ?: 1
                val cellEndSlot = if (rowSpan > 1) {
                    val lookAhead = rowIdx + rowSpan - 1
                    if (lookAhead < rowSlotRanges.size && rowSlotRanges[lookAhead].first > 0)
                        rowSlotRanges[lookAhead].second
                    else endSlot + rowSpan - 1
                } else endSlot

                // Parse course fields from <br>-separated lines
                val lines = text.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) continue

                // Split multi-course cells (e.g. 单双周 different courses)
                val blocks = splitCourseBlocks(lines)
                for (block in blocks) {
                    val (name, teacher, room, weeksRaw) = extractCourseFields(block)
                    if (name.isEmpty() || name.length < 2) continue
                    if (!isValidCourseName(name)) continue

                    val weeks = parseWeeks(weeksRaw)
                    if (weeks.isEmpty()) continue

                    courses.add(CourseEntity(
                        name = name, teacher = teacher, room = room,
                        dayOfWeek = day, startSlot = startSlot, endSlot = cellEndSlot,
                        weeks = weeks.joinToString(",") { it.toString() },
                        weekType = computeWeekType(weeks),
                        colorIndex = colors[courses.size % colors.size]
                    ))
                }
            }
        }

        android.util.Log.d("ZhengfangClient", "Vertical: parsed ${courses.size} courses")
        return courses.distinctBy { "${it.name}_${it.dayOfWeek}_${it.startSlot}" }
    }


    /** Existing horizontal (横排) table parsing — rows=slots, columns=days. */
    private fun parseHorizontalTable(rows: List<MatchResult>, html: String): List<CourseEntity> {
        val courses = mutableListOf<CourseEntity>()

        // Parse header row for day-of-week mapping
        val headerCells = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""",
            setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
            .findAll(rows[0].value).toList()

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
            val cells = Regex("""<t[dh][^>]*>(.*?)</t[dh]>""",
                setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
                .findAll(row.value).toList()
            if (cells.isEmpty()) continue

            val firstCellText = cells.getOrNull(0)?.groupValues?.get(1)
                ?.replace(Regex("<[^>]*>"), "")?.trim() ?: ""
            val slotMatch = Regex("""第\s*(\d+)\s*节""").find(firstCellText)
            if (slotMatch != null) {
                currentSlot = slotMatch.groupValues[1].toIntOrNull() ?: (currentSlot + 1)
            } else if (firstCellText.isNotEmpty() &&
                !firstCellText.contains("上午") && !firstCellText.contains("下午") && !firstCellText.contains("晚上")) {
                currentSlot++
            }

            for ((colIdx, cell) in cells.withIndex()) {
                val day = dayColumnMap[colIdx] ?: continue
                val cellHtml = cell.groupValues[1]
                val cellText = cellHtml.replace(Regex("<br[^>]*>", RegexOption.IGNORE_CASE), "\n")
                    .replace(Regex("<[^>]*>"), "").trim()
                if (cellText.isBlank() || cellText.length < 2) continue
                if (Regex("""^[\d\s.]*$""").matches(cellText)) continue

                val lines = cellText.split("\n").map { it.trim() }.filter { it.isNotBlank() }
                if (lines.isEmpty()) continue

                val name = lines.getOrElse(0) { "" }.replace("&nbsp;", "").trim()
                val teacher = lines.getOrElse(1) { "" }.replace("&nbsp;", "").trim()
                val room = lines.getOrElse(2) { "" }.replace("&nbsp;", "").trim()
                val weeksRaw = lines.getOrElse(3) { "" }.replace("&nbsp;", "").trim()
                if (name.isEmpty()) continue

                val weeks = parseWeeks(weeksRaw.ifEmpty {
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

        // Method 2: If horizontal found nothing, try regex-based extraction
        if (courses.isEmpty()) {
            val allCellContent = Regex("""<td[^>]*>([\s\S]*?)</td>""", RegexOption.IGNORE_CASE)
                .findAll(html)
                .map { it.groupValues[1].replace(Regex("<[^>]*>"), "").trim() }
                .filter { it.length > 5 && Regex("""[一-鿿]""").containsMatchIn(it) }
                .toList()

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
                        dayOfWeek = inferredDay, startSlot = inferredSlot, endSlot = inferredSlot + 1,
                        weekType = computeWeekType(weeks),
                        weeks = weeks.joinToString(",") { it.toString() },
                        colorIndex = (inferredDay * 3 + inferredSlot) % 15
                    ))
                    inferredSlot++
                    if (inferredSlot > 12) { inferredSlot = 1; inferredDay++ }
                    if (inferredDay > 7) break
                    i += 4
                } else { i++ }
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
