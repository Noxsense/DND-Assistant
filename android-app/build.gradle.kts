plugins {
  id("com.android.application")
  kotlin("android")
  id("kotlin-parcelize")
}

dependencies {
  /* Own modules */
  implementation(project(":core")) {
    // exclude(group = "org.jetbrains", module = "annotations")
  }

  /* android libaries */
  implementation("com.google.android.material:material:1.12.0")
  // implementation("androidx.appcompat:appcompat:1.2.0")
  // implementation("androidx.constraintlayout:constraintlayout:2.0.1")
  // implementation("androidx.core:core-ktx:1.20") // not found.
}

android {
  namespace = "de.noxsense"

  compileSdk = 29

  defaultConfig {
    applicationId = "de.nox.dndassistant.app"
    minSdk = 21
    targetSdk = 29
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
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  buildFeatures {
    viewBinding = true
  }

  lint {
     warningsAsErrors = false
     abortOnError = true

    // disable("HardcodedText") // ignore since it's mostly place holder text.
    // disable("IconLauncherShape") // ignore my shape
    // disable("SmallSp") // ignore too small size warnings.
    // disable("UnsafeExperimentalUsageError") // Obsolete custom lint check
    // disable("UnsafeExperimentalUsageWarning") // Obsolete custom lint check
  }

  packagingOptions {
    resources {
    excludes += ("META-INF/kotlin-stdlib-common.kotlin_module")
    excludes += ("META-INF/kotlin-stdlib-jdk7.kotlin_module")
    excludes += ("META-INF/kotlin-stdlib-jdk8.kotlin_module")
    excludes += ("META-INF/kotlin-stdlib.kotlin_module")
    excludes += ("kotlin/annotation/annotation.kotlin_builtins")
    excludes += ("kotlin/collections/collections.kotlin_builtins")
    excludes += ("kotlin/coroutines/coroutines.kotlin_builtins")
    excludes += ("kotlin/internal/internal.kotlin_builtins")
    excludes += ("kotlin/kotlin.kotlin_builtins")
    excludes += ("kotlin/ranges/ranges.kotlin_builtins")
    excludes += ("kotlin/reflect/reflect.kotlin_builtins")
    }
  }
}
