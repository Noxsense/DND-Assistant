pluginManagement {
  resolutionStrategy {
    eachPlugin {
      if (requested.id.id == "kotlin-multiplatform") {
        useModule("org.jetbrains.kotlin:kotlin-gradle-plugin:${requested.version}")
      }
      if (requested.id.namespace == "com.android" || requested.id.name == "kotlin-android-extension") {
        useModule("com.android.tools.build:gradle:4.0.1")
      }
    }
  }
}

rootProject.name = "dndassistant"

include(
  ":core",
  ":app"
)
