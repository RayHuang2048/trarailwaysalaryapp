// build.gradle.kts (Root Project)

// 聲明 Gradle 插件及其版本
plugins {
    // Android 應用程式插件
    id("com.android.application") version "8.10.1" apply false
    // Android 函式庫插件 (如果您有函式庫模組，也可能需要)
    id("com.android.library") version "8.10.1" apply false
    // Kotlin Android 插件
    id("org.jetbrains.kotlin.android") version "1.9.23" apply false
    // Dagger Hilt 插件
    id("com.google.dagger.hilt.android") version "2.51.1" apply false
    // Kotlin Symbol Processing (KSP) 插件 (與 Hilt 搭配使用)
    id("com.google.devtools.ksp") version "1.9.23-1.0.19" apply false
    // Google Services 插件 (用於 Firebase 等)
    id("com.google.gms.google-services") version "4.4.1" apply false
    // 其他您可能需要的插件...
}



// 根專案的任務或其他全局配置 (通常比較少)
// 移除了 allprojects { repositories { ... } } 區塊，因為倉庫已在 settings.gradle.kts 中統一管理
tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
