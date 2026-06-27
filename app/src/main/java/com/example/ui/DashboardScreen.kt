package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.viewmodel.AuthViewModel

@Composable
fun DashboardScreen(
    viewModel: AuthViewModel,
    onSignOut: () -> Unit
) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Button(onClick = {
            viewModel.signOut()
            onSignOut()
        }) {
            Text("Sign Out")
        }
    }
}
