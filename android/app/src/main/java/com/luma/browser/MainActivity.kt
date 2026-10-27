package com.luma.browser

import android.content.Intent
import android.graphics.Color as AndroidColor
import android.os.Bundle
import android.webkit.WebView
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.luma.browser.browser.BrowserController
import com.luma.browser.data.LumaSettings
import com.luma.browser.data.ThemeMode
import com.luma.browser.ui.LumaBrowserScreen
import com.luma.browser.ui.theme.LumaTheme

class MainActivity : ComponentActivity() {
    private lateinit var settings: LumaSettings
    private lateinit var controller: BrowserController
    private val sheetRequest = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.auto(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        WebView.setWebContentsDebuggingEnabled(BuildConfig.DEBUG)

        settings = LumaSettings(this)
        controller = BrowserController(this, settings) { command ->
            sheetRequest.value = command
        }

        intent?.dataString?.let { initial ->
            if (initial.startsWith("http")) controller.navigate(initial)
        }

        setContent {
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.theme) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            LumaTheme(darkTheme = darkTheme) {
                val view = LocalView.current
                SideEffect {
                    WindowCompat.getInsetsController(window, view).apply {
                        isAppearanceLightStatusBars = !darkTheme
                        isAppearanceLightNavigationBars = !darkTheme
                    }
                    window.isStatusBarContrastEnforced = false
                    window.isNavigationBarContrastEnforced = false
                    @Suppress("DEPRECATION")
                    window.navigationBarColor = if (darkTheme) {
                        AndroidColor.rgb(19, 19, 24)
                    } else {
                        AndroidColor.rgb(252, 248, 255)
                    }
                }
                LumaBrowserScreen(
                    controller = controller,
                    settings = settings,
                    sheetRequest = sheetRequest.value,
                    onSheetRequestHandled = { sheetRequest.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { url ->
            if (url.startsWith("http")) controller.navigate(url)
        }
    }

    override fun onDestroy() {
        if (isFinishing) controller.destroy()
        super.onDestroy()
    }
}
