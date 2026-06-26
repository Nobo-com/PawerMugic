package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.AuthViewModel
import com.example.viewmodel.DownloadState
import com.example.viewmodel.DownloadViewModel
import com.example.viewmodel.DownloadableItem
import com.example.viewmodel.ResourceType
import java.io.File

// Sub-screen navigation states for premium dashboard experience
sealed class DashboardSubScreen {
    object Home : DashboardSubScreen()
    object Downloads : DashboardSubScreen()
    data class VideoPlayer(val file: File, val title: String) : DashboardSubScreen()
    data class PdfReader(val file: File, val title: String) : DashboardSubScreen()
    data class ImageViewer(val file: File, val title: String) : DashboardSubScreen()
}

@Composable
fun DownloadIcon(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(24.dp)) {
        val w = size.width
        val h = size.height
        val arrowPath = Path().apply {
            moveTo(w * 0.5f, h * 0.15f)
            lineTo(w * 0.5f, h * 0.65f)
            moveTo(w * 0.25f, h * 0.45f)
            lineTo(w * 0.5f, h * 0.7f)
            lineTo(w * 0.75f, h * 0.45f)
        }
        drawPath(
            path = arrowPath,
            color = tint,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * 0.85f),
            end = Offset(w * 0.8f, h * 0.85f),
            strokeWidth = 2.5.dp.toPx(),
            cap = androidx.compose.ui.graphics.StrokeCap.Round
        )
    }
}

@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    val context = LocalContext.current
    val downloadViewModel: DownloadViewModel = viewModel()

    val currentUser by viewModel.currentUser.collectAsState()
    val deviceInfo by viewModel.deviceInfo.collectAsState()
    val ipAddress by viewModel.ipAddress.collectAsState()
    val downloadStates by downloadViewModel.downloadStates.collectAsState()

    var activeSubScreen by remember { mutableStateOf<DashboardSubScreen>(DashboardSubScreen.Home) }

    // Synchronize downlad files checking on start
    LaunchedEffect(Unit) {
        downloadViewModel.initDownloadedStatus(context)
    }

    Scaffold(
        bottomBar = {
            // Render Bottom Bar only for main screens to ensure fullscreen video / pdf viewers
            if (activeSubScreen is DashboardSubScreen.Home || activeSubScreen is DashboardSubScreen.Downloads) {
                NavigationBar(
                    containerColor = Color(0xFF1E293B),
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeSubScreen is DashboardSubScreen.Home,
                        onClick = { activeSubScreen = DashboardSubScreen.Home },
                        label = { Text("প্রোফাইল", fontWeight = FontWeight.Bold) },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile"
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF6366F1),
                            selectedTextColor = Color(0xFF6366F1),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFF334155)
                        )
                    )

                    NavigationBarItem(
                        selected = activeSubScreen is DashboardSubScreen.Downloads,
                        onClick = { activeSubScreen = DashboardSubScreen.Downloads },
                        label = { Text("ডাউনলোড", fontWeight = FontWeight.Bold) },
                        icon = {
                            DownloadIcon(
                                tint = if (activeSubScreen is DashboardSubScreen.Downloads) Color(0xFF0D9488) else Color(0xFF94A3B8),
                                modifier = Modifier.size(22.dp)
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color(0xFF0D9488),
                            selectedTextColor = Color(0xFF0D9488),
                            unselectedIconColor = Color(0xFF94A3B8),
                            unselectedTextColor = Color(0xFF94A3B8),
                            indicatorColor = Color(0xFF334155)
                        )
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0F172A))
        ) {
            // Dynamic Screen Swapping Routing
            when (val screen = activeSubScreen) {
                is DashboardSubScreen.Home -> {
                    HomeScreenContent(
                        displayName = currentUser?.displayName ?: "User",
                        userEmail = currentUser?.email ?: "No Email",
                        deviceInfo = deviceInfo,
                        ipAddress = ipAddress,
                        onSignOut = {
                            viewModel.signOut()
                            onSignOut()
                        }
                    )
                }

                is DashboardSubScreen.Downloads -> {
                    DownloadsScreenContent(
                        items = downloadViewModel.downloadableItems,
                        downloadStates = downloadStates,
                        onDownload = { item -> downloadViewModel.downloadFile(context, item) },
                        onDelete = { item -> downloadViewModel.deleteFile(context, item) },
                        onView = { item ->
                            val localFile = downloadViewModel.getLocalFile(context, item)
                            when (item.type) {
                                ResourceType.VIDEO -> {
                                    activeSubScreen = DashboardSubScreen.VideoPlayer(localFile, item.title)
                                }
                                ResourceType.PDF -> {
                                    activeSubScreen = DashboardSubScreen.PdfReader(localFile, item.title)
                                }
                                ResourceType.IMAGE -> {
                                    activeSubScreen = DashboardSubScreen.ImageViewer(localFile, item.title)
                                }
                            }
                        }
                    )
                }

                is DashboardSubScreen.VideoPlayer -> {
                    LocalVideoPlayer(
                        videoFile = screen.file,
                        title = screen.title,
                        onBack = { activeSubScreen = DashboardSubScreen.Downloads }
                    )
                }

                is DashboardSubScreen.PdfReader -> {
                    LocalPdfReader(
                        pdfFile = screen.file,
                        title = screen.title,
                        onBack = { activeSubScreen = DashboardSubScreen.Downloads }
                    )
                }

                is DashboardSubScreen.ImageViewer -> {
                    LocalImageViewer(
                        imageFile = screen.file,
                        title = screen.title,
                        onBack = { activeSubScreen = DashboardSubScreen.Downloads }
                    )
                }
            }
        }
    }
}

