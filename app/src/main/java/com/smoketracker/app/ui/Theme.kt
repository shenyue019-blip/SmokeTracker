package com.smoketracker.app.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Teal = Color(0xFF00897B)
private val TealDark = Color(0xFF4DB6AC)

private val LightColors = lightColorScheme(
    primary = Teal,
    secondary = Color(0xFFFF7043),
)

private val DarkColors = darkColorScheme(
    primary = TealDark,
    secondary = Color(0xFFFF8A65),
)

@Composable
fun SmokeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
