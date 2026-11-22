extra["kibuModules"] = listOf(
    "kibu-schematic-api",
    "kibu-schematic-fabric",
)

extra["gacoModules"] = listOf<String>()

dependencies {
    listOf(libs.ejml.core, libs.ejml.simple, libs.ejml.ddense).forEach {
        implementation(it)
        include(it)
    }
}

publishing {
    publications {
        named<MavenPublication>("mavenJava") {
            pom {
                name = "gaco-math"
                description = "Math utilities for game development"
            }
        }
    }
}
