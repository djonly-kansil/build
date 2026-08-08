import java.io.FileOutputStream
import java.util.Base64

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing material comes from GitHub Actions secrets:
// KEYSTORE (base64), KEYSTORE_PASSWORD, ALIAS, ALIAS_PASSWORD
val keystoreBase64: String? = System.getenv("KEYSTORE")
val keystorePasswordEnv: String? = System.getenv("KEYSTORE_PASSWORD")
val keyAliasEnv: String? = System.getenv("ALIAS")
val keyAliasPasswordEnv: String? = System.getenv("ALIAS_PASSWORD")

val decodedKeystore = layout.buildDirectory.file("keystore/release.jks").get().asFile
val hasReleaseSigning = !keystoreBase64.isNullOrBlank() &&
    !keystorePasswordEnv.isNullOrBlank() &&
    !keyAliasEnv.isNullOrBlank() &&
    !keyAliasPasswordEnv.isNullOrBlank()

if (hasReleaseSigning) {
    decodedKeystore.parentFile.mkdirs()
    FileOutputStream(decodedKeystore).use {
        it.write(Base64.getMimeDecoder().decode(keystoreBase64!!.trim()))
    }
}

android {
    namespace = "com.taloarane.appcontroll"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.taloarane.appcontroll"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        vectorDrawables.useSupportLibrary = true
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = decodedKeystore
                storePassword = keystorePasswordEnv
                keyAlias = keyAliasEnv
                keyPassword = keyAliasPasswordEnv
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
        debug {
            applicationIdSuffix = ".debug"
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
        isCoreLibraryDesugaringEnabled = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    val composeBom = platform("androidx.compose:compose-bom:2024.12.01")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
}
