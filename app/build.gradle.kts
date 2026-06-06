plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.licenseScribe.screen)
}

android {
    namespace = "com.miahina.ongekimai"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.miahina.ongekimai"
        minSdk = 29
        @Suppress("ExpiredTargetSdkVersion")
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

licenseScribeScreen {
    generatedPackageName.set("com.miahina.ongekimai")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    implementation(libs.androidx.security.crypto) // アカウント暗号化
    implementation(libs.google.gson)
    implementation(libs.androidx.biometric) // 生体認証
    implementation(libs.androidx.swiperefreshlayout) // WebView更新

    // DataStore & Tink
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.google.tink.android)
    implementation(libs.kotlinx.coroutines.android)

    // 画像切り抜き用ライブラリ
    implementation(libs.ucrop)
    implementation(libs.androidx.exifinterface)

    // OSSライセンス表示
    implementation(libs.licenseScribe.runtime)

    // Preference
    implementation(libs.androidx.preference.ktx)
}
