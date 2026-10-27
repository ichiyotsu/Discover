@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)

package com.luma.browser.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Tab
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.luma.browser.browser.BrowserController
import com.luma.browser.browser.BrowserTab
import com.luma.browser.data.LumaSettings
import kotlinx.coroutines.delay

@Composable
fun BrowserChrome(
    controller: BrowserController,
    active: BrowserTab?,
    settings: LumaSettings,
    editing: Boolean,
    onOpenTabs: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenHistory: () -> Unit,
    onEditingChanged: (Boolean) -> Unit,
    onQueryChanged: (String) -> Unit,
    onSubmit: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current

    var fieldValue by remember { mutableStateOf(TextFieldValue("")) }
    var menuOpen by remember { mutableStateOf(false) }
    val addressFocusRequester = remember { FocusRequester() }

    LaunchedEffect(active?.id, active?.url, active?.isNewTab) {
        val current = active
        if (current != null && !current.isNewTab && current.url.isNotBlank()) {
            onEditingChanged(false)
            onQueryChanged("")
            focusManager.clearFocus(force = true)
            keyboard?.hide()
            fieldValue = TextFieldValue(
                text = current.url,
                selection = TextRange.Zero,
            )
        } else if (!editing) {
            fieldValue = TextFieldValue(
                text = if (current?.isNewTab == true) "" else current?.url.orEmpty(),
                selection = TextRange.Zero,
            )
        }
    }

    LaunchedEffect(editing) {
        if (editing) {
            delay(70)
            val current = active
            val text = if (current?.isNewTab == true) "" else current?.url.orEmpty()
            fieldValue = TextFieldValue(
                text = text,
                selection = TextRange(0, text.length),
            )
            onQueryChanged(text)
            addressFocusRequester.requestFocus()
        } else {
            focusManager.clearFocus(force = true)
            keyboard?.hide()
        }
    }

    fun completeSubmit() {
        val value = fieldValue.text.trim()
        onEditingChanged(false)
        onQueryChanged("")
        focusManager.clearFocus(force = true)
        keyboard?.hide()
        if (value.isNotEmpty()) onSubmit(value)
    }

    fun copyUrl() {
        val url = active?.url.orEmpty()
        if (url.isBlank() || active?.isNewTab == true) return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clipboard.setPrimaryClip(ClipData.newPlainText("Luma URL", url))
        Toast.makeText(context, "网址已复制", Toast.LENGTH_SHORT).show()
    }

    Surface(
        modifier = modifier
            .height(64.dp)
            .shadow(
                elevation = 16.dp,
                shape = RoundedCornerShape(32.dp),
                clip = false,
                ambientColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                spotColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f),
            ),
        shape = RoundedCornerShape(32.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f),
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(horizontal = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(1.dp),
        ) {
            ChromeIconButton(
                icon = Icons.Rounded.ArrowBack,
                contentDescription = "后退",
                enabled = active?.canGoBack == true || active?.isNewTab == false,
                onClick = { controller.goBack() },
            )
            ChromeIconButton(
                icon = Icons.Rounded.ArrowForward,
                contentDescription = "前进",
                enabled = active?.canGoForward == true,
                onClick = { controller.goForward() },
            )
            ChromeIconButton(
                icon = if (active?.isLoading == true) Icons.Rounded.Close else Icons.Rounded.Refresh,
                contentDescription = if (active?.isLoading == true) "停止" else "刷新",
                enabled = active?.isNewTab != true,
                onClick = { controller.reload() },
            )

            Surface(
                onClick = { onEditingChanged(true) },
                enabled = !editing,
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLowest.copy(alpha = 0.78f),
                border = BorderStroke(
                    1.dp,
                    if (editing) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.52f),
                ),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .fillMaxHeight()
                        .padding(start = 12.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val secure = active?.url?.startsWith("https://") == true
                    val icon = when {
                        active?.isNewTab == true -> Icons.Rounded.AutoAwesome
                        secure -> Icons.Rounded.Lock
                        else -> Icons.Rounded.Language
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = if (secure) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(7.dp))

                    if (editing) {
                        BasicTextField(
                            value = fieldValue,
                            onValueChange = { value ->
                                fieldValue = value
                                onQueryChanged(value.text)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(addressFocusRequester),
                            singleLine = true,
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                            ),
                            cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Uri,
                                imeAction = ImeAction.Go,
                            ),
                            keyboardActions = KeyboardActions(onGo = { completeSubmit() }),
                            decorationBox = { innerTextField ->
                                Box(contentAlignment = Alignment.CenterStart) {
                                    if (fieldValue.text.isEmpty()) {
                                        Text(
                                            text = "搜索或输入网址",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                    innerTextField()
                                }
                            },
                        )
                        if (fieldValue.text.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(30.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        fieldValue = TextFieldValue("")
                                        onQueryChanged("")
                                    },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "清空地址栏",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp),
                                )
                            }
                        }
                    } else {
                        val displayText = if (active?.isNewTab == true) "" else active?.url.orEmpty()
                        Text(
                            text = displayText.ifBlank { "搜索或输入网址" },
                            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                            fontWeight = if (displayText.isBlank()) FontWeight.Normal else FontWeight.Medium,
                            color = if (displayText.isBlank()) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.onSurface
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            Box(contentAlignment = Alignment.Center) {
                ChromeIconButton(
                    icon = Icons.Rounded.Tab,
                    contentDescription = "标签页",
                    onClick = onOpenTabs,
                )
                if (controller.tabs.size > 1) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(top = 6.dp, end = 4.dp)
                            .size(16.dp)
                            .background(MaterialTheme.colorScheme.primary, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = controller.tabs.size.coerceAtMost(99).toString(),
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }

            Box {
                ChromeIconButton(
                    icon = Icons.Rounded.MoreVert,
                    contentDescription = "菜单",
                    onClick = { menuOpen = true },
                )
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { menuOpen = false },
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                ) {
                    DropdownMenuItem(
                        text = { Text("新建标签页") },
                        leadingIcon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            controller.newTab()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("主页") },
                        leadingIcon = { Icon(Icons.Rounded.Home, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            controller.showNewTabPage()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("刷新") },
                        leadingIcon = { Icon(Icons.Rounded.Refresh, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            controller.reload()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("复制网址") },
                        leadingIcon = { Icon(Icons.Rounded.ContentCopy, contentDescription = null) },
                        enabled = active?.isNewTab != true && active?.url?.isNotBlank() == true,
                        onClick = {
                            menuOpen = false
                            copyUrl()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("历史记录") },
                        leadingIcon = { Icon(Icons.Rounded.History, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenHistory()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("设置") },
                        leadingIcon = { Icon(Icons.Rounded.Settings, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onOpenSettings()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChromeIconButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Box(
        modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) MaterialTheme.colorScheme.onSurfaceVariant
            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.30f),
            modifier = Modifier.size(21.dp),
        )
    }
}
