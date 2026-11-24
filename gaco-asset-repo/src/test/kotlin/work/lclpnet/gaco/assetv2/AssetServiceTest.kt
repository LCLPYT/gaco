package work.lclpnet.gaco.assetv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.nio.file.Path
import kotlin.io.path.absolute

class AssetServiceTest {

    val minimalRoot: Path = Path.of("src/test/resources/assets/minimal")
    val logger: Logger = LoggerFactory.getLogger(AssetServiceTest::class.java)

    @Test
    fun minimal_arena_getFile() = runTest {
        val transport = PathRepoTransport(minimalRoot, logger)
        val repo = AssetRepo(transport, logger)
        val service = AssetService(repos = listOf(repo), logger = logger)

        val res = service.get(
            asset = "maps/spleef/arena",
            file = "map.txt",
        )

        if (res !is AssetFetchResult.Success) {
            fail("Expected success")
        }

        val content = withContext(Dispatchers.IO) {
            res.data.readAllBytes().decodeToString()
        }

        assertEquals("This is map 'arena', version 2", content)
    }

    @Test
    fun minimal_arena_getUri() = runTest {
        val transport = PathRepoTransport(minimalRoot, logger)
        val repo = AssetRepo(transport, logger)
        val service = AssetService(repos = listOf(repo), logger = logger)

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

    }
}