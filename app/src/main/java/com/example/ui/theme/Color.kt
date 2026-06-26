package com.example.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PowerBackground: Color
    @Composable get() = LocalAppTheme.current.background
    
val PowerSurface: Color
    @Composable get() = LocalAppTheme.current.surface
    
val PowerAccent: Color
    @Composable get() = LocalAppTheme.current.accent
    
val PowerAccentDark: Color
    @Composable get() = LocalAppTheme.current.accentDark
    
val PowerText: Color
    @Composable get() = LocalAppTheme.current.text
    
val PowerTextSecondary: Color
    @Composable get() = LocalAppTheme.current.textSecondary
    
val PowerProgressTrack: Color
    @Composable get() = LocalAppTheme.current.progressTrack

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
