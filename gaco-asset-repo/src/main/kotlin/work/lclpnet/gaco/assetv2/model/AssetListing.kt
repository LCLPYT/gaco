package work.lclpnet.gaco.assetv2.model

import kotlinx.serialization.Serializable

/**
 * Content of index.yml, representing an asset listing.
 */
@Serializable
data class AssetListing(
    val items: Set<AssetListingItem>,
)