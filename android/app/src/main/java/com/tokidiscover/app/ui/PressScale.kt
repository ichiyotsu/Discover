package com.tokidiscover.app.ui
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
/** Tokinote press feedback: scale to 0.92 while pressed. */
fun Modifier.pressScale(source: MutableInteractionSource, scale: Float = 0.92f): Modifier = composed {
    val pressed by source.collectIsPressedAsState(); val s by animateFloatAsState(if (pressed) scale else 1f, label = "pressScale")
    graphicsLayer { scaleX = s; scaleY = s }
}
