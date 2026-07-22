package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Neutral editorial palette. Red is reserved for the brand and urgent news.
val Ink950 = Color(0xFF101214)
val Ink900 = Color(0xFF15181B)
val Ink850 = Color(0xFF1B1F23)
val Ink800 = Color(0xFF22272C)
val Ink700 = Color(0xFF343A40)
val Ink300 = Color(0xFFAAB0B5)
val Ink100 = Color(0xFFE8EAEC)
val Paper = Color(0xFFF5F5F2)
val PaperSurface = Color(0xFFFFFFFF)
val PaperMuted = Color(0xFFECEDE9)

val EditorialRed = Color(0xFFD94B45)
val EditorialRedDark = Color(0xFFAE302D)
val EditorialTeal = Color(0xFF397A75)
val EditorialTealDark = Color(0xFF235E5A)
val EditorialAmber = Color(0xFFD39A3B)

// Compatibility aliases used by older screens while they migrate to MaterialTheme.
val NavyBackgroundDark = Ink950
val NavySurfaceDark = Ink850
val NavyCardDark = Ink800
val NavyCardBorder = Ink700
val AccentBlue = EditorialRed
val AccentPurple = EditorialTeal
val AccentCyan = EditorialAmber
val AccentGradientStart = EditorialRed
val AccentGradientEnd = EditorialRedDark
val TextPrimaryDark = Ink100
val TextSecondaryDark = Ink300
val TextMutedDark = Color(0xFF777E84)
val BackgroundLight = Paper
val SurfaceLight = PaperSurface
val TextPrimaryLight = Color(0xFF191C1E)
val TextSecondaryLight = Color(0xFF5D6368)

val StatusOfficialConfirmed = Color(0xFF35A071)
val StatusMultiSourceConfirmed = EditorialTeal
val StatusSingleSourceReport = EditorialAmber
val StatusUnverifiedClaim = Color(0xFFC76C43)
val StatusDevelopingStory = Color(0xFF6D7890)
val StatusSourcesConflict = EditorialRed
val StatusInsufficientInfo = Color(0xFF7B8287)
