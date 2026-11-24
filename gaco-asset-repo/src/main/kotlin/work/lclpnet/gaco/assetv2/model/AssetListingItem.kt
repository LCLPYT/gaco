package work.lclpnet.gaco.assetv2.model

import com.charleskorn.kaml.YamlNode
import kotlinx.serialization.Serializable

@Serializable
data class AssetListingItem(
    val asset: String,
    val properties: Map<String, YamlNode> = mapOf(),
) {
}