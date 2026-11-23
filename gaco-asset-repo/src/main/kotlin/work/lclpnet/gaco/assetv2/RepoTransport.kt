package work.lclpnet.gaco.assetv2

import java.io.InputStream

interface RepoTransport {

    suspend fun getFile(path: String): InputStream
}