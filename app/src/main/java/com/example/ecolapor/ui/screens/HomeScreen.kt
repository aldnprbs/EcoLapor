package com.example.ecolapor.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.ui.HomeViewModel
import com.example.ecolapor.ui.Screen
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val reports = viewModel.reports
    val primaryColor = MaterialTheme.colorScheme.primary

    // State untuk mengontrol Dialog Logout
    var showLogoutDialog by remember { mutableStateOf(false) }

    // --- DIALOG KONFIRMASI LOGOUT ---
    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("Logout Confirmation") },
            text = { Text("Are you sure you want to logout?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        viewModel.logout()
                        // Arahkan kembali ke halaman Welcome (bukan Login langsung, biar rapi)
                        navController.navigate(Screen.Welcome.route) {
                            popUpTo(Screen.Home.route) { inclusive = true }
                        }
                    }
                ) {
                    Text("Yes, Logout", color = Color.Red) // Warna merah biar tegas
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Laporan Warga", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = {
                        // Saat tombol ditekan, jangan langsung logout, tapi munculkan dialog dulu
                        showLogoutDialog = true
                    }) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddReport.route) },
                containerColor = primaryColor,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, contentDescription = "Buat Laporan")
            }
        }
    ) { innerPadding ->
        if (reports.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("Belum ada laporan masuk.", color = Color.Gray)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(reports) { report ->
                    ReportItem(report)
                }
            }
        }
    }
}

@Composable
fun ReportImageLoader(imageUrl: String, modifier: Modifier = Modifier) {
    var isLoading by remember { mutableStateOf(true) }
    var hasError by remember { mutableStateOf(false) }
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    
    // Check if it's a Base64 data URL
    if (imageUrl.startsWith("data:image/")) {
        // Decode Base64 asynchronously to avoid blocking UI
        LaunchedEffect(imageUrl) {
            try {
                val base64String = imageUrl.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                
                if (decodedBitmap != null) {
                    bitmap = decodedBitmap
                    hasError = false
                } else {
                    hasError = true
                }
            } catch (e: Exception) {
                hasError = true
                android.util.Log.e("ReportImageLoader", "Failed to decode Base64: ${e.message}")
            } finally {
                isLoading = false
            }
        }
        
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.size(40.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                hasError -> {
                    Text("Gambar error", color = Color.Gray, fontSize = 12.sp)
                }
                bitmap != null -> {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Foto Laporan",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        }
    } else if (imageUrl.startsWith("http")) {
        // Regular URL from Firebase Storage - use Coil
        AsyncImage(
            model = imageUrl,
            contentDescription = "Foto Laporan",
            modifier = modifier,
            contentScale = ContentScale.Crop,
            onLoading = { isLoading = true },
            onSuccess = { isLoading = false },
            onError = { 
                isLoading = false
                hasError = true
            }
        )
    } else {
        // Empty or invalid URL
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada gambar", color = Color.Gray, fontSize = 12.sp)
        }
    }
}

@Composable
fun ReportItem(report: Report) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column {
            // Tampilkan gambar jika URL tidak kosong
            if (report.imageUrl.isNotBlank()) {
                ReportImageLoader(
                    imageUrl = report.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AssistChip(
                        onClick = {},
                        label = { Text(report.category) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.secondaryContainer
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (report.status == "Selesai") Color(0xFF4CAF50) else Color(0xFFFFC107)
                    ) {
                        Text(
                            text = report.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = if (report.status == "Selesai") Color.White else Color.Black
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, null, modifier = Modifier.size(16.dp), tint = Color.Gray)
                    Spacer(modifier = Modifier.width(4.dp))

                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    val dateString = report.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "-"

                    Text(text = dateString, style = MaterialTheme.typography.bodySmall, color = Color.Gray)
                }
            }
        }
    }
}