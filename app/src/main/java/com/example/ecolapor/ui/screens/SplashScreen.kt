package com.example.ecolapor.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.ecolapor.R
import com.example.ecolapor.ui.Screen
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(navController: NavController) {
    // Logika: Tunggu 2.5 detik, lalu pindah ke Welcome
    LaunchedEffect(Unit) {
        delay(2500)
        navController.navigate(Screen.Welcome.route) {
            popUpTo(Screen.Splash.route) { inclusive = true }
        }
    }

    // Tampilan UI
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.primary), // Warna Hijau Utama
        contentAlignment = Alignment.Center
    ) {
        Column(
            // --- PERUBAHAN DISINI ---
            // Menambahkan offset (geser) vertikal ke bawah sebanyak 40dp.
            // Anda bisa menambah angkanya jika ingin lebih ke bawah lagi (misal 60.dp).
            modifier = Modifier.offset(y = 40.dp),
            // ------------------------
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // 1. LOGO GAMBAR PUTIH
            Image(
                painter = painterResource(id = R.drawable.logo_ecolapor_white),
                contentDescription = "Logo EcoLapor",
                modifier = Modifier.size(120.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // 2. TEKS JUDUL
            Text(
                text = "EcoLapor",
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            // 3. Slogan
            Text(
                text = "Turn trash into a blessing",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}