package com.example.ecolapor

import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.ecolapor.ui.Screen
import com.example.ecolapor.ui.screens.AddReportScreen
import com.example.ecolapor.ui.screens.HomeScreen
import com.example.ecolapor.ui.screens.LoginScreen
import com.example.ecolapor.ui.screens.RegisterScreen
import com.example.ecolapor.ui.screens.SplashScreen
import com.example.ecolapor.ui.screens.WelcomeScreen
import com.example.ecolapor.ui.theme.EcoLaporTheme
import com.google.firebase.auth.FirebaseAuth

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Sembunyikan navigation bar dan status bar
        hideSystemUI()
        
        enableEdgeToEdge()
        setContent {
            EcoLaporTheme {
                val navController = rememberNavController()

                // CEK STATUS LOGIN
                // Jika user tidak null (artinya sudah login), langsung ke Home
                // Jika null, mulai dari Splash seperti biasa
                val startDest = if (FirebaseAuth.getInstance().currentUser != null) {
                    Screen.Home.route
                } else {
                    Screen.Splash.route
                }

                NavHost(
                    navController = navController,
                    startDestination = startDest // Gunakan variabel dinamis ini
                ) {
                    composable(Screen.Splash.route) {
                        SplashScreen(navController)
                    }
                    composable(Screen.Welcome.route) {
                        WelcomeScreen(navController)
                    }
                    composable(Screen.Login.route) {
                        LoginScreen(navController)
                    }
                    composable(Screen.Register.route) {
                        RegisterScreen(navController)
                    }
                    composable(Screen.Home.route) {
                        HomeScreen(navController)
                    }
                    composable(Screen.AddReport.route) {
                        AddReportScreen(navController)
                    }
                }
            }
        }
    }
    
    private fun hideSystemUI() {
        // Get window insets controller
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        
        // Configure behavior
        windowInsetsController.systemBarsBehavior = 
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        
        // Hide system bars (navigation and status bar)
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
        
        // Make sure status bar is translucent
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