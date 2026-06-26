package com.example.ui

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

@Composable
fun LocalImageViewer(
    imageFile: File,
    title: String,
    onBack: () -> Unit
) {
    val bitmap = remember(imageFile) {
        try {
            if (imageFile.exists() && imageFile.length() > 0L) {
                BitmapFactory.decodeFile(imageFile.absolutePath)
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0F19)) // Very dark navy
    ) {
        // App bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = Color.White.copy(alpha = 0.1f),
                    contentColor = Color.White
                )
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back"
                )
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "সুরক্ষিত অফলাইন ইমেজ ভিউয়ার",
                    fontSize = 12.sp,
                    color = Color(0xFF94A3B8)
                )
            }
        }

        // Image container
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            if (bitmap != null) {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent)
                ) {
                    Image(
                        bitmap = bitmap.asImageBitmap(),
                        contentDescription = "Downloaded Offline Image",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                    )
                }
            } else {
                Text(
                    text = "ছবি লোড করা সম্ভব হয়নি। ফাইলটি ক্ষতিগ্রস্ত বা অনুপস্থিত হতে পারে।",
                    color = Color(0xFFF87171),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Explanation section
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E293B).copy(alpha = 0.6f)
            ),
            modifier = Modifier
                .padding(24.dp)
                .fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp)
            ) {
                Text(
                    text = "🔐 ফুল প্রাইভেসি প্রোটেকশন",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0D9488)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "এই ইমেজ ফাইলটি অ্যাপের সুরক্ষিত লোকাল স্টোরেজে রাখা হয়েছে। গ্যালারি বা গুগল ফটোজ অ্যাপের কাছে এর কোনো অ্যাক্সেস নেই। এটি শুধুমাত্র আমাদের অ্যাপের ভিতরেই নিরাপদে দেখতে পাবেন।",
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    color = Color(0xFFCBD5E1)
                )
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}
