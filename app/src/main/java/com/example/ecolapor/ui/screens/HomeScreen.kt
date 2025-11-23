package com.example.ecolapor.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val reports = viewModel.reports
    val primaryColor = MaterialTheme.colorScheme.primary
    
    // Pisahkan laporan berdasarkan status
    val savedReports = reports.filter { it.status == "Tersimpan" }
    val sentReports = reports.filter { it.status == "Terkirim" || it.status == "Selesai" }

    // State untuk mengontrol Dialog Logout
    var showLogoutDialog by remember { mutableStateOf(false) }
    
    // Pull to refresh state
    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshReports()
            // Simulate refresh delay
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                isRefreshing = false
            }
        }
    )

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
        var selectedReport by remember { mutableStateOf<Report?>(null) }
            val context = androidx.compose.ui.platform.LocalContext.current
            
            // Dialog untuk draft report (Tersimpan)
            if (selectedReport != null && selectedReport!!.status == "Tersimpan") {
                AlertDialog(
                    onDismissRequest = { selectedReport = null },
                    title = { Text("Opsi Laporan Draft") },
                    text = { Text("Pilih aksi untuk laporan yang tersimpan") },
                    confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    viewModel.sendDraftReport(selectedReport!!)
                                    selectedReport = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📤 Kirim Laporan", color = Color(0xFF4CAF50))
                            }
                            TextButton(
                                onClick = {
                                    val report = selectedReport!!
                                    val lat = report.location?.latitude ?: 0.0
                                    val lng = report.location?.longitude ?: 0.0
                                    
                                    if (lat != 0.0 && lng != 0.0) {
                                        val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(Lokasi Laporan)")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")
                                        
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            // Fallback jika Google Maps tidak ada
                                            val browserIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                            )
                                            context.startActivity(browserIntent)
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context, 
                                            "Lokasi tidak tersedia", 
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    selectedReport = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📍 Lacak Lokasi", color = Color(0xFF2196F3))
                            }
                            TextButton(
                                onClick = {
                                    viewModel.deleteDraftReport(selectedReport!!.id)
                                    selectedReport = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🗑️ Hapus Laporan", color = Color.Red)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedReport = null }) {
                            Text("Batal")
                        }
                    }
                )
            }
            
            // Dialog untuk sent report (Terkirim)
            if (selectedReport != null && selectedReport!!.status == "Terkirim") {
                AlertDialog(
                    onDismissRequest = { selectedReport = null },
                    title = { Text("Opsi Laporan") },
                    text = { Text("Pilih aksi untuk laporan ini") },
                    confirmButton = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(
                                onClick = {
                                    val report = selectedReport!!
                                    val lat = report.location?.latitude ?: 0.0
                                    val lng = report.location?.longitude ?: 0.0
                                    
                                    if (lat != 0.0 && lng != 0.0) {
                                        val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(Lokasi Laporan)")
                                        val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, uri)
                                        intent.setPackage("com.google.android.apps.maps")
                                        
                                        try {
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            val browserIntent = android.content.Intent(
                                                android.content.Intent.ACTION_VIEW,
                                                android.net.Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
                                            )
                                            context.startActivity(browserIntent)
                                        }
                                    } else {
                                        android.widget.Toast.makeText(
                                            context, 
                                            "Lokasi tidak tersedia", 
                                            android.widget.Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                    selectedReport = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("📍 Lacak Lokasi", color = Color(0xFF2196F3))
                            }
                            TextButton(
                                onClick = {
                                    viewModel.deleteSentReport(selectedReport!!.id)
                                    selectedReport = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("🗑️ Hapus Laporan", color = Color.Red)
                            }
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { selectedReport = null }) {
                            Text("Batal")
                        }
                    }
                )
            }
            
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .pullRefresh(pullRefreshState)
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                // Section: Tersimpan (Draft)
                if (savedReports.isNotEmpty()) {
                    item {
                        Text(
                            text = "📝 Tersimpan (${savedReports.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(savedReports) { report ->
                        ReportItem(
                            report = report,
                            onReportClick = {
                                selectedReport = report
                            }
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
                
                // Section: Terkirim
                if (sentReports.isNotEmpty()) {
                    item {
                        Text(
                            text = "📤 Terkirim (${sentReports.size})",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    }
                    items(sentReports) { report ->
                        ReportItem(
                            report = report,
                            onReportClick = {
                                selectedReport = report
                            }
                        )
                    }
                }
                
                // Empty state
                if (savedReports.isEmpty() && sentReports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 100.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Belum ada laporan", color = Color.Gray)
                        }
                    }
                }
                }
                
                PullRefreshIndicator(
                    refreshing = isRefreshing,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
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
fun ReportItem(
    report: Report, 
    onReportClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReportClick() },
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
                        color = when (report.status) {
                            "Terkirim" -> Color(0xFF4CAF50)  // Hijau
                            "Tersimpan" -> Color(0xFFFFC107)  // Kuning
                            "Selesai" -> Color(0xFF2196F3)   // Biru
                            else -> Color.Gray
                        }
                    ) {
                        Text(
                            text = report.status,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            fontSize = 12.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Bold
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