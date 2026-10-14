plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val stickerAuthority = "com.lumi.stickerize.stickercontentprovider"

android {
    namespace = "com.lumi.stickerize"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.lumi.stickerize"
        minSdk = 26
        targetSdk = 35
        versionCode = 4
        versionName = "1.3.0"

        // Hanya prosesor HP Android (ARM). Emulator x86 tidak disertakan agar APK lebih kecil.
        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }

        manifestPlaceholders["contentProviderAuthority"] = stickerAuthority
        buildConfigField("String", "CONTENT_PROVIDER_AUTHORITY", "\"$stickerAuthority\"")
    }

    signingConfigs {
        create("lumi") {
            // Kalau kamu punya keystore sendiri, isi env LUMI_KEYSTORE_PATH dkk.
            // Kalau tidak, dipakai keystore debug bawaan repo (cukup untuk dipasang manual).
            val ksPath = System.getenv("LUMI_KEYSTORE_PATH")
            if (!ksPath.isNullOrBlank() && file(ksPath).exists()) {
                storeFile = file(ksPath)
                storePassword = System.getenv("LUMI_KEYSTORE_PASSWORD")
                keyAlias = System.getenv("LUMI_KEY_ALIAS")
                keyPassword = System.getenv("LUMI_KEY_PASSWORD")
            } else {
                storeFile = file("lumi-debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("lumi")
        }
        debug {
            signingConfig = signingConfigs.getByName("lumi")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.exifinterface:exifinterface:1.3.7")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")

    // Deteksi wajah di HP (model ikut di dalam APK, tidak butuh internet)
    implementation("com.google.mlkit:face-detection:16.1.7")
}
