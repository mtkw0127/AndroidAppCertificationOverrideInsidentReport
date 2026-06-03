import java.io.File
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "io.github.mtkw0127.app2"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "io.github.mtkw0127.app2"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        // App2 signs with a rotated key. The lineage (ancestor -> app2key) is embedded
        // by the signDebugWithLineage / signReleaseWithLineage tasks below.
        create("app2Sign") {
            storeFile = rootProject.file("keystore2.jks")
            storePassword = "app2storepass"
            keyAlias = "app2key"
            keyPassword = "app2storepass"

            // Enable V3 signing to carry the rotation proof (lineage) in the APK.
            enableV3Signing = true
        }
    }

    buildTypes {
        debug {
            signingConfig = signingConfigs.getByName("app2Sign")
        }
        release {
            signingConfig = signingConfigs.getByName("app2Sign")
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

listOf("debug", "release").forEach { variant ->
    val variantCapitalized = variant.replaceFirstChar { it.uppercase() }
    val apkFile = layout.buildDirectory.file("outputs/apk/$variant/app2-$variant.apk").get().asFile

    // Re-signs the APK with ancestor signer + app2key signer + lineage,
    // producing a V3-signed APK that carries the full rotation proof.
    tasks.register("sign${variantCapitalized}WithLineage") {
        println("Signing $variant APK with lineage... ")
        dependsOn("assemble$variantCapitalized")
        doLast {
            fun run(vararg args: String) {
                val code = ProcessBuilder(*args).inheritIO().start().waitFor()
                require(code == 0) { "${args[0]} exited with $code" }
            }

            run(
                "apksigner", "sign",
                "--ks", rootProject.file("keystore_ancestor.jks").absolutePath,
                "--ks-key-alias", "ancestorkey",
                "--ks-pass", "pass:ancestorpass",
                "--next-signer",
                "--ks", rootProject.file("keystore2.jks").absolutePath,
                "--ks-key-alias", "app2key",
                "--ks-pass", "pass:app2storepass",
                "--lineage", rootProject.file("lineage_app2.bin").absolutePath,
                apkFile.absolutePath
            )
            println("Lineage embedded: ${apkFile.absolutePath}")
        }
    }

    // Extend AGP's installDebug/installRelease: embed lineage first, then launch.
    afterEvaluate {
        tasks.named("install$variantCapitalized") {
            dependsOn("sign${variantCapitalized}WithLineage")
            doLast {
                ProcessBuilder("adb", "shell", "am", "start", "-n", "io.github.mtkw0127.app2/.MainActivity")
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
