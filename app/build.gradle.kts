import java.io.FileOutputStream
import java.util.Base64
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
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

    buildFeatures {
        compose = true
    }

    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

dependencies {
    coreLibraryDesugaring(libs.desugar.jdk.libs)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.kotlinx.coroutines.android)
}