@Composable
fun HomeScreenContent(
    displayName: String,
    userEmail: String,
    deviceInfo: String,
    ipAddress: String,
    onSignOut: () -> Unit
) {
    val initials = displayName.split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.toString() }
        .joinToString("")
        .uppercase()

    Box(modifier = Modifier.fillMaxSize()) {
        // Aesthetic background subtle gradients
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.12f), Color.Transparent),
                    center = Offset(size.width * 0.9f, size.height * 0.1f),
                    radius = size.width * 0.7f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0D9488).copy(alpha = 0.08f), Color.Transparent),
                    center = Offset(size.width * 0.1f, size.height * 0.8f),
                    radius = size.width * 0.7f
                )
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
        ) {
            Spacer(modifier = Modifier.height(36.dp))

            // Premium Header with Profile Details
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 28.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(68.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF6366F1), Color(0xFF14B8A6))
                            )
                        )
                ) {
                    Text(
                        text = initials.ifEmpty { "S" },
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column {
                    Text(
                        text = "স্বাগতম, $displayName",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = userEmail,
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            // Quick Status Pill
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF06B6D4).copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, Color(0xFF06B6D4).copy(alpha = 0.4f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 20.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFF22D3EE),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "সেশন মেয়াদ: ৬ মাস (সক্রিয়)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFFE2E8F0)
                    )
                }
            }

            // Cards for Metadata
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                // Turso & Account Info Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF10B981)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "সংযুক্ত ডেটাবেজ (Turso)",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Text(
                            text = "সংযুক্ত: Shikkhaloy-nobo SQL",
                            fontSize = 14.sp,
                            color = Color(0xFF94A3B8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "সংযুক্তি স্থিতি: সুরক্ষিত ও সিঙ্ক্রোনাইজড",
                            fontSize = 13.sp,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Device and IP Information Card
                Card(
                    shape = RoundedCornerShape(24.dp),
                    border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.4f)),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 14.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF6366F1)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "ডিভাইস ও আইপি তথ্য",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = deviceInfo,
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }

                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "IP ঠিকানা: $ipAddress",
                                fontSize = 14.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                    }
                }
            }

            // Beautiful logout button
            Button(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                    contentColor = Color(0xFFF87171)
                ),
                border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "সাইন আউট করুন",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun DownloadsScreenContent(
    items: List<DownloadableItem>,
    downloadStates: Map<String, DownloadState>,
    onDownload: (DownloadableItem) -> Unit,
    onDelete: (DownloadableItem) -> Unit,
    onView: (DownloadableItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(36.dp))

        // Title and security statement
        Text(
            text = "অফলাইন ডাউনলোড সেন্টার",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
        Text(
            text = "নিরাপদ ইন-অ্যাপ ফাইল স্টোরেজ",
            fontSize = 13.sp,
            color = Color(0xFF0D9488),
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 2.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Glassmorphism Security Shield Callout
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0D9488).copy(alpha = 0.1f)),
            border = BorderStroke(1.dp, Color(0xFF0D9488).copy(alpha = 0.3f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = Color(0xFF14B8A6),
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "সব ডাউনলোড ডিরেক্টরি এনক্রিপ্টেড। এই শিক্ষালায় অ্যাপ ছাড়া অন্য কোথাও এই ছবি, ভিডিও বা পিডিএফ ফাইল চালানো যাবে না।",
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = Color(0xFFE2E8F0)
                )
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // List of downloadable materials
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(items) { item ->
                val state = downloadStates[item.id] ?: DownloadState.Idle
                DownloadItemCard(
                    item = item,
                    state = state,
                    onDownload = { onDownload(item) },
                    onDelete = { onDelete(item) },
                    onView = { onView(item) }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun DownloadItemCard(
    item: DownloadableItem,
    state: DownloadState,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(22.dp),
        border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            // Header row with Icon, Title and file size badge
            Row(
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth()
            ) {
                // Type specific badge
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            when (item.type) {
                                ResourceType.VIDEO -> Color(0xFF6366F1).copy(alpha = 0.2f)
                                ResourceType.PDF -> Color(0xFFEF4444).copy(alpha = 0.2f)
                                ResourceType.IMAGE -> Color(0xFF0D9488).copy(alpha = 0.2f)
                            }
                        )
                ) {
                    when (item.type) {
                        ResourceType.VIDEO -> Icon(Icons.Default.PlayArrow, "Video", tint = Color(0xFF818CF8), modifier = Modifier.size(22.dp))
                        ResourceType.PDF -> Icon(Icons.Default.Info, "PDF", tint = Color(0xFFF87171), modifier = Modifier.size(20.dp))
                        ResourceType.IMAGE -> Icon(Icons.Default.CheckCircle, "Image", tint = Color(0xFF2DD4BF), modifier = Modifier.size(20.dp))
                    }
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(top = 4.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF334155),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = when (item.type) {
                                    ResourceType.VIDEO -> "ভিডিও ক্লাস"
                                    ResourceType.PDF -> "পিডিএফ বই"
                                    ResourceType.IMAGE -> "চিত্রপত্র"
                                },
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFCBD5E1),
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = item.originalSize,
                            fontSize = 11.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Short Description
            Text(
                text = item.description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // State specific view renderers
            when (state) {
                is DownloadState.Idle -> {
                    Button(
                        onClick = onDownload,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0D9488),
                            contentColor = Color.White
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            DownloadIcon(tint = Color.White, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("ডাউনলোড করুন (ইন-অ্যাপ)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                is DownloadState.Downloading -> {
                    val percent = (state.progress * 100).toInt()
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "ডাউনলোড হচ্ছে... $percent%",
                                fontSize = 12.sp,
                                color = Color(0xFF2DD4BF),
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = String.format("%.1f KB/s", state.speedKbps),
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { state.progress },
                            color = Color(0xFF2DD4BF),
                            trackColor = Color(0xFF334155),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }

                is DownloadState.Completed -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = onView,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF6366F1),
                                contentColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (item.type == ResourceType.VIDEO) Icons.Default.PlayArrow else Icons.Default.Info,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (item.type) {
                                        ResourceType.VIDEO -> "প্লে করুন"
                                        ResourceType.PDF -> "পড়ুন (পিডিএফ)"
                                        ResourceType.IMAGE -> "দেখুন (ইমেজ)"
                                    },
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        IconButton(
                            onClick = onDelete,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.15f),
                                contentColor = Color(0xFFF87171)
                            ),
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f), RoundedCornerShape(12.dp))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Offline File",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                is DownloadState.Failed -> {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Failed",
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "ত্রুটি: ${state.error}",
                                fontSize = 12.sp,
                                color = Color(0xFFF87171),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onDownload,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFEF4444).copy(alpha = 0.2f),
                                contentColor = Color(0xFFF87171)
                            ),
                            border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Refresh, null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("পুনরায় চেষ্টা করুন", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}
