package com.tokidiscover.app.ui.components
import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.tokidiscover.app.ui.Dest
import com.tokidiscover.app.ui.pressScale
private data class NavEntry(val dest: Dest, val icon: ImageVector, val selectedIcon: ImageVector, val label: String)
private val navEntries = listOf(
    NavEntry(Dest.Browser, Icons.Outlined.Public, Icons.Filled.Public, "浏览"),
    NavEntry(Dest.Tabs, Icons.Outlined.Tab, Icons.Filled.Tab, "标签"),
    NavEntry(Dest.Library, Icons.Outlined.BookmarkBorder, Icons.Filled.Bookmark, "书库"),
    NavEntry(Dest.Settings, Icons.Outlined.Settings, Icons.Filled.Settings, "设置"),
)
/** Tokinote FloatingNavBar复刻: 36dp capsule, one glass layer (passed in), selected = solid primary pill,
 *  label spring open 0.6/440 close 0.8/440, LongPress haptics, pressScale 0.92. Caller pads content by measured height. */
@Composable fun FloatingNavBar(current: Dest, onSelect: (Dest) -> Unit, modifier: Modifier = Modifier, glass: Modifier = Modifier) {
    Row(modifier = modifier.clip(RoundedCornerShape(36.dp)).then(glass).padding(horizontal = 8.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        navEntries.forEach { entry -> NavItem(entry, current == entry.dest, onSelect) } }
}
@Composable private fun NavItem(entry: NavEntry, selected: Boolean, onSelect: (Dest) -> Unit) {
    val interaction = remember { MutableInteractionSource() }; val haptic = LocalHapticFeedback.current
    Row(modifier = Modifier.height(48.dp).clip(CircleShape)
        .background(if (selected) MaterialTheme.colorScheme.primary else Color.Transparent)
        .clickable(interactionSource = interaction, indication = ripple()) { haptic.performHapticFeedback(HapticFeedbackType.LongPress); onSelect(entry.dest) }
        .pressScale(interaction, 0.92f).padding(horizontal = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(if (selected) entry.selectedIcon else entry.icon, entry.label, tint = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(22.dp))
        AnimatedVisibility(visible = selected, enter = expandHorizontally(spring(dampingRatio = 0.6f, stiffness = 440f)) + fadeIn(), exit = shrinkHorizontally(spring(dampingRatio = 0.8f, stiffness = 440f)) + fadeOut()) {
            Row(verticalAlignment = Alignment.CenterVertically) { Spacer(Modifier.width(8.dp)); Text(entry.label, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onPrimary, maxLines = 1) } }
    }
}
