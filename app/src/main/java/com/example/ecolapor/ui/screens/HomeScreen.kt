package com.example.ecolapor.ui.screens

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.animation.core.*
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.ecolapor.data.model.Report
import com.example.ecolapor.ui.HomeViewModel
import com.example.ecolapor.ui.Screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
fun HomeScreen(navController: NavController, viewModel: HomeViewModel = viewModel()) {
    val reports by viewModel.reports.collectAsState()
    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceColor = MaterialTheme.colorScheme.surface

    var searchQuery by remember { mutableStateOf("") }
    var showLogoutDialog by remember { mutableStateOf(false) }
    var selectedReport by remember { mutableStateOf<Report?>(null) }
    var isRefreshing by remember { mutableStateOf(false) }

    val context = LocalContext.current

    val filteredReports = reports.filter { report ->
        searchQuery.isEmpty() ||
                report.category.contains(searchQuery, ignoreCase = true) ||
                report.description.contains(searchQuery, ignoreCase = true) ||
                report.status.contains(searchQuery, ignoreCase = true)
    }

    val savedReports = filteredReports.filter { it.status == "Tersimpan" }
    val sentReports = filteredReports.filter { it.status == "Terkirim" || it.status == "Selesai" }

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

    val infiniteTransition = rememberInfiniteTransition(label = "")
    val fabRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 20000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    if (showLogoutDialog) {
        Dialog(onDismissRequest = { showLogoutDialog = false }) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ExitToApp,
                            contentDescription = null,
                            tint = Color(0xFFF44336),
                            modifier = Modifier.size(32.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        "Konfirmasi Logout",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        "Apakah Anda yakin ingin keluar dari akun ini?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showLogoutDialog = false },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Batal")
                        }

                        Button(
                            onClick = {
                                showLogoutDialog = false
                                viewModel.logout()
                                navController.navigate(Screen.Welcome.route) {
                                    popUpTo(Screen.Home.route) { inclusive = true }
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Logout")
                        }
                    }
                }
            }
        }
    }

    if (selectedReport != null) {
        ReportActionDialog(
            report = selectedReport!!,
            onDismiss = { selectedReport = null },
            onSend = {
                viewModel.sendDraftReport(selectedReport!!)
                selectedReport = null
            },
            onLocation = { report ->
                val lat = report.location?.latitude ?: 0.0
                val lng = report.location?.longitude ?: 0.0
                if (lat != 0.0 && lng != 0.0) {
                    val uri = android.net.Uri.parse("geo:$lat,$lng?q=$lat,$lng(Lokasi)")
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
                    android.widget.Toast.makeText(context, "Lokasi tidak tersedia", android.widget.Toast.LENGTH_SHORT).show()
                }
                selectedReport = null
            },
            onDelete = {
                viewModel.deleteReport(selectedReport!!.id)
                selectedReport = null
            }
        )
    }

    Scaffold(
        topBar = {
            // Ganti SmallTopAppBar dengan TopAppBar biasa
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            Icons.Default.Eco,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                        Text(
                            "EcoLapor",
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            fontSize = 22.sp
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor
                ),
                actions = {
                    IconButton(
                        onClick = {
                            android.widget.Toast.makeText(context, "Notifikasi coming soon!", android.widget.Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Box {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = Color.White
                            )
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(Color.Red)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }

                    IconButton(
                        onClick = { navController.navigate(Screen.Profile.route) }
                    ) {
                        Icon(
                            Icons.Default.AccountCircle,
                            contentDescription = null,
                            tint = Color.White
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .rotate(fabRotation)
                        .clip(CircleShape)
                        .background(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    primaryColor.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        )
                )

                FloatingActionButton(
                    onClick = { navController.navigate(Screen.AddReport.route) },
                    containerColor = primaryColor,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(65.dp)
                        .shadow(12.dp, CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pullRefresh(pullRefreshState)
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color(0xFFF5F7FA),
                            Color(0xFFE8EFF5)
                        )
                    )
                )
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    WelcomeCard(primaryColor)
                }

                item {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        surfaceColor = surfaceColor
                    )
                }

                item {
                    StatisticsCards(
                        total = reports.size,
                        saved = savedReports.size,
                        sent = sentReports.size
                    )
                }

                if (savedReports.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.Drafts,
                            title = "Draft Tersimpan",
                            count = savedReports.size,
                            subtitle = "Laporan yang belum dikirim",
                            color = Color(0xFFFFC107)
                        )
                    }
                    items(savedReports) { report ->
                        PremiumReportCard(
                            report = report,
                            onClick = { selectedReport = report }
                        )
                    }
                }

                if (sentReports.isNotEmpty()) {
                    item {
                        SectionHeader(
                            icon = Icons.Default.Send,
                            title = "Laporan Terkirim",
                            count = sentReports.size,
                            subtitle = "Laporan yang sudah dikirim",
                            color = Color(0xFF4CAF50)
                        )
                    }
                    items(sentReports) { report ->
                        PremiumReportCard(
                            report = report,
                            onClick = { selectedReport = report }
                        )
                    }
                }

                if (savedReports.isEmpty() && sentReports.isEmpty()) {
                    item {
                        EmptyState(hasSearch = searchQuery.isNotEmpty())
                    }
                }
            }

            PullRefreshIndicator(
                refreshing = isRefreshing,
                state = pullRefreshState,
                modifier = Modifier.align(Alignment.TopCenter),
                contentColor = primaryColor,
                backgroundColor = Color.White
            )
        }
    }
}

