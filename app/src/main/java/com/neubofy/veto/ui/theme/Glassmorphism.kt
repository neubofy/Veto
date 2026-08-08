package com.neubofy.veto.ui.theme

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Modifier
import androidx.compose.runtime.Composable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

// To achieve true glassmorphism with backdrop blur in Compose natively, you generally need
// RenderEffect.createBlurEffect() combined with a graphicsLayer that draws beneath the content.
// However, implementing complex custom layout drawing is often overkill.
// To provide perfect visibility and adhere to Material 3 standard translucent styling without blurring the foreground text:
@Composable
fun Modifier.glassmorphism(
    shape: Shape = RoundedCornerShape(16.dp),
): Modifier {
    val isDark = isSystemInDarkTheme()

    // In Dark Mode, a very slight white overlay creates a nice elevated "glass" look over dark backgrounds.
    // In Light Mode, a more pronounced white overlay creates the frosted effect.
    val backgroundColor = if (isDark) {
        Color.White.copy(alpha = 0.05f)
    } else {
        Color.White.copy(alpha = 0.4f)
    }

    val borderColor = if (isDark) {
        Color.White.copy(alpha = 0.1f)
    } else {
        Color.White.copy(alpha = 0.2f)
    }

    return this
        .clip(shape)
        .background(backgroundColor)
        .border(1.dp, borderColor, shape)
}
