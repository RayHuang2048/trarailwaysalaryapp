// settings.gradle.kts
pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 如果您有任何來自第三方或私有 Maven 倉庫的依賴，也需要在此處添加
        // 例如：maven("https://jitpack.io")
    }
}

rootProject.name = "TraRailwaySalaryApp"
include(":app") // 確保您的應用程式模組被包含
