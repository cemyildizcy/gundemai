package com.example.ui.presentation

fun shouldShowFeedControls(selectedTab: Int): Boolean = selectedTab == 0

fun shouldShowVerificationBadgeInFeed(status: String?): Boolean =
    status.equals("OFFICIAL_CONFIRMED", ignoreCase = true) ||
        status.equals("MULTI_SOURCE_CONFIRMED", ignoreCase = true) ||
        status.equals("UNVERIFIED_CLAIM", ignoreCase = true) ||
        status.equals("SOURCES_CONFLICT", ignoreCase = true)
