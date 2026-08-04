package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Legacy aliases now resolve to the shared navy/teal editorial palette.
val Ink950 = BrandNavyDark
val Ink900 = BrandNavy
val Ink850 = BrandNavySurface
val Ink800 = BrandNavySurfaceHigh
val Ink700 = Color(0xFF29404D)
val Ink300 = Color(0xFFAAB0B5)
val Ink100 = Color(0xFFE8EAEC)
val Paper = PaperBackground
val PaperMuted = BrandTealTint

val EditorialRed = BreakingRed
val EditorialRedDark = Color(0xFFB9343A)
val EditorialTeal = BrandTeal
val EditorialTealDark = Color(0xFF0F766E)
val EditorialAmber = WarningSoft

// Compatibility aliases used by older screens while they migrate to MaterialTheme.
val NavyBackgroundDark = Ink950
val NavySurfaceDark = Ink850
val NavyCardDark = Ink800
val NavyCardBorder = Ink700
val AccentBlue = BrandTeal
val AccentPurple = BrandNavy
val AccentCyan = EditorialAmber
val TextPrimaryDark = Ink100
val TextSecondaryDark = Ink300
val TextMutedDark = Color(0xFF777E84)
val BackgroundLight = Paper
val SurfaceLight = PaperSurface
val TextPrimaryLight = InkPrimary
val TextSecondaryLight = InkSecondary

val StatusOfficialConfirmed = Color(0xFF35A071)
val StatusMultiSourceConfirmed = EditorialTeal
val StatusSingleSourceReport = EditorialAmber
val StatusUnverifiedClaim = Color(0xFFC76C43)
val StatusDevelopingStory = Color(0xFF6D7890)
val StatusSourcesConflict = ErrorSoft
val StatusInsufficientInfo = Color(0xFF7B8287)
