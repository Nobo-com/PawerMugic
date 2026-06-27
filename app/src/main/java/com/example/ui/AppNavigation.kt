package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.viewmodel.AuthViewModel

@Composable
fun AppNavigation(viewModel: AuthViewModel) {
    val navController = rememberNavController()
    
    val startDestination = "downloader"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("downloader") {
            DownloaderScreen()
        }
    }
}
