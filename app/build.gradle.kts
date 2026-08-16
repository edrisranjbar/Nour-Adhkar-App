import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
}

val releaseSigningFile = rootProject.file("release-signing.properties")
val releaseSigningProperties = Properties().apply {
  if (releaseSigningFile.exists()) {
    releaseSigningFile.inputStream().use(::load)
  }
}

fun releaseSecret(environmentName: String, propertyName: String): String? =
  System.getenv(environmentName)?.takeIf(String::isNotBlank)
    ?: releaseSigningProperties.getProperty(propertyName)?.takeIf(String::isNotBlank)

val releaseBuildRequested =
  gradle.startParameter.taskNames.any { it.contains("release", ignoreCase = true) }

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "ir.adhkar.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 9
    versionName = "1.2.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    create("release") {
      val keystorePath = releaseSecret("KEYSTORE_PATH", "storeFile")
      val signingStorePassword = releaseSecret("STORE_PASSWORD", "storePassword")
      val signingKeyAlias = releaseSecret("KEY_ALIAS", "keyAlias")
      val signingKeyPassword = releaseSecret("KEY_PASSWORD", "keyPassword")

      if (releaseBuildRequested) {
        val missingValues = buildList {
          if (keystorePath == null) add("KEYSTORE_PATH/storeFile")
          if (signingStorePassword == null) add("STORE_PASSWORD/storePassword")
          if (signingKeyAlias == null) add("KEY_ALIAS/keyAlias")
          if (signingKeyPassword == null) add("KEY_PASSWORD/keyPassword")
        }
        check(missingValues.isEmpty()) {
          "Release signing is not configured. Missing: ${missingValues.joinToString()}. " +
            "Set environment variables or create the ignored release-signing.properties file."
        }
      }

      keystorePath?.let { storeFile = rootProject.file(it) }
      storePassword = signingStorePassword
      keyAlias = signingKeyAlias
      keyPassword = signingKeyPassword
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    // Keep development builds separate from a release-signed installation so local
    // testing never requires uninstalling the user's app or deleting its data.
    debug {
      applicationIdSuffix = ".debug"
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.concurrent.futures)
  // implementation(libs.androidx.camera.camera2)
  // implementation(libs.androidx.camera.core)
  // implementation(libs.androidx.camera.lifecycle)
  // implementation(libs.androidx.camera.view)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  // implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  // implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  // implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  // implementation(libs.play.services.location)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.roborazzi)
  testImplementation(libs.roborazzi.compose)
  testImplementation(libs.roborazzi.junit.rule)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.runner)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
  "ksp"(libs.androidx.room.compiler)
}
