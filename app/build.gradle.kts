plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.gms.google-services")
    // !!! 新增這行 !!!
    id("org.jetbrains.kotlin.plugin.compose")

}

android {
    namespace = "com.ray.trarailwaysalaryapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.ray.trarailwaysalaryapp"
        minSdk = 26
        targetSdk = 35
        versionCode = 18
        versionName = "1.0.17"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "2.2.0-RC2"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    kotlinOptions {
        jvmTarget = "21"
    }

    testOptions {
        unitTests.all {
            it.useJUnitPlatform()
        }
    }
}

// ❌ 移除錯誤或重複的 JDK 8 toolchain 與設定
// kotlin { jvmToolchain(8) } ← 移除這段
// java.toolchain {...} ← 也移除，讓它使用你目前本機安裝的 JDK 21

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.0")
    implementation("androidx.activity:activity-compose:1.9.0")

    // Jetpack Compose UI
    implementation("androidx.compose.ui:ui:1.6.7")
    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.ui:ui-tooling-preview:1.6.7")
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    debugImplementation("androidx.compose.ui:ui-tooling:1.6.7")

    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")


    implementation("org.apache.poi:poi:5.2.5")
    implementation("org.apache.poi:poi-ooxml:5.2.5")


    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.webkit:webkit:1.10.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    implementation("com.android.billingclient:billing-ktx:6.1.0")

    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.cardview:cardview:1.0.0")
    // For RecyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")

    implementation(platform("com.google.firebase:firebase-bom:33.15.0"))
    implementation("com.google.firebase:firebase-analytics")

    // Firebase Cloud Messaging (FCM)
    implementation("com.google.firebase:firebase-messaging")

// Firebase Authentication (如果需要根據使用者ID儲存令牌，則需要)
    implementation("com.google.firebase:firebase-auth-ktx")


    // Firebase Firestore 核心 SDK
    implementation("com.google.firebase:firebase-firestore-ktx")
    // Kotlin 協程 Firebase 擴展 (讓 Firestore 操作可使用 suspend/await 語法)
    implementation("com.google.firebase:firebase-firestore-ktx:24.11.1")

    // Kotlin 協程 (如果您專案還沒有的話)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
}
