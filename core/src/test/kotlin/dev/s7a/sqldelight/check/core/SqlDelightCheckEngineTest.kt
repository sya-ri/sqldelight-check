package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.adapter.spi.AnalysisInput
import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for core rule execution.
 */
class SqlDelightCheckEngineTest {
    @Test
    fun `disabled rule set suppresses auto rules`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet()),
                config =
                    CheckConfig(
                        ruleSets = mapOf(ruleSetId to RuleSetConfig(ruleSetId, Enablement.Disabled)),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `rule override enables disabled rule set and changes severity`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet()),
                config =
                    CheckConfig(
                        ruleSets = mapOf(ruleSetId to RuleSetConfig(ruleSetId, Enablement.Disabled)),
                        rules = mapOf(ruleId to RuleConfig(ruleId, Enablement.Enabled, Severity.Error)),
                    ),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(Severity.Error, diagnostics.single().severity)
    }

    private fun testRuleSet(): RuleSetProvider =
        object : RuleSetProvider {
            override val id: RuleSetId = ruleSetId

            override fun ruleProviders(): Set<RuleProvider> = setOf(RuleProvider { testRule() })
        }

    private fun testRule(): Rule =
        object : Rule {
            override val id: RuleId = ruleId
            override val defaultSeverity: Severity = Severity.Warning
            override val defaultEnablement: Enablement = Enablement.Auto

            override fun run(
                context: RuleContext,
                reporter: DiagnosticReporter,
            ) {
                reporter.report(
                    Diagnostic(
                        ruleId = id,
                        severity = defaultSeverity,
                        message = "test diagnostic",
                        file = context.file,
                        range = null,
                        database = context.database,
                    ),
                )
            }
        }

    private fun testInput(): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect = SqlDialect(family = DialectFamily.SQLite, displayName = "SQLite"),
                ),
            files = listOf(SourceFile(path = "src/main/sqldelight/Test.sq", content = "SELECT 1;")),
            sourceFolders = emptyList(),
            dependencyFolders = emptyList(),
            dialectClasspath = emptyList(),
            compilerClasspath = emptyList(),
        )

    private companion object {
        val ruleSetId = RuleSetId("standard")
        val ruleId = RuleId("standard:test")
    }
}
