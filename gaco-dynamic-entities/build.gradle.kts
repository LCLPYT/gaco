extra["kibuModules"] = listOf(
    "kibu-hook-api",
    "kibu-hooks",
    "kibu-scheduler-api",
    "kibu-translation-api",
)

extra["gacoModules"] = listOf(
    "gaco-ds",
)

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-dynamic-entities"
                description = "API for dynamic entities, that don't exist physically in the world."
            }
        }
    }
}
