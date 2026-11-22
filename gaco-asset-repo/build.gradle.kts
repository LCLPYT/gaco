plugins {
    alias(libs.plugins.kotlin.jvm)
}

extra["kibuModules"] = listOf(
    "kibu-assets-api",
)

extra["gacoModules"] = listOf<String>()

dependencies {
    implementation(libs.sqlite.jdbc)
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
