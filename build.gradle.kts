
// github.com/Kotlin/kmm-sample

buildscript {
  // ext.kotlin_version = "1.4.0"
  // ext.android_version = "4.0.1"
  repositories {
    gradlePluginPortal()
    google()
    mavenCentral()
    jcenter()
  }
  dependencies {
    classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.6.0") // 1.4.32
    classpath("org.jetbrains.kotlin:kotlin-serialization:2.0.0")
    classpath("com.android.tools.build:gradle:7.3.0")

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

tasks.create<Delete>("clean") {
  delete(rootProject.layout.buildDirectory)
}

open class GreetingTask: DefaultTask() {
  @TaskAction
  fun greet() {
    println("Hello greettings, https://docs.gradle.org/current/userguide/custom_tasks.html")
  }
}

tasks.register("runDebug", Exec::class) {
  dependsOn(":android-app:installDebug")
  description = "Install the app"
  group = "build" // to display
  commandLine = "adb shell am start -n de.nox.dndassistant.app.debug/de.nox.dndassistant.app.MainActivity -a android.intent.action.MAIN -c android.intent.category.LAUNCHER".split(" ")
}
