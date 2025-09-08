package com.example.e_zuka

import android.app.Application
import androidx.compose.ui.text.intl.Locale
import com.example.e_zuka.data.settings.AppSettings
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.firebase.FirebaseApp

class EzukaApplication : Application() {
    lateinit var appSettings: AppSettings
        private set

    override fun onCreate() {
        super.onCreate()

        // AppSettingsの初期化
        appSettings = AppSettings(this)

        // Firebase初期化
        FirebaseApp.initializeApp(this)

        // Google Play Servicesの可用性チェック
        val availability = GoogleApiAvailability.getInstance()
        val resultCode = availability.isGooglePlayServicesAvailable(this)
        if (resultCode != ConnectionResult.SUCCESS) {
            availability.showErrorNotification(this, resultCode)
        }

        // アクセシビリティの設定
        setupAccessibility()
    }

    private fun setupAccessibility() {
        // 日本語のTalkBack読み上げ設定
        val locale = Locale("ja")
        val contentDescriptions = mapOf(
            "error_network" to "ネットワークエラーが発生しました",
            "error_auth" to "認証エラーが発生しました",
            "loading" to "データを読み込んでいます",
            "success" to "処理が完了しました"
        )

        // アプリ全体で使用するアクセシビリティ設定を保存
        AccessibilityConfig.initialize(
            locale = locale,
            contentDescriptions = contentDescriptions,
            announceTimeouts = true,
            useHighContrast = false
        )
    }

    companion object {
        @Volatile
        private var instance: EzukaApplication? = null

        fun getInstance(): EzukaApplication =
            instance ?: throw IllegalStateException("Application not initialized")
    }

    init {
        instance = this
    }
}

object AccessibilityConfig {
    private var locale: Locale = Locale("ja")
    private var contentDescriptions: Map<String, String> = emptyMap()
    private var announceTimeouts: Boolean = true
    private var useHighContrast: Boolean = false

    fun initialize(
        locale: Locale,
        contentDescriptions: Map<String, String>,
        announceTimeouts: Boolean,
        useHighContrast: Boolean
    ) {
        this.locale = locale
        this.contentDescriptions = contentDescriptions
        this.announceTimeouts = announceTimeouts
        this.useHighContrast = useHighContrast
    }

    fun getContentDescription(key: String): String {
        return contentDescriptions[key] ?: ""
    }

    fun shouldAnnounceTimeouts(): Boolean = announceTimeouts
    fun shouldUseHighContrast(): Boolean = useHighContrast
    fun getLocale(): Locale = locale
}