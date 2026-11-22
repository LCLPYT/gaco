extra["kibuModules"] = listOf(
    "kibu-hook-api",
    "kibu-hooks",
    "kibu-scheduler-api",
)

extra["gacoModules"] = listOf(
    "gaco-math",
    "gaco-ds",
)

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-collisions"
                description = "API for detecting and handling collisions / movement"
            }
        }
    }
}
