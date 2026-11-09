package dev.ichiyotsu.tokidiscover

import android.os.Bundle
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import dev.ichiyotsu.tokidiscover.browser.BrowserViewModel
import dev.ichiyotsu.tokidiscover.ui.BrowserScreen
import dev.ichiyotsu.tokidiscover.ui.TokiTheme

class MainActivity : ComponentActivity() {
    private val model: BrowserViewModel by viewModels()
    private var pendingFile: ValueCallback<Array<android.net.Uri>>? = null

    private val chooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val uris = WebChromeClient.FileChooserParams.parseResult(result.resultCode, result.data)
        pendingFile?.onReceiveValue(uris)
        pendingFile = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        model.fileChooser = { callback, params ->
            pendingFile?.onReceiveValue(null)
            pendingFile = callback
            runCatching { chooser.launch(params.createIntent()) }.onFailure {
                callback?.onReceiveValue(null)
                pendingFile = null
            }
        }
        setContent {
            TokiTheme {
                LaunchedEffect(Unit) {
                    intent?.dataString?.let { model.openExternal(it) }
                }
                BrowserScreen(model)
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.dataString?.let { model.openExternal(it) }
    }
}
