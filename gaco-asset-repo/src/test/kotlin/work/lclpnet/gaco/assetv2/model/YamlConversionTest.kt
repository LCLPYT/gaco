package work.lclpnet.gaco.assetv2.model

import com.charleskorn.kaml.Yaml
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class YamlConversionTest {

    private fun convert(yaml: String): Any? = Yaml.default.parseToYamlNode(yaml).toKotlin()

    @Test
    fun plainScalars_resolvedByCoreSchema() {
        val actual = convert(
            """
            str: hello
            colon: minecraft:stone
            int: 42
            negative: -7
            big: 9999999999
            hex: 0x1F
            oct: 0o17
            float: 1.5
            exp: 1e3
            inf: -.inf
            yes: true
            no: False
            nil: null
            tilde: ~
            empty:
            """.trimIndent()
        )

        val expected = mapOf(
            "str" to "hello",
            "colon" to "minecraft:stone",
            "int" to 42L,
            "negative" to -7L,
            "big" to 9999999999L,
            "hex" to 31L,
            "oct" to 15L,
            "float" to 1.5,
            "exp" to 1000.0,
            "inf" to Double.NEGATIVE_INFINITY,
            "yes" to true,
            "no" to false,
            "nil" to null,
            "tilde" to null,
            "empty" to null,
        )

        assertEquals(expected, actual)
    }

    @Test
    fun quotedScalars_keptAsString() {
        val actual = convert(
            """
            a: "42"
            b: 'true'
            c: "null"
            d: ""
            """.trimIndent()
        )

        assertEquals(mapOf("a" to "42", "b" to "true", "c" to "null", "d" to ""), actual)
    }

    @Test
    fun nestedCollections_converted() {
        val actual = convert(
            """
            list: [1, two, [3]]
            map:
              inner:
                - x: 1
              tagged: !custom 5
            """.trimIndent()
        )

        val expected = mapOf(
            "list" to listOf(1L, "two", listOf(3L)),
            "map" to mapOf(
                "inner" to listOf(mapOf("x" to 1L)),
                "tagged" to 5L,
            ),
        )

        assertEquals(expected, actual)
    }
}
