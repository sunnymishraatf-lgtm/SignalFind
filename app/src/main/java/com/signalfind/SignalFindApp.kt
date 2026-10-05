package com.signalfind

import android.app.Application
import com.signalfind.data.SettingsRepository
import com.signalfind.data.db.DatabaseHelper
import com.signalfind.data.repo.HistoryRepository
import com.signalfind.data.repo.TrustedNumberRepository
import com.signalfind.location.LocationProvider
import com.signalfind.security.CryptoManager
import com.signalfind.security.RateLimiter
import com.signalfind.sms.SmsResponder
import com.signalfind.sms.SmsSender
import com.signalfind.system.NotificationHelper

/**
 * Application entry point. Builds the manual dependency graph ([AppContainer])
 * and creates notification channels. No singletons leak outside the process.
 */
class SignalFindApp : Application() {

    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannels(this)
    }
}

/**
 * Manual service locator. Keeps the graph explicit and testable without a DI
 * framework.
 */
class AppContainer(context: android.content.Context) {
    private val appContext = context.applicationContext

    val crypto = CryptoManager(appContext)
    val settings = SettingsRepository(appContext, crypto)

    val db = DatabaseHelper(appContext)

    val trustedRepo = TrustedNumberRepository(db)
    val historyRepo = HistoryRepository(db)
    val rateLimiter = RateLimiter(appContext)
    val locationProvider = LocationProvider(appContext)
    val smsSender = SmsSender(appContext, settings)

    val responder by lazy {
        SmsResponder(appContext, settings, locationProvider, smsSender, historyRepo)
    }
}
