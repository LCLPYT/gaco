package work.lclpnet.gaco.assetv2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.slf4j.Logger
import work.lclpnet.gaco.assetv2.model.Asset
import java.io.InputStream
import java.net.URI

class AssetRepo(
    val transport: RepoTransport,
    val logger: Logger,
) {

    suspend fun getAsset(path: String): Asset {
        val path = path.removeSuffix("/")

        logger.debug("{}::getAsset {}", this, path)

        return withContext(Dispatchers.IO) {
            val stream = transport.getFile("$path/asset.yml")

            Yaml.default.decodeFromStream<Asset>(stream)
        }
    }

    suspend fun getFile(path: String): InputStream {
        logger.debug("{}::getFile {}", this, path)

        return transport.getFile(path)
    }

    suspend fun getUri(path: String): URI {
        logger.debug("{}::getUri {}", this, path)

        return transport.getUri(path)
    }

    override fun toString(): String {
        return "AssetRepo{$transport}"
    }
}