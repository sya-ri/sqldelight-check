package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.SqlResultColumnFacts
import dev.s7a.sqldelight.check.rule.api.SqlSelectFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import dev.s7a.sqldelight.check.rule.api.SqlTableReferenceFacts
import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentReferenceQualificationRuleTest {
    @Test
    fun `reports mixed qualified and unqualified columns in single table select`() {
        val sql =
            """
            selectPlayers:
            SELECT player.id, name
            FROM player;
            """.asSqlDelightFile()
        val diagnostics =
            ConsistentReferenceQualificationRule().diagnostics(
                sql,
                facts = sql.asFacts(tableRef = "player", resultColumns = listOf("player.id" to null, "name" to null)),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(
            "Single-table SELECT result columns should use qualification consistently.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `accepts consistently qualified columns`() {
        val sql =
            """
            selectPlayers:
            SELECT player.id, player.name
            FROM player;
            """.asSqlDelightFile()
        ConsistentReferenceQualificationRule().assertDiagnosticCount(
            sql,
            0,
            facts = sql.asFacts(tableRef = "player", resultColumns = listOf("player.id" to null, "player.name" to null)),
        )
    }

    @Test
    fun `accepts consistently unqualified columns`() {
        val sql =
            """
            selectPlayers:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile()
        ConsistentReferenceQualificationRule().assertDiagnosticCount(
            sql,
            0,
            facts = sql.asFacts(tableRef = "player", resultColumns = listOf("id" to null, "name" to null)),
        )
    }

    @Test
    fun `accepts mixed columns in multi table select`() {
        val sql =
            """
            selectPlayers:
            SELECT player.id, name
            FROM player
            JOIN team ON team.id = player.team_id;
            """.asSqlDelightFile()
        ConsistentReferenceQualificationRule().assertDiagnosticCount(
            sql,
            0,
            facts =
                sql.asFacts(
                    tableRef = "player",
                    extraTableRefs = listOf("team"),
                    resultColumns = listOf("player.id" to null, "name" to null),
                ),
        )
    }

    @Test
    fun `ignores expressions and wildcard targets`() {
        val sql =
            """
            selectPlayers:
            SELECT player.id, lower(name) AS name, *
            FROM player;
            """.asSqlDelightFile()
        ConsistentReferenceQualificationRule().assertDiagnosticCount(
            sql,
            0,
            facts =
                sql.asFacts(
                    tableRef = "player",
                    resultColumns =
                        listOf(
                            "player.id" to null,
                            "lower(name) AS name" to "name",
                            "*" to null,
                        ),
                    wildcardColumns = setOf("*"),
                ),
        )
    }
}

private fun String.asFacts(
    tableRef: String,
    resultColumns: List<Pair<String, String?>>,
    extraTableRefs: List<String> = emptyList(),
    wildcardColumns: Set<String> = emptySet(),
): SqlFacts {
    val allTableRefs = listOf(tableRef) + extraTableRefs
    return SqlFacts(
        statements =
            listOf(
                SqlStatementFacts(
                    kind = SqlStatementKind.Select,
                    range = rangeAtOffsets(0, length),
                    select =
                        SqlSelectFacts(
                            selectListRange = rangeFor("SELECT"),
                            resultColumns =
                                resultColumns.map { (text, alias) ->
                                    SqlResultColumnFacts(
                                        range = rangeFor(text),
                                        alias = alias,
                                        wildcard = text in wildcardColumns,
                                    )
                                },
                        ),
                    tableReferences =
                        allTableRefs.map { reference ->
                            SqlTableReferenceFacts(range = rangeFor(reference), name = reference.substringAfterLast('.'))
                        },
                ),
            ),
    )
}

private fun String.rangeFor(text: String): SourceRange {
    val start = indexOf(text)
    require(start >= 0) { "Missing test text: $text" }
    return rangeAtOffsets(start, start + text.length)
}

private fun String.rangeAtOffsets(
    startOffset: Int,
    endOffset: Int,
): SourceRange = SourceRange(start = positionAt(startOffset), end = positionAt(endOffset))

private fun String.positionAt(offset: Int): SourcePosition {
    var line = 1
    var column = 1
    for (index in 0..<offset.coerceAtMost(length)) {
        if (this[index] == '\n') {
            line++
            column = 1
        } else {
            column++
        }
    }
    return SourcePosition(line = line, column = column)
}
