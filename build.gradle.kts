import work.lclpnet.build.task.GithubDeploymentTask
import java.net.URI
import java.util.Properties
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element

plugins {
    alias(libs.plugins.java)
    alias(libs.plugins.maven.publish)
    alias(libs.plugins.fabric.loom)
    alias(libs.plugins.gradle.build.utils)
    alias(libs.plugins.lombok)
    alias(libs.plugins.kotlin.jvm) apply false
}

val env: Map<String, String> = System.getenv()
val props: Properties = buildUtils.loadProperties("publish.properties")  // will be empty, if the file is missing

val local = System.getProperty("build.local") == "true"
val versionSuffix = if (local) "+local" else ""

val modId = property("mod_id") as String
val javaVersion = libs.versions.java.get().toInt()
val minecraftVersion = libs.versions.minecraft.get()

// root extensions, captured so that they can be referenced inside allprojects / subprojects blocks
val catalog = libs
val rootBuildUtils = buildUtils

version = "${property("mod_version")}+$minecraftVersion$versionSuffix"

// Subprojects declare kibu and gaco dependencies via "kibuModules" and "gacoModules".
// This has to be registered before Loom is applied to the subprojects as Loom resolves the dependency configurations in its own afterEvaluate hook.
subprojects {
    afterEvaluate {
        configureModuleDependencies(this)
    }
}

allprojects {
    group = property("maven_group") as String

    apply(plugin = catalog.plugins.java.get().pluginId)
    apply(plugin = catalog.plugins.maven.publish.get().pluginId)
    apply(plugin = catalog.plugins.fabric.loom.get().pluginId)
    apply(plugin = catalog.plugins.lombok.get().pluginId)

    repositories {
        maven {
            url = uri("https://repo.lclpnet.work/releases")
        }

        exclusiveContent {
            forRepository {
                maven {
                    name = "Modrinth"
                    url = uri("https://api.modrinth.com/maven")
                }
            }
            filter {
                includeGroup("maven.modrinth")
            }
        }
    }

    dependencies {
        minecraft(catalog.minecraft)
        implementation(catalog.fabric.loader)
        implementation(catalog.fabric.api)

        compileOnly(catalog.jetbrains.annotations)

        testImplementation(catalog.junit.jupiter.api)
        testRuntimeOnly(catalog.junit.jupiter.engine)
        testImplementation(catalog.mockito.core)
        testImplementation(catalog.junit.jupiter.params)
        testImplementation(catalog.fabric.loader.junit)
    }

    tasks.named<Test>("test") {
        useJUnitPlatform()
        systemProperty("fabric.side", "server")
    }

    java {
        // Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
        withSourcesJar()

        sourceCompatibility = JavaVersion.toVersion(javaVersion)
        targetCompatibility = JavaVersion.toVersion(javaVersion)
    }

    tasks.withType<JavaCompile>().configureEach {
        options.release.set(javaVersion)
    }

    listOf("jar", "sourcesJar").forEach { taskName ->
        tasks.named<Jar>(taskName) {
            from("LICENSE") {
                rename { "${it}_${project.base.archivesName.get()}" }
            }
        }
    }

    publishing {
        // automatically use DEPLOY_URL, DEPLOY_USER and DEPLOY_PASSWORD environment variables
        // or mavenHost, mavenUser and mavenPassword from props
        rootBuildUtils.setupPublishRepository(repositories, props)
    }
}

subprojects {
    version = getSubprojectVersion(project)

    publishing {
        publications {
            create<MavenPublication>("mavenJava") {
                artifactId = project.base.archivesName.get()

                from(components["java"])
            }
        }
    }

    base {
        archivesName = project.name
    }

    configureSubstitutions(tasks.named<ProcessResources>("processResources"), substitutions(project))

    tasks.withType<PublishToMavenRepository>()
        .matching { it.name == "publishMavenJavaPublicationToMavenRepository" }
        .configureEach {
            onlyIf { !checkProjectIsDeployed(project, props) }
        }

    afterEvaluate {
        listOf("genSources", "genSourcesWithCfr", "genSourcesWithFernFlower", "genSourcesWithVineflower").forEach {
            tasks.findByName(it)?.enabled = false
        }
    }
}

// the root project depends on the kibu modules of all subprojects; evaluate them first to read their declarations
evaluationDependsOnChildren()

useKibuModules(project, subprojects.flatMap { it.declaredModules("kibuModules") }.toSet())

base {
    archivesName = modId
}

sourceSets.create("testmod") {
    val main = sourceSets.main.get()

    compileClasspath += main.compileClasspath + main.output
    runtimeClasspath += main.runtimeClasspath + main.output
}

dependencies {
    implementation(libs.kibu.physics)
    implementation(libs.fabric.language.kotlin)
}

loom {
    serverOnlyMinecraftJar()

    runs {
        remove(getByName("client"))

        named("server") {
            source("testmod")
        }
    }
}

configureSubstitutions(tasks.processResources, substitutions(project))
configureSubstitutions(tasks.named<ProcessResources>("processTestmodResources"), substitutions(project))

// main jar must be built after all the subprojects
tasks.jar {
    subprojects.forEach { dependsOn("${it.path}:jar") }
}

dependencies {
    subprojects.forEach { api(project(it.path)) }
}

// add subprojects as nested jar to the main jar
val nestedJars = configurations.create("nestedJars") {
    isTransitive = false
}

