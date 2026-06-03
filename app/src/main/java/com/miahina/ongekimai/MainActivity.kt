package com.miahina.ongekimai
import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Message
import android.util.Base64
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.widget.SwitchCompat
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.viewpager2.widget.ViewPager2
import java.io.File
import java.io.FileOutputStream
import java.util.Calendar
import java.util.concurrent.Executor
import androidx.core.net.toUri
import java.util.Locale


class MainActivity : AppCompatActivity() {

    private lateinit var webView: WebView
    private lateinit var credentialManager: CredentialManager
    private lateinit var rootLayout: View
    private lateinit var viewPager: ViewPager2

    // === MainActivity クラスの変数宣言部分に追加 ===
    private val requestNotificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        if (isGranted) {
            showToast("通知が許可されました。リマインダーをセットします。")
            scheduleDailyReminder() // 許可されたらアラームをセット
        } else {
            showToast("通知が拒否されたため、リマインダーが届かない可能性があります。")
        }
    }

    private var isAuthCleared = false
    private var isAuthPromptShowing = false

    // 💡 複数画像のプレビューを1つのダイアログで管理するための変数
    private var imagePreviewDialog: AlertDialog? = null
    private val previewImageBitmaps = mutableListOf<Bitmap>()
    private var imagePagerAdapter: ImagePagerAdapter? = null

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        credentialManager = CredentialManager(this)
        applySavedColorMode()

        setContentView(R.layout.activity_main)
        rootLayout = findViewById(android.R.id.content)

        // アプリ起動時に通知チャンネルを作成
        createNotificationChannel()

        // 毎日のリマインダーをセット
        checkAndRequestNotificationPermission()

        if (credentialManager.isBiometricEnabled()) {
            rootLayout.visibility = View.INVISIBLE
        } else {
            isAuthCleared = true
        }

        webView = findViewById(R.id.webView)
        val btnSwitchOngeki: Button = findViewById(R.id.btnSwitchOngeki)
        val btnSwitchMaimai: Button = findViewById(R.id.btnSwitchMaimai)
        val btnSettings: Button = findViewById(R.id.btnSettings)

        viewPager = findViewById(R.id.buttonViewPager)
        val adapter = ButtonPagerAdapter(
            onTallyClick = { executeTally() },
            onGetJewelsClick = { executeGetJewels() },
            onAnalyzerClick = { executeAnalyzer() },
            onScoreLogClick = { executeScoreLog() },
            onNotificationTestClick = { sendTestNotification() }
        )
        viewPager.adapter = adapter

        val cookieManager = CookieManager.getInstance()
        cookieManager.setAcceptCookie(true)
        cookieManager.setAcceptThirdPartyCookies(webView, true)
        cookieManager.flush()

        webView.settings.apply {
            // 1. ズーム機能を有効にする
            setSupportZoom(true)          // ズーム操作自体をサポートする
            builtInZoomControls = true    // ピンチイン・アウトによるズームを有効にする
            displayZoomControls = false // 画面右下に出るズーム用の「＋/ー」ボタンを非表示にする（今風の挙動に）
            javaScriptEnabled = true // JavaScriptを有効にする
            domStorageEnabled = true
            useWideViewPort = true
            loadWithOverviewMode = true
            // 💡 複数ウィンドウ（別タブ形式のリンク）の展開を許可する
            setSupportMultipleWindows(true)
            javaScriptCanOpenWindowsAutomatically = true
        }

        // 3. コピペ（テキスト選択）を確実に動作させるための設定
        webView.isLongClickable = true    // 長押しを有効にする

        webView.webViewClient = myWebViewClient

        // 💡 別タブで画像が開かれる挙動をキャッチする WebChromeClient を設定
        webView.webChromeClient = myWebChromeClient

        webView.addJavascriptInterface(WebAppInterface(this), "AndroidBridge")

        webView.loadUrl("https://ongeki-net.com/ongeki-mobile/")

        btnSwitchOngeki.setOnClickListener { webView.loadUrl("https://ongeki-net.com/ongeki-mobile/") }
        btnSwitchMaimai.setOnClickListener { webView.loadUrl("https://maimaidx.jp/maimai-mobile/") }
        btnSettings.setOnClickListener { showCombinedSettingsDialog() }
    }

    // 💡 シンプルで安全なWebChromeClient（画像奪取はJavaScriptフックで行うため軽量化）
    private val myWebChromeClient = object : WebChromeClient() {
        override fun onCreateWindow(
            view: WebView?,
            isDialog: Boolean,
            isUserGesture: Boolean,
            resultMsg: Message?
        ): Boolean {
            // 解析ツールが勝手に別タブを開こうとしたら、空のWebViewを渡してエラーを防ぐ
            val dummyWebView = WebView(this@MainActivity)
            val transport = resultMsg?.obj as? WebView.WebViewTransport
            transport?.webView = dummyWebView
            resultMsg?.sendToTarget()
            return true
        }
    }


    // --- 生成された画像のプレビュー表示 ＆ 保存処理 ---
    fun handleGeneratedImage(url: String) {
        // Blob URL や特殊な生成方式の場合、フロント側（WebView内）からBase64に変換して引っ張るスクリプトを走らせる
        if (url.startsWith("blob:")) {
            val getBlobJs = """
                (async function() {
                    try {
                        let blob = await fetch('$url').then(r => r.blob());
                        let reader = new FileReader();
                        reader.readAsDataURL(blob);
                        reader.onloadend = function() {
                            // Android側のインターフェースにBase64データを直接投げ返す
                            if(window.AndroidBridge && window.AndroidBridge.showPreviewImage) {
                                window.AndroidBridge.showPreviewImage(reader.result);
                            }
                        }
                    } catch(e) { console.error(e); }
                })();
            """.trimIndent()
            webView.evaluateJavascript(getBlobJs, null)
            return
        }

        // すでに data:image/... (Base64) の形式であればそのままプレビューへ
        if (url.startsWith("data:image")) {
            showImagePreviewDialog(url)
        }
    }

    // 💡 プレビューダイアログの表示と保存ボタン（複数画像スライド対応版）
    fun showImagePreviewDialog(base64Data: String) {
        runOnUiThread {
            try {
                // "data:image/png;base64," のヘッダーを削る
                val pureBase64 = base64Data.substringAfter(",")
                val imageBytes = Base64.decode(pureBase64, Base64.DEFAULT)
                val bitmap = BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.size)
                    ?: return@runOnUiThread

                // すでにダイアログが表示されている場合は、2枚目以降としてスライドに追加する
                if (imagePreviewDialog?.isShowing == true) {
                    previewImageBitmaps.add(bitmap)
                    imagePagerAdapter?.notifyItemInserted(previewImageBitmaps.size - 1)
                    imagePreviewDialog?.setTitle("Rating解析結果画像 (${previewImageBitmaps.size}枚)")
                    return@runOnUiThread
                }

                // 1枚目の場合は新しくリストを作って初期化
                previewImageBitmaps.clear()
                previewImageBitmaps.add(bitmap)

                val context = this
                // 画面の高さの60%を画像表示エリアの高さにする（高さが0になるのを防ぐ対策）
                val displayHeight = resources.displayMetrics.heightPixels
                val viewPagerHeight = (displayHeight * 0.6).toInt()

                val viewPager = ViewPager2(context).apply {
                    layoutParams = android.view.ViewGroup.LayoutParams(
                        android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                        viewPagerHeight
                    )
                }

                // 左右スライド用のアダプターをセット
                imagePagerAdapter = ImagePagerAdapter(previewImageBitmaps)
                viewPager.adapter = imagePagerAdapter

                // ダイアログを作成
                imagePreviewDialog = AlertDialog.Builder(context)
                    .setTitle("Rating解析結果画像 (1枚)")
                    .setView(viewPager)
                    .setPositiveButton("すべての画像を保存") { _, _ ->
                        var successCount = 0
                        previewImageBitmaps.forEachIndexed { index, bmp ->
                            if (saveBitmapToStorage(bmp, index)) successCount++
                        }
                        if (successCount > 0) {
                            showToast("${successCount}枚の画像をダウンロードフォルダに保存しました")
                        }
                    }
                    .setNegativeButton("閉じる") { _, _ ->
                        imagePreviewDialog = null
                    }
                    .setOnCancelListener {
                        imagePreviewDialog = null
                    }
                    .create()

                imagePreviewDialog?.show()
            } catch (_: Exception) {
                showToast("画像のプレビュー生成に失敗しました")
            }
        }
    }


    // 💡 ビットマップ画像をストレージ（Downloadフォルダ）に保存する（名前衝突回避版）
    private fun saveBitmapToStorage(bitmap: Bitmap, index: Int = 0): Boolean {
        return try {
            // 同時保存でもファイル名が被らないようにインデックスを付与
            val suffix = if (index > 0) "_$index" else ""
            val filename = "maimai_rating_${System.currentTimeMillis()}$suffix.png"
            val downloadDir =
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            val file = File(downloadDir, filename)

            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            true
        } catch (e: Exception) {
            Log.e("MAL_ANALYZER", "画像の保存に失敗しました", e)
            false
        }
    }

    // --- 以下、既存の認証や各実行コード（前回の状態を維持） ---
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && !isAuthCleared && !isAuthPromptShowing && credentialManager.isBiometricEnabled()) {
            checkAndShowBiometricPrompt()
        }
    }

    private fun checkAndShowBiometricPrompt() {
        val biometricManager = BiometricManager.from(this)
        val authenticators =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        if (biometricManager.canAuthenticate(authenticators) == BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED) {
            isAuthCleared = true
            rootLayout.visibility = View.VISIBLE
            return
        }
        showBiometricPrompt()
    }

    private fun showBiometricPrompt() {
        isAuthPromptShowing = true
        val executor: Executor = ContextCompat.getMainExecutor(this)
        val biometricPrompt = BiometricPrompt(
            this, executor,
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    super.onAuthenticationSucceeded(result)
                    isAuthCleared = true
                    isAuthPromptShowing = false
                    rootLayout.visibility = View.VISIBLE
                    showToast("認証に成功しました")
                }

                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    super.onAuthenticationError(errorCode, errString)
                    isAuthPromptShowing = false
                    finish()
                }
            })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("アプリのロック解除")
            .setSubtitle("登録されている生体認証、または暗証番号で解除してください")
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()

        biometricPrompt.authenticate(promptInfo)
    }

    private fun executeTally() {
        val currentUrl = webView.url ?: ""
        if (!currentUrl.contains("ongeki-net.com") && !currentUrl.contains("maimaidx.jp")) {
            showToast("対象のサイトを開いてください")
            return
        }
        showToast("バックグラウンドでプレイ履歴を集計中...")

        // ファイルから読み込む
        val rawJs = loadJsFromAssets("on-mai_PlayTally.js")
        if (rawJs.isNotEmpty()) {
            val base64Js = Base64.encodeToString(rawJs.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            webView.evaluateJavascript(getUnpackWrapper(base64Js), null)
        }
    }

    private fun executeGetJewels() {
        val currentUrl = webView.url ?: ""
        if (!currentUrl.contains("ongeki-net.com")) {
            AlertDialog.Builder(this).setMessage("この機能はオンゲキ-NETでのみ使用できます。")
                .setPositiveButton("OK", null).show()
            return
        }
        showToast("各種情報をバックグラウンドで同期中...")

        // ファイルから読み込む
        val rawJs = loadJsFromAssets("get_jewels.js")
        if (rawJs.isNotEmpty()) {
            val base64Js = Base64.encodeToString(rawJs.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
            webView.evaluateJavascript(getUnpackWrapper(base64Js), null)
        }
    }

    private fun executeAnalyzer() {
        val currentUrl = webView.url ?: ""
        val isMaimai = currentUrl.contains("maimaidx.jp")
        val isOngeki = currentUrl.contains("ongeki-net.com")

        if (isMaimai || isOngeki) {
            // ファイルから読み込む
            val scriptJs = loadJsFromAssets("analyzer.js")
            if (scriptJs.isNotEmpty()) {
                webView.evaluateJavascript(scriptJs, null)
                showToast("あならいざもどきを実行しました")
            }
        } else {
            AlertDialog.Builder(this)
                .setMessage("対象のサイト（maimai NET または オンゲキ-NET）を開いてください。")
                .setPositiveButton("OK", null)
                .show()
        }
    }

    private fun executeScoreLog() {
        val currentUrl = webView.url ?: ""
        if (!currentUrl.contains("ongeki-net.com")) {
            AlertDialog.Builder(this).setMessage("この機能はオンゲキ-NETでのみ使用できます。")
                .setPositiveButton("OK", null).show()
            return
        }
        val rawJs = credentialManager.getScoreLogJs()
        if (rawJs.isEmpty()) {
            AlertDialog.Builder(this).setTitle("コード未登録")
                .setMessage("設定画面からOngekiScoreLogのコードを登録してください。")
                .setPositiveButton("OK", null).show()
            return
        }
        var cleanJs = if (rawJs.startsWith(
                "javascript:",
                ignoreCase = true
            )
        ) rawJs.substring("javascript:".length) else rawJs
        try {
            cleanJs = java.net.URLDecoder.decode(cleanJs, "UTF-8")
        } catch (_: Exception) {
        }
        val base64Js = Base64.encodeToString(cleanJs.toByteArray(Charsets.UTF_8), Base64.NO_WRAP)
        webView.evaluateJavascript(getUnpackWrapper(base64Js), null)
        showToast("ScoreLog登録を要求しました")
    }

    private fun getUnpackWrapper(base64Str: String): String {
        return """
            (function() {
                var binStr = atob('$base64Str');
                var bytes = new Uint8Array(binStr.length);
                for (var i = 0; i < binStr.length; i++) { bytes[i] = binStr.charCodeAt(i); }
                eval(new TextDecoder('utf-8').decode(bytes));
            })();
        """.trimIndent()
    }

    // ======= MainActivity 内の showCombinedSettingsDialog() を以下に差し替え =======
    private fun showCombinedSettingsDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null)
        val etSegaId = dialogView.findViewById<EditText>(R.id.etSegaId)
        val etSegaPassword = dialogView.findViewById<EditText>(R.id.etSegaPassword)
        val etScoreLogJs = dialogView.findViewById<EditText>(R.id.etScoreLogJs)
        val rgColorMode = dialogView.findViewById<RadioGroup>(R.id.rgColorMode)
        val switchBiometric = dialogView.findViewById<SwitchCompat>(R.id.switchBiometric)

        // 💡 今回追加したリマインダー用UIの紐付け
        val switchReminder = dialogView.findViewById<SwitchCompat>(R.id.switchReminder)
        val layoutReminderTime = dialogView.findViewById<android.widget.LinearLayout>(R.id.layoutReminderTime)
        val tvReminderTime = dialogView.findViewById<TextView>(R.id.tvReminderTime)

        // 既存データの読み込み
        etSegaId.setText(credentialManager.getId())
        etSegaPassword.setText(credentialManager.getPassword())
        etScoreLogJs.setText(credentialManager.getScoreLogJs())
        switchBiometric.isChecked = credentialManager.isBiometricEnabled()

        // 💡 リマインダーの初期状態を設定
        switchReminder.isChecked = credentialManager.isReminderEnabled()
        var selectedHour = credentialManager.getReminderHour()
        var selectedMinute = credentialManager.getReminderMinute()
        tvReminderTime.text = String.format(Locale.getDefault(), "%02d:%02d", selectedHour, selectedMinute)

        // 💡 スイッチのON/OFFで時間設定エリアの表示・非表示を切り替え ＆ 権限チェック
        layoutReminderTime.visibility = if (switchReminder.isChecked) View.VISIBLE else View.GONE

        switchReminder.setOnCheckedChangeListener { _, isChecked ->
            layoutReminderTime.visibility = if (isChecked) View.VISIBLE else View.GONE

            // 💡 ONにされた時、Android 12以上であれば「正確なアラーム権限」があるかチェックする
            if (isChecked && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager

                // まだ権限が許可されていない場合、システムの設定画面へ誘導する
                if (!alarmManager.canScheduleExactAlarms()) {
                    // ユーザーに理由を伝えるトーストやダイアログを挟むとより親切です
                    showToast("正確な時間に通知を表示するため、次の画面でこのアプリを許可してください。")

                    try {
                        val intent = Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                            data = "package:$packageName".toUri()
                        }
                        startActivity(intent)
                    } catch (e: Exception) {
                        Log.e("Notification", "設定画面への遷移に失敗しました", e)
                    }
                }
            }
        }

        // 時間のテキストをタップしたらタイムピッカーを表示する
        tvReminderTime.setOnClickListener {
            android.app.TimePickerDialog(this, { _, hourOfDay, minute ->
                selectedHour = hourOfDay
                selectedMinute = minute
                tvReminderTime.text = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute)
            }, selectedHour, selectedMinute, true).show()
        }

        when (credentialManager.getColorMode()) {
            1 -> dialogView.findViewById<RadioButton>(R.id.rbThemeLight).isChecked = true
            2 -> dialogView.findViewById<RadioButton>(R.id.rbThemeDark).isChecked = true
            else -> dialogView.findViewById<RadioButton>(R.id.rbThemeSystem).isChecked = true
        }

        AlertDialog.Builder(this)
            .setTitle("⚙️ アプリの設定")
            .setView(dialogView)
            .setPositiveButton("保存") { _, _ ->
                // 各種保存処理
                credentialManager.saveCredentials(etSegaId.text.toString(), etSegaPassword.text.toString())
                credentialManager.saveScoreLogJs(etScoreLogJs.text.toString())
                val selectedMode = when (rgColorMode.checkedRadioButtonId) {
                    R.id.rbThemeLight -> 1
                    R.id.rbThemeDark -> 2
                    else -> 0
                }
                credentialManager.saveColorMode(selectedMode)
                applySavedColorMode()
                credentialManager.saveBiometricEnabled(switchBiometric.isChecked)
                if (!switchBiometric.isChecked) isAuthCleared = true

                // 💡 リマインダー設定の保存
                credentialManager.saveReminderEnabled(switchReminder.isChecked)
                credentialManager.saveReminderTime(selectedHour, selectedMinute)

                // 💡 スケジュールを新しい設定で更新（オフならここで自動解除されます）
                scheduleDailyReminder()

                webView.reload()
                showToast("設定を保存しました")
            }
            .setNegativeButton("キャンセル", null)
            .show()
    }

    private fun applySavedColorMode() {
        when (credentialManager.getColorMode()) {
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }
    }

    // ====== このブロックを丸ごと差し替えます ======
    private val myWebViewClient = object : WebViewClient() {
        override fun onPageFinished(view: WebView?, url: String?) {
            super.onPageFinished(view, url)
            CookieManager.getInstance().flush()

            if (view != null) {
                // 💡 Webサイト側のコピペ禁止（CSS/JS）と、ズーム禁止（メタタグ）を強制解除するJS
                val forceEnableFeaturesJs = """
                    (function() {
                        // --- 1. CSSによるテキスト選択禁止（user-select: none）をすべて上書き解除 ---
                        var style = document.createElement('style');
                        style.type = 'text/css';
                        style.innerHTML = '* { user-select: text !important; -webkit-user-select: text !important; -ms-user-select: text !important; -moz-user-select: text !important; }';
                        document.head.appendChild(style);

                        // --- 2. JavaScriptによる長押しメニュー・コピー禁止イベントを無効化 ---
                        var allowEvents = function(e) {
                            e.stopImmediatePropagation();
                            return true;
                        };
                        document.addEventListener('contextmenu', allowEvents, true);
                        document.addEventListener('copy', allowEvents, true);
                        document.addEventListener('selectstart', allowEvents, true);
                        document.addEventListener('mousedown', allowEvents, true);

                        // --- 3. メタタグによるズーム禁止設定（user-scalable=no）を強制的に解除 ---
                        var meta = document.querySelector('meta[name="viewport"]');
                        if (meta) {
                            var content = meta.getAttribute('content');
                            // user-scalable=no を yes に、maximum-scale を大きな値に書き換える
                            content = content.replace(/user-scalable\s*=\s*no/g, 'user-scalable=yes');
                            if (!content.includes('user-scalable')) {
                                content += ', user-scalable=yes';
                            }
                            if (content.includes('maximum-scale')) {
                                content = content.replace(/maximum-scale\s*=\s*[^,]+/g, 'maximum-scale=5.0');
                            } else {
                                content += ', maximum-scale=5.0';
                            }
                            meta.setAttribute('content', content);
                        }
                    })();
                """.trimIndent()

                // JavaScriptを実行して適用
                view.evaluateJavascript(forceEnableFeaturesJs, null)
            }

            if (url != null) handleAutoLogin(url)
        }
    }
    // =============================================

    private fun handleAutoLogin(url: String) {
        val isOngekiLogin =
            url.startsWith("https://ongeki-net.com/ongeki-mobile/") && !url.contains("record")
        val isMaimaiLogin =
            url.startsWith("https://maimaidx.jp/maimai-mobile/") && !url.contains("record")
        val isSegaCommonLogin = url.contains("sega-id") || url.contains("login")

        if (isOngekiLogin || isMaimaiLogin || isSegaCommonLogin) {
            val savedId = credentialManager.getId()
            val savedPass = credentialManager.getPassword()
            if (savedId.isNotEmpty() && savedPass.isNotEmpty()) {
                val loginJs = """
                    (function() {
                        var idInput = document.getElementById('sid') || document.querySelector('input[name="segaId"]') || document.querySelector('input[name="sid"]') || document.querySelector('input[type="text"]');
                        var passInput = document.getElementById('password') || document.querySelector('input[name="password"]') || document.querySelector('input[type="password"]');
                        if (idInput && passInput) {
                            idInput.value = '$savedId'; passInput.value = '$savedPass';
                            idInput.dispatchEvent(new Event('input', { bubbles: true })); idInput.dispatchEvent(new Event('change', { bubbles: true }));
                            passInput.dispatchEvent(new Event('input', { bubbles: true })); passInput.dispatchEvent(new Event('change', { bubbles: true }));
                        }
                    })();
                """.trimIndent()
                webView.evaluateJavascript(loginJs, null)
            }
        }
    }

    private fun showToast(msg: String) {
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()
    }

    // ======= 通知チャンネルの作成関数 =======
    private fun createNotificationChannel() {
        val notificationManager =
            getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // ======= 🗑️ 登録済みの全チャンネルを自動で取得して一括削除 =======
        try {
            // 現在アプリに登録されているすべての通知チャンネルを取得
            val existingChannels = notificationManager.notificationChannels
            for (channel in existingChannels) {
                // チャンネルを1つずつ削除
                notificationManager.deleteNotificationChannel(channel.id)
            }
            Log.d("Notification", "すべての古いチャンネルを一括削除しました")
        } catch (e: Exception) {
            Log.e("Notification", "チャンネルの削除中にエラーが発生しました", e)
        }

        // --- 1つ目のチャンネル（既存：お知らせ用） ---
        val channel1 = NotificationChannel(
            "mia_login_channel", // ID
            "美亜からのメッセージ", // 設定画面でユーザーに見える名前
            NotificationManager.IMPORTANCE_HIGH //  割り込み表示（ポップアップ）させたい場合はHIGH
        ).apply {
            description = "アプリからの通知やアップデート情報をお知らせします"
        }
        notificationManager.createNotificationChannel(channel1)

        // 月替わりリマインダー用のチャンネル
        val monthChannel = NotificationChannel(
            "mia_monthlogin_channel",
            "美亜からの月替わり通知",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "月ごとにメッセージが変化する毎日のリマインダー通知です"
        }
        notificationManager.createNotificationChannel(monthChannel)
    }

    // === 毎日夜20時にスケジュールする ===

    // ======= MainActivity 内の関数をこのように変更してください =======
    private fun scheduleDailyReminder() {
        val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
        val intent = Intent(this, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        // スイッチがオフなら解除
        if (!credentialManager.isReminderEnabled()) {
            alarmManager.cancel(pendingIntent)
            Log.d("Notification", "リマインダーがオフに設定されたため、アラームを解除しました")
            return
        }

        val hour = credentialManager.getReminderHour()
        val minute = credentialManager.getReminderMinute()

        val calendar = Calendar.getInstance().apply {
            timeInMillis = System.currentTimeMillis()
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
        }

        // 💡 念のため、現在時刻から「10秒以上」未来であることを保証する（過去の時間なら明日に回す）
        if (calendar.timeInMillis <= System.currentTimeMillis() + 10000) {
            calendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        // 💡 Android 12以降かつ正確なアラーム権限がない場合は、大まかな時間で登録（クラッシュ回避）
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            // 権限がない時はこちら（多少ズレますが動きます）
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("Notification", "正確なアラーム権限がないため、不正確なアラームで登録しました")
        } else {
            // ✨ 1分もズレずにスリープ中も強制発動させる設定に変更
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("Notification", "正確なリマインダーをスケジュールしました: " + String.format(Locale.getDefault(), "%02d:%02d", hour, minute))
        }
    }

    // アプリ起動時のパーミッションチェック時にも、オンの時だけスケジュールを走らせるように調整
    private fun checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED -> {
                    scheduleDailyReminder() // 権限が許可されている場合は、アラームを scheduleDailyReminder() が呼ばれます
                }
                else -> {
                    requestNotificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        } else {
            scheduleDailyReminder()
        }
    }

