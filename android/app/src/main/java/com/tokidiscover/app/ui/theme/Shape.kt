@file:OptIn(androidx.compose.material3.ExperimentalMaterial3ExpressiveApi::class)
package com.tokidiscover.app.ui.theme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp
/** Copied from Tokinote Shape.kt (same as tokimusic). */
val TokiShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(36.dp), largeIncreased = RoundedCornerShape(32.dp),
    extraLargeIncreased = RoundedCornerShape(44.dp), extraExtraLarge = RoundedCornerShape(56.dp),
)
val ExpressiveCardShape = RoundedCornerShape(topStart = 34.dp, topEnd = 22.dp, bottomEnd = 34.dp, bottomStart = 22.dp)
val ExpressiveChipShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomEnd = 20.dp, bottomStart = 7.dp)
val ExpressiveIslandShape = RoundedCornerShape(34.dp)
val ExpressiveSheetShape = RoundedCornerShape(topStart = 34.dp, topEnd = 34.dp)
