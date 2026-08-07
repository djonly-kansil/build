plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.devtools.ksp)
    alias(libs.plugins.roborazzi)
    alias(libs.plugins.secrets)
    // alias(libs.plugins.google.services) // Firebase/Google Services unused
}

android {
    namespace = "com.example"

    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.taloarane.appcontroller"
        minSdk = 24
        targetSdk = 36

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    /*
     * Release signing
     *
     * Semua nilai signing berasal dari environment variable
     * yang diberikan oleh GitHub Actions.
     *
     * KEYSTORE_PATH
     * KEYSTORE_PASSWORD
     * ALIAS
     * ALIAS_PASSWORD
     *
     * File .jks tidak perlu disimpan di repository.
     */
    signingConfigs {
        create("release") {
            val keystorePath = System.getenv("KEYSTORE_PATH")

            if (!keystorePath.isNullOrBlank()) {
                storeFile = file(keystorePath)
                storePassword = System.getenv("KEYSTORE_PASSWORD")
                keyAlias = System.getenv("ALIAS")
                keyPassword = System.getenv("ALIAS_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            isCrunchPngs = false

            isMinifyEnabled = true
            isShrinkResources = true

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )

            /*
             * Gunakan release signing hanya jika
             * KEYSTORE_PATH tersedia.
             *
             * GitHub Actions akan membuat file keystore
             * sementara dan memberikan path melalui
             * KEYSTORE_PATH.
             */
            if (!System.getenv("KEYSTORE_PATH").isNullOrBlank()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    /*
     * Java 21
     */
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    /*
     * Compose
     */
    buildFeatures {
        compose = true
        buildConfig = true
    }

    /*
     * Unit Test
     */
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}


/*
 * Secrets Gradle Plugin
 *
 * Ini terpisah dari Android signing.
 * Plugin ini digunakan untuk membaca .env / .env.example
 * jika aplikasi membutuhkannya.
 */
secrets {
    propertiesFileName = ".env"
    defaultPropertiesFileName = ".env.example"
}


/*
 * Dependencies
 */
dependencies {

    implementation(platform(libs.androidx.compose.bom))

    // Firebase BOM
    // implementation(platform(libs.firebase.bom))

    // Permissions
    // implementation(libs.accompanist.permissions)

    /*
     * Activity
     */
    implementation(libs.androidx.activity.compose)

    /*
     * CameraX
     */
    // implementation(libs.androidx.camera.camera2)
    // implementation(libs.androidx.camera.core)
    // implementation(libs.androidx.camera.lifecycle)
    // implementation(libs.androidx.camera.view)

    /*
     * Compose
     */
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)

    /*
     * AndroidX Core
     */
    implementation(libs.androidx.core.ktx)

    /*
     * DataStore
     */
    // implementation(libs.androidx.datastore.preferences)

    /*
     * Lifecycle
     */
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    /*
     * Navigation
     */
    // implementation(libs.androidx.navigation.compose)

    /*
     * Room
     */
    // implementation(libs.androidx.room.ktx)
    // implementation(libs.androidx.room.runtime)

    /*
     * Coil
     */
    implementation(libs.coil.compose)

    /*
     * Moshi
     */
    // implementation(libs.converter.moshi)
    // implementation(libs.moshi.kotlin)

    /*
     * Firebase
     */
    // implementation(libs.firebase.ai)
    // implementation(libs.firebase.firestore)

    /*
     * Firebase Auth + Google Sign-In
     */
    // implementation(libs.firebase.auth)
    // implementation(libs.androidx.credentials)
    // implementation(libs.androidx.credentials.play.services)
    // implementation(libs.googleid)

    /*
     * Firebase App Check
     */
    // implementation(libs.firebase.appcheck.recaptcha)

    /*
     * Kotlin Coroutines
     */
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.core)

    /*
     * Networking
     */
    // implementation(libs.logging.interceptor)
    // implementation(libs.okhttp)
    // implementation(libs.retrofit)

    /*
     * Shizuku
     */
    implementation(libs.shizuku.api)
    implementation(libs.shizuku.provider)

    /*
     * Location
     */
    // implementation(libs.play.services.location)

    /*
     * Unit Tests
     */
    testImplementation(libs.androidx.compose.ui.test.junit4)
    testImplementation(libs.androidx.core)
    testImplementation(libs.androidx.junit)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)

    /*
     * Instrumentation Tests
     */
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.runner)

    /*
     * Debug
     */
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)

    /*
     * KSP
     */
    // "ksp"(libs.androidx.room.compiler)
    // "ksp"(libs.moshi.kotlin.codegen)
}