extra["kibuModules"] = listOf(
    "kibu-scheduler-api",
)

extra["gacoModules"] = listOf(
    "gaco-core",
    "gaco-dynamic-entities",
)

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-scene"
                description = "Scene graph implementation for managing 3d object hierarchies."
            }
        }
    }
}
