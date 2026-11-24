package work.lclpnet.gaco.assetv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import work.lclpnet.gaco.assetv2.resolved.ListingItem
import work.lclpnet.gaco.assetv2.resolved.ResolvedListing
import work.lclpnet.gaco.assetv2.resolved.ResolvedListingItem
import java.io.IOException
import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.absolute

class AssetServiceTest {

    val minimalRoot: Path = Path.of("src/test/resources/assets/minimal")
    val advancedRoot: Path = Path.of("src/test/resources/assets/advanced")
    val nonExistentRoot: Path = Path.of("src/test/resources/assets/does_not_exist")
    val logger: Logger = LoggerFactory.getLogger(AssetServiceTest::class.java)

    @Test
    fun minimal_arena_get() = runTest {
        val service = service(repo(minimalRoot))

        val res = service.get(
            asset = "maps/spleef/arena",
            file = "map.txt",
        )

        assertEquals("This is map 'arena', version 2", res.readText())
    }

    @Test
    fun minimal_arena_getUri() = runTest {
        val service = service(repo(minimalRoot))

        val res = service.getUri(
            asset = "maps/spleef/arena",
            file = "map.txt",
        )

        if (res !is AssetFetchUriResult.Success) {
            fail("Expected success")
        }

        val expected = "file://${minimalRoot.absolute()}/maps/spleef/arena/@2/map.txt"

        assertEquals(expected, res.uri.toString())
    }

    @Test
    fun minimal_spleef_getListing() = runTest {
        val transport = PathRepoTransport(minimalRoot, logger)
        val repo = AssetRepo(transport, logger)
        val service = AssetService(repos = listOf(repo), logger = logger)

        val res = service.getListing("maps/spleef")

        val expected = ResolvedListing(items = setOf(
            ResolvedListingItem(
                item = ListingItem("maps/spleef/arena"),
                repo = repo,
            ),
            ResolvedListingItem(
                item = ListingItem("maps/spleef/snow_valley"),
                repo = repo,
            ),
        ))

        assertEquals(expected, res)
    }

    @Test
    fun advanced_spleef_getListing_properties() = runTest {
        val advanced = repo(advancedRoot)
        val service = service(advanced)

        val res = service.getListing("maps/spleef")

        val items = res.items.associateBy { it.item.asset }
        assertEquals(setOf("maps/spleef/arena", "maps/spleef/snow_valley"), items.keys)
        assertTrue(res.items.all { it.repo === advanced })

        // the properties of "arena" are equal in v2 and v3 of the listing
        val arena = items.getValue("maps/spleef/arena").item.properties
        assertEquals(
            mapOf(
                "icon" to "minecraft:spruce_planks",
                "authors" to listOf("@person.lclp"),
                "name" to mapOf(
                    "en" to "Arena",
                    "de" to "Spleef-Arena",
                ),
            ),
            arena,
        )

        // "snow_valley" only has properties in v3 of the listing
        val snowValley = items.getValue("maps/spleef/snow_valley").item.properties
        assertEquals(
            mapOf(
                "icon" to "minecraft:snow_block",
                "authors" to listOf("@person.bops", "@person.lclp"),
                "name" to mapOf(
                    "en" to "Snow Valley",
                    "de" to "Schneetal",
                ),
            ),
            snowValley,
        )
    }

    @Test
    fun missingAsset_get_assetNotFound() = runTest {
        val service = service(repo(minimalRoot))

        val res = service.get("maps/spleef/does_not_exist", "map.txt")

        assertSame(AssetFetchResult.AssetNotFound, res)
    }

    @Test
    fun missingAsset_getUri_assetNotFound() = runTest {
        val service = service(repo(minimalRoot))

        val res = service.getUri("maps/spleef/does_not_exist", "map.txt")

        assertSame(AssetFetchUriResult.AssetNotFound, res)
    }

