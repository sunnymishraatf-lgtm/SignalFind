plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.signalfind"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.signalfind"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            // Shrink + optimize for the Play release. Requires signingConfigs.release
            // (see below) and proguard-rules.pro. Smoke-test install + one full
            // SMS recovery round-trip on a real device after enabling.
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            // signingConfig = signingConfigs.getByName("release")
        }
    }

    // Release signing: create the keystore ONCE, back it up, and never commit it.
    // Provide values via environment variables or ~/.gradle/gradle.properties:
    //
    //     signingConfigs {
    //         create("release") {
    //             storeFile = file(System.getenv("SIGNALFIND_KEYSTORE") ?: "signalfind-release.keystore")
    //             storePassword = System.getenv("SIGNALFIND_STORE_PASSWORD")
    //             keyAlias = System.getenv("SIGNALFIND_KEY_ALIAS") ?: "signalfind"
    //             keyPassword = System.getenv("SIGNALFIND_KEY_PASSWORD")
    //         }
    //     }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.14"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.activity:activity-compose:1.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.navigation:navigation-compose:2.7.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.3")

    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    implementation("com.google.android.gms:play-services-location:21.3.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    debugImplementation("androidx.compose.ui:ui-tooling")
}
