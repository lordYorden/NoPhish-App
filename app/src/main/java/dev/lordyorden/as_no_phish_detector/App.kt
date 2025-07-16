package dev.lordyorden.as_no_phish_detector

import android.app.Application
import dev.lordyorden.tradely.utilities.ImageLoader

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
    }
}