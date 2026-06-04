# OngekiApp プロジェクト概要

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
    - **あならいざもどき2**: あならいざもどきも起動。
    - **ScoreLog 連携**: 外部サービス「OngekiScoreLog」へのスコア登録。
- **自動ログイン & 認証情報管理**:
    - 設定された SEGA ID とパスワードを用いて、ログイン画面で自動的にフォームを入力。
    - `EncryptedSharedPreferences` を使用し、認証情報を安全に暗号化して保存。
- **セキュリティ**:
    - 生体認証（指紋・顔認証）またはデバイス認証によるアプリ起動ロック。
- **UX/UI**:
    - ダークモード対応。
    - 下に引っ張って更新 (SwipeRefreshLayout)。
    - 画面下部の ViewPager2 によるクイックアクションボタン。
    - WebView 内でのコピペ制限やズーム制限の強制解除。
- **通知機能**:
    - 毎日指定した時刻にログインを促すリマインダー通知（美亜からのメッセージ）。

## 4. 技術スタック
- **言語**: Kotlin
- **UI コンポーネント**:
    - `WebView` (JavaScriptInterface を使用した Android-JS 連携)
    - `ViewPager2` (ボタンページ、画像プレビュー)
    - `SwipeRefreshLayout`
    - `ViewBinding` (推奨事項として遵守)
- **Android API**:
    - `BiometricPrompt` (生体認証)
    - `AlarmManager` (正確なアラーム設定による通知)
    - `NotificationManager` (通知チャンネル管理)

## 5. ファイル構成 (主要なもの)
- `MainActivity.kt`: アプリのメインロジック、WebView 制御、通知・認証処理。
- `CredentialManager.kt`: 設定や認証情報の永続化管理。
- `assets/`: 注入用の JavaScript ファイル (`analyzer.js`, `get_jewels.js`, `on-mai_PlayTally.js` 等)。
- `res/layout/`:
    - `activity_main.xml`: メイン画面。
    - `dialog_settings.xml`: 設定ダイアログ。
    - `item_button_page*.xml`: ViewPager 用のボタンレイアウト。
