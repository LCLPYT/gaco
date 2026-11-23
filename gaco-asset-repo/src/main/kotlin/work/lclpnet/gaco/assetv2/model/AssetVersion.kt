package work.lclpnet.gaco.assetv2.model

import kotlinx.serialization.Serializable

@Serializable
data class AssetVersion(
    val id: String,
    val depends: Map<String, String> = mapOf(),
)