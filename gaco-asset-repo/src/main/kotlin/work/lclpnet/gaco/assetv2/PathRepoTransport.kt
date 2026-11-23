package work.lclpnet.gaco.assetv2

import java.io.InputStream
import java.net.URI
import java.nio.file.Path
import kotlin.io.path.inputStream

class PathRepoTransport(
    val root: Path,
): RepoTransport {

    override suspend fun getFile(path: String): InputStream {
        return root.resolve(path).inputStream()
    }

    override suspend fun getUri(path: String): URI {
        return root.resolve(path).toUri()
    }
}