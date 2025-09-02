// *** تم تحديث هذا الملف بالكامل لضمان التوافق التام ***
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    // *** هذا هو الجزء الذي سيحل المشكلة ***
    // نحن نجبر المشروع على استخدام إصدارات محددة من الإضافات
    plugins {
        id("com.android.application") version "8.4.1" apply false
        id("org.jetbrains.kotlin.android") version "1.9.22" apply false
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ManusAgent"
include(":app")
