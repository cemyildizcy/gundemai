import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import org.gradle.api.tasks.testing.Test
import java.util.Properties

val sampleAdMobAppId = "ca-app-pub-3940256099942544~3347511713"
val sampleAdMobBannerId = "ca-app-pub-3940256099942544/9214589741"
val configuredAdMobAppId = providers.gradleProperty("ADMOB_APP_ID")
  .orElse(providers.environmentVariable("ADMOB_APP_ID"))
  .orElse(sampleAdMobAppId)
  .get()
val adsEnabled = configuredAdMobAppId != sampleAdMobAppId &&
  configuredAdMobAppId.matches(Regex("ca-app-pub-\\d{16}~\\d{10}"))
val releaseKeystorePath = System.getenv("KEYSTORE_PATH")
val releaseStorePassword = System.getenv("STORE_PASSWORD")
val releaseKeyAlias = System.getenv("KEY_ALIAS") ?: "upload"
val releaseKeyPassword = System.getenv("KEY_PASSWORD")
val hasReleaseSigning = listOf(releaseKeystorePath, releaseStorePassword, releaseKeyPassword)
  .all { !it.isNullOrBlank() }

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.gundemai.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 107
    versionName = "1.0.7"
    manifestPlaceholders["ADMOB_APP_ID"] = configuredAdMobAppId
    buildConfigField("boolean", "ADS_ENABLED", adsEnabled.toString())

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  signingConfigs {
    if (hasReleaseSigning) {
      create("release") {
        storeFile = file(requireNotNull(releaseKeystorePath))
        storePassword = releaseStorePassword
        keyAlias = releaseKeyAlias
        keyPassword = releaseKeyPassword
      }
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.findByName("release")
    }
    debug { }
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

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

tasks.withType<Test>().configureEach {
  // Conscrypt's native library lookup is locale-sensitive on Turkish Windows.
  systemProperty("user.language", "en")
  systemProperty("user.country", "US")
}

val verifyReleaseConfiguration = tasks.register("verifyReleaseConfiguration") {
  doLast {
    require(hasReleaseSigning) {
      "Release build requires KEYSTORE_PATH, STORE_PASSWORD and KEY_PASSWORD."
    }
    require(file(requireNotNull(releaseKeystorePath)).isFile) {
      "KEYSTORE_PATH does not point to a signing key file."
    }

    val googleServicesFile = project.file("google-services.json")
    require(googleServicesFile.isFile) {
      "Release build requires app/google-services.json from the Firebase project."
    }
    require(googleServicesFile.readText().contains("\"package_name\": \"com.gundemai.app\"")) {
      "google-services.json must contain the com.gundemai.app Android app."
    }

    val envFile = rootProject.file(".env")
    require(envFile.isFile) { "Release build requires a configured root .env file." }
    val releaseEnv = Properties().apply {
      envFile.reader(Charsets.UTF_8).use(::load)
    }
    fun env(name: String) = releaseEnv.getProperty(name)?.trim().orEmpty()

    val apiBaseUrl = env("GUNDEMAI_API_BASE_URL")
    require(apiBaseUrl.startsWith("https://") && apiBaseUrl.endsWith("/") &&
      !apiBaseUrl.contains("example.com", ignoreCase = true)) {
      "GUNDEMAI_API_BASE_URL must be the deployed HTTPS service URL and end with /."
    }
    require(env("GOOGLE_WEB_CLIENT_ID").endsWith(".apps.googleusercontent.com")) {
      "GOOGLE_WEB_CLIENT_ID must be the Firebase Web OAuth client ID."
    }
    val bannerId = env("ADMOB_BANNER_AD_UNIT_ID")
    require(!adsEnabled || (
      bannerId != sampleAdMobBannerId &&
        bannerId.matches(Regex("ca-app-pub-\\d{16}/\\d{10}"))
      )) {
      "When ads are enabled, ADMOB_BANNER_AD_UNIT_ID must be a real banner ad unit ID."
    }
    val privacyUrl = env("PRIVACY_POLICY_URL")
    require(privacyUrl.startsWith("https://") && !privacyUrl.contains("example.com", ignoreCase = true)) {
      "PRIVACY_POLICY_URL must be the public HTTPS privacy policy URL."
    }
    require(env("SUPPORT_EMAIL").matches(Regex("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) &&
      env("SUPPORT_EMAIL") != "support@example.com") {
      "SUPPORT_EMAIL must be your real support email address."
    }
  }
}

tasks.configureEach {
  if (name == "preReleaseBuild") dependsOn(verifyReleaseConfiguration)
}

// Some unused dependencies are commented out below instead of being removed.
// This makes it easy to add them back in the future if needed.
dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(platform(libs.firebase.bom))
  // implementation(libs.accompanist.permissions)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.fragment)
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
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.room.ktx)
  implementation(libs.androidx.room.runtime)
  implementation(libs.coil.compose)
  implementation(libs.converter.moshi)

  // Firebase Auth & Google Play Billing
  implementation(libs.firebase.auth)
  implementation(libs.firebase.messaging)
  implementation(libs.androidx.credentials)
  implementation(libs.androidx.credentials.play.services)
  implementation(libs.googleid)
  implementation(libs.google.mobile.ads)
  implementation(libs.google.ump)
  implementation(libs.play.billing.ktx)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.androidx.work.runtime.ktx)
  implementation(libs.moshi.kotlin)
  implementation(libs.okhttp)
  // implementation(libs.play.services.location)
  implementation(libs.retrofit)
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
  "ksp"(libs.moshi.kotlin.codegen)
}
