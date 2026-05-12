package dev.lordyorden.as_no_phish_detector

import android.app.Application
import com.clerk.api.Clerk
import dev.lordyorden.as_no_phish_detector.utilities.ConvexHelper
import dev.lordyorden.as_no_phish_detector.utilities.ImageLoader
import dev.lordyorden.as_no_phish_detector.utilities.MaliciousNotificationStore
import dev.lordyorden.as_no_phish_detector.utilities.NotificationHelper

class App: Application() {
    override fun onCreate() {
        super.onCreate()
        ImageLoader.init(this)
        NotificationHelper.init(this)
        MaliciousNotificationStore.init(this)

        val publishableKey = "pk_test_c291Z2h0LXBlYWNvY2stMzYuY2xlcmsuYWNjb3VudHMuZGV2JA"
        Clerk.initialize(
            this,
            publishableKey = publishableKey
        )

        ConvexHelper.init(this)
    }
}