    @Test
    fun missingFile_get_fileNotFound() = runTest {
        // asset.yml and the version directory exist, but the requested file does not
        val service = service(repo(minimalRoot))

        val res = service.get("maps/spleef/arena", "does_not_exist.txt")

        assertSame(AssetFetchResult.FileNotFound, res)
    }

    @Test
    fun missingVersionDirectory_get_assetNotFound() = runTest {
        // advanced/maps/parkour/tower/asset.yml declares version 2, but there is no @2 directory
        val service = service(repo(advancedRoot))

        val res = service.get("maps/parkour/tower", "map.txt")

        // file not found -> repo fault in this case (it declared that it has the version)
        assertSame(AssetFetchResult.FileNotFound, res)
    }

    @Test
    fun missingAssetYml_getListing_empty() = runTest {
        val service = service(repo(minimalRoot))

        val res = service.getListing("maps/does_not_exist")

        assertEquals(ResolvedListing(items = setOf()), res)
    }

    @Test
    fun missingIndexYml_getListing_empty() = runTest {
        // advanced/maps/tnt_run/asset.yml exists, but @1/index.yml does not
        val service = service(repo(advancedRoot))

        val res = service.getListing("maps/tnt_run")

        assertEquals(ResolvedListing(items = setOf()), res)
    }

    @Test
    fun multiRepo_get_firstRepoHasPriority() = runTest {
        // "maps/parkour/jungle" exists in both repos
        val advanced = repo(advancedRoot)
        val minimal = repo(minimalRoot)

        val advancedFirst = service(advanced, minimal).get("maps/parkour/jungle", "map.txt")
        assertEquals("This is map 'jungle', version 1 (advanced repo)", advancedFirst.readText())

        val minimalFirst = service(minimal, advanced).get("maps/parkour/jungle", "map.txt")
        assertEquals("This is map 'jungle', version 1", minimalFirst.readText())
    }

    @Test
    fun multiRepo_getUri_firstRepoHasPriority() = runTest {
        val advanced = repo(advancedRoot)
        val minimal = repo(minimalRoot)

        val advancedFirst = service(advanced, minimal).getUri("maps/parkour/jungle", "map.txt")
        assertEquals(uriOf(advancedRoot, "maps/parkour/jungle/@1/map.txt"), advancedFirst.uri())

        val minimalFirst = service(minimal, advanced).getUri("maps/parkour/jungle", "map.txt")
        assertEquals(uriOf(minimalRoot, "maps/parkour/jungle/@1/map.txt"), minimalFirst.uri())
    }

    @Test
    fun multiRepo_assetOnlyInSecondRepo_get() = runTest {
        // advanced has no maps/tnt_run/desert/asset.yml, so the asset must be found in minimal
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.get("maps/tnt_run/desert", "map.txt")

        assertEquals("This is map 'desert', version 1", res.readText())
    }

    @Test
    fun multiRepo_assetOnlyInSecondRepo_getUri() = runTest {
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.getUri("maps/tnt_run/desert", "map.txt")

        assertEquals(uriOf(minimalRoot, "maps/tnt_run/desert/@1/map.txt"), res.uri())
    }

    @Test
    fun multiRepo_assetInNoRepo_assetNotFound() = runTest {
        val service = service(repo(advancedRoot), repo(minimalRoot))

        assertSame(AssetFetchResult.AssetNotFound, service.get("maps/parkour/does_not_exist", "map.txt"))
        assertSame(AssetFetchUriResult.AssetNotFound, service.getUri("maps/parkour/does_not_exist", "map.txt"))
    }

    @Test
    fun multiRepo_fileIsNotResolvedInOtherRepo_get() = runTest {
        // advanced declares tower version 2 without providing its files.
        // minimal has maps/parkour/tower/@2/map.txt, but it must not be used, because the
        // asset version was resolved in the advanced repo.
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.get("maps/parkour/tower", "map.txt")

        assertSame(AssetFetchResult.FileNotFound, res)
    }

