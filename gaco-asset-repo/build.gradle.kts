plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

extra["kibuModules"] = listOf(
    "kibu-assets-api",
)

extra["gacoModules"] = listOf<String>()

dependencies {
    implementation(libs.sqlite.jdbc)
    implementation(libs.kotlinx.coroutines.test)
    implementation(libs.kotaml.core)
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-asset-repo"
                description = "Provides an API for interacting with asset repositories"
            }
        }
    }
}
