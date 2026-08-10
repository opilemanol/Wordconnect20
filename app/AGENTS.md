# Kotlin, Compose, & Gradle Sync Instructions

To ensure a clean, successful compile and build of the application across both Google AI Studio and GitHub workflows, follow these exact dependency, versioning, and project property adjustments:

## 1. Gradle Properties Configuration (`gradle.properties`)
To bypass built-in Kotlin strict source set errors regarding KSP directories, add the following flag to your `gradle.properties` file:
```properties
# Suppress strict Kotlin source set validation with modern built-in Kotlin toolchains
android.disallowKotlinSourceSets=false
```

## 2. Version Catalog Synchronization (`gradle/libs.versions.toml`)
Ensure the Kotlin compiler, Gradle plugins, and Fragment helpers are structurally matched and fully compatible. Add or modify the following references:
```toml
[versions]
kotlin = "2.2.10"
googleDevtoolsKsp = "2.3.5"
fragment = "1.8.5"

[libraries]
androidx-fragment-ktx = { group = "androidx.fragment", name = "fragment-ktx", version.ref = "fragment" }

[plugins]
# Avoid introducing 'kotlin-android' if kotlin-compose is active to prevent namespace collisions
kotlin-compose = { id = "org.jetbrains.kotlin.plugin.compose", version.ref = "kotlin" }
google-devtools-ksp = { id = "com.google.devtools.ksp", version.ref = "googleDevtoolsKsp" }
```

## 3. Top-Level Build Configuration (`build.gradle.kts`)
Ensure plugin declarations remain sleek and do not apply duplicate or conflicting Kotlin hooks:
```kotlin
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.roborazzi) apply false
}
```

## 4. App-Level Build Configuration (`app/build.gradle.kts`)
Apply the standardized plugins and introduce the fragment runtime extension to support standard ActivityResult and Fragment flows smoothly:
```kotlin
plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.google.devtools.ksp)
  alias(libs.plugins.roborazzi)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  defaultConfig {
    applicationId = "Application ID Here"
    // ...
  }
}

dependencies {
  // core fragment helpers
  implementation(libs.androidx.fragment.ktx)
  // ...
}
```
