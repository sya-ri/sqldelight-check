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
