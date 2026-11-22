extra["kibuModules"] = listOf<String>()

extra["gacoModules"] = listOf<String>()

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-core"
                description = "Core API for game commons"
            }
        }
    }
}
