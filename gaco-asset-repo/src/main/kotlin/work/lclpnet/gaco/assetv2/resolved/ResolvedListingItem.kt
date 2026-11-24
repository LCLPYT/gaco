package work.lclpnet.gaco.assetv2.resolved

import work.lclpnet.gaco.assetv2.AssetRepo

data class ResolvedListingItem(
    val item: ListingItem,
    val repo: AssetRepo,
)
