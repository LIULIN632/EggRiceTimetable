package com.eggrice.timetable

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.eggrice.timetable.network.SchoolIndexUpdater
import com.eggrice.timetable.ui.navigation.AppNavigation
import com.eggrice.timetable.ui.theme.EggRiceTheme
import com.eggrice.timetable.ui.theme.ThemeType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TimetableApplication
        setContent {
            val darkMode by app.appContainer.darkMode.collectAsState()
            val isDark = when (darkMode) {
                "dark" -> true
                "system" -> androidx.compose.foundation.isSystemInDarkTheme()
                else -> false
            }
            val colorThemeKey by app.appContainer.colorTheme.collectAsState()
            val themeType = ThemeType.fromKey(colorThemeKey)

            // 启动时自动检查学校索引（「自动检查更新」开启时，静默拉取，有新版本才提示）
            val context = LocalContext.current
            LaunchedEffect(Unit) {
                val container = app.appContainer
                if (container.autoUpdate.value) {
                    val result = withContext(Dispatchers.IO) { container.schoolIndexUpdater.update() }
                    if (result is SchoolIndexUpdater.Result.Updated) {
                        container.schoolRegistry.reload()
                        Toast.makeText(
                            context,
                            "学校列表已更新（${result.schoolCount} 所学校）",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            }

            EggRiceTheme(themeType = themeType, darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
