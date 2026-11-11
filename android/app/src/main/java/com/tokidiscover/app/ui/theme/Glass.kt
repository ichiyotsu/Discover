package com.tokidiscover.app.ui.theme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.blur.HazeBlurStyle
import dev.chrisbanes.haze.blur.HazeColorEffect
import dev.chrisbanes.haze.blur.hazeBlur
/** Copied from Tokinote Glass.kt: single-layer glass recipe. */
fun Modifier.tokiGlass(hazeState: HazeState, blurRadius: Dp, tints: List<Color>, fallback: Color, noise: Float = 0.02f): Modifier = hazeBlur(
    input = HazeInput.Sources(hazeState),
    style = HazeBlurStyle { blurRadius(blurRadius); noiseFactor(noise)
        colorEffects(tints.map { HazeColorEffect.tint(it) }); fallbackColorEffect(HazeColorEffect.tint(fallback)) },
)
/** Floating capsule glass: 30dp blur, black 3%/16% + white 10%/8%, fallback black 30%/55%, NO solid layer. Used by FloatingNavBar + FloatingOmnibox. */
@Composable fun Modifier.tokiNavGlass(hazeState: HazeState): Modifier {
    val dark = isSystemInDarkTheme()
    val black = if (dark) 0.16f else 0.03f; val white = if (dark) 0.08f else 0.10f
    return tokiGlass(hazeState, 30.dp, listOf(Color.Black.copy(alpha = black), Color.White.copy(alpha = white)),
        fallback = Color.Black.copy(alpha = if (dark) 0.55f else 0.30f))
}
