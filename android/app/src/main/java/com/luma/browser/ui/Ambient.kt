package com.luma.browser.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun StatusBarFrost(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.verticalGradient(
                colors = listOf(
                    scheme.surface.copy(alpha = 0.84f),
                    scheme.surface.copy(alpha = 0.64f),
                    scheme.surface.copy(alpha = 0.46f),
                ),
            ),
        ),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            scheme.onSurface.copy(alpha = 0.045f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.14f),
                            Color.Transparent,
                            scheme.tertiary.copy(alpha = 0.12f),
                        ),
                    ),
                )
                .blur(24.dp),
        )
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(1.dp)
                .background(scheme.outlineVariant.copy(alpha = 0.34f)),
        )
    }
}

@Composable
fun LumaAmbientBackground(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.62f),
                        scheme.surface,
                        scheme.tertiaryContainer.copy(alpha = 0.35f),
                    ),
                ),
            ),
    ) {
        Box(
            modifier = Modifier
                .offset(x = (-130).dp, y = (-80).dp)
                .size(430.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.primary.copy(alpha = 0.26f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .offset(x = 200.dp, y = 220.dp)
                .size(360.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.tertiary.copy(alpha = 0.18f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
        Box(
            modifier = Modifier
                .offset(x = 40.dp, y = 600.dp)
                .size(340.dp)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            scheme.secondary.copy(alpha = 0.14f),
                            Color.Transparent,
                        ),
                    ),
                ),
        )
    }
}
