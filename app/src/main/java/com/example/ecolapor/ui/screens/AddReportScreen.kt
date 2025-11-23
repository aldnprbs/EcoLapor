package com.example.ecolapor.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.ecolapor.ui.ReportState
import com.example.ecolapor.ui.ReportViewModel
import com.google.firebase.firestore.GeoPoint
import java.io.File
import java.util.Objects

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddReportScreen(
    navController: NavController,
    homeViewModel: com.example.ecolapor.ui.HomeViewModel? = null
) {
    val context = LocalContext.current
    val viewModel: ReportViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return ReportViewModel(context.applicationContext as android.app.Application) as T
            }
        }
    )
    val primaryColor = MaterialTheme.colorScheme.primary
    val scrollState = rememberScrollState()

    // State Form
    var description by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Sampah") }
    var imageUri by remember { mutableStateOf<Uri?>(null) }
    var showImageSourceDialog by remember { mutableStateOf(false) }
    var currentLocation by remember { mutableStateOf<GeoPoint?>(null) }
    
    // Location permission launcher
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true) {
            // Permission granted, get location
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = GeoPoint(location.latitude, location.longitude)
                        Toast.makeText(context, "Lokasi ditemukan: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()
                        android.util.Log.d("AddReportScreen", "Location: ${location.latitude}, ${location.longitude}")
                    } else {
                        Toast.makeText(context, "Lokasi tidak ditemukan, coba lagi", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: SecurityException) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin lokasi ditolak", Toast.LENGTH_SHORT).show()
        }
    }
    
    // Get current location
    LaunchedEffect(Unit) {
        val hasFineLocation = ContextCompat.checkSelfPermission(
            context, 
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        val hasCoarseLocation = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        
        if (hasFineLocation || hasCoarseLocation) {
            // Already have permission, get location
            val fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        currentLocation = GeoPoint(location.latitude, location.longitude)
                        android.util.Log.d("AddReportScreen", "Location: ${location.latitude}, ${location.longitude}")
                    }
                }
            } catch (e: SecurityException) {
                android.util.Log.e("AddReportScreen", "Security exception: ${e.message}")
            }
        } else {
            // Request permission
            locationPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    // --- SETUP GALLERY PICKER ---
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            imageUri = uri
        }
    }

    // --- SETUP KAMERA ---
    var tempUri by remember { mutableStateOf<Uri?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && tempUri != null) {
            imageUri = tempUri
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            try {
                val file = File.createTempFile("report_img_", ".jpg", context.externalCacheDir)
                tempUri = FileProvider.getUriForFile(
                    context,
                    context.packageName + ".provider",
                    file
                )
                cameraLauncher.launch(tempUri!!)
            } catch (e: Exception) {
                Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(context, "Izin kamera ditolak", Toast.LENGTH_SHORT).show()
        }
    }

    // --- CEK STATUS UPLOAD ---
    val uiState = viewModel.uiState
    LaunchedEffect(uiState) {
        when (uiState) {
            is ReportState.Success -> {
                Toast.makeText(context, "Berhasil!", Toast.LENGTH_LONG).show()
                homeViewModel?.refreshReports() // Refresh list di home
                viewModel.resetState()
                navController.popBackStack()
            }
            is ReportState.Error -> {
                Toast.makeText(context, uiState.message, Toast.LENGTH_LONG).show()
                viewModel.resetState()
            }
            else -> {}
        }
    }

    // Dialog untuk memilih sumber gambar
    if (showImageSourceDialog) {
        AlertDialog(
            onDismissRequest = { showImageSourceDialog = false },
            title = { Text("Pilih Sumber Gambar") },
            text = { Text("Pilih dari mana Anda ingin mengambil gambar") },
            confirmButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    galleryLauncher.launch("image/*")
                }) {
                    Text("Galeri")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showImageSourceDialog = false
                    val permissionCheck = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA)
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        try {
                            val file = File.createTempFile("report_img_", ".jpg", context.externalCacheDir)
                            tempUri = FileProvider.getUriForFile(
                                context,
                                context.packageName + ".provider",
                                file
                            )
                            cameraLauncher.launch(tempUri!!)
                        } catch (e: Exception) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    } else {
                        cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                }) {
                    Text("Kamera")
                }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Buat Laporan", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Kembali")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = primaryColor,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. KOTAK FOTO
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(250.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFFF0F0F0))
                    .border(2.dp, primaryColor, RoundedCornerShape(16.dp))
                    .clickable {
                        showImageSourceDialog = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (imageUri != null) {
                    Image(
                        painter = rememberAsyncImagePainter(imageUri),
                        contentDescription = "Bukti Foto",
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.Image, null, modifier = Modifier.size(48.dp), tint = Color.Gray)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tekan untuk pilih gambar", color = Color.Gray)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 2. PILIH KATEGORI
            Text("Pilih Kategori:", fontWeight = FontWeight.Bold, modifier = Modifier.fillMaxWidth())
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("Sampah", "Jalan", "Fasilitas").forEach { item ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(selected = category == item, onClick = { category = item })
                        Text(text = item)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 3. DESKRIPSI
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Deskripsi Masalah") },
                modifier = Modifier.fillMaxWidth().height(150.dp),
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // 4. TOMBOL SIMPAN DRAFT
            OutlinedButton(
                onClick = {
                    viewModel.submitReport(description, category, imageUri, currentLocation, isDraft = true)
                },
                enabled = uiState !is ReportState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50.dp),
                border = BorderStroke(2.dp, primaryColor)
            ) {
                if (uiState is ReportState.Loading) {
                    CircularProgressIndicator(color = primaryColor, modifier = Modifier.size(24.dp))
                } else {
                    Text("SIMPAN DRAFT", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = primaryColor)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 5. TOMBOL KIRIM LAPORAN
            Button(
                onClick = {
                    viewModel.submitReport(description, category, imageUri, currentLocation, isDraft = false)
                },
                enabled = uiState !is ReportState.Loading,
                modifier = Modifier.fillMaxWidth().height(56.dp),
                shape = RoundedCornerShape(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryColor)
            ) {
                if (uiState is ReportState.Loading) {
                    CircularProgressIndicator(color = Color.White)
                } else {
                    Text("KIRIM LAPORAN", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}