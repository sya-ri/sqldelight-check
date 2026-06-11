package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectCapabilities
import dev.s7a.sqldelight.check.api.DialectCapability
import dev.s7a.sqldelight.check.api.DialectFamily
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.Enablement
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.core.AnalysisInput
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
    fun `auto rule is skipped when target capability rejects the database`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            DialectFamily.MySql,
                            capabilities = setOf(DialectCapabilities.MySql),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(targetCapability = DialectCapabilities.SQLite),
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
    fun `explicit rule enablement overrides target capability auto applicability`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs =
                    listOf(
                        testInput(
                            DialectFamily.MySql,
                            capabilities = setOf(DialectCapabilities.MySql),
                        ),
                    ),
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(targetCapability = DialectCapabilities.SQLite),
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
                                -- sqldelight-check-disable-next-line
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
                                -- sqldelight-check-disable-next-line standard:other
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
            )

        assertEquals(1, diagnostics.size)
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
                                -- sqldelight-check-disable-file standard:test
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
                                -- sqldelight-check-disable-file standard:test
                                SELECT 1;
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule())),
            )

        assertEquals(1, diagnostics.size)
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
                ruleSetProviders =
                    listOf(
                        testRuleSet(
                            testRule(
                                message = { "suppression reason" },
                                rangeLine = 1,
                                id = "require-suppression-reason",
                            ),
                        ),
                    ),
            )

        assertEquals(1, diagnostics.size)
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
                                -- sqldelight-check-disable standard:test
                                SELECT 1;
                                -- sqldelight-check-enable standard:test
                                """.trimIndent(),
                        ),
                    ),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 2))),
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
        targetCapability: DialectCapability? = null,
        isApplicable: (RuleContext) -> Boolean = { true },
        message: (RuleContext) -> String = { "test diagnostic" },
        rangeLine: Int? = null,
    ): Rule =
        object : Rule {
            override val id: String = id
            override val defaultSeverity: Severity = severity
            override val defaultEnable: Boolean = true
            override val targetCapability: DialectCapability? = targetCapability

            override fun isApplicable(context: RuleContext): Boolean = isApplicable.invoke(context)

            override fun run(
                context: RuleContext,
                reporter: DiagnosticReporter,
            ) {
                reporter.report(
                    Diagnostic(
                        ruleId = RuleId(id),
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
        family: DialectFamily = DialectFamily.SQLite,
        capabilities: Set<DialectCapability> = setOf(DialectCapabilities.SQLite),
        content: String = "SELECT 1;",
    ): AnalysisInput =
        AnalysisInput(
            database =
                DatabaseContext(
                    name = "Database",
                    dialect = SqlDialect(family = family, displayName = family.name, capabilities = capabilities),
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

private fun singleCharacterRange(line: Int): SourceRange =
    SourceRange(
        start = SourcePosition(line = line, column = 1),
        end = SourcePosition(line = line, column = 2),
    )
