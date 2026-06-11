package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.core.AnalysisInput
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
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
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

    @Test
    fun `resolved rule options are exposed to rule context`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                message = { context -> context.options.getValue("max") + ":" + context.options.getValue("mode") },
                            ),
                        ),
                    ),
                config =
                    CheckConfig(
                        rules =
                            mapOf(
                                ruleId to
                                    RuleConfig(
                                        ruleId,
                                        Enablement.Enabled,
                                        Severity.Warning,
                                        options = mapOf("max" to "8", "mode" to "global"),
                                    ),
                            ),
                        databases =
                            mapOf(
                                "Database" to
                                    DatabaseConfig(
                                        name = "Database",
                                        rules =
                                            mapOf(
                                                ruleId to
                                                    RuleConfig(
                                                        ruleId,
                                                        Enablement.Auto,
                                                        Severity.Warning,
                                                        options = mapOf("max" to "12"),
                                                    ),
                                            ),
                                    ),
                            ),
                    ),
            )

        assertEquals("12:global", diagnostics.single().message)
    }

    @Test
    fun `source sql facts are exposed to rule context`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                selectPlayers:
                                SELECT player.id AS player_id, team.name AS team_name
                                FROM player
                                JOIN team ON team.id = player.team_id;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                message = { context ->
                                    val statement = context.facts.statements.single()
                                    listOf(
                                        statement.kind.name,
                                        statement.select?.resultColumns?.size,
                                        statement.tableReferences.size,
                                        statement.joins.size,
                                        statement.qualifiedReferences.joinToString(",") { reference ->
                                            reference.qualifier + "." + reference.name
                                        },
                                    ).joinToString(":")
                                },
                            ),
                        ),
                    ),
            )

        assertEquals("Select:2:2:1:player.id,team.name,team.id,player.team_id", diagnostics.single().message)
        assertEquals(SqlStatementKind.Select, SqlStatementKind.valueOf(diagnostics.single().message.substringBefore(":")))
    }

    private fun testRuleSet(rule: Rule = testRule()): RuleSetProvider =
        object : RuleSetProvider {
            override val id: RuleSetId = ruleSetId

            override fun ruleProviders(): Set<RuleProvider> = setOf(RuleProvider { rule })
        }

    private fun testRule(
        isApplicable: (RuleContext) -> Boolean = { true },
        message: (RuleContext) -> String = { "test diagnostic" },
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
                        message = message(context),
                        file = context.file,
                        range = null,
                        database = context.database,
                    ),
                )
            }
        }

    private fun testInput(
        family: DialectFamily = DialectFamily.SQLite,
        content: String = "SELECT 1;",
    ): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect = SqlDialect(family = family, displayName = family.name),
                ),
            files = listOf(SourceFile(path = "src/main/sqldelight/Test.sq", content = content)),
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
