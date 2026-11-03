plugins {
    id("com.android.application")
    kotlin("android")
    kotlin("plugin.compose")
    id("com.google.devtools.ksp")
    id("com.google.dagger.hilt.android")
}
android {
    namespace = "com.formacoach.app"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.formacoach.app"
        minSdk = 29
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0-alpha01"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    buildFeatures { compose = true }
    lint {
        disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "OldTargetApi")
    }
}
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:coach"))
    implementation(project(":core:data"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.compose.ui:ui:1.7.6")
    implementation("androidx.compose.foundation:foundation:1.7.6")
    implementation("androidx.compose.material3:material3:1.3.1")
    implementation("com.google.dagger:hilt-android:2.60.1")
    ksp("com.google.dagger:hilt-compiler:2.60.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.room:room-runtime:2.8.4")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:1.7.6")
    debugImplementation("androidx.compose.ui:ui-test-manifest:1.7.6")
}
