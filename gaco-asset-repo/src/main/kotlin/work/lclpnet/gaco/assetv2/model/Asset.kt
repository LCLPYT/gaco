package work.lclpnet.gaco.assetv2.model

import kotlinx.serialization.Serializable

/**
 * Content of asset.yml
 */
@Serializable
data class Asset(
    val versions: List<AssetVersion>
)
