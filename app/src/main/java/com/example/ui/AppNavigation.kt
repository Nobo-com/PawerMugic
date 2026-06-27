package com.example.ui

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    val startDestination = "downloader"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("downloader") {
            DownloaderScreen()
        }
    }
}
