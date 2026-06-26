package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.ui.AppNavigation
import com.example.ui.theme.ShikkhaloyTheme
import com.example.viewmodel.AuthViewModel

class MainActivity : ComponentActivity() {
    
    private val authViewModel: AuthViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ShikkhaloyTheme {
                AppNavigation(viewModel = authViewModel)
            }
        }
    }
}

