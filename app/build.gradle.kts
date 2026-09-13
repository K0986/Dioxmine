import com.google.gms.googleservices.GoogleServicesPlugin.MissingGoogleServicesStrategy
import java.util.Calendar

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
  alias(libs.plugins.google.services)
}

android {
  namespace = "io.github.rhythmcache.dioxamine"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "io.github.rhythmcache.dioxamine"
    minSdk = 24
    targetSdk = 36
    versionCode = 10003
    versionName = "0.0.3-stable"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }

    val currentYear = Calendar.getInstance().get(Calendar.YEAR).toString()
    buildConfigField("String", "APP_NAME", "\"Dioxamine\"")
    buildConfigField("String", "AUTHOR", "\"rhythmcache\"")
    buildConfigField("String", "COPYRIGHT_YEAR", "\"$currentYear\"")
    buildConfigField("String", "GITHUB_URL", "\"https://github.com/rhythmcache/\"")
    buildConfigField("String", "TELEGRAM_URL", "\"https://t.me/tr1ple_fault\"")
    buildConfigField("String", "SOURCE_CODE_URL", "\"https://github.com/rhythmcache/Dioxamine\"")
    buildConfigField("String", "DOCUMENTATION_URL", "\"https://rhythmcache.github.io/Dioxamine/book/\"")
    buildConfigField("String", "TERMINAL_PLUGIN_URL", "\"https://github.com/rhythmcache/Terminal\"")
    buildConfigField("String", "PLUGIN_DOCS_URL", "\"https://rhythmcache.github.io/Dioxamine/book/plugins/overview.html\"")
    buildConfigField("String", "TRANSLATION_URL", "\"https://github.com/rhythmcache/Dioxamine#translations\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
    create("debugConfig") {
      storeFile = file("${rootDir}/debug.keystore")
      storePassword = "android"
      keyAlias = "androiddebugkey"
      keyPassword = "android"
    }
  }

  packaging {
    resources {
      excludes += "META-INF/LICENSE.md"
      excludes += "META-INF/LICENSE.txt"
      excludes += "META-INF/NOTICE.md"
      excludes += "META-INF/NOTICE.txt"
      excludes += "META-INF/DEPENDENCIES"
      excludes += "META-INF/*.kotlin_module"
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("release")
    }
    debug { signingConfig = signingConfigs.getByName("debugConfig") }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
  compilerOptions {
    freeCompilerArgs.add("-Xskip-metadata-version-check")
  }
}

configurations.all {
  resolutionStrategy {
    force("org.jetbrains.kotlin:kotlin-stdlib:2.2.10")
    force("org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.2.10")
    force("org.jetbrains.kotlin:kotlin-stdlib-jdk7:2.2.10")
    force("org.jetbrains.kotlin:kotlin-reflect:2.2.10")
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = ".env"
  defaultPropertiesFileName = ".env.example"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
}

googleServices { missingGoogleServicesStrategy = MissingGoogleServicesStrategy.WARN }

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.appcompat)
  implementation(libs.androidx.webkit)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.lifecycle.viewmodel.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.kotlinx.coroutines.core)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kotlinx.serialization.json)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.adb.kt)
  implementation(libs.fastboot.kt)
  implementation(libs.qrose)
  testImplementation(libs.junit)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