    @Test
    fun multiRepo_fileIsNotResolvedInOtherRepo_getUri() = runTest {
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.getUri("maps/parkour/tower", "map.txt")

        // getUri is not required to check that the file exists, but it must never point into another repo
        if (res is AssetFetchUriResult.Success) {
            assertEquals(uriOf(advancedRoot, "maps/parkour/tower/@2/map.txt"), res.uri)
        }
    }

    @Test
    fun multiRepo_fileOnlyInOtherRepo_isNotResolvedInOtherRepo_get() = runTest {
        // jungle v1 exists in both repos and is resolved in advanced (first repo).
        // only minimal has jungle/@1/preview.txt, but it must not be used as a fallback.
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.get("maps/parkour/jungle", "preview.txt")

        assertSame(AssetFetchResult.FileNotFound, res)
    }

    @Test
    fun multiRepo_fileOnlyInOtherRepo_isNotResolvedInOtherRepo_getUri() = runTest {
        val service = service(repo(advancedRoot), repo(minimalRoot))

        val res = service.getUri("maps/parkour/jungle", "preview.txt")

        // getUri is not required to check that the file exists, but it must never point into another repo
        if (res is AssetFetchUriResult.Success) {
            assertEquals(uriOf(advancedRoot, "maps/parkour/jungle/@1/preview.txt"), res.uri)
        }
    }

    @Test
    fun multiRepo_getListing_mergedWithFirstRepoPriority() = runTest {
        val advanced = repo(advancedRoot)
        val minimal = repo(minimalRoot)
        val service = service(advanced, minimal)

        val res = service.getListing("maps/parkour")

        val items = res.items.associateBy { it.item.asset }
        assertEquals(setOf("maps/parkour/tower", "maps/parkour/jungle", "maps/parkour/cave"), items.keys)
        assertEquals(3, res.items.size, "Every asset should be listed exactly once")

        assertSame(advanced, items.getValue("maps/parkour/tower").repo)
        assertSame(advanced, items.getValue("maps/parkour/jungle").repo)
        assertSame(minimal, items.getValue("maps/parkour/cave").repo)

        // properties are taken from the item of the repo with the highest priority
        val tower = items.getValue("maps/parkour/tower").item.properties
        assertEquals(
            mapOf(
                "icon" to "minecraft:stone_bricks",
                "name" to mapOf(
                    "en" to "Tower",
                    "de" to "Turm",
                ),
            ),
            tower,
        )

        // "jungle" has no properties in the advanced repo
        assertEquals(mapOf<String, Any?>(), items.getValue("maps/parkour/jungle").item.properties)
    }

    @Test
    fun multiRepo_getListing_reversedOrder() = runTest {
        val advanced = repo(advancedRoot)
        val minimal = repo(minimalRoot)
        val service = service(minimal, advanced)

        val res = service.getListing("maps/parkour")

        val expected = ResolvedListing(items = setOf(
            ResolvedListingItem(ListingItem("maps/parkour/tower"), minimal),
            ResolvedListingItem(ListingItem("maps/parkour/jungle"), minimal),
            ResolvedListingItem(ListingItem("maps/parkour/cave"), minimal),
        ))

        assertEquals(expected, res)
    }

    @Test
    fun multiRepo_spleef_getListing_firstRepoHasPriority() = runTest {
        val advanced = repo(advancedRoot)
        val service = service(advanced, repo(minimalRoot))

        val res = service.getListing("maps/spleef")

        assertEquals(setOf("maps/spleef/arena", "maps/spleef/snow_valley"), res.items.map { it.item.asset }.toSet())
        assertEquals(2, res.items.size, "Every asset should be listed exactly once")
        assertTrue(res.items.all { it.repo === advanced })
    }

    @Test
    fun multiRepo_getListing_indexMissingInFirstRepo() = runTest {
        // advanced has maps/tnt_run/asset.yml but no index.yml; minimal provides a complete listing
        val minimal = repo(minimalRoot)
        val service = service(repo(advancedRoot), minimal)

        val res = service.getListing("maps/tnt_run")

        val expected = ResolvedListing(items = setOf(
            ResolvedListingItem(ListingItem("maps/tnt_run/desert"), minimal),
        ))

        assertEquals(expected, res)
    }

