package work.lclpnet.gaco.assetv2

import com.charleskorn.kaml.Yaml
import com.charleskorn.kaml.decodeFromStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.net.URI

class AssetRepo(
    val transport: RepoTransport,
) {

    suspend fun getAsset(path: String): Asset {
        val path = path.removeSuffix("/")

        return withContext(Dispatchers.IO) {
            val stream = transport.getFile("$path/asset.yml")
            Yaml.default.decodeFromStream<Asset>(stream)
        }
    }

    suspend fun getFile(path: String): InputStream {
        return transport.getFile(path)
    }

    suspend fun getUri(path: String): URI {
        return transport.getUri(path)
    }
}