// ======= テスト通知 クラスの一番下あたりに追加 =======

    // 💡 必要なインポートが不足している場合は、関数の上かファイル上部に追加してください
    // import androidx.core.app.Person
    // import androidx.core.graphics.drawable.IconCompat

    private fun sendTestNotification() {
        val channelId = "mia_login_channel"
        val notificationId = 888

        // 1. 通知タップ時にアプリを開くインテントの準備
        val mainIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent, PendingIntent.FLAG_IMMUTABLE
        )

        // 2. メッセージをランダムで抽選
        val messages = listOf(
            "にゃっふにゃっふ！今日はもうオンゲキ遊んだ？忘れずに遊んでね！",
            "にゃっふ！オンゲキもいいけどmaimaiもいいよね！",
            "にゃっふ！？ログインボーナスは大丈夫？"
        )
        val randomMessage = messages.random()

        // 💡 3. 右側に表示する画像をBitmapとして読み込む
        // 現在はテストとしてアプリアイコン（ic_launcher）を設定しています。
        // ここに表示したい美亜の四角いイラスト画像（mia_avatar.pngなど）を res/drawable/ に追加した場合、
        // R.mipmap.ic_launcher の部分を R.drawable.mia_avatar に書き換えることで、完全に再現できます！
        val rightImageBitmap = BitmapFactory.decodeResource(resources, R.drawable.miadda)

        // 4. 通知全体の組み立て（シンプルなBigTextStyleに戻します）
        val builder = NotificationCompat.Builder(this, channelId)
            // 💡【重要】左側の変な青い丸を消す対策
            // Androidの仕様に則り、ステータスバー用の小さな白抜きアイコンを指定します。
            // ここに「android.R.drawable.ic_dialog_info」などのシステム標準アイコンを指定するか、
            // もし通知用の白抜きアイコン（通知バーに表示される小さなシルエット）を自作して
            // res/drawable/ic_notification.xml 等に置いている場合は、それを指定してください。
            .setSmallIcon(R.drawable.ic_stat_name)

            // 💡 タイトルを「美亜」、本文をランダムメッセージに設定
            .setContentTitle("美亜")
            .setContentText(randomMessage)

            // ✨ 右側に大きく四角い画像を表示するための設定
            .setLargeIcon(rightImageBitmap)

            // 長文対応のスタイル
            .setStyle(NotificationCompat.BigTextStyle().bigText(randomMessage))

            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            with(NotificationManagerCompat.from(this)) {
                // Android 13以上用の通知権限チェック
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    ContextCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.POST_NOTIFICATIONS
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    showToast("通知権限が許可されていません")
                    return
                }

                // 通知を発行
                notify(notificationId, builder.build())
            }
        } catch (e: Exception) {
            Log.e("Notification", "通知の送信に失敗しました", e)
            showToast("通知の送信に失敗しました: ${e.message}")
        }
    }
    // assetsフォルダからJSファイルを文字列として読み込む共通関数
    private fun loadJsFromAssets(fileName: String): String {
        return try {
            assets.open(fileName).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            Log.e("WebViewJS", "JSファイルの読み込みに失敗しました: $fileName", e)
            ""
        }
    }
}