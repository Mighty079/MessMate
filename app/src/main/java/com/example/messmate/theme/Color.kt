package com.example.messmate.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

// Theme Detection
val isDark: Boolean @Composable get() = MaterialTheme.colorScheme.background.luminance() < 0.5f

// Core Theme Palette
val WarmBackground: Color @Composable get() = if (isDark) Color(0xFF141210) else Color(0xFFFFFDF6)
val AppWhite: Color @Composable get() = if (isDark) Color(0xFF1E1B18) else Color(0xFFFFFFFF)
val CardBorder: Color @Composable get() = if (isDark) Color(0xFF2E2A25) else Color(0xFFF5EFE6)
val TextPrimary: Color @Composable get() = if (isDark) Color(0xFFECE5DF) else Color(0xFF2C241E)
val TextSecondary: Color @Composable get() = if (isDark) Color(0xFF9E9288) else Color(0xFF8C827A)

// Gradients & Headers (Keep vibrancies, but make text readable)
val OrangeStart = Color(0xFFF27022)
val OrangeEnd = Color(0xFFFFA040)
val PurpleStart = Color(0xFF7E42F5)
val PurpleEnd = Color(0xFFB18CFF)
val BlueStart = Color(0xFF4A4AF4)
val BlueEnd = Color(0xFF8C8CFF)

// Meal Categories
val BreakfastBg: Color @Composable get() = if (isDark) Color(0xFF382315) else Color(0xFFFFF2E6)
val BreakfastAccent = Color(0xFFF27022)
val LunchBg: Color @Composable get() = if (isDark) Color(0xFF363214) else Color(0xFFFFFCE5)
val LunchAccent = Color(0xFFD69C00)
val SnacksBg: Color @Composable get() = if (isDark) Color(0xFF153321) else Color(0xFFEAFBF0)
val SnacksAccent = Color(0xFF107C41)
val DinnerBg: Color @Composable get() = if (isDark) Color(0xFF16203D) else Color(0xFFEEF2FF)
val DinnerAccent = Color(0xFF2B5DF2)

// Inactive States
val CapsuleInactiveBg: Color @Composable get() = if (isDark) Color(0xFF262320) else Color(0xFFF0ECE6)
val CapsuleInactiveText: Color @Composable get() = if (isDark) Color(0xFF6B6258) else Color(0xFFB0A69D)
val CapsuleActiveText = Color(0xFFFFFFFF)

// General
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

