package com.example.ecolapor.ui

sealed class Screen(val route: String) {
    object Splash : Screen("splash")
    object Welcome : Screen("welcome")
    object Login : Screen("login")
    object Register : Screen("register")
    object Home : Screen("home")
    object AddReport : Screen("add_report")
    object History : Screen("history")
}