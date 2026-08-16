package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val SandBackground = Color(0xFFF7FAF3) // Clean minimalist light sage background
val SandDark: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val CardBackground: Color @Composable get() = MaterialTheme.colorScheme.surface
val SoftBorder: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant

val SunGold = Color(0xFF3A6931)         // Deep forest green accent for morning/highlight
val NightBlue: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

val TextArabic: Color @Composable get() = MaterialTheme.colorScheme.onSurface
val TextPersian: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant

