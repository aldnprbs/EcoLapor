package com.example.ecolapor

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ecolapor.ui.Screen
import com.example.ecolapor.ui.screens.*
import com.example.ecolapor.ui.theme.EcoLaporTheme
import com.example.ecolapor.ui.theme.ThemeManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemUI()

        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val isDarkMode by ThemeManager.getDarkModeFlow(context)
                .collectAsState(initial = false)

            ThemeManager.isDarkMode.value = isDarkMode

            EcoLaporTheme(darkTheme = isDarkMode) {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = Screen.Splash.route
                ) {
                    // Splash Screen - Fade in dengan bounce effect
                    composable(
                        Screen.Splash.route,
                        enterTransition = {
                            fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                )
                            ) + scaleIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialScale = 0.8f
                            )
                        },
                        exitTransition = {
                            fadeOut(
                                animationSpec = tween(400)
                            ) + scaleOut(
                                animationSpec = tween(400),
                                targetScale = 1.2f
                            )
                        }
                    ) {
                        SplashScreen(navController)
                    }

                    // Welcome Screen - Simple fade
                    composable(
                        Screen.Welcome.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        WelcomeScreen(navController)
                    }

// Login Screen - Simple fade
                    composable(
                        Screen.Login.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        LoginScreen(navController)
                    }

// Register Screen - Simple fade
                    composable(
                        Screen.Register.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        RegisterScreen(navController)
                    }

                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }

                    // Add Report Screen - Slide dari kanan saat masuk, ke kiri saat keluar
                    composable(
                        Screen.AddReport.route,
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        AddReportScreen(navController)
                    }


                    // Profile Screen - Slide dari kanan saat masuk, ke kiri saat keluar
                    composable(
                        Screen.Profile.route,
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { fullWidth -> fullWidth }
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            slideInHorizontally(
                                animationSpec = tween(400),
                                initialOffsetX = { fullWidth -> -fullWidth }
                            ) + fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> fullWidth }
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        ProfileScreen(navController)
                    }
                }
            }
        }
    }

    private fun hideSystemUI() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)

        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())

        window.setFlags(
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
        )
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUI()
        }
    }
}