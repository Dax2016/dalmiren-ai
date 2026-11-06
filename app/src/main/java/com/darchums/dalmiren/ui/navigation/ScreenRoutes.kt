package com.darchums.dalmiren.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoGraph
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector

sealed class ScreenRoute(val route: String) {
    object Onboarding : ScreenRoute("onboarding")
    object Dashboard : ScreenRoute("dashboard")
    object Companion : ScreenRoute("companion")
    object Behaviour : ScreenRoute("behaviour")
    object Emotional : ScreenRoute("emotional")
    object Communication : ScreenRoute("communication")
    object Discipline : ScreenRoute("discipline")
    object Learning : ScreenRoute("learning")
    object Insights : ScreenRoute("insights")
    object Settings : ScreenRoute("settings")
}

sealed class BottomNavItem(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Dashboard : BottomNavItem(ScreenRoute.Dashboard.route, "Home", Icons.Default.Home)
    object Companion : BottomNavItem(ScreenRoute.Companion.route, "AI Companion", Icons.Default.Psychology)
    object Insights : BottomNavItem(ScreenRoute.Insights.route, "Insights", Icons.Default.AutoGraph)
    object Settings : BottomNavItem(ScreenRoute.Settings.route, "Settings", Icons.Default.Settings)
}

val bottomNavItems = listOf(
    BottomNavItem.Dashboard,
    BottomNavItem.Companion,
    BottomNavItem.Insights,
    BottomNavItem.Settings
)
