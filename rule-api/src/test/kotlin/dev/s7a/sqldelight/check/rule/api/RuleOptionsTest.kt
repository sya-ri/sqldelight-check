package dev.s7a.sqldelight.check.rule.api

import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.Severity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RuleOptionsTest {
    @Test
    fun `reads typed options`() {
        val rule = TestRule()
        val options =
            RuleOptions(
                mapOf(
                    "max" to "80",
                    "enabled" to "true",
                    "words" to "alpha, beta,,gamma",
                    "name" to "custom",
                    "optionalName" to "custom-optional",
                    "title" to "Custom Title",
                    "offset" to "-2",
                    "optionalLimit" to "42",
                    "padding" to "2",
                    "size" to "9223372036854775807",
                    "ids" to "1, 2,,3",
                    "positiveIds" to "4, 5",
                    "ordinals" to "0, 1",
                    "bigIds" to "9223372036854775807",
                    "mode" to "Relaxed",
                    "modes" to "Strict, Relaxed",
                    "keyedMode" to "relaxed-mode",
                    "keyedModes" to "strict-mode, relaxed-mode",
                ),
            )

        assertEquals(80, rule.readMax(options))
        assertTrue(rule.readEnabled(options))
        assertEquals(listOf("alpha", "beta", "gamma"), rule.readWords(options))
        assertEquals("custom", rule.readName(options))
        assertEquals("custom-optional", rule.readOptionalName(options))
        assertEquals("Custom Title", rule.readTitle(options))
        assertEquals(-2, rule.readOffset(options))
        assertEquals(42, rule.readOptionalLimit(options))
        assertEquals(2, rule.readPadding(options))
        assertEquals(Long.MAX_VALUE, rule.readSize(options))
        assertEquals(listOf(1, 2, 3), rule.readIds(options))
        assertEquals(listOf(4, 5), rule.readPositiveIds(options))
        assertEquals(listOf(0, 1), rule.readOrdinals(options))
        assertEquals(listOf(Long.MAX_VALUE), rule.readBigIds(options))
        assertEquals(Mode.Relaxed, rule.readMode(options))
        assertEquals(listOf(Mode.Strict, Mode.Relaxed), rule.readModes(options))
        assertEquals(KeyedMode.Relaxed, rule.readKeyedMode(options))
        assertEquals(listOf(KeyedMode.Strict, KeyedMode.Relaxed), rule.readKeyedModes(options))
    }

    @Test
    fun `uses typed defaults`() {
        val rule = TestRule()
        val options = RuleOptions()

        assertEquals(120, rule.readMax(options))
        assertFalse(rule.readEnabled(options))
        assertNull(rule.readWords(options))
        assertEquals("default", rule.readName(options))
        assertNull(rule.readOptionalName(options))
        assertEquals("title", rule.readTitle(options))
        assertEquals(0, rule.readOffset(options))
        assertNull(rule.readOptionalLimit(options))
        assertEquals(0, rule.readPadding(options))
        assertEquals(0L, rule.readSize(options))
        assertEquals(listOf(1), rule.readIds(options))
        assertNull(rule.readPositiveIds(options))
        assertNull(rule.readOrdinals(options))
        assertNull(rule.readBigIds(options))
        assertEquals(Mode.Strict, rule.readMode(options))
        assertNull(rule.readModes(options))
        assertEquals(KeyedMode.Strict, rule.readKeyedMode(options))
        assertNull(rule.readKeyedModes(options))
    }

    @Test
    fun `rejects invalid typed option values`() {
        val rule = TestRule()
        val options = RuleOptions(mapOf("max" to "zero"))

        assertFailsWith<IllegalArgumentException> {
            rule.readMax(options)
        }
    }

    @Test
    fun `keeps raw string access for custom rules`() {
        val options = RuleOptions(mapOf("message" to "Avoid SELECT *"))

        assertEquals("Avoid SELECT *", options["message"])
        assertEquals("Avoid SELECT *", options.getValue("message"))
        assertTrue("message" in options)
        assertEquals(setOf("message"), options.names)
    }

    @Test
    fun `configurable rule declares options through delegates`() {
        val rule = TestRule()

        assertEquals(
            setOf(
                "max",
                "enabled",
                "words",
                "name",
                "optionalName",
                "title",
                "offset",
                "optionalLimit",
                "padding",
                "size",
                "ids",
                "positiveIds",
                "ordinals",
                "bigIds",
                "mode",
                "modes",
                "keyedMode",
                "keyedModes",
            ),
            rule.options.mapTo(linkedSetOf()) { option -> option.name },
        )
        assertEquals(80, rule.readMax(RuleOptions(mapOf("max" to "80"))))
        assertFalse(rule.readEnabled(RuleOptions()))
        assertEquals(Mode.Relaxed, rule.readMode(RuleOptions(mapOf("mode" to "relaxed"))))
    }

    @Test
    fun `configurable rule rejects duplicate option names`() {
        assertFailsWith<IllegalArgumentException> {
            DuplicateOptionRule()
                .options
        }
    }

    private class TestRule : Rule {
        private val maxOption by positiveIntOption("max", 120)
        private val enabledOption by booleanOption("enabled", false)
        private val wordsOption by stringListOption("words")
        private val nameOption by stringOption("name", "default")
        private val optionalNameOption by stringOption("optionalName")
        private val titleOption by nonBlankStringOption("title", "title")
        private val offsetOption by intOption("offset", 0)
        private val optionalLimitOption by option("optionalLimit") { value -> value.toInt() }
        private val paddingOption by nonNegativeIntOption("padding", 0)
        private val sizeOption by longOption("size", 0L)
        private val idsOption by listOption("ids", listOf(1)) { value -> value.toInt() }
        private val positiveIdsOption by positiveIntListOption("positiveIds")
        private val ordinalsOption by nonNegativeIntListOption("ordinals")
        private val bigIdsOption by longListOption("bigIds")
        private val modeOption by enumOption("mode", Mode.Strict)
        private val modesOption by enumListOption<Mode>("modes")
        private val keyedModeOption by keyedEnumOption("keyedMode", KeyedMode.Strict)
        private val keyedModesOption by keyedEnumListOption<KeyedMode>("keyedModes")

        override val id: RuleId = RuleId("test")
        override val defaultSeverity: Severity = Severity.Warning

        fun readMax(options: RuleOptions): Int = options[maxOption]

        fun readEnabled(options: RuleOptions): Boolean = options[enabledOption]

        fun readWords(options: RuleOptions): List<String>? = options[wordsOption]

        fun readName(options: RuleOptions): String = options[nameOption]

        fun readOptionalName(options: RuleOptions): String? = options[optionalNameOption]

        fun readTitle(options: RuleOptions): String = options[titleOption]

        fun readOffset(options: RuleOptions): Int = options[offsetOption]

        fun readOptionalLimit(options: RuleOptions): Int? = options[optionalLimitOption]

        fun readPadding(options: RuleOptions): Int = options[paddingOption]

        fun readSize(options: RuleOptions): Long = options[sizeOption]

        fun readIds(options: RuleOptions): List<Int> = options[idsOption]

        fun readPositiveIds(options: RuleOptions): List<Int>? = options[positiveIdsOption]

        fun readOrdinals(options: RuleOptions): List<Int>? = options[ordinalsOption]

        fun readBigIds(options: RuleOptions): List<Long>? = options[bigIdsOption]

        fun readMode(options: RuleOptions): Mode = options[modeOption]

        fun readModes(options: RuleOptions): List<Mode>? = options[modesOption]

        fun readKeyedMode(options: RuleOptions): KeyedMode = options[keyedModeOption]

        fun readKeyedModes(options: RuleOptions): List<KeyedMode>? = options[keyedModesOption]

        override fun run(
            context: RuleContext,
            reporter: DiagnosticReporter,
        ) {
        }
    }

    private class DuplicateOptionRule : Rule {
        @Suppress("unused")
        private val maxOption by positiveIntOption("max", 120)

        @Suppress("unused")
        private val otherMaxOption by intOption("max", 80)

        override val id: RuleId = RuleId("duplicate")
        override val defaultSeverity: Severity = Severity.Warning

        override fun run(
            context: RuleContext,
            reporter: DiagnosticReporter,
        ) {
        }
    }

    private enum class Mode {
        Strict,
        Relaxed,
    }

    private enum class KeyedMode(
        override val key: String,
    ) : KeyedEnum {
        Strict("strict-mode"),
        Relaxed("relaxed-mode"),
    }
}
