package work.lclpnet.gaco.assetv2

data class ResolvedAsset(
    val path: String,
    val version: AssetVersion,
    val repo: AssetRepo,
)