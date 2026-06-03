import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.mtkw0127.app1"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.mtkw0127.app1"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // App1 signs with the ancestor key — the common root shared with app2's lineage.
        create("app1Sign") {
            storeFile = rootProject.file("keystore_ancestor.jks")
            storePassword = "ancestorpass"
            keyAlias = "ancestorkey"
            keyPassword = "ancestorpass"

            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("app1Sign")
        }
        release {
            signingConfig = signingConfigs.getByName("app1Sign")
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

    buildFeatures {
        compose = true
    }
}

// Extend AGP's installDebug/installRelease to also launch the app after installation.
afterEvaluate {
    listOf("debug", "release").forEach { variant ->
        val variantCapitalized = variant.replaceFirstChar { it.uppercase() }
        tasks.named("install$variantCapitalized") {
            doLast {
                ProcessBuilder("adb", "shell", "am", "start", "-n", "io.github.mtkw0127.app1/.MainActivity")
                    .inheritIO().start().waitFor()
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}
