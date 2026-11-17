plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

android {
    namespace = "com.jinuo.kiosk"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.jinuo.kiosk"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.animation:animation")
    debugImplementation("androidx.compose.ui:ui-tooling")
}

// Android Studio / IntelliJ Gradle sync asks every module for the Kotlin DSL
// script model via a task named "prepareKotlinBuildScriptModel". Gradle 8.9
// registers a no-op version of it on the root build.gradle.kts, but NOT on
// this module (Kotlin Gradle Plugin 2.0.21), so IDE sync aborts with
// "Task 'prepareKotlinBuildScriptModel' not found in project ':app'".
// Registering the same no-op here matches the root behaviour and changes
// nothing about the actual build.
if (tasks.findByName("prepareKotlinBuildScriptModel") == null) {
    tasks.register("prepareKotlinBuildScriptModel") {
        group = "ide"
        description = "No-op IDE compatibility task for the Kotlin DSL script model"
    }
}
