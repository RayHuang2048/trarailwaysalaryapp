plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services") // Google Services Plugin for Firebase
    id("org.jetbrains.kotlin.plugin.compose") // 假設這是您專案所需，保留
    id("org.jetbrains.kotlin.kapt") // 支援 Kotlin 註解處理，特別是 Glide
    // 如果您有使用 Firebase App Distribution，請保留這行：
    // id("com.google.firebase.appdistribution")
}

android {
    namespace = "com.ray.trarailwaysalaryapp"
    compileSdk = 35 // 使用最新的編譯 SDK

    defaultConfig {
        applicationId = "com.ray.trarailwaysalaryapp"
        minSdk = 26
        targetSdk = 35 // <--- 修正：與 compileSdk 保持一致
        versionCode = 36
        versionName = "1.1.6"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true // 啟用 Jetpack Compose
        viewBinding = true // 如果您同時使用 View Binding，請保留這行
    }

    composeOptions {
        // 請注意：RC 版本可能不穩定，如果遇到問題，請考慮使用穩定版
        kotlinCompilerExtensionVersion = "2.2.0-RC2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21 // Java 編譯版本
        targetCompatibility = JavaVersion.VERSION_21 // Java 目標版本
    }

    // <--- 修正：將已棄用的 kotlinOptions 替換為推薦的 kotlin DSL ---
    kotlin {
        jvmToolchain(21) // 設定 JVM 目標為 1.8，與 JavaVersion.VERSION_1_8 匹配
    }
    // --- 修正結束 ---

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

dependencies {
    // AndroidX 核心和 Lifecycle 庫
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0") // Jetpack Compose Activity

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")

    // Android 傳統 UI 組件 (如果專案同時使用傳統 View 和 Compose)
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0") // 統一版本，移除重複導入
    implementation("androidx.constraintlayout:constraintlayout:2.1.4") // 移除 libs.androidx.constraintlayout，直接使用版本

    // Navigation 元件 (移除重複導入，統一版本)
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")

    // Android WebKit
    implementation("androidx.webkit:webkit:1.10.0")

    // Kotlin 協程
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")

    // In-app Billing
    implementation("com.android.billingclient:billing-ktx:6.1.0")

    // CardView
    implementation("androidx.cardview:cardview:1.0.0")

    // RecyclerView (如果使用傳統 View 的 RecyclerView)
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    // Apache POI (保留一份，如果需要)
    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")


    // --- Firebase 依賴項：使用單一 BOM 導入來管理版本 ---
    // Import the BoM for the Firebase platform (這是您檔案中最高的版本，因此保留它)
    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))

    // Declare the dependency for the Cloud Firestore library
    // 當使用 BoM 時，您不需要在 Firebase 庫的依賴項中指定版本
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-messaging")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx") // 建議使用 -ktx 版本，它提供了 Kotlin 擴展功能
    // --- Firebase 依賴項結束 ---

    // Glide (使用 kapt 進行註解處理，而不是 annotationProcessor)
    implementation("com.github.bumptech.glide:glide:4.16.0")
    kapt("com.github.bumptech.glide:compiler:4.16.0") // 確保這裡使用 kapt
    // 注意：如果您遇到有關 kapt 的問題，請檢查您的項目是否正確配置了 Kapt 插件。
    // 在本檔案開頭，您已經正確包含了 id("org.jetbrains.kotlin.kapt")
    // --- Glide 依賴項結束 ---


    // 測試依賴 (保留)
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")

    // 移除重複的 libs.androidx.activity
    // 移除 libs.androidx.constraintlayout (已在上面直接導入)
    // 移除 libs.androidx.navigation.fragment.ktx (已在上面直接導入)
    // 移除 libs.androidx.navigation.ui.ktx (已在上面直接導入)

    // 移除重複的 Firebase BOM 和 Firestore 導入
    // implementation(platform("com.google.firebase:firebase-bom:33.1.0"))
    // implementation("com.google.firebase:firebase-firestore")
    // implementation("com.google.firebase:firebase-firestore-ktx")
    // implementation platform('com.google.firebase:firebase-bom:32.x.x')
}
