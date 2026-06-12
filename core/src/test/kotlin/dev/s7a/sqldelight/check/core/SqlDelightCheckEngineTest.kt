package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
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
    fun `rule override replaces severity emitted by rule`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(severity = Severity.Info))),
                config =
                    CheckConfig(
                        rules = mapOf(ruleId to RuleConfig(ruleId, Enablement.Auto, Severity.Error)),
                    ),
            )

        assertEquals(Severity.Error, diagnostics.single().severity)
    }

    @Test
    fun `auto rule is skipped when dialect applicability rejects the database`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput(ids = setOf(SourceDialectId))),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                isApplicable = { context -> TargetDialectId in context.database.dialect.ids },
                            ),
                        ),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `auto rule is skipped when target dialect rejects the database`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            ids = setOf(SourceDialectId),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(targetDialect = TargetDialectId),
                        ),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `explicit rule enablement overrides dialect auto applicability`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput(ids = setOf(SourceDialectId))),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                isApplicable = { context -> TargetDialectId in context.database.dialect.ids },
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
    fun `explicit rule enablement overrides target dialect auto applicability`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            ids = setOf(SourceDialectId),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(targetDialect = TargetDialectId),
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

    @Test
    fun `disable next line directive suppresses next line rule diagnostics`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line -- intentional
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `disable next line directive respects rule ids`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line standard:other -- wrong rule
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(
            listOf(
                qualifiedRuleId("standard:test"),
                qualifiedRuleId("core:no-redundant-suppression"),
            ),
            diagnostics.map { diagnostic -> diagnostic.ruleId },
        )
    }

    @Test
    fun `disable next line directive ignores trailing reason text`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line standard:test -- legacy export
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `disable file directive suppresses matching rule diagnostics`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-file standard:test -- intentional
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `disable file directive does not suppress diagnostics without source ranges`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-file standard:test -- intentional
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule())),
            )

        assertEquals(
            listOf(
                qualifiedRuleId("standard:test"),
                qualifiedRuleId("core:no-redundant-suppression"),
            ),
            diagnostics.map { diagnostic -> diagnostic.ruleId },
        )
    }

    @Test
    fun `disable file directive does not suppress suppression reason diagnostics`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-file
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
            )

        assertEquals(
            listOf(
                qualifiedRuleId("core:require-suppression-reason"),
                qualifiedRuleId("core:no-redundant-suppression"),
            ),
            diagnostics.map { it.ruleId },
        )
    }

    @Test
    fun `suppression reason diagnostic can be disabled`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-file
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                config =
                    CheckConfig(
                        rules =
                            mapOf(
                                qualifiedRuleId("core:require-suppression-reason") to
                                    RuleConfig(
                                        qualifiedRuleId("core:require-suppression-reason"),
                                        Enablement.Disabled,
                                        Severity.Warning,
                                    ),
                            ),
                    ),
            )

        assertEquals(
            listOf(
                qualifiedRuleId("core:no-redundant-suppression"),
            ),
            diagnostics.map { diagnostic -> diagnostic.ruleId },
        )
    }

    @Test
    fun `disable and enable directives suppress diagnostics inside a block`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable standard:test -- intentional
                                SELECT 1;
                                -- sqldelight-check-enable standard:test
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `redundant suppression rule reports unused disable next line directives`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line standard:test -- stale suppression
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(qualifiedRuleId("core:no-redundant-suppression"), diagnostics.single().ruleId)
        assertEquals(Severity.Warning, diagnostics.single().severity)
        assertEquals(1, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `redundant suppression rule can be disabled`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line standard:test -- stale suppression
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                config =
                    CheckConfig(
                        rules =
                            mapOf(
                                qualifiedRuleId("core:no-redundant-suppression") to
                                    RuleConfig(
                                        qualifiedRuleId("core:no-redundant-suppression"),
                                        Enablement.Disabled,
                                        Severity.Warning,
                                    ),
                            ),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `redundant suppression rule accepts used disable next line directives`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable-next-line standard:test -- intentional
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(testRule(rangeLine = 2)),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `redundant suppression rule accepts used block disable directives`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            content =
                                """
                                -- sqldelight-check-disable standard:test -- intentional
                                SELECT 1;
                                -- sqldelight-check-enable standard:test
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(testRule(rangeLine = 2)),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    private fun testRuleSet(rule: Rule = testRule()): RuleSetProvider =
        object : RuleSetProvider {
            override val id: RuleSetId = ruleSetId

            override fun ruleProviders(): Set<RuleProvider> = setOf(RuleProvider { rule })
        }

    private fun testRule(
        id: String = "test",
        severity: Severity = Severity.Warning,
        targetDialect: DialectId? = null,
        isApplicable: (RuleContext) -> Boolean = { true },
        message: (RuleContext) -> String = { "test diagnostic" },
        rangeLine: Int? = null,
    ): Rule =
        object : Rule {
            override val id: RuleId = RuleId(id)
            override val defaultSeverity: Severity = severity
            override val defaultEnable: Boolean = true
            override val targetDialect: DialectId? = targetDialect

            override fun isApplicable(context: RuleContext): Boolean = isApplicable.invoke(context)

            override fun run(
                context: RuleContext,
                reporter: DiagnosticReporter,
            ) {
                reporter.report(
                    RuleDiagnostic(
                        severity = defaultSeverity,
                        message = message(context),
                        file = context.file,
                        range = rangeLine?.let(::singleCharacterRange),
                        database = context.database,
                    ),
                )
            }
        }

    private fun testInput(
        ids: Set<DialectId> = setOf(TargetDialectId),
        content: String = "SELECT 1;",
    ): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect = SqlDialect(ids = ids),
                ),
            files = listOf(SourceFile(path = "src/main/sqldelight/Test.sq", content = content)),
        )

    private companion object {
        val ruleSetId = RuleSetId("standard")
        val ruleId = qualifiedRuleId("standard:test")

        val SourceDialectId: DialectId = DialectId("source")
        val TargetDialectId: DialectId = DialectId("target")
    }
}

private fun singleCharacterRange(line: Int): SourceRange =
    SourceRange(
        start = SourcePosition(line = line, column = 1),
        end = SourcePosition(line = line, column = 2),
    )

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    val delimiter = value.indexOf(':')
    require(delimiter > 0 && delimiter < value.lastIndex)
    return QualifiedRuleId(
        ruleSetId = RuleSetId(value.substring(0, delimiter)),
        ruleId = RuleId(value.substring(delimiter + 1)),
    )
}
