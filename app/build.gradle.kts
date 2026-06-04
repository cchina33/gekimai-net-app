plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.miahina.ongekimai"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.miahina.ongekimai"
        minSdk = 29
        targetSdk = 36
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

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    implementation("androidx.security:security-crypto:1.1.0-alpha06") //アカウント暗号化
    implementation("com.google.code.gson:gson:2.10.1")
    // 生体認証用のライブラリを追加
    implementation("androidx.biometric:biometric:1.2.0-alpha05")
    // WebViewを引っ張って更新
    implementation("androidx.swiperefreshlayout:swiperefreshlayout:1.1.0")
}