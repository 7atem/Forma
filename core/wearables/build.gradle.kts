plugins { kotlin("jvm"); kotlin("plugin.serialization") }
kotlin { jvmToolchain(21) }
sourceSets.test { resources.srcDir("../../contracts/apple-watch") }
dependencies {
    api(project(":core:model"))
    testImplementation("junit:junit:4.13.2")
}
