package work.lclpnet.gaco.assetv2

import net.fabricmc.loader.api.SemanticVersion
import net.fabricmc.loader.api.Version

sealed interface VersioningScheme<T : Any> {

    fun convertValue(v: Any): T

    fun compareRaw(x: Any, y: Any): Int {
        val vx = convertValue(x)
        val vy = convertValue(y)

        return compare(vx, vy)
    }

    fun compare(x: T, y: T): Int

    object IntScheme : VersioningScheme<Int> {
        override fun convertValue(v: Any): Int = when(v) {
            is Number -> v.toInt()
            is String -> v.toInt()
            else -> throw IllegalArgumentException("Value not convertible to int: $v")
        }

        override fun compare(x: Int, y: Int): Int = x.compareTo(y)
    }

    object SemverScheme : VersioningScheme<String> {

        override fun convertValue(v: Any): String = when (v) {
            is String -> v
            else -> throw IllegalArgumentException("Value is not a string: $v")
        }

        override fun compare(x: String, y: String): Int {
            val sx = SemanticVersion.parse(x)
            val sy = SemanticVersion.parse(y)

            return sx.compareTo(sy as Version)
        }
    }

    companion object {
        @JvmStatic
        fun detect(x: Any): VersioningScheme<out Any>? = when (x) {
            is Int -> IntScheme
            is String if x.toIntOrNull() != null -> IntScheme
            is String if SemanticVersion.parse(x) != null -> SemverScheme
            else -> null
        }
    }
}
