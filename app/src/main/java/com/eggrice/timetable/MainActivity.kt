package com.eggrice.timetable

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.eggrice.timetable.ui.navigation.AppNavigation
import com.eggrice.timetable.ui.theme.EggRiceTheme
import com.eggrice.timetable.ui.theme.ThemeType

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
            EggRiceTheme(themeType = themeType, darkTheme = isDark) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }
}
