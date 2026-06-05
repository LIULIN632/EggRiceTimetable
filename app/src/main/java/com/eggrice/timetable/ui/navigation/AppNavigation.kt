package com.eggrice.timetable.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.eggrice.timetable.ui.profile.ProfileScreen
import com.eggrice.timetable.ui.theme.*
import com.eggrice.timetable.ui.timetable.TimetableScreen

data class BottomNavItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

private val bottomNavItems = listOf(
    BottomNavItem("timetable", "课程", Icons.Outlined.CalendarMonth),
    BottomNavItem("profile", "我的", Icons.Outlined.Person)
)

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    var isSubPage by remember { mutableStateOf(false) }

    val colors = LocalEggRiceColors.current

    Scaffold(
        bottomBar = {
            if (!isSubPage) {
                NavigationBar(
                containerColor = colors.surfaceCard,
                tonalElevation = 4.dp,
                modifier = Modifier.shadow(12.dp).height(56.dp)
            ) {
                bottomNavItems.forEach { item ->
                    val selected = currentDestination?.hierarchy?.any { it.route == item.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                item.icon,
                                contentDescription = item.label,
                                modifier = Modifier.size(20.dp),
                                tint = if (selected) colors.accentMain else colors.textTertiary
                            )
                        },
                        label = {
                            Text(
                                item.label,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) colors.accentMain else colors.textTertiary
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor = colors.surfaceHighlight
                        )
                    )
                }
            }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = "timetable",
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(200)) + slideInHorizontally { it / 24 } },
            exitTransition = { fadeOut(tween(200)) }
        ) {
            composable("timetable") {
                TimetableScreen(onSubPageChange = { isSubPage = it })
            }
            composable("profile") {
                ProfileScreen(
                    onSubPageChange = { isSubPage = it },
                    onBack = {
                        navController.navigate("timetable") {
                            popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                            launchSingleTop = true
                        }
                    }
                )
            }
        }
    }
}
