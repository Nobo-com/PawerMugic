package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.common.api.ApiException
import java.security.MessageDigest
import java.util.Locale

// Helper function to dynamically retrieve SHA-1 of the app's signing certificate at runtime
fun getAppSignatureSHA1(context: Context): String {
    try {
        val packageName = context.packageName
        val pm = context.packageManager
        val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNING_CERTIFICATES)
            packageInfo.signingInfo?.apkContentsSigners
        } else {
            @Suppress("DEPRECATION")
            val packageInfo = pm.getPackageInfo(packageName, PackageManager.GET_SIGNATURES)
            packageInfo.signatures
        }

        if (signatures != null && signatures.isNotEmpty()) {
            val signature = signatures[0]
            val md = MessageDigest.getInstance("SHA-1")
            val publicKey = md.digest(signature.toByteArray())
            val hexString = StringBuilder()
            for (i in publicKey.indices) {
                val appendString = Integer.toHexString(0xFF and publicKey[i].toInt())
                    .uppercase(Locale.US)
                if (appendString.length == 1) {
                    hexString.append("0")
                }
                hexString.append(appendString)
                if (i < publicKey.size - 1) {
                    hexString.append(":")
                }
            }
            return hexString.toString()
        }
    } catch (e: Exception) {
        Log.e("SignatureHelper", "Error getting signature SHA-1", e)
    }
    return "UNKNOWN"
}

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onLoginSuccess: () -> Unit
) {
    val context = LocalContext.current
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            onLoginSuccess()
        }
    }

    val signInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.setLoading(false)
        val data = result.data
        if (data != null) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                if (result.resultCode == Activity.RESULT_OK) {
                    viewModel.handleSignInResult(task)
                } else {
                    // Try to get result to extract ApiException if present
                    val account = task.getResult(ApiException::class.java)
                    viewModel.handleSignInResult(task)
                }
            } catch (e: ApiException) {
                val statusCode = e.statusCode
                val errorMsg = when (statusCode) {
                    10 -> "Developer Error (10): সাধারণত SHA-1 বা প্যাকেজ নাম Firebase কনসোলে ভুল থাকলে অথবা Web Client ID অসঙ্গতিপূর্ণ হলে এটি ঘটে।"
                    12500 -> "Sign-in Failed (12500): গুগল প্লে সার্ভিস সাইন-ইন করতে পারেনি। আপনার ফোনের গুগল প্লে সার্ভিস আপডেট করুন।"
                    12501 -> "Sign-in Canceled (12501): ব্যবহারকারী সাইন-ইন বাতিল করেছেন।"
                    7 -> "Network Error (7): ইন্টারনেট সংযোগ বা সার্ভার সংযোগের সমস্যা।"
                    else -> "গুগল সাইন-ইন ব্যর্থ হয়েছে। স্ট্যাটাস কোড: $statusCode\nবার্তা: ${e.localizedMessage}"
                }
                
                val currentSha1 = getAppSignatureSHA1(context)
                viewModel.setErrorMessage(
                    "$errorMsg\n\n" +
                    "অনুগ্রহ করে নিশ্চিত করুন যে আপনার Firebase কনসোলে নিচের তথ্যগুলো যুক্ত করা আছে:\n" +
                    "প্যাকেজ নাম: ${context.packageName}\n" +
                    "চলতি অ্যাপের SHA-1: $currentSha1"
                )
                Log.e("LoginScreen", "Google Sign In API Exception: Status Code $statusCode", e)
            }
        } else {
            val currentSha1 = getAppSignatureSHA1(context)
            viewModel.setErrorMessage(
                "গুগল সাইন-ইন সম্পন্ন হয়নি বা বাতিল করা হয়েছে। (রেজাল্ট কোড: ${result.resultCode})\n\n" +
                "অনুগ্রহ করে নিশ্চিত করুন যে আপনার Firebase কনসোলে নিচের তথ্যগুলো যুক্ত করা আছে:\n" +
                "প্যাকেজ নাম: ${context.packageName}\n" +
                "চলতি অ্যাপের SHA-1: $currentSha1"
            )
        }
    }

    // Cohesive Deep Dark Gradient theme with modern accents
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF0F172A), // Deep slate
                        Color(0xFF0B132B), // Very deep indigo-navy
                        Color(0xFF020617)  // Pitch dark
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative background glow
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF6366F1).copy(alpha = 0.15f), Color.Transparent),
                    center = Offset(size.width * 0.2f, size.height * 0.2f),
                    radius = size.width * 0.6f
                )
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF0D9488).copy(alpha = 0.1f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.8f),
                    radius = size.width * 0.6f
                )
            )
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(28.dp)
        ) {
            // Elegant glowing programmatic app logo representation
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(bottom = 24.dp)
            ) {
                Canvas(modifier = Modifier.size(110.dp)) {
                    // Outer rings for depth
                    drawCircle(
                        color = Color(0xFF6366F1).copy(alpha = 0.12f),
                        radius = size.minDimension / 1.6f
                    )
                    drawCircle(
                        color = Color(0xFF0D9488).copy(alpha = 0.08f),
                        radius = size.minDimension / 1.3f
                    )

                    // Open book pages path
                    val path = Path().apply {
                        // Left page
                        moveTo(size.width * 0.15f, size.height * 0.72f)
                        quadraticBezierTo(size.width * 0.35f, size.height * 0.64f, size.width * 0.5f, size.height * 0.72f)
                        lineTo(size.width * 0.5f, size.height * 0.32f)
                        quadraticBezierTo(size.width * 0.35f, size.height * 0.24f, size.width * 0.15f, size.height * 0.32f)
                        close()

                        // Right page
                        moveTo(size.width * 0.85f, size.height * 0.72f)
                        quadraticBezierTo(size.width * 0.65f, size.height * 0.64f, size.width * 0.5f, size.height * 0.72f)
                        lineTo(size.width * 0.5f, size.height * 0.32f)
                        quadraticBezierTo(size.width * 0.65f, size.height * 0.24f, size.width * 0.85f, size.height * 0.32f)
                        close()
                    }
                    drawPath(path = path, color = Color.White)

                    // Highlight overlay for page dimension
                    val pageOverlayLeft = Path().apply {
                        moveTo(size.width * 0.15f, size.height * 0.32f)
                        quadraticBezierTo(size.width * 0.35f, size.height * 0.24f, size.width * 0.5f, size.height * 0.32f)
                        lineTo(size.width * 0.5f, size.height * 0.72f)
                        quadraticBezierTo(size.width * 0.35f, size.height * 0.64f, size.width * 0.15f, size.height * 0.72f)
                        close()
                    }
                    drawPath(path = pageOverlayLeft, color = Color(0xFFEEF2F6).copy(alpha = 0.85f))

                    // Bookmark Ribbon
                    val ribbon = Path().apply {
                        moveTo(size.width * 0.47f, size.height * 0.28f)
                        lineTo(size.width * 0.53f, size.height * 0.28f)
                        lineTo(size.width * 0.53f, size.height * 0.80f)
                        lineTo(size.width * 0.5f, size.height * 0.75f)
                        lineTo(size.width * 0.47f, size.height * 0.80f)
                        close()
                    }
                    drawPath(path = ribbon, color = Color(0xFFF59E0B))

                    // Sparkles (little stars)
                    drawCircle(color = Color(0xFFF59E0B), radius = 3.dp.toPx(), center = Offset(size.width * 0.22f, size.height * 0.18f))
                    drawCircle(color = Color(0xFF818CF8), radius = 2.dp.toPx(), center = Offset(size.width * 0.78f, size.height * 0.16f))
                }
            }

            // Title
            Text(
                text = "Shikkhaloy",
                fontSize = 36.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            // Slogan
            Text(
                text = "শিক্ষা যেখানে সহজ ও আনন্দদায়ক",
                fontSize = 15.sp,
                color = Color(0xFF94A3B8),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 6.dp, bottom = 40.dp)
            )

            // Glassmorphism Card
            Card(
                shape = RoundedCornerShape(28.dp),
                border = BorderStroke(1.dp, Color(0xFF334155).copy(alpha = 0.5f)),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E293B).copy(alpha = 0.75f)
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Text(
                        text = "লগইন করুন",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    Text(
                        text = "আপনার গুগল অ্যাকাউন্ট দিয়ে শিক্ষালায়ে প্রবেশ করুন",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 28.dp)
                    )

                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                color = Color(0xFF6366F1),
                                modifier = Modifier.size(32.dp)
                            )
                        } else {
                            Button(
                                onClick = {
                                    viewModel.setLoading(true)
                                    viewModel.setErrorMessage(null)
                                    val signInIntent = viewModel.getGoogleSignInClient().signInIntent
                                    signInLauncher.launch(signInIntent)
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(54.dp),
                                shape = RoundedCornerShape(16.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color.White,
                                    contentColor = Color(0xFF0F172A)
                                ),
                                elevation = ButtonDefaults.buttonElevation(
                                    defaultElevation = 2.dp,
                                    pressedElevation = 4.dp
                                )
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    // Custom beautifully drawn Google "G" representation
                                    Canvas(modifier = Modifier.size(20.dp).padding(end = 8.dp)) {
                                        val radius = size.minDimension / 2
                                        val strokeWidth = 3.dp.toPx()
                                        // Draw simplified stylish Google colored multi-arc
                                        drawArc(
                                            color = Color(0xFFEA4335), // Red
                                            startAngle = 180f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )
                                        drawArc(
                                            color = Color(0xFFFBBC05), // Yellow
                                            startAngle = 90f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )
                                        drawArc(
                                            color = Color(0xFF34A853), // Green
                                            startAngle = 0f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )
                                        drawArc(
                                            color = Color(0xFF4285F4), // Blue
                                            startAngle = 270f,
                                            sweepAngle = 90f,
                                            useCenter = false,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Google দিয়ে সাইন ইন",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0F172A)
                                    )
                                }
                            }
                        }
                    }

                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = errorMessage!!,
                            color = Color(0xFFF87171),
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        }
    }
}
