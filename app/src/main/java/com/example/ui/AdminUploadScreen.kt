package com.example.ui

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.viewmodel.VideoUploadState
import com.example.viewmodel.VideoUploadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminUploadScreen(
    onSignOut: () -> Unit,
    viewModel: VideoUploadViewModel = viewModel()
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val videos by viewModel.videos.collectAsState()
    
    var title by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var fbTokenInput by remember { mutableStateOf("") }
    var selectedVideoUri by remember { mutableStateOf<Uri?>(null) }
    var toPlay by remember { mutableStateOf<String?>(null) }
    
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        selectedVideoUri = uri
    }
    
    if (toPlay != null) {
        FacebookVideoPlayerScreen(
            fbVideoId = toPlay!!,
            onBack = { toPlay = null }
        )
        return
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Text(
                text = "Admin Panel",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp, top = 32.dp)
            )
            
            Button(
                onClick = { viewModel.initializeDatabase() },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Initialize Database Tables (First Time)", color = Color.White)
            }
            
            Text(
                text = "Manage Facebook Token",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )
            
            OutlinedTextField(
                value = fbTokenInput,
                onValueChange = { fbTokenInput = it },
                label = { Text("New FB Access Token") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            Button(
                onClick = { viewModel.updateFbToken(fbTokenInput) },
                enabled = fbTokenInput.isNotBlank() && uiState !is VideoUploadState.Uploading,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F766E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Update FB Token in DB", color = Color.White)
            }
            
            Text(
                text = "Upload Video",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 16.dp).fillMaxWidth()
            )
            
            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text("Video Title") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
            
            OutlinedTextField(
                value = category,
                onValueChange = { category = it },
                label = { Text("Category") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF6366F1),
                    unfocusedBorderColor = Color.Gray,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color(0xFF6366F1),
                    unfocusedLabelColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 24.dp)
            )
            
            Button(
                onClick = { videoPicker.launch("video/*") },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155)),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 16.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Pick Video", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (selectedVideoUri != null) "Video Selected!" else "Select Video (.mp4)",
                    color = Color.White
                )
            }
            
            Button(
                onClick = {
                    selectedVideoUri?.let { uri ->
                        viewModel.uploadVideo(context, uri, title, category)
                    }
                },
                enabled = title.isNotBlank() && category.isNotBlank() && selectedVideoUri != null && uiState !is VideoUploadState.Uploading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF6366F1),
                    disabledContainerColor = Color.Gray
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .padding(bottom = 32.dp),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = "Upload & Sync to DB", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
            
            when (val state = uiState) {
                is VideoUploadState.Uploading -> {
                    CircularProgressIndicator(color = Color(0xFF6366F1))
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(text = state.progressMessage, color = Color.White)
                }
                is VideoUploadState.Success -> {
                    Text(
                        text = "Operation Successful!",
                        color = Color.Green,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = {
                        title = ""
                        category = ""
                        selectedVideoUri = null
                        viewModel.resetState()
                    }) {
                        Text("Dismiss")
                    }
                }
                is VideoUploadState.Error -> {
                    Text(
                        text = "Error: ${state.message}",
                        color = Color.Red,
                        modifier = Modifier.padding(16.dp)
                    )
                    Button(onClick = { viewModel.resetState() }) {
                        Text("Dismiss")
                    }
                }
                is VideoUploadState.Idle -> { }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            Text(
                text = "Uploaded Videos",
                color = Color.White,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 16.dp)
            )
        }
        
        items(videos) { video ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 8.dp)
                    .clickable { toPlay = video.fbVideoId },
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color(0xFF6366F1),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column {
                        Text(
                            text = video.title,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Text(
                            text = "Category: ${video.category}",
                            color = Color.Gray,
                            fontSize = 14.sp
                        )
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(32.dp))
            
            OutlinedButton(
                onClick = onSignOut,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text("Sign Out", color = Color.White)
            }
        }
    }
}
