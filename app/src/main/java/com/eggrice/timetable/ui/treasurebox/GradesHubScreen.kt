package com.eggrice.timetable.ui.treasurebox

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.eggrice.timetable.data.SchoolRegistry
import com.eggrice.timetable.di.AppContainer
import com.eggrice.timetable.network.ZhengfangClient
import com.eggrice.timetable.ui.theme.*

/**
 * 课程与成绩（合并入口）：
 * - Tab 1 课程管理：按学期分组展示已保存的课程成绩，可从教务一键同步
 * - Tab 2 查询成绩：在线查成绩，可导出到课程管理
 * 两个 ViewModel 都挂在本页，切 Tab 不丢状态；共享 ZhengfangClient 单例会话。
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GradesHubScreen(
    onBack: () -> Unit,
    client: ZhengfangClient,
    schoolRegistry: SchoolRegistry,
    appContainer: AppContainer
) {
    val colors = LocalEggRiceColors.current
    var tabIndex by remember { mutableIntStateOf(0) }

    val savedViewModel: SavedGradesViewModel = viewModel(
        factory = SavedGradesViewModel.Factory(client, schoolRegistry, appContainer)
    )
    val gradeViewModel: GradeQueryViewModel = viewModel(
        factory = GradeQueryViewModel.Factory(client, schoolRegistry, appContainer)
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("课程与成绩", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "返回")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.surfaceCard)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(colors.surfaceAlt)
        ) {
            TabRow(
                selectedTabIndex = tabIndex,
                containerColor = colors.surfaceCard,
                contentColor = colors.accentMain
            ) {
                Tab(
                    selected = tabIndex == 0,
                    onClick = { tabIndex = 0 },
                    text = {
                        Text(
                            "课程管理",
                            fontWeight = if (tabIndex == 0) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
                Tab(
                    selected = tabIndex == 1,
                    onClick = { tabIndex = 1 },
                    text = {
                        Text(
                            "查询成绩",
                            fontWeight = if (tabIndex == 1) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
            when (tabIndex) {
                0 -> SavedGradesContent(savedViewModel)
                else -> GradeQueryContent(gradeViewModel)
            }
        }
    }
}
