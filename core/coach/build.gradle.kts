plugins { kotlin("jvm") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    api("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")
    testImplementation("junit:junit:4.13.2")
}
