package com.miahina.ongekimai
import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Rect
import android.net.http.SslError
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Base64
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.PixelCopy
import android.view.View
import android.webkit.CookieManager
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import androidx.core.view.GravityCompat
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.viewpager2.widget.ViewPager2
import com.google.gson.Gson
import com.miahina.ongekimai.databinding.ActivityMainBinding
import com.yalantis.ucrop.UCrop
import java.io.File


class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "OngekiApp_Main"
    }

    private lateinit var binding: ActivityMainBinding
    private lateinit var credentialManager: CredentialManager
    private lateinit var intimacyAdapter: IntimacyAdapter

    // 現在のWeb表示モードを保持 (0: Ongeki, 1: Maimai)
    private var currentWebMode = -1

    // 💡 追加：最後に読み込んだデフォルトWebViewのモードを記憶する変数
    private var lastDefaultWebViewMode = -1

    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showToast("通知が許可されました。")
            NotificationHelper.scheduleDailyReminder(this)
        }
    }

    private var isAuthCleared = false
    private var isAuthPromptShowing = false

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        credentialManager = CredentialManager(this)
        applySavedColorMode()

        // Toolbarの設定
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = "Ongeki Tools"

        // Drawerの設定 (ハンバーガーメニュー)
        val toggle = ActionBarDrawerToggle(
            this, binding.drawerLayout, binding.toolbar,
            0, 0
        )
        binding.drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // Navigation Drawerのクリックイベント
        binding.navigationView.setNavigationItemSelectedListener { item ->
            Log.d(TAG, "Navigation item clicked: ${item.title}")
            when (item.itemId) {
                R.id.nav_ongeki -> {
                    if (currentWebMode != 0) {
                        Log.d(TAG, "Switching to Ongeki WebView (Reload)")
                        currentWebMode = 0
                        binding.webView.stopLoading()
                        binding.webView.loadUrl("https://ongeki-net.com/ongeki-mobile/")
                    } else {
                        Log.d(TAG, "Showing Ongeki WebView (Keep state)")
                    }
                    switchPage(isHome = true, title = getString(R.string.menu_ongeki))
                }
                R.id.nav_maimai -> {
                    if (currentWebMode != 1) {
                        Log.d(TAG, "Switching to Maimai WebView (Reload)")
                        currentWebMode = 1
                        binding.webView.stopLoading()
                        binding.webView.loadUrl("https://maimaidx.jp/maimai-mobile/")
                    } else {
                        Log.d(TAG, "Showing Maimai WebView (Keep state)")
                    }
                    switchPage(isHome = true, title = getString(R.string.menu_maimai))
                }
                R.id.nav_intimacy -> {
                    Log.d(TAG, "Navigating to Intimacy Page")
                    switchPage(isHome = false)
                }
            }
            binding.drawerLayout.closeDrawer(GravityCompat.START)
            true
        }

        // 戻るボタンの制御
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (binding.drawerLayout.isDrawerOpen(GravityCompat.START)) {
                    binding.drawerLayout.closeDrawer(GravityCompat.START)
                } else if (binding.pageIntimacy.isVisible) {
                    // 親密度ページからWebViewに戻る
                    val title = if (currentWebMode == 1) "maimai(WebView)" else "オンゲキ(WebView)"
                    val navId = if (currentWebMode == 1) R.id.nav_maimai else R.id.nav_ongeki
                    switchPage(isHome = true, title = title)
                    binding.navigationView.setCheckedItem(navId)
                } else if (binding.webView.canGoBack()) {
                    binding.webView.goBack()
                } else {
                    // 通常の戻る動作（アプリ終了など）
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                    isEnabled = true
                }
            }
        })

        // 起動時はホーム(WebView)を表示
        switchPage(isHome = true)

        // 通知・認証の初期化
        NotificationHelper.createNotificationChannel(this)
        checkAndRequestNotificationPermission()
        if (credentialManager.isBiometricEnabled()) {
            binding.root.visibility = View.INVISIBLE
            showBiometricPrompt()
        } else {
            isAuthCleared = true
        }

        // WebView初期化
        initWebView()

        // 💡 生体認証が有効な場合は、認証成功後にロードを行う
        if (!credentialManager.isBiometricEnabled()) {
            loadDefaultWebView()
        }

        // ボタンパネル設定
        val adapter = ButtonPagerAdapter(
            onTallyClick = { executeTally() },
            onGetJewelsClick = { executeGetJewels() },
            onAnalyzerClick = { executeAnalyzer() },
            onScoreLogClick = { executeScoreLog() },
            onSelectiveScreenshotClick = { startSelectiveScreenshot() }
        ) { executeOverPrint() }
        binding.buttonViewPager.adapter = adapter

        // 親密度テーブル設定
        intimacyAdapter = IntimacyAdapter(emptyList())
        binding.rvIntimacyTable.layoutManager = LinearLayoutManager(this)
        binding.rvIntimacyTable.adapter = intimacyAdapter

        // 保存された親密度データの読み込み
        loadSavedIntimacyData()

        binding.btnResetIntimacy.setOnClickListener {
            AlertDialog.Builder(this)
                .setTitle("親密度データのリセット")
                .setMessage("保存されている親密度データをリセットしますか？")
                .setPositiveButton("リセット") { _, _ -> resetIntimacyData() }
                .setNegativeButton("キャンセル", null)
                .show()
        }

        binding.swipeRefreshLayout.setOnRefreshListener {
            vibratePhone()
            binding.webView.reload()
        }
    }

    private fun vibratePhone() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(VIBRATOR_SERVICE) as Vibrator
        }

        vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE))
    }

    private fun loadDefaultWebView() {
        val defaultMode = credentialManager.getDefaultWebView()
        Log.d(TAG, "loadDefaultWebView: mode=$defaultMode")
        lastDefaultWebViewMode = defaultMode
        currentWebMode = defaultMode

        binding.webView.post {
            if (defaultMode == 1) {
                Log.d(TAG, "Loading default: Maimai")
                switchPage(isHome = true, title = getString(R.string.menu_maimai))
                binding.webView.stopLoading()
                binding.webView.loadUrl("https://maimaidx.jp/maimai-mobile/")
                binding.navigationView.setCheckedItem(R.id.nav_maimai)
            } else {
                Log.d(TAG, "Loading default: Ongeki")
                switchPage(isHome = true, title = getString(R.string.menu_ongeki))
                binding.webView.stopLoading()
                binding.webView.loadUrl("https://ongeki-net.com/ongeki-mobile/")
                binding.navigationView.setCheckedItem(R.id.nav_ongeki)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        if (credentialManager.isBiometricEnabled() && !isAuthCleared && !isAuthPromptShowing) {
            showBiometricPrompt()
        }
    }

    // 💡 追加：設定画面から戻ってきたときに変更を即座に反映させる
    override fun onResume() {
        super.onResume()
        // 生体認証がクリアされている、または最初から無効な場合のみ実行
        if (isAuthCleared) {
            val currentMode = credentialManager.getDefaultWebView()
            // 最初の一回（-1）ではなく、設定画面等で値が変わって戻ってきた場合のみ再ロード
            if (lastDefaultWebViewMode != -1 && (lastDefaultWebViewMode != currentMode)) {
                loadDefaultWebView()
            }
            // 親密度データの再読み込み（目標レベル変更の反映のため）
            loadSavedIntimacyData()
        }
    }

    private fun showBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        if (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL) != BiometricManager.BIOMETRIC_SUCCESS) {
            isAuthCleared = true
            binding.root.visibility = View.VISIBLE
            loadDefaultWebView()
            return
        }

        isAuthPromptShowing = true
        val executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                isAuthCleared = true
                isAuthPromptShowing = false
                binding.root.visibility = View.VISIBLE
                showToast("認証に成功しました")
                loadDefaultWebView()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                isAuthPromptShowing = false
                if (errorCode == BiometricPrompt.ERROR_USER_CANCELED || errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON) {
                    finish()
                } else {
                    showToast("認証エラー: $errString")
                }
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("生体認証")
            .setSubtitle("アプリを利用するために認証してください")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun switchPage(isHome: Boolean, title: String = "メイン(WebView)") {
        Log.d(TAG, "switchPage: isHome=$isHome, title=$title")
        if (isHome) {
            binding.pageHome.visibility = View.VISIBLE
            binding.pageIntimacy.visibility = View.GONE
            supportActionBar?.title = title
        } else {
            binding.pageHome.visibility = View.GONE
            binding.pageIntimacy.visibility = View.VISIBLE
            supportActionBar?.title = "親密度データ表"
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.options_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private fun initWebView() {
        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(binding.webView, true)

        binding.webView.settings.apply {
            setSupportZoom(true)
            builtInZoomControls = true
            displayZoomControls = false
            javaScriptEnabled = true
            domStorageEnabled = true
            @Suppress("DEPRECATION")
            databaseEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            setSupportMultipleWindows(false) // 複数ウィンドウをオフにしてみる
            javaScriptCanOpenWindowsAutomatically = true
            mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_ALWAYS_ALLOW
            // より一般的なUserAgentに更新
            userAgentString = "Mozilla/5.0 (Linux; Android 13; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Mobile Safari/537.36"
        }
        binding.webView.isLongClickable = true
        binding.webView.webViewClient = myWebViewClient
        binding.webView.webChromeClient = myWebChromeClient
        binding.webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")
        // 背景色を白に固定
        binding.webView.setBackgroundColor(android.graphics.Color.WHITE)
    }

    // 💡 親密度データをUIに反映
    fun updateIntimacyDrawer(data: OverPrintData) {
        // データの保存
        val json = Gson().toJson(data)
        credentialManager.saveIntimacyData(json)

        runOnUiThread {
            binding.tvItemsHeld.text = getString(R.string.items_held_format, data.item_big, data.item_mid, data.item_small)
            binding.tvMoney.text = getString(R.string.money_held_format, data.money_data)
            val goal = credentialManager.getIntimacyGoal()
            intimacyAdapter.updateData(data.friendly_data, goal, data.item_big, data.item_mid, data.item_small)
        }
    }

    private fun loadSavedIntimacyData() {
        val json = credentialManager.getIntimacyData()
        if (json.isNotEmpty()) {
            try {
                val data = Gson().fromJson(json, OverPrintData::class.java)
                binding.tvItemsHeld.text = getString(R.string.items_held_format, data.item_big, data.item_mid, data.item_small)
                binding.tvMoney.text = getString(R.string.money_held_format, data.money_data)
                val goal = credentialManager.getIntimacyGoal()
                intimacyAdapter.updateData(data.friendly_data, goal, data.item_big, data.item_mid, data.item_small)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load saved intimacy data", e)
            }
        }
    }

    private fun resetIntimacyData() {
        credentialManager.saveIntimacyData("")
        binding.tvItemsHeld.text = getString(R.string.items_held_format, 0, 0, 0)
        binding.tvMoney.text = getString(R.string.money_held_format, "0")
        intimacyAdapter.updateData(emptyList())
        showToast("親密度データをリセットしました")
    }

    // 💡 ページを切り替える
    fun openIntimacyPage() {
        runOnUiThread {
            switchPage(isHome = false)
            binding.navigationView.setCheckedItem(R.id.nav_intimacy)
        }
    }

    @SuppressLint("WebViewClientOnReceivedSslError")
    private val myWebViewClient = object : WebViewClient() {
        override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: SslError?) {
            val url = error?.url ?: ""
            Log.e(TAG, "SSL Error: $error at $url")
            // セガのドメインであれば、証明書エラーを無視して続行する
            if (url.contains("maimaidx.jp") || url.contains("ongeki-net.com") || url.contains("sega-id")) {
                Log.w(TAG, "Trusting SSL certificate for: $url")
                handler?.proceed()
            } else {
                handler?.cancel()
            }
        }

        override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
            super.onReceivedError(view, request, error)
            Log.e(TAG, "Network Error: ${error?.description} (Code: ${error?.errorCode}) URL: ${request?.url}")
        }

        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            Log.d(TAG, "Page load finished: $url")
            (view?.parent as? androidx.swiperefreshlayout.widget.SwipeRefreshLayout)?.isRefreshing = false
            CookieManager.getInstance().flush()
            
            if (view != null && credentialManager.isCopyPasteEnabled()) {
                val js = loadJsFromAssets("enable_copy_paste.js")
                if (js.isNotEmpty()) {
                    view.evaluateJavascript(js, null)
                }
            }
            if (url != null) handleAutoLogin(url)
        }
    }

    private val myWebChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(v: WebView?, isD: Boolean, isU: Boolean, r: Message?): Boolean {
            val t = r?.obj as? WebView.WebViewTransport
            if (t != null) { t.webView = v; r.sendToTarget(); return true }
            return false
        }
    }

    private fun handleAutoLogin(url: String) {
        val isLogin = url.contains("ongeki-net.com") || url.contains("maimaidx.jp") || url.contains("sega-id")
        if (isLogin) {
            val sid = credentialManager.getId()
            val pass = credentialManager.getPassword()
            if (sid.isNotEmpty() && pass.isNotEmpty()) {
                val js = "(function() { var id = document.querySelector('input[name=\"segaId\"]') || document.querySelector('input[type=\"text\"]'); var p = document.querySelector('input[name=\"password\"]') || document.querySelector('input[type=\"password\"]'); if (id && p) { id.value = '$sid'; p.value = '$pass'; } })();"
                binding.webView.evaluateJavascript(js, null)
            }
        }
    }

    fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    private fun executeTally() {
        val js = loadJsFromAssets("on-mai_PlayTally.js")
        if (js.isNotEmpty()) {
            val b64 = Base64.encodeToString(js.toByteArray(), Base64.NO_WRAP)
            binding.webView.evaluateJavascript(getUnpackWrapper(b64), null)
        }
    }

    private fun executeGetJewels() {
        val js = loadJsFromAssets("get_jewels.js")
        if (js.isNotEmpty()) {
            val b64 = Base64.encodeToString(js.toByteArray(), Base64.NO_WRAP)
            binding.webView.evaluateJavascript(getUnpackWrapper(b64), null)
        }
    }

    private fun executeAnalyzer() {
        val js = loadJsFromAssets("analyzer.js")
        if (js.isNotEmpty()) binding.webView.evaluateJavascript(js, null)
    }

    private fun executeScoreLog() {
        var raw = credentialManager.getScoreLogJs().trim()
        if (raw.isEmpty()) {
            showToast("設定からScoreLogのブックマークレットを登録してください")
            return
        }

        // javascript: プレフィックスを除去
        if (raw.startsWith("javascript:", ignoreCase = true)) {
            raw = raw.substring(11).trim()
        }

        // URLエンコードされている場合はデコード
        if (raw.contains("%")) {
            try {
                val decoded = java.net.URLDecoder.decode(raw, "UTF-8")
                // デコードに成功し、かつ意味のある変化があれば採用
                if (decoded != raw) raw = decoded
            } catch (e: Exception) {
                Log.e(TAG, "URL Decoding failed", e)
            }
        }

        try {
            val b64 = Base64.encodeToString(raw.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            val script = """
                (function() {
                    try {
                        var s = atob('$b64');
                        var bytes = new Uint8Array(s.length);
                        for (var i = 0; i < s.length; i++) {
                            bytes[i] = s.charCodeAt(i);
                        }
                        var code = new TextDecoder('utf-8').decode(bytes);
                        eval(code);
                    } catch (e) {
                        console.error('ScoreLog error:', e);
                        if (window.AndroidBridge) window.AndroidBridge.showToast('実行エラー: ' + e.message);
                    }
                })();
            """.trimIndent()
            binding.webView.evaluateJavascript(script, null)
        } catch (e: Exception) {
            Log.e(TAG, "ScoreLog preparation failed", e)
            showToast("実行準備に失敗しました")
        }
    }

    private fun executeOverPrint() {
        showToast("親密度データを解析中...")
        val js = loadJsFromAssets("intimacy_data.js")
        if (js.isNotEmpty()) {
            binding.webView.evaluateJavascript(js, null)
        }
    }

    private fun getUnpackWrapper(b64: String) = "(function() { var s = atob('$b64'); eval(new TextDecoder('utf-8').decode(new Uint8Array(s.length).map((_, i) => s.charCodeAt(i)))); })();"

    private fun loadJsFromAssets(n: String) = try { assets.open(n).bufferedReader().readText() } catch (_: Exception) { "" }

    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            NotificationHelper.scheduleDailyReminder(this)
        }
    }

    private fun applySavedColorMode() {
        val mode = when (credentialManager.getColorMode()) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO
            2 -> AppCompatDelegate.MODE_NIGHT_YES
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
        }
        AppCompatDelegate.setDefaultNightMode(mode)
    }

    private fun startSelectiveScreenshot() {
        captureWebView { bmp ->
            if (bmp == null) return@captureWebView
            val f = File(cacheDir, "temp.png")
            f.outputStream().use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            UCrop.of(android.net.Uri.fromFile(f), android.net.Uri.fromFile(File(cacheDir, "crop.png")))
                .withOptions(UCrop.Options().apply { setToolbarTitle("保存範囲を選択"); setFreeStyleCropEnabled(true); })
                .start(this)
        }
    }

    private fun captureWebView(cb: (Bitmap?) -> Unit) {
        val v = binding.webView
        if (v.width <= 0 || v.height <= 0) { cb(null); return }
        val b = createBitmap(v.width, v.height, Bitmap.Config.ARGB_8888)
        val loc = IntArray(2); v.getLocationInWindow(loc)
        PixelCopy.request(window, Rect(loc[0], loc[1], loc[1] + v.width, loc[1] + v.height), b, { if (it == PixelCopy.SUCCESS) cb(b) else cb(null) }, Handler(Looper.getMainLooper()))
    }

    override fun onActivityResult(req: Int, res: Int, d: Intent?) {
        super.onActivityResult(req, res, d)
        if (res == RESULT_OK && req == UCrop.REQUEST_CROP) {
            val uri = UCrop.getOutput(d!!) ?: return
            val b = BitmapFactory.decodeStream(contentResolver.openInputStream(uri)) ?: return
            saveBitmapToStorage(b)
            showToast("保存しました")
        }
    }

    private fun saveBitmapToStorage(b: Bitmap) = try {
        val v = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "ongeki_${System.currentTimeMillis()}.png")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "image/png")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
            }
        }
        val uri = contentResolver.insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, v)
        if (uri != null) contentResolver.openOutputStream(uri).use { b.compress(Bitmap.CompressFormat.PNG, 100, it!!) }
        true
    } catch (_: Exception) { false }

    fun handleGeneratedImage(url: String) {
        if (url.startsWith("blob:")) {
            val js = "(async function() { var b = await fetch('$url').then(r => r.blob()); var r = new FileReader(); r.readAsDataURL(b); r.onloadend = () => window.AndroidBridge.showPreviewImage(r.result); })();"
            binding.webView.evaluateJavascript(js, null)
        } else if (url.startsWith("data:image")) {
            showImagePreviewDialog(url)
        }
    }

    private var imagePreviewDialog: AlertDialog? = null
    private val previewImageBitmaps = mutableListOf<Bitmap>()
    private var imagePagerAdapter: ImagePagerAdapter? = null

    fun showImagePreviewDialog(data: String) {
        runOnUiThread {
            val b = Base64.decode(data.substringAfter(","), Base64.DEFAULT).let { BitmapFactory.decodeByteArray(it, 0, it.size) } ?: return@runOnUiThread
            if (imagePreviewDialog?.isShowing == true) {
                previewImageBitmaps.add(b)
                imagePagerAdapter?.notifyItemInserted(previewImageBitmaps.size - 1)
                return@runOnUiThread
            }
            previewImageBitmaps.clear()
            previewImageBitmaps.add(b)
            val vp = ViewPager2(this).apply {
                layoutParams = android.view.ViewGroup.LayoutParams(-1, (resources.displayMetrics.heightPixels * 0.6).toInt())
                adapter = ImagePagerAdapter(previewImageBitmaps).also { imagePagerAdapter = it }
            }
            imagePreviewDialog = AlertDialog.Builder(this)
                .setTitle("画像プレビュー")
                .setView(vp)
                .setPositiveButton("すべて保存") { _, _ ->
                    previewImageBitmaps.forEach { bmp -> saveBitmapToStorage(bmp) }
                    showToast("保存しました")
                }
                .setNegativeButton("閉じる", null)
                .show()
        }
    }
}
