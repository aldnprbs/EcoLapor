package com.example.ecolapor.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
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
    val reports by viewModel.reports.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var searchQuery by remember { mutableStateOf("") }

    val filteredReports = reports.filter { report ->
        searchQuery.isEmpty() ||
                report.category.contains(searchQuery, ignoreCase = true) ||
                report.description.contains(searchQuery, ignoreCase = true) ||
                report.status.contains(searchQuery, ignoreCase = true)
    }

    val savedReports = filteredReports.filter { it.status == "Tersimpan" }
    val sentReports = filteredReports.filter { it.status == "Terkirim" || it.status == "Selesai" }

    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    val pullRefreshState = rememberPullRefreshState(
        refreshing = isRefreshing,
        onRefresh = {
            isRefreshing = true
            viewModel.refreshReports()
            CoroutineScope(Dispatchers.Main).launch {
                delay(1000)
                isRefreshing = false
            }
        }
    )

    val context = LocalContext.current

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = {
                Text(
                    "Konfirmasi Logout",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Text(
                        "Apakah Anda yakin ingin logout?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Anda harus login kembali untuk menggunakan aplikasi.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray.copy(alpha = 0.7f)
                    )
                }
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            showLogoutDialog = false
                            viewModel.logout()
                            navController.navigate(Screen.Welcome.route) {
                                popUpTo(Screen.Home.route) { inclusive = true }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = "Logout",
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            "Ya, Logout",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    OutlinedButton(
                        onClick = { showLogoutDialog = false },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.Gray
                        )
                    ) {
                        Text(
                            "Batal",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (selectedReport != null && selectedReport!!.status == "Tersimpan") {
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            title = {
                Text(
                    "Opsi Laporan Draft",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Pilih aksi untuk laporan yang tersimpan",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = {
                            viewModel.sendDraftReport(selectedReport!!)
                            selectedReport = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF4CAF50)
                        )
                    ) {
                        Text(
                            "Kirim Laporan",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    Button(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            "Lacak Lokasi",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.deleteReport(selectedReport!!.id)
                            selectedReport = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text(
                            "Hapus Laporan",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedReport = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text(
                        "Batal",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    if (selectedReport != null && (selectedReport!!.status == "Terkirim" || selectedReport!!.status == "Selesai")) {
        AlertDialog(
            onDismissRequest = { selectedReport = null },
            title = {
                Text(
                    "Opsi Laporan",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    "Pilih aksi untuk laporan ini",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            },
            confirmButton = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF2196F3)
                        )
                    ) {
                        Text(
                            "Lacak Lokasi",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }

                    Button(
                        onClick = {
                            viewModel.deleteReport(selectedReport!!.id)
                            selectedReport = null
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFF44336)
                        )
                    ) {
                        Text(
                            "Hapus Laporan",
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp
                        )
                    }
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = { selectedReport = null },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.Gray
                    )
                ) {
                    Text(
                        "Batal",
                        fontWeight = FontWeight.Medium,
                        fontSize = 16.sp
                    )
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(20.dp)
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "EcoLapor",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = {
                        android.widget.Toast.makeText(
                            context,
                            "Fitur notifikasi coming soon!",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }) {
                        Icon(
                            Icons.Default.Notifications,
                            contentDescription = "Notifikasi",
                            tint = Color.White
                        )
                    }
                    IconButton(onClick = {
                        navController.navigate(Screen.Profile.route)
                    }) {
                        Icon(
                            Icons.Default.Person,
                            contentDescription = "Profil",
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.AddReport.route) },
                containerColor = primaryColor,
                contentColor = Color.White,
                modifier = Modifier
                    .size(65.dp)
                    .shadow(8.dp, CircleShape)
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Buat Laporan",
                    modifier = Modifier.size(30.dp)
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .pullRefresh(pullRefreshState)
                .background(Color(0xFFF8F9FA))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Column {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = primaryColor
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.linearGradient(
                                            colors = listOf(
                                                primaryColor,
                                                Color(0xFF1976D2)
                                            )
                                        )
                                    )
                            ) {
                                Column(
                                    modifier = Modifier
                                        .padding(20.dp)
                                        .align(Alignment.CenterStart)
                                ) {
                                    Text(
                                        "Selamat Datang!",
                                        style = MaterialTheme.typography.titleLarge,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        "Laporkan masalah lingkungan di sekitarmu",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = Color.White.copy(alpha = 0.9f)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        TextField(
                            value = searchQuery,
                            onValueChange = { query ->
                                searchQuery = query
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            placeholder = { Text("Cari laporan...") },
                            leadingIcon = {
                                Icon(
                                    Icons.Default.Search,
                                    contentDescription = "Search",
                                    tint = Color.Gray
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" }
                                    ) {
                                        Icon(
                                            Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            tint = Color.Gray
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                unfocusedContainerColor = surfaceColor,
                                focusedContainerColor = surfaceColor,
                                unfocusedIndicatorColor = Color.Transparent,
                                focusedIndicatorColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            StatCard(
                                title = "Total",
                                value = reports.size.toString(),
                                color = Color(0xFF4CAF50),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Tersimpan",
                                value = reports.filter { it.status == "Tersimpan" }.size.toString(),
                                color = Color(0xFFFFC107),
                                modifier = Modifier.weight(1f)
                            )
                            StatCard(
                                title = "Terkirim",
                                value = reports.filter { it.status == "Terkirim" || it.status == "Selesai" }.size.toString(),
                                color = Color(0xFF2196F3),
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                if (savedReports.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "📝 Draft Tersimpan",
                            count = savedReports.size,
                            subtitle = "Laporan yang belum dikirim"
                        )
                    }
                    items(savedReports) { report ->
                        ModernReportItem(
                            report = report,
                            onReportClick = {
                                selectedReport = report
                            }
                        )
                    }
                }

                if (sentReports.isNotEmpty()) {
                    item {
                        SectionHeader(
                            title = "📤 Laporan Terkirim",
                            count = sentReports.size,
                            subtitle = "Laporan yang sudah dikirim"
                        )
                    }
                    items(sentReports) { report ->
                        ModernReportItem(
                            report = report,
                            onReportClick = {
                                selectedReport = report
                            }
                        )
                    }
                }

                if (savedReports.isEmpty() && sentReports.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 60.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                Text(
                                    if (searchQuery.isNotEmpty()) "🔍" else "📝",
                                    fontSize = 48.sp
                                )
                                Text(
                                    if (searchQuery.isNotEmpty()) "Tidak ada hasil pencarian" else "Belum ada laporan",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = Color.Gray
                                )
                                Text(
                                    if (searchQuery.isNotEmpty())
                                        "Coba kata kunci lain"
                                    else
                                        "Mulai buat laporan pertama Anda",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.Gray.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = primaryColor
            )
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                title,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
            Text(
                value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
fun SectionHeader(
    title: String,
    count: Int,
    subtitle: String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "$title ($count)",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
}

@Composable
fun ModernReportItem(
    report: Report,
    onReportClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onReportClick() }
            .shadow(2.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column {
            if (report.imageUrl.isNotBlank()) {
                ReportImageLoader(
                    imageUrl = report.imageUrl,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                )
            }

            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = report.category,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    Box(
                        modifier = Modifier
                            .background(
                                color = when (report.status) {
                                    "Terkirim" -> Color(0xFF4CAF50).copy(alpha = 0.1f)
                                    "Tersimpan" -> Color(0xFFFFC107).copy(alpha = 0.1f)
                                    "Selesai" -> Color(0xFF2196F3).copy(alpha = 0.1f)
                                    else -> Color.Gray.copy(alpha = 0.1f)
                                },
                                shape = RoundedCornerShape(8.dp)
                            )
                    ) {
                        Text(
                            text = report.status,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = when (report.status) {
                                "Terkirim" -> Color(0xFF4CAF50)
                                "Tersimpan" -> Color(0xFFFFC107)
                                "Selesai" -> Color(0xFF2196F3)
                                else -> Color.Gray
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = report.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            null,
                            modifier = Modifier.size(16.dp),
                            tint = Color.Gray
                        )
                        Spacer(modifier = Modifier.width(4.dp))

                        val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                        val dateString = report.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "-"

                        Text(
                            text = dateString,
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Gray
                        )
                    }
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

    if (imageUrl.startsWith("data:image/")) {
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
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center
        ) {
            Text("Tidak ada gambar", color = Color.Gray, fontSize = 12.sp)
        }
    }
}