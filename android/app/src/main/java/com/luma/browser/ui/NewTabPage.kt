package com.luma.browser.ui

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luma.browser.data.LumaSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileNotFoundException

/**
 * Minimal TokiUI-style home: one wordmark, one search field, and a background
 * that can be the default gradient or a user-picked image.
 */
@Composable
fun NewTabPage(
    settings: LumaSettings,
    onOpen: (String) -> Unit,
    onFocusAddress: () -> Unit,
    topContentPadding: Dp = 0.dp,
    bottomContentPadding: Dp = 0.dp,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val background = rememberBackgroundBitmap(settings.backgroundUri)

    BoxWithConstraints(
        modifier = modifier
            .clipToBounds()
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.primaryContainer.copy(alpha = 0.90f),
                        scheme.surface,
                        scheme.tertiaryContainer.copy(alpha = 0.52f),
                    ),
                ),
            ),
    ) {
        val compact = maxWidth < 390.dp
        if (background != null) {
            Image(
                bitmap = background.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.34f),
                                Color.Black.copy(alpha = 0.08f),
                                Color.Black.copy(alpha = 0.42f),
                            ),
                        ),
                    ),
            )
        } else {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(320.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scheme.primary.copy(alpha = 0.16f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .size(280.dp)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                scheme.tertiary.copy(alpha = 0.14f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = topContentPadding, bottom = bottomContentPadding)
                .padding(horizontal = 28.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 560.dp)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Luna",
                    style = TextStyle(
                        fontFamily = MaterialTheme.typography.displayLarge.fontFamily,
                        fontSize = if (compact) 54.sp else 66.sp,
                        lineHeight = if (compact) 60.sp else 72.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = (-1.8).sp,
                        shadow = if (background != null) {
                            Shadow(
                                color = Color.Black.copy(alpha = 0.34f),
                                blurRadius = 22f,
                            )
                        } else {
                            null
                        },
                    ),
                    color = if (background != null) Color.White else scheme.onSurface,
                )
                Spacer(Modifier.height(28.dp))
                HomeSearchField(
                    onClick = onFocusAddress,
                    onBackground = background != null,
                )
            }
        }
    }
}

@Composable
private fun HomeSearchField(
    onClick: () -> Unit,
    onBackground: Boolean,
) {
    val scheme = MaterialTheme.colorScheme
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp),
        shape = RoundedCornerShape(32.dp),
        color = if (onBackground) {
            scheme.surface.copy(alpha = 0.94f)
        } else {
            scheme.surfaceContainerLowest.copy(alpha = 0.86f)
        },
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (onBackground) {
                Color.White.copy(alpha = 0.42f)
            } else {
                scheme.outlineVariant.copy(alpha = 0.55f)
            },
        ),
        shadowElevation = 12.dp,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = 20.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Rounded.Search,
                contentDescription = null,
                tint = scheme.onSurfaceVariant,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(13.dp))
            Text(
                text = "搜索或输入网址",
                style = MaterialTheme.typography.bodyLarge,
                color = scheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Surface(
                shape = CircleShape,
                color = scheme.primary,
                modifier = Modifier.size(48.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowForward,
                        contentDescription = "搜索",
                        tint = scheme.onPrimary,
                        modifier = Modifier.size(21.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun rememberBackgroundBitmap(uriString: String?): Bitmap? {
    val context = LocalContext.current
    var bitmap by remember(uriString) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(uriString) {
        bitmap = if (uriString.isNullOrBlank()) {
            null
        } else {
            withContext(Dispatchers.IO) {
                runCatching {
                    decodeBackground(context, Uri.parse(uriString))
                }.getOrNull()
            }
        }
    }
    return bitmap
}

private fun decodeBackground(context: android.content.Context, uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri).use { stream ->
        if (stream == null) throw FileNotFoundException(uri.toString())
        BitmapFactory.decodeStream(stream, null, bounds)
    }
    if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
    var sample = 1
    while (bounds.outWidth / sample > 1600 || bounds.outHeight / sample > 1600) {
        sample *= 2
    }
    val options = BitmapFactory.Options().apply {
        inSampleSize = sample
        inPreferredConfig = Bitmap.Config.ARGB_8888
    }
    return context.contentResolver.openInputStream(uri).use { stream ->
        if (stream == null) null else BitmapFactory.decodeStream(stream, null, options)
    }
}
