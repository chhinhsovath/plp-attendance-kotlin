// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.android.application") version "8.1.2" apply false
    id("org.jetbrains.kotlin.android") version "1.9.0" apply false
    id("com.google.dagger.hilt.android") version "2.48" apply false
    id("org.jetbrains.kotlin.kapt") version "1.9.0" apply false
    id("androidx.room") version "2.6.1" apply false
    id("com.google.gms.google-services") version "4.4.0" apply false
}

// Repositories are now managed in settings.gradle.kts

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}