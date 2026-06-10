package dev.s7a.sqldelight.check.rules.standard

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.FixSafety
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for the standard rule set.
 */
class StandardRuleSetProviderTest {
    @Test
    fun `standard rule set provides built in style rules`() {
        val ruleIds =
            StandardRuleSetProvider()
                .ruleProviders()
                .map { provider -> provider.create().id }
                .toSet()

        assertEquals(
            setOf(
                RuleId("standard:final-newline"),
                RuleId("standard:keyword-case"),
                RuleId("standard:line-ending-lf"),
                RuleId("standard:max-blank-lines"),
                RuleId("standard:no-tab-indentation"),
                RuleId("standard:no-trailing-whitespace"),
            ),
            ruleIds,
        )
    }

    @Test
    fun `final newline rule reports safe fix`() {
        val diagnostics = runRule(RuleId("standard:final-newline"), "SELECT 1;")

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("\n", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `trailing whitespace rule reports safe fix`() {
        val diagnostics = runRule(RuleId("standard:no-trailing-whitespace"), "SELECT 1;  \n")

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `line ending rule reports safe fix`() {
        val diagnostics = runRule(RuleId("standard:line-ending-lf"), "SELECT 1;\r\n")

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("\n", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `tab indentation rule reports safe fix`() {
        val diagnostics = runRule(RuleId("standard:no-tab-indentation"), "\tSELECT 1;\n")

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("    ", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `max blank lines rule reports safe fix`() {
        val diagnostics = runRule(RuleId("standard:max-blank-lines"), "SELECT 1;\n\n\nSELECT 2;\n")

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals("", diagnostics.single().fixes.single().edits.single().replacement)
    }

    @Test
    fun `keyword case rule reports unsafe fix`() {
        val diagnostics = runRule(RuleId("standard:keyword-case"), "select null from player;\n")

        assertEquals(3, diagnostics.size)
        assertEquals(FixSafety.Unsafe, diagnostics.first().fixes.single().safety)
        assertEquals("SELECT", diagnostics.first().fixes.single().edits.single().replacement)
    }

    @Test
    fun `keyword case rule ignores comments and strings`() {
        val diagnostics =
            runRule(
                RuleId("standard:keyword-case"),
                "-- select from\nSELECT 'select from';\n",
            )

        assertEquals(0, diagnostics.size)
    }

    private fun runRule(
        ruleId: RuleId,
        content: String,
    ) = StandardRuleSetProvider()
        .ruleProviders()
        .map { provider -> provider.create() }
        .single { rule -> rule.id == ruleId }
        .diagnostics(content)

    private fun Rule.diagnostics(content: String): List<Diagnostic> {
        val diagnostics = mutableListOf<Diagnostic>()
        run(
            context =
                object : RuleContext {
                    override val database: DatabaseContext =
                        DatabaseContext(
                            name = "Database",
                            dialect = SqlDialect(family = DialectFamily.SQLite, displayName = "SQLite"),
                        )
                    override val file: SourceFile = SourceFile(path = "fixture.sq", content = content)
                },
            reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic },
        )
        return diagnostics
    }
}
