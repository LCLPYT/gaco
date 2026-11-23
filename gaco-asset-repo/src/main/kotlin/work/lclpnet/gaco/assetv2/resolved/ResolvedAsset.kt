package work.lclpnet.gaco.assetv2.resolved

import work.lclpnet.gaco.assetv2.AssetRepo
import work.lclpnet.gaco.assetv2.model.AssetVersion

data class ResolvedAsset(
    val path: String,
    val version: AssetVersion,
    val repo: AssetRepo,
) {
    fun filePath(file: String): String {
        return buildFilePath(path, version.id, file)
    }

    companion object {
        @JvmStatic
        fun buildFilePath(assetPath: String, version: String, file: String): String {
            return "$assetPath/@$version/$file"
        }
    }
}