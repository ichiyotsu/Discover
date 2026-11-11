package com.tokidiscover.app
import android.app.Application
import com.tokidiscover.app.data.BrowserStore
import com.tokidiscover.app.data.SettingsStore
class TokiDiscoverApplication : Application() {
    lateinit var settings: SettingsStore; lateinit var library: BrowserStore
    override fun onCreate() { super.onCreate(); settings = SettingsStore(this); library = BrowserStore(this) }
}
