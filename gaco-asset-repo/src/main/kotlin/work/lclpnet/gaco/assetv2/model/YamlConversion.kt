package work.lclpnet.gaco.assetv2.model

import com.charleskorn.kaml.*
import work.lclpnet.gaco.assetv2.resolved.ListingItem

internal fun AssetListingItem.toListingItem(): ListingItem = ListingItem(
    asset = asset,
    properties = properties.mapValues { (_, node) -> node.toKotlin() },
)

/**
 * Converts a [YamlNode] into plain Kotlin types.
 * Plain (unquoted) scalars are resolved according to the YAML 1.2 core schema,
 * quoted scalars are always kept as [String].
 */
internal fun YamlNode.toKotlin(): Any? = when (this) {
    is YamlNull -> null
    is YamlScalar -> if (plain) resolvePlainScalar(content) else content
    is YamlList -> items.map { it.toKotlin() }
    is YamlMap -> entries.entries.associate { (key, value) -> key.content to value.toKotlin() }
    is YamlTaggedNode -> innerNode.toKotlin()
}

private val NULL = Regex("null|Null|NULL|~|")
private val TRUE = Regex("true|True|TRUE")
private val FALSE = Regex("false|False|FALSE")
private val INT_DEC = Regex("[-+]?[0-9]+")
private val INT_OCT = Regex("0o[0-7]+")
private val INT_HEX = Regex("0x[0-9a-fA-F]+")
private val FLOAT = Regex("[-+]?(\\.[0-9]+|[0-9]+(\\.[0-9]*)?)([eE][-+]?[0-9]+)?")
private val INF = Regex("[-+]?\\.(inf|Inf|INF)")
private val NAN = Regex("\\.(nan|NaN|NAN)")

private fun resolvePlainScalar(content: String): Any? = when {
    NULL.matches(content) -> null
    TRUE.matches(content) -> true
    FALSE.matches(content) -> false
    INT_DEC.matches(content) -> content.toLongOrNull() ?: content.toDouble()
    INT_OCT.matches(content) -> content.substring(2).toLongOrNull(8) ?: content
    INT_HEX.matches(content) -> content.substring(2).toLongOrNull(16) ?: content
    FLOAT.matches(content) -> content.toDouble()
    INF.matches(content) -> if (content.startsWith('-')) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
    NAN.matches(content) -> Double.NaN
    else -> content
}
