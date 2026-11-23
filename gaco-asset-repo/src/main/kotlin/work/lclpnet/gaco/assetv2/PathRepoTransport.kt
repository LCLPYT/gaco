package work.lclpnet.gaco.assetv2

import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

class PathRepoTransport(
    val root: Path,
): RepoTransport {

    override suspend fun getFile(path: String): InputStream {
        return root.resolve(path).inputStream()
    }
}