import java.util.Properties

plugins {
    id("com.android.application")
    kotlin("android")
    id("com.google.devtools.ksp")
}

val signingProperties = Properties().apply {
    val local = rootProject.file("local.properties")
    if (local.exists()) {
        local.inputStream().use(::load)
    }
}

val releaseStore = signingProperties.getProperty("xingyue.release.storeFile")?.let { file(it) }
val hasReleaseSigning = releaseStore?.exists() == true &&
    signingProperties.getProperty("xingyue.release.storePassword").orEmpty().isNotBlank() &&
    signingProperties.getProperty("xingyue.release.keyAlias").orEmpty().isNotBlank() &&
    signingProperties.getProperty("xingyue.release.keyPassword").orEmpty().isNotBlank()

android {
    namespace = "com.xingyue.english"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.xingyue.english"
        minSdk = 26
        targetSdk = 35
        versionCode = 14
        versionName = rootProject.version.toString()
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        ndk {
            abiFilters += listOf("arm64-v8a", "armeabi-v7a")
        }
    }

    signingConfigs {
        create("releaseLocal") {
            if (hasReleaseSigning) {
                storeFile = releaseStore
                storePassword = signingProperties.getProperty("xingyue.release.storePassword")
                keyAlias = signingProperties.getProperty("xingyue.release.keyAlias")
                keyPassword = signingProperties.getProperty("xingyue.release.keyPassword")
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = if (hasReleaseSigning) {
                signingConfigs.getByName("releaseLocal")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    packaging {
        jniLibs {
            useLegacyPackaging = true
        }
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.5"
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "17"
}

dependencies {
    implementation(project(":core"))

    implementation(platform("androidx.compose:compose-bom:2024.04.01"))
    implementation("androidx.activity:activity-compose:1.9.0")
    implementation("androidx.compose.foundation:foundation")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    implementation("androidx.room:room-ktx:2.6.1")
    implementation("androidx.room:room-runtime:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("androidx.media3:media3-exoplayer:1.3.1")
    implementation("androidx.media3:media3-ui:1.3.1")
    implementation("com.mrljdx:ffmpeg-kit-full:6.1.4")
    implementation("io.github.junkfood02.youtubedl-android:library:0.18.1")
    implementation("io.github.junkfood02.youtubedl-android:ffmpeg:0.18.1")
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    testImplementation(kotlin("test"))
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
    testImplementation("org.json:json:20240303")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:core:1.5.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
}
