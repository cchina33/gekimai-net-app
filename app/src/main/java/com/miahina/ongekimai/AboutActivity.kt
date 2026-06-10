package com.miahina.ongekimai

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

class AboutActivity : AppCompatActivity() {

    private lateinit var binding: com.miahina.ongekimai.databinding.ActivityAboutBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = com.miahina.ongekimai.databinding.ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.title = getString(R.string.about_title)

        // バージョン情報の取得と設定
        val version = try {
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            packageInfo.versionName
        } catch (_: Exception) {
            "1.0.0"
        }
        binding.tvVersion.text = getString(R.string.version_format, version ?: "1.0.0")

        // GitHubリンクの設定
        binding.btnGithub.setOnClickListener {
            val url = getString(R.string.github_url)
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }
}
