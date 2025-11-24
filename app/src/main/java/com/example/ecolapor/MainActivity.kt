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

                    // Welcome Screen - Elegant fade dengan slight scale
                    composable(
                        Screen.Welcome.route,
                        enterTransition = {
                            fadeIn(animationSpec = tween(600)) +
                                    scaleIn(
                                        animationSpec = tween(600),
                                        initialScale = 0.95f
                                    )
                        },
                        exitTransition = {
                            fadeOut(animationSpec = tween(400)) +
                                    scaleOut(
                                        animationSpec = tween(400),
                                        targetScale = 1.05f
                                    )
                        }
                    ) {
                        WelcomeScreen(navController)
                    }

                    // Auth Screens (Login & Register) - Smooth horizontal slide dengan fade
                    composable(
                        Screen.Login.route,
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
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
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
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
                        LoginScreen(navController)
                    }

                    composable(
                        Screen.Register.route,
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
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
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
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
                        RegisterScreen(navController)
                    }

                    // Home Screen - Hero entrance dari bawah dengan scale
                    composable(
                        Screen.Home.route,
                        enterTransition = {
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { fullHeight -> (fullHeight * 0.8).toInt() }
                            ) + fadeIn(animationSpec = tween(500)) +
                                    scaleIn(
                                        animationSpec = tween(600),
                                        initialScale = 0.9f
                                    )
                        },
                        exitTransition = {
                            slideOutVertically(
                                animationSpec = tween(450),
                                targetOffsetY = { fullHeight -> -fullHeight }
                            ) + fadeOut(animationSpec = tween(400))
                        },
                        popEnterTransition = {
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioMediumBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                ),
                                initialOffsetY = { fullHeight -> -fullHeight }
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        popExitTransition = {
                            slideOutVertically(
                                animationSpec = tween(450),
                                targetOffsetY = { fullHeight -> (fullHeight * 0.8).toInt() }
                            ) + fadeOut(animationSpec = tween(400))
                        }
                    ) {
                        HomeScreen(navController)
                    }

                    // Add Report Screen - Modal slide dari bawah
                    composable(
                        Screen.AddReport.route,
                        enterTransition = {
                            slideInVertically(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessLow
                                ),
                                initialOffsetY = { fullHeight -> fullHeight }
                            ) + fadeIn(animationSpec = tween(400))
                        },
                        exitTransition = {
                            slideOutVertically(
                                animationSpec = tween(350),
                                targetOffsetY = { fullHeight -> fullHeight }
                            ) + fadeOut(animationSpec = tween(300))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300))
                        },
                        popExitTransition = {
                            slideOutVertically(
                                animationSpec = tween(350),
                                targetOffsetY = { fullHeight -> fullHeight }
                            ) + fadeOut(animationSpec = tween(300))
                        }
                    ) {
                        AddReportScreen(navController)
                    }

                    // Profile Screen - Elegant slide dari kanan
                    composable(
                        Screen.Profile.route,
                        enterTransition = {
                            slideInHorizontally(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                ),
                                initialOffsetX = { fullWidth -> (fullWidth * 0.3).toInt() }
                            ) + fadeIn(animationSpec = tween(500))
                        },
                        exitTransition = {
                            slideOutHorizontally(
                                animationSpec = tween(400),
                                targetOffsetX = { fullWidth -> (fullWidth * 0.3).toInt() }
                            ) + fadeOut(animationSpec = tween(350))
                        },
                        popEnterTransition = {
                            fadeIn(animationSpec = tween(300)) +
                                    slideInHorizontally(
                                        animationSpec = tween(400),
                                        initialOffsetX = { -it / 3 }
                                    )
                        },
                        popExitTransition = {
                            fadeOut(animationSpec = tween(300)) +
                                    slideOutHorizontally(
                                        animationSpec = tween(400),
                                        targetOffsetX = { it / 3 }
                                    )
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