package com.eggrice.timetable.ui.import_

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.net.http.SslError
import android.view.ViewGroup
import android.webkit.*
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.TimetableApplication
import com.eggrice.timetable.data.JwSystemType
import com.eggrice.timetable.data.School
import com.eggrice.timetable.ui.components.SchoolFavoriteButton
import com.eggrice.timetable.network.CookieStore
import com.eggrice.timetable.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebImportScreen(
    onBack: () -> Unit,
    freeMode: Boolean = false,
    sessionKey: Int = 0
) {
    val colors = LocalEggRiceColors.current
    val isDark = LocalDarkMode.current
    val context = LocalContext.current
    val app = context.applicationContext as TimetableApplication
    val viewModel: WebImportViewModel = viewModel(
        factory = WebImportViewModel.Factory(app.repository, app.appContainer.schoolRegistry, app.appContainer) { app.appContainer.activeSchemeId.value }
    )

    // Reset to school list when entering in non-free mode (each sessionKey = fresh entry)
    LaunchedEffect(sessionKey) {
        if (!freeMode) {
            viewModel.clearSchool()
        }
    }

    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedSchool by viewModel.selectedSchool.collectAsState()
    val filteredSchools by viewModel.filteredSchools.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val importedCount by viewModel.importedCount.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val toastMessage by viewModel.toastMessage.collectAsState()
    val parseLogs by viewModel.parseLogs.collectAsState()
    val customUrl by viewModel.customUrl.collectAsState()
    val freeUrlActive by viewModel.freeUrlActive.collectAsState()
    val urlHistory by viewModel.urlHistory.collectAsState()

    var loadingProgress by remember { mutableFloatStateOf(0f) }
    var currentUrl by remember { mutableStateOf("") }
    var pageTitle by remember { mutableStateOf("教务登录") }
    var showLogs by remember { mutableStateOf(false) }
    var showUrlEditor by remember { mutableStateOf(false) }
    var urlEditText by remember { mutableStateOf("") }
    var showFreeUrlDialog by remember { mutableStateOf(freeMode) }
    var freeUrlInput by remember { mutableStateOf("") }
    var showCredentialSheet by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(true) }

    // Initialize URL history from SharedPreferences
    LaunchedEffect(Unit) {
        viewModel.initUrlHistory(context)
    }

    // Show toast messages
    LaunchedEffect(toastMessage) {
        toastMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.toastShown()
        }
    }

    // Load saved account when school changes
    LaunchedEffect(selectedSchool) {
        val baseUrl = viewModel.getEffectiveUrl()?.trimEnd('/') ?: selectedSchool?.baseUrl?.trimEnd('/')
        if (!baseUrl.isNullOrBlank()) {
            viewModel.loadSavedAccount(baseUrl)
        }
    }

    // ScheduleBridge — connects JS to ViewModel
    val scheduleBridge = remember {
        ScheduleBridge(
            viewModel = viewModel,
            onHtmlCaptured = { html -> viewModel.onHtmlCaptured(html) }
        )
    }

    // WebView instance
    val webView = remember {
        WebView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            with(settings) {
                javaScriptEnabled = true
                domStorageEnabled = true
                loadWithOverviewMode = true
                useWideViewPort = true
                builtInZoomControls = true
                displayZoomControls = false
                mixedContentMode = WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
                allowFileAccess = false
                @Suppress("DEPRECATION")
                savePassword = true
                @Suppress("DEPRECATION")
                saveFormData = true
                cacheMode = WebSettings.LOAD_NO_CACHE
                // Allow access to asset files for JS adapter scripts
                @Suppress("DEPRECATION")
                allowFileAccessFromFileURLs = true
            }
            addJavascriptInterface(scheduleBridge, "AndroidBridge")
        }
    }

    // Give ViewModel access to WebView for promise resolution
    LaunchedEffect(webView) {
        viewModel.webView = webView
    }

    // Configure WebView client when school changes
    LaunchedEffect(selectedSchool) {
        webView.webViewClient = object : WebViewClient() {
            override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                loadingProgress = 0f
                url?.let { currentUrl = it }
                // 页面开始加载即作废旧页面的待执行注入重试，防止跳转到非登录页后
                // 延迟任务仍把账号密码填入新页面（如成绩查询页的输入框）。
                viewModel.cancelPendingInjections()
            }

            override fun onPageFinished(view: WebView?, url: String?) {
                loadingProgress = 1f
                // Inject bridge promise polyfill after every page navigation
                viewModel.injectBridgePromise()
                // Auto-fill saved credentials into WebView login form
                viewModel.injectCredentialsToWebView(url)
                // Extract and persist cookies for OkHttp reuse
                CookieStore.extractFromWebView(url)
            }

            override fun onReceivedSslError(
                view: WebView?, handler: SslErrorHandler?, error: SslError?
            ) {
                handler?.proceed()
            }
        }
        webView.webChromeClient = object : WebChromeClient() {
            override fun onProgressChanged(view: WebView?, newProgress: Int) {
                loadingProgress = newProgress / 100f
            }
            override fun onReceivedTitle(view: WebView?, title: String?) {
                title?.let { pageTitle = it }
            }
        }
    }

    // Cleanup
    DisposableEffect(Unit) {
        onDispose {
            viewModel.flushAutoSave()
            viewModel.cancelPendingInjections()
            webView.stopLoading()
            webView.clearCache(true)
            CookieManager.getInstance().removeAllCookies(null)
            webView.destroy()
        }
    }

    // 系统返回：网页可回退则先回退网页，再逐级退出（防止直接关闭整个导入界面）
    BackHandler {
        if (webView.canGoBack()) {
            webView.goBack()
        } else if (freeUrlActive) {
            viewModel.exitFreeUrlMode()
        } else if (selectedSchool != null) {
            viewModel.clearSchool()
        } else {
            onBack()
        }
    }

    Scaffold(
        floatingActionButton = {
            if (selectedSchool != null || freeUrlActive) {
                FloatingActionButton(
                    onClick = { showCredentialSheet = true; passwordVisible = true },
                    containerColor = if (viewModel.hasSavedCredentials) Color(0xFF4CAF50) else colors.surfaceCard,
                    contentColor = if (viewModel.hasSavedCredentials) Color.White else accentColor(),
                    modifier = Modifier.size(40.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        if (viewModel.hasSavedCredentials) Icons.Outlined.Lock else Icons.Outlined.VpnKey,
                        null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        topBar = {
            TopAppBar(
                title = {
                    if (selectedSchool != null) {
                        Column {
                            Text(selectedSchool!!.name, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(pageTitle, fontSize = 11.sp, color = colors.textTertiary, maxLines = 1)
                        }
                    } else if (freeUrlActive) {
                        Column {
                            Text("通用导入", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Text(pageTitle, fontSize = 11.sp, color = colors.textTertiary, maxLines = 1)
                        }
                    } else {
                        Text("WebView导入", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (freeUrlActive) viewModel.exitFreeUrlMode()
                        else if (selectedSchool != null) viewModel.clearSchool()
                        else onBack()
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                actions = {
                    if (selectedSchool != null || freeUrlActive) {
                        // URL edit button
                        IconButton(onClick = {
                            urlEditText = customUrl.ifEmpty { selectedSchool?.baseUrl ?: "" }
                            showUrlEditor = true
                        }) {
                            Icon(Icons.Outlined.Edit, "修改网址", tint = accentColor())
                        }
                        IconButton(onClick = { showLogs = !showLogs }) {
                            Icon(Icons.Outlined.Terminal, "日志")
                        }
                        IconButton(onClick = { webView.reload() }) {
                            Icon(Icons.Filled.Refresh, "刷新")
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        },
        bottomBar = {
            if (selectedSchool != null || freeUrlActive) {
                Surface(
                    color = colors.surfaceCard,
                    shadowElevation = 8.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp)
                    ) {
                        // URL hint
                        if (customUrl.isNotEmpty()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Outlined.Link, null, tint = accentColor(), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    "自定义网址: ${customUrl.take(50)}${if (customUrl.length > 50) "..." else ""}",
                                    fontSize = 11.sp, color = accentColor(), maxLines = 1
                                )
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Cookie status
                        val hasCookies = remember { mutableStateOf(CookieStore.hasCookies()) }
                        LaunchedEffect(currentUrl) {
                            hasCookies.value = CookieStore.hasCookies()
                        }
                        if (hasCookies.value) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.Lock, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Cookie 已保存", fontSize = 11.sp, color = Color(0xFF4CAF50))
                            }
                            Spacer(Modifier.height(4.dp))
                        }

                        // Status hint
                        if (importedCount > 0) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF4CAF50), modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("已导入 $importedCount 门课程", fontSize = 12.sp, color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
                            }
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Outlined.Info, null, tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("提示：登录后进入课表页面，点击「制作」导入", fontSize = 11.sp, color = colors.textTertiary)
                            }
                        }

                        Spacer(Modifier.height(8.dp))

                        // 制作按钮 — JS 适配器一键导入
                        Button(
                            onClick = {
                                if (!isImporting) {
                                    viewModel.executeJsAdapter()
                                }
                            },
                            enabled = !isImporting,
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor(),
                                contentColor = Color.White
                            )
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                                Text("解析中…", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(Icons.Outlined.FileDownload, null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("一键导入", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                            }
                        }

                        // OkHttp 直接抓取 — 使用 WebView Cookie
                        if (hasCookies.value) {
                            Spacer(Modifier.height(6.dp))
                            OutlinedButton(
                                onClick = {
                                    if (!isImporting) {
                                        viewModel.fetchCourseViaOkHttp(currentUrl)
                                    }
                                },
                                enabled = !isImporting,
                                modifier = Modifier.fillMaxWidth().height(40.dp),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, accentColor())
                            ) {
                                Icon(Icons.Outlined.Wifi, null, tint = accentColor(), modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("OkHttp 直接抓取", fontSize = 13.sp, color = accentColor())
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->
        Box(modifier = Modifier.padding(padding).fillMaxSize()) {
            if (selectedSchool == null && !freeUrlActive) {
                // School selection screen + 通用导入 entry
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Text("选择学校", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text("登录教务系统后进入课表页面，点击「一键导入」自动识别课程", fontSize = 13.sp, color = colors.textTertiary)
                    }
                    // 添加自己的学校入口 — 手动添加学校名称+网址
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(accentColor().copy(alpha = 0.12f))
                                .clickable { showFreeUrlDialog = true }
                                .padding(horizontal = 14.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.AddLink, null, tint = accentColor(), modifier = Modifier.size(24.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text("添加自己的学校", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = accentColor())
                                Text("输入学校名称和教务系统地址，自动保存到列表", fontSize = 12.sp, color = colors.textSecondary)
                            }
                            Icon(Icons.Outlined.Add, null, tint = accentColor(), modifier = Modifier.size(20.dp))
                        }
                    }
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { viewModel.updateSearch(it) },
                            placeholder = { Text("搜索学校名称或城市") },
                            leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textTertiary) },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(8.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = accentColor(),
                                unfocusedBorderColor = colors.borderDivider
                            )
                        )
                    }
                    if (filteredSchools.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("未找到学校", fontSize = 13.sp, color = colors.textTertiary)
                            }
                        }
                    } else {
                        val favSchools = filteredSchools.filter { it.id in favoriteIds }
                        val otherSchools = filteredSchools.filter { it.id !in favoriteIds }

                        // ── Favorites section ──
                        if (favSchools.isNotEmpty()) {
                            item {
                                Text(
                                    "⭐ 我的收藏",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                            items(favSchools) { school ->
                                SchoolRow(
                                    school = school,
                                    isFavorite = true,
                                    onSelect = {
                                        viewModel.selectSchool(school)
                                        viewModel.setCustomUrl("")
                                        val loadUrl = viewModel.getEffectiveUrl() ?: school.baseUrl
                                        webView.loadUrl(loadUrl.trimEnd('/'))
                                    },
                                    onToggleFavorite = { viewModel.toggleFavorite(school) }
                                )
                            }
                            item {
                                HorizontalDivider(
                                    color = colors.textTertiary.copy(alpha = 0.3f),
                                    thickness = 1.dp,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            item {
                                Text(
                                    "全部学校",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                                )
                            }
                        }

                        // ── All schools section ──
                        items(otherSchools) { school ->
                            SchoolRow(
                                school = school,
                                isFavorite = school.id in favoriteIds,
                                onSelect = {
                                    viewModel.selectSchool(school)
                                    viewModel.setCustomUrl("")
                                    val loadUrl = viewModel.getEffectiveUrl() ?: school.baseUrl
                                    webView.loadUrl(loadUrl.trimEnd('/'))
                                },
                                onToggleFavorite = { viewModel.toggleFavorite(school) }
                            )
                        }
                    }
                }
            } else {
                // WebView
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { webView },
                    update = {}
                )

                // Loading progress
                if (loadingProgress < 1f) {
                    LinearProgressIndicator(
                        progress = { loadingProgress },
                        modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
                        color = accentColor(),
                        trackColor = Color.Transparent
                    )
                }

                // Import result overlay
                if (importedCount > 0) {
                    Card(
                        modifier = Modifier.align(Alignment.TopCenter).padding(16.dp),
                        colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1B3B23) else Color(0xFFE8F5E9)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Outlined.CheckCircle, null, tint = Color(0xFF4CAF50))
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("导入成功！", fontWeight = FontWeight.ExtraBold, color = colors.textPrimary)
                                Text("已导入 $importedCount 门课程", fontSize = 13.sp, color = colors.textSecondary)
                            }
                            Spacer(Modifier.weight(1f))
                            TextButton(onClick = onBack) { Text("返回课表", color = accentColor()) }
                        }
                    }
                }

                // Parsing logs overlay
                if (showLogs && parseLogs.isNotEmpty()) {
                    Card(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("解析日志", color = SurfaceCard, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { showLogs = false }) {
                                    Text("关闭", color = Color(0xFF90CAF9), fontSize = 12.sp)
                                }
                            }
                            Spacer(Modifier.height(6.dp))
                            Column(
                                modifier = Modifier.verticalScroll(rememberScrollState())
                            ) {
                                parseLogs.forEach { log ->
                                    Text(
                                        log,
                                        color = Color(0xFFAAAAAA),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ── Credential BottomSheet ──
    if (showCredentialSheet) {
        ModalBottomSheet(
            onDismissRequest = { showCredentialSheet = false },
            containerColor = colors.surfaceCard,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    "登录凭证",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = colors.textPrimary
                )
                Text(
                    "填写后将加密保存在本机，下次打开自动填充到网页表单",
                    fontSize = 12.sp,
                    color = colors.textTertiary
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = viewModel.username,
                    onValueChange = { viewModel.username = it; viewModel.scheduleAutoSave() },
                    label = { Text("学号/工号") },
                    placeholder = { Text("输入学号或工号") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Outlined.Person, null, tint = colors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor(),
                        unfocusedBorderColor = colors.borderDivider
                    )
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = viewModel.password,
                    onValueChange = { viewModel.password = it; viewModel.scheduleAutoSave() },
                    label = { Text("密码") },
                    placeholder = { Text("输入教务系统密码") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { passwordVisible = !passwordVisible }) {
                            Icon(
                                if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                                if (passwordVisible) "隐藏密码" else "显示密码",
                                tint = colors.textTertiary
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    leadingIcon = { Icon(Icons.Outlined.Lock, null, tint = colors.textTertiary) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = accentColor(),
                        unfocusedBorderColor = colors.borderDivider
                    )
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = viewModel.isRememberChecked,
                        onCheckedChange = {
                            viewModel.isRememberChecked = it
                            if (!it) viewModel.clearSavedCredentials()
                        },
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentColor(),
                            uncheckedColor = colors.textTertiary
                        )
                    )
                    Text(
                        "记住账号密码",
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            viewModel.saveCredentials()
                            viewModel.injectCredentialsToWebView()
                            showCredentialSheet = false
                            Toast.makeText(context, "已保存并填充到网页", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.weight(1f).height(46.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                    ) {
                        Icon(Icons.Outlined.Save, null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("填充到网页", fontWeight = FontWeight.Bold)
                    }
                    if (viewModel.hasSavedCredentials) {
                        OutlinedButton(
                            onClick = {
                                viewModel.clearSavedCredentials()
                                showCredentialSheet = false
                                Toast.makeText(context, "已清除保存的账号", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.weight(1f).height(46.dp),
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, Color(0xFFE57373))
                        ) {
                            Icon(Icons.Outlined.Delete, null, tint = Color(0xFFE57373), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("清除", color = Color(0xFFE57373), fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }

    // ── Add Custom School Dialog (添加自己的学校) ──
    if (showFreeUrlDialog) {
        var schoolNameInput by remember { mutableStateOf("") }
        var selectedJwType by remember { mutableStateOf(JwSystemType.ZHENGFANG) }
        var showJwTypePicker by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = {
                showFreeUrlDialog = false
            },
            title = {
                Text("添加自己的学校", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column {
                    Text(
                        "输入你的学校名称和教务系统网址，\n添加后将自动保存到学校列表。",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(
                        value = schoolNameInput,
                        onValueChange = { schoolNameInput = it },
                        placeholder = { Text("例如：北京大学", color = colors.textTertiary) },
                        label = { Text("学校名称") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            cursorColor = accentColor()
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = freeUrlInput,
                        onValueChange = { freeUrlInput = it },
                        placeholder = { Text("https://jiaowu.example.edu.cn", color = colors.textTertiary) },
                        label = { Text("教务系统网址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            cursorColor = accentColor()
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    // 教务类型选择
                    Text("教务系统类型", fontSize = 13.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(accentSoftColor().copy(alpha = 0.3f))
                            .clickable { showJwTypePicker = true }
                            .padding(horizontal = 12.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            selectedJwType.label,
                            fontSize = 14.sp,
                            color = accentColor(),
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            selectedJwType.description,
                            fontSize = 11.sp,
                            color = colors.textTertiary,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Outlined.ChevronRight, null, tint = colors.textTertiary, modifier = Modifier.size(18.dp))
                    }

                    // URL history
                    if (urlHistory.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        Text("最近使用", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = colors.textSecondary)
                        Spacer(Modifier.height(6.dp))
                        Column(
                            modifier = Modifier.heightIn(max = 120.dp).verticalScroll(rememberScrollState())
                        ) {
                            urlHistory.take(4).forEach { url ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(6.dp))
                                        .clickable {
                                            freeUrlInput = url
                                            // Try to extract school name from URL
                                            val host = try {
                                                java.net.URL(url).host.removePrefix("jw").removePrefix("jiaowu").removePrefix("www.")
                                                    .split(".").firstOrNull()?.replaceFirstChar { it.uppercase() } ?: ""
                                            } catch (_: Exception) { "" }
                                            if (schoolNameInput.isBlank() && host.isNotBlank()) {
                                                schoolNameInput = host
                                            }
                                        }
                                        .padding(vertical = 6.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Outlined.History, null, tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        url.take(40) + if (url.length > 40) "..." else "",
                                        fontSize = 12.sp,
                                        color = accentColor(),
                                        maxLines = 1,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        onClick = { viewModel.removeUrlFromHistory(url) },
                                        modifier = Modifier.size(24.dp)
                                    ) {
                                        Icon(Icons.Outlined.Close, "删除", tint = colors.textTertiary, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        var url = freeUrlInput.trim()
                        val name = schoolNameInput.trim()
                        if (url.isNotEmpty() && name.isNotEmpty()) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            // Save as custom school
                            viewModel.addCustomSchool(name, url, selectedJwType)
                            viewModel.enterFreeUrlMode(url)
                            webView.loadUrl(url)
                            showFreeUrlDialog = false
                        }
                    },
                    enabled = freeUrlInput.isNotBlank() && schoolNameInput.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                ) {
                    Icon(Icons.Outlined.Add, null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("添加学校")
                }
            },
            dismissButton = {
                Row {
                    // Quick URL-only mode: skip name, just open the URL
                    TextButton(onClick = {
                        var url = freeUrlInput.trim()
                        if (url.isNotEmpty()) {
                            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                                url = "https://$url"
                            }
                            viewModel.enterFreeUrlMode(url)
                            webView.loadUrl(url)
                            showFreeUrlDialog = false
                        }
                    }) {
                        Text("仅打开", color = colors.textTertiary, fontSize = 13.sp)
                    }
                    TextButton(onClick = {
                        showFreeUrlDialog = false
                    }) {
                        Text("取消")
                    }
                }
            }
        )

        // ── 教务类型选择器 ──
        if (showJwTypePicker) {
            AlertDialog(
                onDismissRequest = { showJwTypePicker = false },
                title = { Text("选择教务系统类型", fontWeight = FontWeight.ExtraBold) },
                text = {
                    Column {
                        JwSystemType.values().forEach { type ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (selectedJwType == type) accentSoftColor().copy(alpha = 0.5f)
                                        else Color.Transparent
                                    )
                                    .clickable { selectedJwType = type; showJwTypePicker = false }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = selectedJwType == type,
                                    onClick = { selectedJwType = type; showJwTypePicker = false },
                                    colors = RadioButtonDefaults.colors(selectedColor = accentColor())
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(type.label, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                                    Text(type.description, fontSize = 12.sp, color = colors.textTertiary)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showJwTypePicker = false }) { Text("取消") }
                }
            )
        }
    }

    // ── URL Edit Dialog ──
    if (showUrlEditor) {
        AlertDialog(
            onDismissRequest = { showUrlEditor = false },
            title = {
                Text("修改教务网址", fontWeight = FontWeight.ExtraBold)
            },
            text = {
                Column {
                    Text(
                        "如需使用自定义教务地址（如IP直接访问、非标端口等），请在下方输入完整网址。\n\n当前地址：${customUrl.ifEmpty { selectedSchool?.baseUrl ?: "" }}",
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        lineHeight = 18.sp
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = urlEditText,
                        onValueChange = { urlEditText = it },
                        placeholder = { Text("https://jiaowu.example.edu.cn", color = colors.textTertiary) },
                        label = { Text("教务系统网址") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = accentColor(),
                            cursorColor = accentColor()
                        )
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "提示：修改后请点击「应用」，将重新加载页面。\n支持 http/https，可带端口号（如 :8080）",
                        fontSize = 11.sp,
                        color = colors.textTertiary,
                        lineHeight = 14.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val url = urlEditText.trim()
                        if (url.isNotEmpty()) {
                            viewModel.setCustomUrl(url)
                            webView.loadUrl(url)
                        }
                        showUrlEditor = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = accentColor())
                ) {
                    Text("应用")
                }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        // Reset to school default or clear in free mode
                        viewModel.setCustomUrl("")
                        val resetUrl = selectedSchool?.baseUrl?.trimEnd('/') ?: ""
                        if (resetUrl.isNotEmpty()) webView.loadUrl(resetUrl)
                        showUrlEditor = false
                    }) {
                        Text("恢复默认", color = Color(0xFFE57373))
                    }
                    TextButton(onClick = { showUrlEditor = false }) {
                        Text("取消")
                    }
                }
            }
        )
    }
}


@Composable
private fun SchoolRow(
    school: School,
    isFavorite: Boolean,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = LocalEggRiceColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(accentSoftColor().copy(alpha = 0.3f))
            .clickable { onSelect() }
            .padding(start = 12.dp, top = 14.dp, bottom = 14.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.School, null, tint = accentColor(), modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(school.name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = colors.textPrimary)
                Spacer(Modifier.width(6.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = accentSoftColor()
                ) {
                    Text(
                        school.jwType.label,
                        fontSize = 11.sp,
                        color = accentColor(),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
            Text(school.baseUrl, fontSize = 11.sp, color = colors.textTertiary)
        }
        SchoolFavoriteButton(
            isFavorite = isFavorite,
            onToggle = onToggleFavorite
        )
        Icon(Icons.Outlined.ChevronRight, null, tint = colors.textTertiary, modifier = Modifier.size(20.dp))
    }
}
