plugins {
  id("com.android.application")
  kotlin("android")
  id("kotlin-android-extensions")
}

dependencies {
  implementation(project(":core"))
  implementation(kotlin("stdlib-jdk7"))
  implementation("com.google.android.material:material:1.2.1")
  implementation("androidx.appcompat:appcompat:1.2.0")
  implementation("androidx.constraintlayout:constraintlayout:2.0.1")
}

android {
  compileSdkVersion(29)

  defaultConfig {
    applicationId = "com.jetbrains.androidApp"
    minSdkVersion(24)
    targetSdkVersion(29)
    versionCode = 1
    versionName = "1.0"
  }

  buildTypes {
    getByName("release") {
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
