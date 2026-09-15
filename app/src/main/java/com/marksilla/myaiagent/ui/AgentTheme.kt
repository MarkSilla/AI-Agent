package com.marksilla.myaiagent.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AgentColors = darkColorScheme(
    primary = Color(0xFF9AAEFF),
    onPrimary = Color(0xFF111522),
    secondary = Color(0xFF7CE2C5),
    background = Color(0xFF070910),
    onBackground = Color(0xFFF4F6FB),
    surface = Color(0xFF0D1019),
    onSurface = Color(0xFFF4F6FB),
    surfaceVariant = Color(0xFF171B27),
    onSurfaceVariant = Color(0xFFADB4C6),
    outline = Color(0xFF2A3040)
)

@Composable
fun AgentTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = AgentColors, content = content)
}
