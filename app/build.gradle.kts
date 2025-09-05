plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'kotlin-kapt'
}

android {
    namespace 'com.manus.agent'
    compileSdk 34

    defaultConfig {
        applicationId "com.manus.agent"
        minSdk 24
        targetSdk 34
        versionCode 1
        versionName "1.0"
    }

    buildFeatures {
        compose true
    }

    composeOptions {
        kotlinCompilerExtensionVersion '1.5.2'
    }

    kotlinOptions {
        jvmTarget = '17'
    }

    packagingOptions {
        resources {
            excludes += ['/META-INF/{AL2.0,LGPL2.1}']
        }
    }
}

dependencies {
    implementation "org.jetbrains.kotlin:kotlin-stdlib:1.9.0"

    // Jetpack Compose
    implementation "androidx.activity:activity-compose:1.9.0"
    implementation "androidx.compose.ui:ui:1.5.2"
    implementation "androidx.compose.material3:material3:1.2.0"
    implementation "androidx.compose.ui:ui-tooling-preview:1.5.2"
    debugImplementation "androidx.compose.ui:ui-tooling:1.5.2"
    implementation "androidx.lifecycle:lifecycle-runtime-ktx:2.6.2"

    // ONNX Runtime
    implementation 'com.microsoft.onnxruntime:onnxruntime:1.15.1'

    // Optional: For coroutines if needed
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
