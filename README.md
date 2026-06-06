# オンゲキ-maimaiツール 概要

> [!IMPORTANT]
> 本アプリは現在**ベータ版 (v0.5)** です。一部の機能が不安定な場合や、将来的に仕様が変更される可能性があります。

## 1. アプリ名
- **オンゲキ-maimaiツール** (パッケージ: `com.miahina.ongekimai`)

## 2. アプリの目的
オンゲキ-NET および maimaiでらっくす NET の閲覧を快適にし、JavaScript 注入によって公式サイト単体では提供されていない便利な機能を統合したハイブリッドアプリ。

## 3. 主要機能
- **マルチサイト・ブラウジング**:
    - オンゲキ-NET と maimaiでらっくす NET をワンタップで切り替え。
    - WebView によるブラウジング。
- **拡張機能 (JavaScript 注入)**:
    - **プレイ履歴集計**: 過去のプレイ履歴をバックグラウンドで取得・集計。
    - **ジュエル情報取得**: 所持ジュエル等の情報を同期。
    - **あならいざもどき2**: あならいざもどきを起動。
    - **ScoreLog 連携**: 外部サービス「OngekiScoreLog」へのスコア登録。
- **自動ログイン & 認証情報管理**:
    - 設定された SEGA ID とパスワードを用いて、ログイン画面で自動的にフォームを入力。
    - `Jetpack DataStore (Preferences)` と `Google Tink` を使用し、認証情報を安全に暗号化（AES256_GCM）して保存。
    - `EncryptedSharedPreferences` からの自動移行機能を搭載。
- **セキュリティ**:
    - 生体認証（指紋・顔認証）またはデバイス認証によるアプリ起動ロック。
- **UX/UI**:
    - ダークモード対応（OS設定連動および手動切り替え）。
    - 下に引っ張って更新 (SwipeRefreshLayout)。
    - 画面下部の ViewPager2 によるクイックアクションボタン。
    - **範囲選択スクリーンショット**: `uCrop` を使用し、画面内の必要な部分だけを切り取って `Download` フォルダに保存。
    - **OSSライセンス一覧**: アプリ内で使用しているライブラリのライセンス情報を一覧表示。
    - WebView 内でのコピペ制限やズーム制限の強制解除。
- **通知機能**:
    - 毎日指定した時刻にログインを促すリマインダー通知（美亜からのメッセージ）。
    - 端末再起動後も通知設定を自動的に維持（BootReceiver）。

## 4. 技術スタック
- **言語**: Kotlin
- **ビルドシステム**: Gradle Kotlin DSL / Version Catalog (`libs.versions.toml`)
- **ターゲットSDK**: Android 15 (API 37)
- **UI コンポーネント**:
    - `WebView` (JavaScriptInterface を使用した Android-JS 連携)
    - `ViewPager2` (ボタンページ、画像プレビュー)
    - `SwipeRefreshLayout`
    - `androidx.preference` (標準的な設定画面の実装)
    - `Material Design 3` & `Edge-to-Edge` (3ボタンナビゲーション等への最適化)
- **Android API**:
    - `BiometricPrompt` (生体認証)
    - `AlarmManager` (正確なアラーム設定による通知)
    - `NotificationManager` (通知チャンネル管理)
    - `Jetpack DataStore` & `Google Tink` (セキュリティ・永続化)

## 5. ファイル構成 (主要なもの)
- `MainActivity.kt`: アプリのメインロジック、WebView 制御、範囲選択撮影の開始。
- `SettingsActivity.kt`: `PreferenceFragmentCompat` を使用した設定画面。
- `CredentialManager.kt`: `DataStore` + `Tink` による安全な設定・認証情報管理。
- `NotificationHelper.kt`: 通知チャンネル管理およびリマインダー予約ロジック。
- `ReminderReceiver.kt` / `BootReceiver.kt`: 通知実行および再起動時の再予約処理。
- `assets/`: 注入用の JavaScript ファイル (`analyzer.js`, `get_jewels.js`, `on-mai_PlayTally.js` 等)。
- `res/xml/root_preferences.xml`: 設定画面の構造定義。
- `res/layout/`:
    - `activity_main.xml`: メイン画面。
    - `activity_settings.xml`: 設定画面（Toolbar 搭載）。
    - `item_button_page*.xml`: ViewPager 用のボタンレイアウト。