@Composable
fun WelcomeCard(primaryColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "")
    val shimmer by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = ""
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            primaryColor,
                            Color(0xFF1976D2),
                            primaryColor
                        ),
                        startX = shimmer - 1000f,
                        endX = shimmer
                    )
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Selamat Datang! 👋",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Mari bersama jaga lingkungan kita",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.95f)
                    )
                }

                // Ganti EcoRounded dengan Eco biasa
                Icon(
                    Icons.Default.Eco,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.2f),
                    modifier = Modifier.size(100.dp)
                )
            }
        }
    }
}

@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    surfaceColor: Color
) {
    var text by remember { mutableStateOf(query) }

    LaunchedEffect(query) {
        text = query
    }

    TextField(
        value = text,
        onValueChange = {
            text = it
            onQueryChange(it)
        },
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        placeholder = {
            Text("Cari laporan...", color = Color.Gray.copy(alpha = 0.6f))
        },
        leadingIcon = {
            // Ganti SearchRounded dengan Search biasa
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = {
            if (text.isNotEmpty()) {
                IconButton(onClick = {
                    text = ""
                    onQueryChange("")
                }) {
                    Icon(
                        Icons.Default.Close,
                        contentDescription = null,
                        tint = Color.Gray
                    )
                }
            }
        },
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = Color.White,
            focusedContainerColor = Color.White,
            unfocusedIndicatorColor = Color.Transparent,
            focusedIndicatorColor = Color.Transparent
        ),
        shape = RoundedCornerShape(16.dp),
        singleLine = true
    )
}

