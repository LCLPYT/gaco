package work.lclpnet.gaco.assetv2.resolved

import work.lclpnet.gaco.assetv2.AssetRepo
import work.lclpnet.gaco.assetv2.model.AssetListingItem
import work.lclpnet.gaco.assetv2.model.AssetVersion

data class ResolvedListingItem(
    val item: AssetListingItem,
    val repo: AssetRepo,
)
