package com.example

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.ui.theme.BrandNavy
import com.example.ui.theme.BrandTeal
import com.example.ui.theme.BreakingRed
import com.example.ui.theme.GundemDesignTokens
import com.example.ui.theme.PaperBackground
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiquidGlassDesignSystemTest {

    @Test
    fun `brand palette follows the approved flat navy teal direction`() {
        assertEquals(Color(0xFF152A3A), BrandNavy)
        assertEquals(Color(0xFF18A999), BrandTeal)
        assertEquals(Color(0xFFE5484D), BreakingRed)
        assertEquals(Color(0xFFF5F6F4), PaperBackground)
    }

    @Test
    fun `shared component dimensions are rounded spacious and accessible`() {
        assertEquals(22.dp, GundemDesignTokens.cardRadius)
        assertEquals(30.dp, GundemDesignTokens.navigationRadius)
        assertTrue(GundemDesignTokens.minimumTouchTarget >= 44.dp)
        assertEquals(8.dp, GundemDesignTokens.spacingUnit)
    }

    @Test
    fun `motion remains short and functional`() {
        assertTrue(GundemDesignTokens.motionFastMs in 150..220)
        assertTrue(GundemDesignTokens.motionStandardMs in 220..300)
    }
}
