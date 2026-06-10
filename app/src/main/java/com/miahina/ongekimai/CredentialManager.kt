@file:Suppress("DEPRECATION")
package com.miahina.ongekimai

import android.content.Context
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.crypto.tink.Aead
import com.google.crypto.tink.KeyTemplates
import com.google.crypto.tink.aead.AeadConfig
import com.google.crypto.tink.integration.android.AndroidKeysetManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

// DataStoreの定義
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CredentialManager(private val context: Context) {

    @Suppress("DEPRECATION")
    private val aead: Aead by lazy {
        AeadConfig.register()
        AndroidKeysetManager.Builder()
            .withSharedPref(context, "tink_keyset", "tink_prefs")
            .withKeyTemplate(KeyTemplates.get("AES256_GCM"))
            .withMasterKeyUri("android-keystore://tink_master_key")
            .build()
            .keysetHandle
            .getPrimitive(Aead::class.java)
    }

    @Suppress("DEPRECATION")
    private object PreferencesKeys {
        val SEGA_ID = stringPreferencesKey("sega_id")
        val SEGA_PASS = stringPreferencesKey("sega_pass")
        val COLOR_MODE = intPreferencesKey("color_mode")
        val DEFAULT_WEBVIEW = intPreferencesKey("default_webview")
        val BIOMETRIC_ENABLED = booleanPreferencesKey("biometric_enabled")
        val SCORE_LOG_JS = stringPreferencesKey("score_log_js")
        val REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val REMINDER_HOUR = intPreferencesKey("reminder_hour")
        val REMINDER_MINUTE = intPreferencesKey("reminder_minute")
        val INTIMACY_DATA = stringPreferencesKey("intimacy_data")
        val INTIMACY_GOAL = intPreferencesKey("intimacy_goal")
        val COPY_PASTE_ENABLED = booleanPreferencesKey("copy_paste_enabled")
        val MIGRATED = booleanPreferencesKey("migrated")
    }

    init {
        // 既存のEncryptedSharedPreferencesからの移行
        checkMigration()
    }

    private fun checkMigration() {
        val isMigrated = runBlocking {
            context.dataStore.data.map { it[PreferencesKeys.MIGRATED] ?: false }.first()
        }
        if (!isMigrated) {
            migrateFromEncryptedPrefs()
        }
    }

    @Suppress("DEPRECATION")
    private fun migrateFromEncryptedPrefs() {
        try {
            val mainKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()

            val oldPrefs = EncryptedSharedPreferences.create(
                context,
                "secure_prefs",
                mainKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )

            val oldId = oldPrefs.getString("sega_id", "") ?: ""
            val oldPass = oldPrefs.getString("sega_pass", "") ?: ""
            val oldColorMode = oldPrefs.getInt("color_mode", 0)
            val oldBiometric = oldPrefs.getBoolean("biometric_enabled", false)
            val oldScoreLogJs = oldPrefs.getString("score_log_js", "") ?: ""
            val oldReminder = oldPrefs.getBoolean("reminder_enabled", true)
            val oldHour = oldPrefs.getInt("reminder_hour", 10)
            val oldMinute = oldPrefs.getInt("reminder_minute", 22)

            runBlocking {
                context.dataStore.edit { prefs ->
                    if (oldId.isNotEmpty()) prefs[PreferencesKeys.SEGA_ID] = encrypt(oldId)
                    if (oldPass.isNotEmpty()) prefs[PreferencesKeys.SEGA_PASS] = encrypt(oldPass)
                    prefs[PreferencesKeys.COLOR_MODE] = oldColorMode
                    prefs[PreferencesKeys.BIOMETRIC_ENABLED] = oldBiometric
                    prefs[PreferencesKeys.SCORE_LOG_JS] = oldScoreLogJs
                    prefs[PreferencesKeys.REMINDER_ENABLED] = oldReminder
                    prefs[PreferencesKeys.REMINDER_HOUR] = oldHour
                    prefs[PreferencesKeys.REMINDER_MINUTE] = oldMinute
                    prefs[PreferencesKeys.MIGRATED] = true
                }
            }
            // 旧データを削除（オプション）
            // oldPrefs.edit().clear().apply()
        } catch (e: Exception) {
            android.util.Log.e("CredentialManager", "Migration failed", e)
        }
    }

    // 暗号化/復号化
    private fun encrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val encrypted = aead.encrypt(value.toByteArray(Charsets.UTF_8), null)
            Base64.encodeToString(encrypted, Base64.DEFAULT)
        } catch (_: Exception) {
            ""
        }
    }

    private fun decrypt(value: String): String {
        if (value.isEmpty()) return ""
        return try {
            val decoded = Base64.decode(value, Base64.DEFAULT)
            String(aead.decrypt(decoded, null), Charsets.UTF_8)
        } catch (_: Exception) {
            ""
        }
    }

    // 同期アクセス用ヘルパー
    private fun <T> getValueSync(key: Preferences.Key<T>, defaultValue: T): T {
        return runBlocking {
            context.dataStore.data.map { it[key] ?: defaultValue }.first()
        }
    }

    private fun <T> setValueSync(key: Preferences.Key<T>, value: T) {
        runBlocking {
            context.dataStore.edit { it[key] = value }
        }
    }

    fun saveCredentials(id: String, pass: String) {
        setValueSync(PreferencesKeys.SEGA_ID, encrypt(id))
        setValueSync(PreferencesKeys.SEGA_PASS, encrypt(pass))
    }

    fun getId(): String = decrypt(getValueSync(PreferencesKeys.SEGA_ID, ""))
    fun getPassword(): String = decrypt(getValueSync(PreferencesKeys.SEGA_PASS, ""))

    fun saveColorMode(mode: Int) = setValueSync(PreferencesKeys.COLOR_MODE, mode)
    fun getColorMode(): Int = getValueSync(PreferencesKeys.COLOR_MODE, 0)

    fun saveDefaultWebView(mode: Int) = setValueSync(PreferencesKeys.DEFAULT_WEBVIEW, mode)
    fun getDefaultWebView(): Int = getValueSync(PreferencesKeys.DEFAULT_WEBVIEW, 0) // 0: Ongeki, 1: Maimai

    fun saveBiometricEnabled(isEnabled: Boolean) = setValueSync(PreferencesKeys.BIOMETRIC_ENABLED, isEnabled)
    fun isBiometricEnabled(): Boolean = getValueSync(PreferencesKeys.BIOMETRIC_ENABLED, false)

    fun saveScoreLogJs(jsCode: String) = setValueSync(PreferencesKeys.SCORE_LOG_JS, jsCode)
    fun getScoreLogJs(): String = getValueSync(PreferencesKeys.SCORE_LOG_JS, "")

    fun isReminderEnabled(): Boolean = getValueSync(PreferencesKeys.REMINDER_ENABLED, true)
    fun saveReminderEnabled(enabled: Boolean) = setValueSync(PreferencesKeys.REMINDER_ENABLED, enabled)

    fun getReminderHour(): Int = getValueSync(PreferencesKeys.REMINDER_HOUR, 10)
    fun getReminderMinute(): Int = getValueSync(PreferencesKeys.REMINDER_MINUTE, 22)
    fun saveReminderTime(hour: Int, minute: Int) {
        setValueSync(PreferencesKeys.REMINDER_HOUR, hour)
        setValueSync(PreferencesKeys.REMINDER_MINUTE, minute)
    }

    fun saveIntimacyData(json: String) = setValueSync(PreferencesKeys.INTIMACY_DATA, json)
    fun getIntimacyData(): String = getValueSync(PreferencesKeys.INTIMACY_DATA, "")

    fun saveIntimacyGoal(goal: Int) = setValueSync(PreferencesKeys.INTIMACY_GOAL, goal)
    fun getIntimacyGoal(): Int = getValueSync(PreferencesKeys.INTIMACY_GOAL, 800)

    fun saveCopyPasteEnabled(enabled: Boolean) = setValueSync(PreferencesKeys.COPY_PASTE_ENABLED, enabled)
    fun isCopyPasteEnabled(): Boolean = getValueSync(PreferencesKeys.COPY_PASTE_ENABLED, true)
}
