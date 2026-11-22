extra["kibuModules"] = listOf<String>()

extra["gacoModules"] = listOf(
    "gaco-scene",
)

dependencies {
    implementation(libs.kibu.physics)
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-scene-physics"
                description = "Provides physics objects to the gaco-scene API"
            }
        }
    }
}
