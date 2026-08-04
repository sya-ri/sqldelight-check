package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.Severity
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinement
import dev.s7a.sqldelight.check.rule.api.DiagnosticRefinementProvider
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleOption
import dev.s7a.sqldelight.check.rule.api.RuleOptionDeprecation
import dev.s7a.sqldelight.check.rule.api.RuleProvider
import dev.s7a.sqldelight.check.rule.api.RuleSetProvider
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.system.measureNanoTime

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
                        ruleSets = mapOf(ruleSetId to RuleSetConfig(ruleSetId, false)),
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
                        ruleSets = mapOf(ruleSetId to RuleSetConfig(ruleSetId, false)),
                        rules = mapOf(ruleId to RuleConfig(ruleId, true, Severity.Error)),
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
                        rules = mapOf(ruleId to RuleConfig(ruleId, null, Severity.Error)),
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
                        rules = mapOf(ruleId to RuleConfig(ruleId, true, Severity.Warning)),
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
                        rules = mapOf(ruleId to RuleConfig(ruleId, true, Severity.Warning)),
                    ),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `deprecated auto rule is skipped`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(deprecation = testDeprecation()))),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `deprecated explicitly enabled rule runs and emits trace warning`() {
        val trace = DeprecationTrace()
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(deprecation = testDeprecation()))),
                config =
                    CheckConfig(
                        rules = mapOf(ruleId to RuleConfig(ruleId, true, Severity.Warning)),
                    ),
                trace = trace,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(
            listOf("Database:standard:test:enabled:standard:replacement"),
            trace.deprecatedRules,
        )
    }

    @Test
    fun `deprecated explicitly disabled rule is skipped and emits trace warning`() {
        val trace = DeprecationTrace()
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(deprecation = testDeprecation()))),
                config =
                    CheckConfig(
                        rules = mapOf(ruleId to RuleConfig(ruleId, false, Severity.Warning)),
                    ),
                trace = trace,
            )

        assertEquals(emptyList(), diagnostics)
        assertEquals(
            listOf("Database:standard:test:disabled:standard:replacement"),
            trace.deprecatedRules,
        )
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
                                        true,
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
                                                        null,
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
    fun `diagnostic refinement can suppress diagnostics from another rule set`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput(content = "SELECT 1;")),
                ruleSetProviders =
                    listOf(
                        testRuleSet(testRule(rangeLine = 1)),
                        object : RuleSetProvider {
                            override val id: RuleSetId = RuleSetId("dialect")

                            override fun ruleProviders(): Set<RuleProvider> = emptySet()

                            override fun diagnosticRefinementProviders(): Set<DiagnosticRefinementProvider> =
                                setOf(
                                    DiagnosticRefinementProvider {
                                        object : DiagnosticRefinement {
                                            override val targetRuleId: QualifiedRuleId = ruleId

                                            override fun refine(
                                                context: RuleContext,
                                                diagnostic: Diagnostic,
                                            ): Diagnostic? = null
                                        }
                                    },
                                )
                        },
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `unknown configured rule option emits trace warning`() {
        val trace = RuleOptionTrace()
        SqlDelightCheckEngine().run(
            inputs = listOf(testInput()),
            ruleSetProviders =
                listOf(
                    testRuleSet(
                        testRule(options = setOf(testRuleOption("max", 120))),
                    ),
                ),
            config =
                CheckConfig(
                    rules =
                        mapOf(
                            ruleId to
                                RuleConfig(
                                    ruleId,
                                    true,
                                    Severity.Warning,
                                    options = mapOf("max" to "80", "width" to "120"),
                                ),
                        ),
                ),
            trace = trace,
        )

        assertEquals(listOf("Database:standard:test:width:max"), trace.unknownOptions)
    }

    @Test
    fun `deprecated configured rule option emits trace warning`() {
        val trace = RuleOptionTrace()
        SqlDelightCheckEngine().run(
            inputs = listOf(testInput()),
            ruleSetProviders =
                listOf(
                    testRuleSet(
                        testRule(
                            options =
                                setOf(
                                    testRuleOption(
                                        "max",
                                        120,
                                        deprecation =
                                            RuleOptionDeprecation(
                                                message = "Use lineLength.",
                                                replacement = "lineLength",
                                            ),
                                    ),
                                ),
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
                                    true,
                                    Severity.Warning,
                                    options = mapOf("max" to "80"),
                                ),
                        ),
                ),
            trace = trace,
        )

        assertEquals(listOf("Database:standard:test:max:lineLength"), trace.deprecatedOptions)
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
    fun `performance trace measures copied files and executed rules`() {
        val trace = PerformanceTrace()
        val content = "SELECT player.id, team.name FROM player JOIN team ON team.id = player.team_id;"
        val input =
            AnalysisInput(
                database = testInput().database,
                files =
                    listOf(
                        SourceFile(path = "src/main/sqldelight/TestOne.sq", content = content),
                        SourceFile(path = "src/main/sqldelight/TestTwo.sq", content = content),
                    ),
            )

        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(input),
                ruleSetProviders = listOf(testRuleSet()),
                trace = trace,
            )

        assertEquals(2, diagnostics.size)
        assertEquals(2, trace.ruleInvocations[ruleId])
        assertEquals(2, trace.phaseInvocations[AnalysisPhase.Tokenization])
        assertEquals(2, trace.phaseInvocations[AnalysisPhase.FactExtraction])
        assertTrue(trace.ruleDurations.all { duration -> duration > 0 })
        assertTrue(trace.phaseDurations.all { duration -> duration > 0 })
    }

    @Test
    fun `copied files scale without superlinear analysis cost`() {
        val content = "SELECT player.id, team.name FROM player JOIN team ON team.id = player.team_id;"
        val oneFile =
            testInput(
                content = content,
            )
        val eightFiles =
            AnalysisInput(
                database = oneFile.database,
                files =
                    (1..8).map { index ->
                        SourceFile(path = "src/main/sqldelight/Test$index.sq", content = content)
                    },
            )
        val engine = SqlDelightCheckEngine()
        val providers = listOf(testRuleSet())
        repeat(2) {
            engine.run(inputs = listOf(oneFile), ruleSetProviders = providers)
            engine.run(inputs = listOf(eightFiles), ruleSetProviders = providers)
        }

        val oneFileNanos = measureNanoTime { engine.run(inputs = listOf(oneFile), ruleSetProviders = providers) }
        val eightFilesNanos = measureNanoTime { engine.run(inputs = listOf(eightFiles), ruleSetProviders = providers) }

        assertTrue(
            eightFilesNanos < oneFileNanos * 16 + 100_000_000L,
            "oneFile=${oneFileNanos / 1_000_000}ms eightFiles=${eightFilesNanos / 1_000_000}ms",
        )
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
                                        false,
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
                                        false,
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

    @Test
    fun `baseline suppresses matching diagnostics`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 1))),
                config =
                    CheckConfig(
                        baseline =
                            Baseline(
                                setOf(
                                    BaselineEntry(
                                        database = "Database",
                                        ruleId = ruleId,
                                        path = "src/main/sqldelight/Test.sq",
                                        line = 1,
                                        column = 1,
                                        message = "test diagnostic",
                                    ),
                                ),
                            ),
                    ),
            )

        assertEquals(emptyList(), diagnostics)
    }

    @Test
    fun `baseline does not suppress diagnostics with different messages`() {
        val diagnostics =
            SqlDelightCheckEngine().run(
                inputs = listOf(testInput()),
                ruleSetProviders = listOf(testRuleSet(testRule(rangeLine = 1))),
                config =
                    CheckConfig(
                        baseline =
                            Baseline(
                                setOf(
                                    BaselineEntry(
                                        database = "Database",
                                        ruleId = ruleId,
                                        path = "src/main/sqldelight/Test.sq",
                                        line = 1,
                                        column = 1,
                                        message = "stale diagnostic",
                                    ),
                                ),
                            ),
                    ),
            )

        assertEquals(listOf(ruleId), diagnostics.map { diagnostic -> diagnostic.ruleId })
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
        deprecation: RuleDeprecation? = null,
        options: Set<RuleOption<*>> = emptySet(),
        isApplicable: (RuleContext) -> Boolean = { true },
        message: (RuleContext) -> String = { "test diagnostic" },
        rangeLine: Int? = null,
    ): Rule =
        object : Rule {
            override val id: RuleId = RuleId(id)
            override val defaultSeverity: Severity = severity
            override val defaultEnable: Boolean = true
            override val targetDialect: DialectId? = targetDialect
            override val deprecation: RuleDeprecation? = deprecation
            override val options: Set<RuleOption<*>> = options

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

    private fun testDeprecation(): RuleDeprecation =
        RuleDeprecation(
            message = "This rule has moved.",
            replacement = qualifiedRuleId("standard:replacement"),
        )

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

private class DeprecationTrace : AnalysisTrace {
    val deprecatedRules = mutableListOf<String>()

    override fun databaseFiles(
        database: DatabaseContext,
        files: List<SourceFile>,
    ) {
    }

    override fun fileRules(
        database: DatabaseContext,
        file: SourceFile,
        ruleIds: List<QualifiedRuleId>,
    ) {
    }

    override fun deprecatedRule(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        deprecation: RuleDeprecation,
        enabled: Boolean,
    ) {
        deprecatedRules +=
            listOf(
                database.name,
                ruleId.value,
                if (enabled) "enabled" else "disabled",
                deprecation.replacement?.value.orEmpty(),
            ).joinToString(":")
    }
}

private class PerformanceTrace : AnalysisTrace {
    override val collectsPerformanceMetrics: Boolean = true
    val ruleInvocations = mutableMapOf<QualifiedRuleId, Int>()
    val phaseInvocations = mutableMapOf<AnalysisPhase, Int>()
    val ruleDurations = mutableListOf<Long>()
    val phaseDurations = mutableListOf<Long>()

    override fun databaseFiles(
        database: DatabaseContext,
        files: List<SourceFile>,
    ) {
    }

    override fun fileRules(
        database: DatabaseContext,
        file: SourceFile,
        ruleIds: List<QualifiedRuleId>,
    ) {
    }

    override fun ruleTiming(
        database: DatabaseContext,
        file: SourceFile,
        ruleId: QualifiedRuleId,
        durationNanos: Long,
    ) {
        ruleInvocations[ruleId] = ruleInvocations.getOrDefault(ruleId, 0) + 1
        ruleDurations += durationNanos
    }

    override fun analysisPhaseTiming(
        database: DatabaseContext,
        file: SourceFile,
        phase: AnalysisPhase,
        durationNanos: Long,
    ) {
        phaseInvocations[phase] = phaseInvocations.getOrDefault(phase, 0) + 1
        phaseDurations += durationNanos
    }
}

private class RuleOptionTrace : AnalysisTrace {
    val unknownOptions = mutableListOf<String>()
    val deprecatedOptions = mutableListOf<String>()

    override fun databaseFiles(
        database: DatabaseContext,
        files: List<SourceFile>,
    ) {
    }

    override fun fileRules(
        database: DatabaseContext,
        file: SourceFile,
        ruleIds: List<QualifiedRuleId>,
    ) {
    }

    override fun unknownRuleOption(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        optionName: String,
        knownOptionNames: Set<String>,
    ) {
        unknownOptions +=
            listOf(
                database.name,
                ruleId.value,
                optionName,
                knownOptionNames.sorted().joinToString(","),
            ).joinToString(":")
    }

    override fun deprecatedRuleOption(
        database: DatabaseContext,
        ruleId: QualifiedRuleId,
        optionName: String,
        deprecation: RuleOptionDeprecation,
    ) {
        deprecatedOptions +=
            listOf(
                database.name,
                ruleId.value,
                optionName,
                deprecation.replacement.orEmpty(),
            ).joinToString(":")
    }
}

private fun singleCharacterRange(line: Int): SourceRange =
    SourceRange(
        start = SourcePosition(line = line, column = 1),
        end = SourcePosition(line = line, column = 2),
    )

private fun <T> testRuleOption(
    name: String,
    defaultValue: T,
    deprecation: RuleOptionDeprecation? = null,
): RuleOption<T> =
    object : RuleOption<T> {
        override val name: String = name
        override val deprecation: RuleOptionDeprecation? = deprecation

        override fun read(values: Map<String, String>): T = defaultValue
    }

private fun qualifiedRuleId(value: String): QualifiedRuleId {
    return QualifiedRuleId(value)
}
