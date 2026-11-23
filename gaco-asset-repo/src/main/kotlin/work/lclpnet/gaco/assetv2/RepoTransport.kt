package work.lclpnet.gaco.assetv2

import java.io.InputStream
import java.net.URI

interface RepoTransport {

    suspend fun getFile(path: String): InputStream

    suspend fun getUri(path: String): URI
}