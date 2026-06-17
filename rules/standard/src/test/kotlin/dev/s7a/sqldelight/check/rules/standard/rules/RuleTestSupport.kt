@file:OptIn(dev.s7a.sqldelight.check.api.InternalSqldelightCheckApi::class)

package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.DatabaseContext
import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.Diagnostic
import dev.s7a.sqldelight.check.api.QualifiedRuleId
import dev.s7a.sqldelight.check.api.RuleDiagnostic
import dev.s7a.sqldelight.check.api.RuleId
import dev.s7a.sqldelight.check.api.RuleSetId
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourceBlockPatterns
import dev.s7a.sqldelight.check.api.SqlDialectSourcePairedBlockPattern
import dev.s7a.sqldelight.check.api.SqlDialectSourceParenthesisDepthTerms
import dev.s7a.sqldelight.check.api.SqlDialectSourceParenthesizedBlockPattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.api.SqlSourceBlockKind
import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.TextEdit
import dev.s7a.sqldelight.check.rule.api.DiagnosticReporter
import dev.s7a.sqldelight.check.rule.api.Rule
import dev.s7a.sqldelight.check.rule.api.RuleContext
import dev.s7a.sqldelight.check.rule.api.RuleOptions
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import kotlin.test.assertEquals

internal const val PLAYER_SQ_PATH: String = "src/main/sqldelight/com/example/Player.sq"

internal const val MIGRATION_SQM_PATH: String = "src/main/sqldelight/com/example/1.sqm"

internal val cleanPlayerSq: String =
    """
    import com.example.PlayerId;

    CREATE TABLE player (
        id INTEGER AS PlayerId NOT NULL PRIMARY KEY,
        name TEXT NOT NULL,
        score INTEGER NOT NULL DEFAULT 0
    );

    CREATE INDEX player_name ON player(name);

    selectAll:
    SELECT id, name, score
    FROM player
    ORDER BY name
    LIMIT 100;

    selectById:
    SELECT id, name, score
    FROM player
    WHERE id = :id;

    insertPlayer:
    INSERT INTO player(id, name, score)
    VALUES (:id, :name, :score);
    """.trimIndent() + "\n"

internal val cleanMigrationSqm: String =
    """
    CREATE TABLE player (
        id INTEGER NOT NULL PRIMARY KEY,
        name TEXT NOT NULL
    );

    ALTER TABLE player ADD COLUMN score INTEGER NOT NULL DEFAULT 0;
    """.trimIndent() + "\n"

internal val braceSubqueryDialect: SqlDialect =
    SqlDialect(
        ids = setOf(DialectId("default")),
        sourcePatterns =
            SqlDialectSourcePatterns(
                blockPatterns =
                    SqlDialectSourceBlockPatterns(
                        parenthesisDepthTerms =
                            setOf(
                                SqlDialectSourceParenthesisDepthTerms(
                                    openTerm = "{",
                                    closeTerm = "}",
                                ),
                            ),
                        parenthesizedBlocks =
                            setOf(
                                SqlDialectSourceParenthesizedBlockPattern(
                                    openTerm = "{",
                                    closeTerm = "}",
                                    defaultKind = SqlSourceBlockKind.ParenthesizedExpression,
                                    innerStartRoles = setOf(SqlDialectSourcePatternRole.SelectListStart),
                                    innerStartKind = SqlSourceBlockKind.Subquery,
                                ),
                            ),
                    ),
            ),
    )

internal val atomicCaseDialect: SqlDialect =
    SqlDialect(
        ids = setOf(DialectId("default")),
        sourcePatterns =
            SqlDialectSourcePatterns(
                blockPatterns =
                    SqlDialectSourceBlockPatterns(
                        pairedBlocks =
                            setOf(
                                SqlDialectSourcePairedBlockPattern.parse(
                                    startExpression = "BEGIN ATOMIC",
                                    endExpression = "END",
                                    kind = SqlSourceBlockKind.CaseExpression,
                                ),
                            ),
                    ),
            ),
    )

internal fun Rule.diagnostics(
    content: String,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
    facts: SqlFacts = SqlFacts(),
    dialect: SqlDialect = SqlDialect(ids = setOf(DialectId("default"))),
): List<Diagnostic> {
    val diagnostics = mutableListOf<Diagnostic>()
    run(
        context =
            object : RuleContext {
                override val database: DatabaseContext =
                    DatabaseContext(
                        name = "Database",
                        dialect = dialect,
                    )
                override val file: SourceFile = SourceFile(path = path, content = content)
                override val options: RuleOptions = RuleOptions(options)
                override val facts: SqlFacts = facts
            },
        reporter = DiagnosticReporter { diagnostic -> diagnostics += diagnostic.withRuleSetPrefix("standard", id) },
    )
    return diagnostics
}

private fun RuleDiagnostic.withRuleSetPrefix(
    prefix: String,
    ruleId: RuleId,
): Diagnostic =
    Diagnostic(
        severity = severity,
        message = message,
        file = file,
        range = range,
        database = database,
        ruleId = QualifiedRuleId(RuleSetId(prefix), ruleId),
        fixes = fixes,
    )

internal fun Rule.assertDiagnosticCount(
    content: String,
    expected: Int,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
    facts: SqlFacts = SqlFacts(),
    dialect: SqlDialect = SqlDialect(ids = setOf(DialectId("default"))),
) {
    assertEquals(expected, diagnostics(content, path, options, facts, dialect).size)
}

internal fun Rule.singleReplacement(
    content: String,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
): String =
    diagnostics(content, path, options)
        .single()
        .fixes
        .single()
        .edits
        .single()
        .replacement

internal fun Rule.applySingleFix(
    content: String,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
): String = content.applyEdit(diagnostics(content, path, options).single().fixes.single().edits.single())

internal fun Rule.applyAllFixes(
    content: String,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
    facts: SqlFacts = SqlFacts(),
    dialect: SqlDialect = SqlDialect(ids = setOf(DialectId("default"))),
): String {
    val edits =
        diagnostics(content, path, options, facts, dialect)
            .flatMap { diagnostic -> diagnostic.fixes.single().edits }
            .sortedByDescending { edit -> content.offsetAt(edit.range.start) }
    return edits.fold(content) { current, edit -> current.applyEdit(edit) }
}

internal fun Rule.assertAllFixes(
    content: String,
    expected: String,
    path: String = PLAYER_SQ_PATH,
    options: Map<String, String> = emptyMap(),
    facts: SqlFacts = SqlFacts(),
    dialect: SqlDialect = SqlDialect(ids = setOf(DialectId("default"))),
) {
    val fixed = applyAllFixes(content, path, options, facts, dialect)
    assertEquals(expected, fixed)
    assertDiagnosticCount(fixed, 0, path, options, facts, dialect)
}

private fun String.applyEdit(edit: TextEdit): String {
    val startOffset = offsetAt(edit.range.start)
    val endOffset = offsetAt(edit.range.end)
    return replaceRange(startOffset, endOffset, edit.replacement)
}

private fun String.offsetAt(position: SourcePosition): Int {
    var line = 1
    var column = 1
    for (index in indices) {
        if (line == position.line && column == position.column) return index
        if (this[index] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return length
}

internal fun String.asSqlDelightFile(): String = trimIndent() + "\n"

internal fun String.withTabs(): String = replace("<TAB>", "\t")

internal fun String.withSpaces(): String = replace("<SP>", " ")
