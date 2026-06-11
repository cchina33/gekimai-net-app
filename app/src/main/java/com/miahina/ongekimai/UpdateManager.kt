package com.miahina.ongekimai

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.appcompat.app.AlertDialog
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URL

class UpdateManager(private val context: Context) {

    // GitHub Pages上のJSONファイルURL
    private val updateUrl = "https://cchina33.github.io/gekimai-net-app/update.json"

    data class UpdateInfo(
        val version_code: Int,
        val version_name: String,
        val download_url: String,
        val changelog: List<String>
    )

    fun checkForUpdates() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val json = URL(updateUrl).readText()
                val info = Gson().fromJson(json, UpdateInfo::class.java)

                withContext(Dispatchers.Main) {
                    val currentVersionCode = try {
                        context.packageManager.getPackageInfo(context.packageName, 0).let {
                            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                                it.longVersionCode.toInt()
                            } else {
                                @Suppress("DEPRECATION")
                                it.versionCode
                            }
                        }
                    } catch (e: Exception) {
                        0
                    }

                    if (info.version_code > currentVersionCode) {
                        showUpdateDialog(info)
                    }
                }
            } catch (e: Exception) {
                Log.e("UpdateManager", "Failed to check for updates", e)
            }
        }
    }

    private fun showUpdateDialog(info: UpdateInfo) {
        val changelogText = info.changelog.joinToString("\n") { "・$it" }
        AlertDialog.Builder(context)
            .setTitle(R.string.update_dialog_title)
            .setMessage(context.getString(R.string.update_dialog_message, info.version_name, changelogText))
            .setPositiveButton(R.string.update_btn_download) { _, _ ->
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.download_url))
                context.startActivity(intent)
            }
            .setNegativeButton(R.string.update_btn_later, null)
            .show()
    }
}
