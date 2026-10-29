import java.util.Properties
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

val localProperties =
  Properties().apply {
    val localFile = rootProject.file("local.properties")
    if (localFile.exists()) localFile.inputStream().use(::load)
  }
val jamendoClientId =
  providers
    .environmentVariable("JAMENDO_CLIENT_ID")
    .orElse(localProperties.getProperty("jamendoClientId", ""))
    .get()
val jamendoClientIdLiteral = "\"${jamendoClientId.replace("\\", "\\\\").replace("\"", "\\\"")}\""
val releaseKeystoreFile = providers.environmentVariable("ADLESS_RELEASE_KEYSTORE_FILE").orNull
val releaseStorePassword = providers.environmentVariable("ADLESS_RELEASE_STORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ADLESS_RELEASE_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ADLESS_RELEASE_KEY_PASSWORD").orNull
val releaseSigningValues =
  listOf(releaseKeystoreFile, releaseStorePassword, releaseKeyAlias, releaseKeyPassword)
val releaseSigningReady = releaseSigningValues.all { !it.isNullOrBlank() }

gradle.taskGraph.whenReady {
  val releaseTasksRequested = allTasks.any { "release" in it.name.lowercase() }
  if (releaseTasksRequested && !releaseSigningReady) {
    throw GradleException(
      "Release builds require ADLESS_RELEASE_KEYSTORE_FILE, ADLESS_RELEASE_STORE_PASSWORD, " +
        "ADLESS_RELEASE_KEY_ALIAS, and ADLESS_RELEASE_KEY_PASSWORD. Keep the keystore and passwords out of Git."
    )
  }
}

android {
  namespace = "app.adless.streaming"
  compileSdk = 36

  defaultConfig {
    applicationId = "app.adless.streaming"
    minSdk = 26
    targetSdk = 36
    versionCode = 1
    versionName = "0.1.0"
    buildConfigField("String", "JAMENDO_CLIENT_ID", jamendoClientIdLiteral)
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  buildFeatures {
    compose = true
    buildConfig = true
  }

  if (releaseSigningReady) {
    signingConfigs {
      create("release") {
        storeFile = file(requireNotNull(releaseKeystoreFile))
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    getByName("release") {
      isMinifyEnabled = true
      isShrinkResources = true
      if (releaseSigningReady) signingConfig = signingConfigs.getByName("release")
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  testOptions { unitTests.isIncludeAndroidResources = true }
}

kotlin {
  compilerOptions {
    jvmTarget = JvmTarget.JVM_17
  }
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.media3.exoplayer)
  implementation(libs.androidx.media3.session)
  implementation(libs.kotlinx.coroutines.android)
  debugImplementation(libs.androidx.compose.ui.tooling)
  testImplementation(libs.junit)
  testImplementation(libs.json)
}
