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

    @Test
    fun `auto rule is skipped when dialect applicability rejects the database`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput(DialectFamily.MySql)),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                isApplicable = { context -> context.database.dialect.family == DialectFamily.SQLite },
                            ),
                        ),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `explicit rule enablement overrides dialect auto applicability`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput(DialectFamily.MySql)),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                isApplicable = { context -> context.database.dialect.family == DialectFamily.SQLite },
                            ),
                        ),
                    ),
                config =
                    CheckConfig(
                        rules = mapOf(ruleId to RuleConfig(ruleId, Enablement.Enabled, Severity.Warning)),
                    ),
            )

        assertEquals(1, diagnostics.size)
    }

    private fun testRuleSet(rule: Rule = testRule()): RuleSetProvider =
        object : RuleSetProvider {
            override val id: RuleSetId = ruleSetId

            override fun ruleProviders(): Set<RuleProvider> = setOf(RuleProvider { rule })
        }

    private fun testRule(
        isApplicable: (RuleContext) -> Boolean = { true },
    ): Rule =
        object : Rule {
            override val id: RuleId = ruleId
            override val defaultSeverity: Severity = Severity.Warning
            override val defaultEnablement: Enablement = Enablement.Auto

            override fun isApplicable(context: RuleContext): Boolean = isApplicable.invoke(context)

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

    private fun testInput(family: DialectFamily = DialectFamily.SQLite): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect = SqlDialect(family = family, displayName = family.name),
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
