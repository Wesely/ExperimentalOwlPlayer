// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    id("com.google.devtools.ksp") version "2.0.21-1.0.27" apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.hilt) apply false
}
// IMPORTANT!! move repository declarations from the project-level build.gradle.kts to the settings.gradle.kts file. to avoud  the error "Build was configured to prefer settings repositories over project repositories but repository 'Google' was added by build file 'build.gradle.kts'"