    @Test
    fun unreachableRepo_get_otherReposAreUsed() = runTest {
        val service = service(unreachableRepo(), repo(minimalRoot))

        val res = service.get("maps/spleef/arena", "map.txt")

        assertEquals("This is map 'arena', version 2", res.readText())
    }

    @Test
    fun unreachableRepo_getUri_otherReposAreUsed() = runTest {
        val service = service(unreachableRepo(), repo(minimalRoot))

        val res = service.getUri("maps/spleef/arena", "map.txt")

        assertEquals(uriOf(minimalRoot, "maps/spleef/arena/@2/map.txt"), res.uri())
    }

    @Test
    fun unreachableRepo_getListing_otherReposAreUsed() = runTest {
        val minimal = repo(minimalRoot)
        val service = service(unreachableRepo(), minimal)

        val res = service.getListing("maps/spleef")

        val expected = ResolvedListing(items = setOf(
            ResolvedListingItem(ListingItem("maps/spleef/arena"), minimal),
            ResolvedListingItem(ListingItem("maps/spleef/snow_valley"), minimal),
        ))

        assertEquals(expected, res)
    }

    @Test
    fun unreachableRepo_only_assetNotFound() = runTest {
        val service = service(unreachableRepo())

        assertSame(AssetFetchResult.AssetNotFound, service.get("maps/spleef/arena", "map.txt"))
        assertSame(AssetFetchUriResult.AssetNotFound, service.getUri("maps/spleef/arena", "map.txt"))
        assertEquals(ResolvedListing(items = setOf()), service.getListing("maps/spleef"))
    }

    @Test
    fun nonExistentRepoRoot_get_otherReposAreUsed() = runTest {
        val service = service(repo(nonExistentRoot), repo(minimalRoot))

        val res = service.get("maps/spleef/arena", "map.txt")

        assertEquals("This is map 'arena', version 2", res.readText())
    }

    @Test
    fun nonExistentRepoRoot_getListing_otherReposAreUsed() = runTest {
        val minimal = repo(minimalRoot)
        val service = service(repo(nonExistentRoot), minimal)

        val res = service.getListing("maps/tnt_run")

        val expected = ResolvedListing(items = setOf(
            ResolvedListingItem(ListingItem("maps/tnt_run/desert"), minimal),
        ))

        assertEquals(expected, res)
    }

    // ---------------------------------------------------------------------------------------------
    // helpers
    // ---------------------------------------------------------------------------------------------

    private fun repo(root: Path) = AssetRepo(PathRepoTransport(root, logger), logger)

    private fun unreachableRepo() = AssetRepo(UnreachableRepoTransport(), logger)

    private fun service(vararg repos: AssetRepo) = AssetService(repos = repos.toList(), logger = logger)

    private fun uriOf(root: Path, path: String): URI = root.resolve(path).toUri()

    private suspend fun AssetFetchResult.readText(): String {
        val success = assertInstanceOf(AssetFetchResult.Success::class.java, this)

        return withContext(Dispatchers.IO) {
            success.data.use { it.readAllBytes().decodeToString() }
        }
    }

    private fun AssetFetchUriResult.uri(): URI {
        return assertInstanceOf(AssetFetchUriResult.Success::class.java, this).uri
    }

    /**
     * Simulates a repo that can't be accessed, e.g. due to network issues.
     */
    private class UnreachableRepoTransport : RepoTransport {
        override suspend fun getFile(path: String): InputStream {
            throw IOException("Simulated connection failure while fetching file $path")
        }

        override suspend fun getUri(path: String): URI {
            throw IOException("Simulated connection failure while fetching uri $path")
        }

        override fun toString(): String = "UnreachableRepoTransport"
    }
}
