package work.lclpnet.gaco.assetv2.resolved

/**
 * A format-agnostic listing item.
 * @param asset The asset path.
 * @param properties Arbitrary item properties. Values are one of `null`, [String], [Boolean], [Long], [Double],
 * `List<Any?>` or `Map<String, Any?>`.
 */
data class ListingItem(
    val asset: String,
    val properties: Map<String, Any?> = mapOf(),
)
