package com.miahina.ongekimai

import android.app.TimePickerDialog
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.preference.EditTextPreference
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.miahina.ongekimai.databinding.ActivitySettingsBinding
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySettingsBinding
    private lateinit var credentialManager: CredentialManager

    override fun onCreate(savedInstanceState: Bundle?) {
        credentialManager = CredentialManager(this)
        // アプリ全体のテーマ設定を反映
        applySavedColorMode()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)

        ViewCompat.setOnApplyWindowInsetsListener(binding.settingsRoot) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updatePadding(
                left = systemBars.left,
                top = systemBars.top,
                right = systemBars.right,
                bottom = systemBars.bottom
            )
            insets
        }

        if (savedInstanceState == null) {
            supportFragmentManager
                .beginTransaction()
                .replace(binding.settings.id, SettingsFragment())
                .commit()
        }
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        title = "設定"
    }

    override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            onBackPressedDispatcher.onBackPressed()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun applySavedColorMode() {
        when (credentialManager.getColorMode()) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var credentialManager: CredentialManager

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            setPreferencesFromResource(R.xml.root_preferences, rootKey)
            credentialManager = CredentialManager(requireContext())

            // 数値入力に制限
            findPreference<EditTextPreference>("intimacy_goal")?.setOnBindEditTextListener { editText ->
                editText.inputType = android.text.InputType.TYPE_CLASS_NUMBER
            }

            // 保存処理のリスナー
            findPreference<EditTextPreference>("sega_id")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveCredentials(newValue as String, credentialManager.getPassword())
                true
            }
            findPreference<EditTextPreference>("sega_pass")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveCredentials(credentialManager.getId(), newValue as String)
                true
            }
            findPreference<EditTextPreference>("score_log_js")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveScoreLogJs(newValue as String)
                true
            }
            findPreference<EditTextPreference>("intimacy_goal")?.setOnPreferenceChangeListener { _, newValue ->
                val goal = (newValue as String).toIntOrNull() ?: 800
                credentialManager.saveIntimacyGoal(goal)
                true
            }
            findPreference<SwitchPreferenceCompat>("copy_paste_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveCopyPasteEnabled(newValue as Boolean)
                true
            }

            // カラーモード（独自ダイアログ）
            findPreference<Preference>("color_mode")?.setOnPreferenceClickListener {
                showColorModeDialog()
                true
            }

            // デフォルトWebView（独自ダイアログ）
            findPreference<Preference>("default_webview")?.setOnPreferenceClickListener {
                showDefaultWebViewDialog()
                true
            }

            findPreference<SwitchPreferenceCompat>("biometric_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveBiometricEnabled(newValue as Boolean)
                true
            }
            findPreference<SwitchPreferenceCompat>("reminder_enabled")?.setOnPreferenceChangeListener { _, newValue ->
                credentialManager.saveReminderEnabled(newValue as Boolean)
                NotificationHelper.scheduleDailyReminder(requireContext())
                true
            }

            findPreference<Preference>("reminder_time")?.setOnPreferenceClickListener {
                showTimePicker()
                true
            }

            findPreference<Preference>("oss_licenses")?.setOnPreferenceClickListener {
                OpenSourceLicensesActivity.start(requireContext(), title = "オープンソースライセンス")
                true
            }

            findPreference<Preference>("about_app")?.let { pref ->
                val version = try {
                    val packageInfo = requireContext().packageManager.getPackageInfo(requireContext().packageName, 0)
                    packageInfo.versionName
                } catch (_: Exception) {
                    "Unknown"
                }
                pref.summary = getString(R.string.version_format, version)
                pref.setOnPreferenceClickListener {
                    startActivity(android.content.Intent(requireContext(), AboutActivity::class.java))
                    true
                }
            }

            updateValues()
        }

        private fun updateValues() {
            findPreference<EditTextPreference>("sega_id")?.text = credentialManager.getId()
            findPreference<EditTextPreference>("sega_pass")?.text = credentialManager.getPassword()
            findPreference<EditTextPreference>("score_log_js")?.text = credentialManager.getScoreLogJs()
            findPreference<EditTextPreference>("intimacy_goal")?.text = credentialManager.getIntimacyGoal().toString()
            findPreference<SwitchPreferenceCompat>("copy_paste_enabled")?.isChecked = credentialManager.isCopyPasteEnabled()
            findPreference<SwitchPreferenceCompat>("biometric_enabled")?.isChecked = credentialManager.isBiometricEnabled()
            findPreference<SwitchPreferenceCompat>("reminder_enabled")?.isChecked = credentialManager.isReminderEnabled()
            updateColorModeSummary()
            updateDefaultWebViewSummary()
            updateReminderTimeSummary()
        }

        private fun updateColorModeSummary() {
            val entries = resources.getStringArray(R.array.color_mode_entries)
            val mode = credentialManager.getColorMode()
            findPreference<Preference>("color_mode")?.summary = entries.getOrElse(mode) { entries[0] }
        }

        private fun updateDefaultWebViewSummary() {
            val entries = resources.getStringArray(R.array.default_webview_entries)
            val mode = credentialManager.getDefaultWebView()
            findPreference<Preference>("default_webview")?.summary = entries.getOrElse(mode) { entries[0] }
        }

        private fun showColorModeDialog() {
            val entries = resources.getStringArray(R.array.color_mode_entries)
            var currentSelection = credentialManager.getColorMode()

            AlertDialog.Builder(requireContext())
                .setTitle("カラーモード設定")
                .setSingleChoiceItems(entries, currentSelection) { _, which ->
                    currentSelection = which
                }
                .setPositiveButton("OK") { _, _ ->
                    credentialManager.saveColorMode(currentSelection)
                    updateColorModeSummary()
                    // 設定画面自体に即座に反映
                    (activity as? SettingsActivity)?.applySavedColorMode()
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        private fun showDefaultWebViewDialog() {
            val entries = resources.getStringArray(R.array.default_webview_entries)
            var currentSelection = credentialManager.getDefaultWebView()

            AlertDialog.Builder(requireContext())
                .setTitle("デフォルトのWebView設定")
                .setSingleChoiceItems(entries, currentSelection) { _, which ->
                    currentSelection = which
                }
                .setPositiveButton("OK") { _, _ ->
                    credentialManager.saveDefaultWebView(currentSelection)
                    updateDefaultWebViewSummary()
                }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        private fun updateReminderTimeSummary() {
            val hour = credentialManager.getReminderHour()
            val minute = credentialManager.getReminderMinute()
            findPreference<Preference>("reminder_time")?.summary = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
        }

        private fun showTimePicker() {
            val hour = credentialManager.getReminderHour()
            val minute = credentialManager.getReminderMinute()
            TimePickerDialog(
                requireContext(),
                { _, h, m ->
                    credentialManager.saveReminderTime(h, m)
                    updateReminderTimeSummary()
                    NotificationHelper.scheduleDailyReminder(requireContext())
                },
                hour,
                minute,
                true
            ).show()
        }
    }
}
