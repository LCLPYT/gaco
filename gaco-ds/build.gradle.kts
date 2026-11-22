extra["kibuModules"] = listOf<String>()

extra["gacoModules"] = listOf(
    "gaco-math",
)

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-ds"
                description = "Data structures for game development"
            }
        }
    }
}
