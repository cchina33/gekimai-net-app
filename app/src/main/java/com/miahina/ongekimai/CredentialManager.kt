package com.miahina.ongekimai

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import androidx.security.crypto.EncryptedSharedPreferences.PrefKeyEncryptionScheme
import androidx.security.crypto.EncryptedSharedPreferences.PrefValueEncryptionScheme


class CredentialManager(context: Context) {

    private val mainKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPreferences = EncryptedSharedPreferences.create(
        context,
        "secure_prefs",
        mainKey,
        PrefKeyEncryptionScheme.AES256_SIV,
        PrefValueEncryptionScheme.AES256_GCM
    )

    fun saveCredentials(id: String, pass: String) {
        sharedPreferences.edit().apply {
            putString("sega_id", id)
            putString("sega_pass", pass)
            apply()
        }
    }

    fun getId(): String = sharedPreferences.getString("sega_id", "") ?: ""
    fun getPassword(): String = sharedPreferences.getString("sega_pass", "") ?: ""
    fun saveColorMode(mode: Int) = sharedPreferences.edit().putInt("color_mode", mode).apply()
    fun getColorMode(): Int = sharedPreferences.getInt("color_mode", 0)
    fun saveBiometricEnabled(isEnabled: Boolean) = sharedPreferences.edit().putBoolean("biometric_enabled", isEnabled).apply()
    fun isBiometricEnabled(): Boolean = sharedPreferences.getBoolean("biometric_enabled", false)

    // 💡 新機能：ScoreLog用のJavaScriptコードの保存
    fun saveScoreLogJs(jsCode: String) {
        sharedPreferences.edit().putString("score_log_js", jsCode).apply()
    }

    // 💡 新機能：ScoreLog用のJavaScriptコードの取得
    fun getScoreLogJs(): String = sharedPreferences.getString("score_log_js", "") ?: ""

    // === CredentialManager クラス内に追加してください ===

    fun isReminderEnabled(): Boolean {
        // デフォルトはオン(true)
        return sharedPreferences.getBoolean("reminder_enabled", true)
    }

    fun saveReminderEnabled(enabled: Boolean) {
        sharedPreferences.edit().putBoolean("reminder_enabled", enabled).apply()
    }

    fun getReminderHour(): Int {
        return sharedPreferences.getInt("reminder_hour", 10) // デフォルト10時
    }

    fun getReminderMinute(): Int {
        return sharedPreferences.getInt("reminder_minute", 22) // デフォルト22分
    }

    fun saveReminderTime(hour: Int, minute: Int) {
        sharedPreferences.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
    }
}