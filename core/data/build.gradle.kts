plugins {
    id("com.android.library")
    kotlin("android")
    id("com.google.devtools.ksp")
}
android {
    namespace = "com.formacoach.data"
    compileSdk = 36
    defaultConfig { minSdk = 29 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}
kotlin { jvmToolchain(21) }
ksp { arg("room.schemaLocation", "$projectDir/schemas") }
dependencies {
    api(project(":core:model"))
    implementation("androidx.room:room-runtime:2.8.4")
    implementation("androidx.room:room-ktx:2.8.4")
    ksp("androidx.room:room-compiler:2.8.4")
    implementation("net.zetetic:sqlcipher-android:4.17.0")
    implementation("androidx.sqlite:sqlite:2.5.0")
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
}
