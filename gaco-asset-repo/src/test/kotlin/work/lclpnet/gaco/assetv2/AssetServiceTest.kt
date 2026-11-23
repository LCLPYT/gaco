package work.lclpnet.gaco.assetv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.nio.file.Path
import kotlin.io.path.absolute

class AssetServiceTest {

    val minimalRoot = Path.of("src/test/resources/assets/minimal")

    @Test
    fun minimal_arena_getFile() = runTest {
        val transport = PathRepoTransport(minimalRoot)
        val repo = AssetRepo(transport)
        val service = AssetService(repos = listOf(repo))

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
        val transport = PathRepoTransport(minimalRoot)
        val repo = AssetRepo(transport)
        val service = AssetService(repos = listOf(repo))

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
}