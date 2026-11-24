package work.lclpnet.gaco.assetv2

import org.slf4j.Logger
import java.io.InputStream
import java.net.URI
import java.nio.file.NoSuchFileException
import java.nio.file.Path
import kotlin.io.path.inputStream

class PathRepoTransport(
    val root: Path,
    val logger: Logger,
): RepoTransport {

    override suspend fun getFile(path: String): InputStream? {
        logger.info("{}::getFile {}", this, path)

        val file = root.resolve(path)

        return try {
            file.inputStream()
        } catch (_: NoSuchFileException) {
            null
        }
    }

    override suspend fun getUri(path: String): URI {
        logger.debug("{}::getUri {}", this, path)

        return root.resolve(path).toUri()
    }

    override fun toString(): String {
        return "PathRepoTransport{root=$root}";
    }
}