dependencies {
    subprojects.forEach { nestedJars(project(it.path)) }
}

loom.nestJars(tasks.jar, nestedJars)

tasks.register<GithubDeploymentTask>("github") {
    dependsOn(tasks.jar)

    config {
        token = env["GITHUB_TOKEN"]
        repository = env["GITHUB_REPOSITORY"]
    }

    release {
        title = "[$minecraftVersion] ${project.name} ${project.version}"
        tag = project.version.toString()
    }

    assets.add(tasks.jar.flatMap { it.archiveFile })
}

// configure the maven publication
publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            artifactId = project.base.archivesName.get()

            from(components["java"])

            pom {
                name = "gaco"
                description = "Game commons libraries for developing minigames for Minecraft: Java edition using FabricMC."
            }
        }
    }
}

/************************************\
 * Utilities                         *
 \************************************/

fun substitutions(project: Project): Map<String, Any> = mapOf(
    "version" to project.version,
    "loader_version" to libs.versions.fabric.loader.get(),
    "java_version" to libs.versions.java.get(),
    "minecraft_compat" to project.property("minecraft_compat")!!,
)

fun configureSubstitutions(task: TaskProvider<ProcessResources>, substitutions: Map<String, Any>) {
    task.configure {
        inputs.properties(substitutions)

        filesMatching("fabric.mod.json") {
            expand(substitutions)
        }
    }
}

fun Project.declaredModules(key: String): List<String> {
    if (!extra.has(key)) return emptyList()

    @Suppress("UNCHECKED_CAST")
    return extra[key] as List<String>
}

/**
 * Adds the declared kibu and gaco modules of a subproject as dependencies.
 * The kibu modules of all (transitively) required gaco modules are added as well.
 */
fun configureModuleDependencies(target: Project) {
    val kibuModules = LinkedHashSet(target.declaredModules("kibuModules"))
    val gacoModules = LinkedHashSet<String>()
    val processGacoModules = ArrayDeque(target.declaredModules("gacoModules"))

    while (processGacoModules.isNotEmpty()) {
        val gacoModule = processGacoModules.removeFirst()

        if (target.name == gacoModule) {
            throw IllegalStateException("Gaco module $gacoModule cannot depend on itself")
        }

        if (!gacoModules.add(gacoModule)) continue

        // make sure the module's build script was evaluated, so that its declarations can be read
        val moduleProject = target.evaluationDependsOn(":$gacoModule")

        kibuModules.addAll(moduleProject.declaredModules("kibuModules"))

        for (next in moduleProject.declaredModules("gacoModules")) {
            if (target.name == next) {
                throw IllegalStateException("Circular dependency detected for gaco module ${target.name} (via $gacoModule)")
            }

            if (next in gacoModules) continue

            processGacoModules.add(next)
        }
    }

    useKibuModules(target, kibuModules)
    useGacoModules(target, gacoModules)
}

fun checkProjectIsDeployed(project: Project, props: Properties): Boolean {
    val url = "${getDeployUrl(props)}/${project.group.toString().replace('.', '/')}/${project.base.archivesName.get()}/maven-metadata.xml"

    val meta = try {
        URI.create(url).toURL().openStream().use {
            DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(it)
        }
    } catch (_: Throwable) {
        return false
    }

    // collect metadata > versioning > versions > version
    val versionNodes = meta.documentElement.childElements("versioning")
        .flatMap { it.childElements("versions") }
        .flatMap { it.childElements("version") }

    return versionNodes.any { it.textContent.trim() == project.version.toString() }
}

fun Element.childElements(tagName: String): List<Element> {
    val nodes = childNodes
    return (0 until nodes.length)
        .map { nodes.item(it) }
        .filterIsInstance<Element>()
        .filter { it.tagName == tagName }
}

fun getDeployUrl(props: Properties): String {
    val env = System.getenv()

    return when {
        env.containsKey("DEPLOY_URL") -> env.getValue("DEPLOY_URL")
        props.containsKey("mavenHost") -> props.getProperty("mavenHost")
        else -> "file:///${rootProject.projectDir}/repo"
    }
}

fun getSubprojectVersion(project: Project): String {
    val version = project.findProperty("${project.name}_version")?.toString()

    if (version.isNullOrEmpty()) {
        throw IllegalStateException("Could not find version for project ${project.name}")
    }

    val local = System.getProperty("build.local") == "true"
    val suffix = if (local) "+local" else ""

    val snapshotSuffix = "-SNAPSHOT"
    val mcVersion = libs.versions.minecraft.get()

    if (version.endsWith(snapshotSuffix)) {
        return version.removeSuffix(snapshotSuffix) + "+" + mcVersion + suffix + snapshotSuffix
    }

    return "$version+$mcVersion$suffix"
}

fun useKibuModules(target: Project, modules: Iterable<String>) {
    for (module in modules) {
        val version = rootProject.findProperty("${module}_version")?.toString()

        if (version.isNullOrEmpty()) {
            throw IllegalStateException("Could not find version for kibu module $module")
        }

        target.dependencies {
            "implementation"("work.lclpnet.mods.kibu:$module:$version")
        }
    }
}

fun useGacoModules(target: Project, modules: Iterable<String>) {
    for (module in modules) {
        target.dependencies {
            "api"(project(":$module"))
        }
    }
}
