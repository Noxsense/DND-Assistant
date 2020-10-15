plugins {
  id("com.android.application")
  kotlin("android")
  id("kotlin-android-extensions")
}

dependencies {
  /* Own modules */
  implementation(project(":core")) {
    exclude(group = "org.jetbrains", module = "annotations")
  }

  /* android libaries */
  implementation("com.google.android.material:material:1.2.1")
  // implementation("androidx.appcompat:appcompat:1.2.0")
  // implementation("androidx.constraintlayout:constraintlayout:2.0.1")
  // implementation("androidx.core:core-ktx:1.20") // not found.
}

android {
  compileSdkVersion(29)

  defaultConfig {
    applicationId = "de.nox.dndassistant.app"
    minSdkVersion(21)
    targetSdkVersion(29)
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    getByName("release") {
        isMinifyEnabled = false
    }

    getByName("debug") {
      applicationIdSuffix = ".debug"
      versionNameSuffix = "-debug"
      isMinifyEnabled = false
    }
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  lintOptions {
    isWarningsAsErrors = false
    isAbortOnError = true

    disable("HardcodedText") // ignore since it's mostly place holder text.
    disable("IconLauncherShape") // ignore my shape
    disable("SmallSp") // ignore too small size warnings.
    disable("UnsafeExperimentalUsageError") // Obsolete custom lint check
    disable("UnsafeExperimentalUsageWarning") // Obsolete custom lint check
  }

  packagingOptions {
    exclude("META-INF/kotlin-stdlib-common.kotlin_module")
    exclude("META-INF/kotlin-stdlib-jdk7.kotlin_module")
    exclude("META-INF/kotlin-stdlib-jdk8.kotlin_module")
    exclude("META-INF/kotlin-stdlib.kotlin_module")
    exclude("kotlin/annotation/annotation.kotlin_builtins")
    exclude("kotlin/collections/collections.kotlin_builtins")
    exclude("kotlin/coroutines/coroutines.kotlin_builtins")
    exclude("kotlin/internal/internal.kotlin_builtins")
    exclude("kotlin/kotlin.kotlin_builtins")
    exclude("kotlin/ranges/ranges.kotlin_builtins")
    exclude("kotlin/reflect/reflect.kotlin_builtins")
  }
}
