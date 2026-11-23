package work.lclpnet.gaco.assetv2

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.nio.file.Path

class AssetServiceTest {

    @Test
    fun minimal_arena_getFile() = runTest {
        val transport = PathRepoTransport(Path.of("src/test/resources/assets/minimal"))
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
}