@Composable
fun StatisticsCards(total: Int, saved: Int, sent: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        StatCard(
            icon = Icons.Default.Assessment,
            title = "Total",
            value = total.toString(),
            color = Color(0xFF4CAF50),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.Drafts,
            title = "Draft",
            value = saved.toString(),
            color = Color(0xFFFFC107),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            icon = Icons.Default.CheckCircle,
            title = "Terkirim",
            value = sent.toString(),
            color = Color(0xFF2196F3),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun StatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .height(100.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column {
                Text(
                    value,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    title,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun SectionHeader(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    count: Int,
    subtitle: String,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(color.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text(
                            count.toString(),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = color
                        )
                    }
                }
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}

@Composable
fun PremiumReportCard(
    report: Report,
    onClick: () -> Unit
) {
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(50)
        visible = true
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .scale(if (visible) 1f else 0.95f),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            if (report.imageUrl.isNotBlank()) {
                Box {
                    ReportImageLoader(
                        imageUrl = report.imageUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(
                                when (report.status) {
                                    "Terkirim" -> Color(0xFF4CAF50)
                                    "Tersimpan" -> Color(0xFFFFC107)
                                    else -> Color(0xFF2196F3)
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            report.status,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Column(modifier = Modifier.padding(20.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            report.category,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    report.description,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.AccessTime,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    val dateFormat = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                    val dateString = report.timestamp?.toDate()?.let { dateFormat.format(it) } ?: "-"
                    Text(
                        dateString,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
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

    LaunchedEffect(imageUrl) {
        try {
            if (imageUrl.startsWith("data:image/")) {
                val base64String = imageUrl.substringAfter("base64,")
                val imageBytes = Base64.decode(base64String, Base64.DEFAULT)
                val decodedBitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                if (decodedBitmap != null) {
                    bitmap = decodedBitmap
                    hasError = false
                } else {
                    hasError = true
                }
            } else {
                // Handle URL images or other cases
                hasError = true
            }
        } catch (e: Exception) {
            hasError = true
        } finally {
            isLoading = false
        }
    }

    Box(modifier = modifier) {
        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 3.dp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            hasError || bitmap == null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Gray.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BrokenImage,
                            contentDescription = "Gagal memuat gambar",
                            modifier = Modifier.size(48.dp),
                            tint = Color.Gray.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "Gagal memuat gambar",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
            else -> {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = "Laporan gambar",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

@Composable
fun ReportActionDialog(
    report: Report,
    onDismiss: () -> Unit,
    onSend: () -> Unit,
    onLocation: (Report) -> Unit,
    onDelete: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 16.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    "Pilih Aksi",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                if (report.status == "Tersimpan") {
                    ActionButton(
                        icon = Icons.Default.Send,
                        text = "Kirim Laporan",
                        color = Color(0xFF4CAF50),
                        onClick = onSend
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                }

                ActionButton(
                    icon = Icons.Default.LocationOn,
                    text = "Lacak Lokasi",
                    color = Color(0xFF2196F3),
                    onClick = { onLocation(report) }
                )

                Spacer(modifier = Modifier.height(12.dp))

                ActionButton(
                    icon = Icons.Default.Delete,
                    text = "Hapus Laporan",
                    color = Color(0xFFF44336),
                    onClick = onDelete
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Batal")
                }
            }
        }
    }
}

@Composable
fun ActionButton(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String,
    color: Color,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(containerColor = color)
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, fontWeight = FontWeight.Medium)
    }
}

@Composable
fun EmptyState(hasSearch: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 80.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            Color.Transparent
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (hasSearch) Icons.Default.SearchOff else Icons.Default.NoteAdd,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.Gray.copy(alpha = 0.4f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            if (hasSearch) "Tidak ada hasil" else "Belum ada laporan",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = Color.Gray
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            if (hasSearch) "Coba kata kunci lain" else "Mulai buat laporan pertama",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray.copy(alpha = 0.7f)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    MaterialTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            // HomeScreen(navController = rememberNavController())
        }
    }
}

@Preview(showBackground = true)
@Composable
fun WelcomeCardPreview() {
    MaterialTheme {
        WelcomeCard(primaryColor = MaterialTheme.colorScheme.primary)
    }
}

@Preview(showBackground = true)
@Composable
fun PremiumReportCardPreview() {
    MaterialTheme {
        val sampleReport = Report(
            id = "1",
            category = "Sampah Plastik",
            description = "Tumpukan sampah plastik di pinggir jalan yang mengganggu pemandangan dan berpotensi menyumbat saluran air.",
            imageUrl = "",
            status = "Tersimpan",
            timestamp = com.google.firebase.Timestamp.now(),
            location = null
        )

        PremiumReportCard(
            report = sampleReport,
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun StatisticsCardsPreview() {
    MaterialTheme {
        StatisticsCards(total = 10, saved = 3, sent = 7)
    }
}

@Preview(showBackground = true)
@Composable
fun SearchBarPreview() {
    MaterialTheme {
        var query by remember { mutableStateOf("") }
        SearchBar(
            query = query,
            onQueryChange = { query = it },
            surfaceColor = MaterialTheme.colorScheme.surface
        )
    }
}

@Preview(showBackground = true)
@Composable
fun SectionHeaderPreview() {
    MaterialTheme {
        SectionHeader(
            icon = Icons.Default.Drafts,
            title = "Draft Tersimpan",
            count = 5,
            subtitle = "Laporan yang belum dikirim",
            color = Color(0xFFFFC107)
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ReportActionDialogPreview() {
    MaterialTheme {
        val sampleReport = Report(
            id = "1",
            category = "Sampah Plastik",
            description = "Sample report",
            imageUrl = "",
            status = "Tersimpan",
            timestamp = com.google.firebase.Timestamp.now(),
            location = null
        )

        ReportActionDialog(
            report = sampleReport,
            onDismiss = {},
            onSend = {},
            onLocation = {},
            onDelete = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun EmptyStatePreview() {
    MaterialTheme {
        EmptyState(hasSearch = false)
    }
}