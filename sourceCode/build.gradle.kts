
// github.com/Kotlin/kmm-sample

buildscript {
  // ext.kotlin_version = "1.4.0"
  // ext.android_version = "4.0.1"
  repositories {
    gradlePluginPortal()
    jcenter()
    google()
    mavenCentral()
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.4.0")
    classpath("org.jetbrains.kotlin:kotlin-serialization:1.4.0")
    classpath("com.android.tools.build:gradle:4.0.1")

    // classpath("org.jlleitschuh.gradle.ktlint:ktlint:9.2.1") // TODO
  }
}

repositories {
  mavenCentral()
}

allprojects {
  group = "de.noxsense"
  version = "0.2.0-SNAPSHOT"

  apply {
    // plugin("org.jlleitschuh.gradle.ktlint") // TODO
  }

  repositories {
    google()
    mavenCentral()
    jcenter()
  }
}
