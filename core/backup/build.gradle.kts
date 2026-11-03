plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(21) }
dependencies {
    implementation(project(":core:model"))
    testImplementation("junit:junit:4.13.2")
}
