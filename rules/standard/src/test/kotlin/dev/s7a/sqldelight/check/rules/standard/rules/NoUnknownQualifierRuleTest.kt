package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.rule.api.SqlFacts
import dev.s7a.sqldelight.check.rule.api.SqlQualifiedReferenceFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementFacts
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import dev.s7a.sqldelight.check.rule.api.SqlTableReferenceFacts
import kotlin.test.Test
import kotlin.test.assertEquals

class NoUnknownQualifierRuleTest {
    @Test
    fun `reports qualifiers that are not visible in from clause`() {
        val sql =
            """
            selectPlayers:
            SELECT missing.id
            FROM player;
            """.asSqlDelightFile()
        val diagnostics =
            NoUnknownQualifierRule().diagnostics(
                sql,
                facts = sql.asSqlFacts(tableRefs = listOf("player"), qualifiedRefs = listOf("missing.id")),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(
            "Qualifier 'missing' is not declared by a table reference in this statement.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `accepts table aliases and table names`() {
        val sql =
            """
            selectPlayers:
            SELECT p.id, team.name
            FROM player AS p
            JOIN team ON team.id = p.team_id;
            """.asSqlDelightFile()
        NoUnknownQualifierRule().assertDiagnosticCount(
            sql,
            0,
            facts =
                sql.asSqlFacts(
                    tableRefs = listOf("player AS p", "team"),
                    qualifiedRefs = listOf("p.id", "team.name", "team.id", "p.team_id"),
                ),
        )
    }

    @Test
    fun `ignores schema qualified table references`() {
        val sql =
            """
            selectPlayers:
            SELECT p.id
            FROM main.player AS p;
            """.asSqlDelightFile()
        NoUnknownQualifierRule().assertDiagnosticCount(
            sql,
            0,
            facts = sql.asSqlFacts(tableRefs = listOf("main.player AS p"), qualifiedRefs = listOf("p.id", "main.player")),
        )
    }

    @Test
    fun `ignores statements without table references`() {
        val sql =
            """
            selectType:
            SELECT kotlin.Int;
            """.asSqlDelightFile()
        NoUnknownQualifierRule().assertDiagnosticCount(
            sql,
            0,
            facts = sql.asSqlFacts(tableRefs = emptyList(), qualifiedRefs = listOf("kotlin.Int")),
        )
    }
}

private fun String.asSqlFacts(
    tableRefs: List<String>,
    qualifiedRefs: List<String>,
): SqlFacts {
    val content = this
    return SqlFacts(
        statements =
            listOf(
                SqlStatementFacts(
                    kind = SqlStatementKind.Select,
                    range = content.rangeAtOffsets(0, content.length),
                    tableReferences = tableRefs.map { reference -> content.tableReferenceFacts(reference) },
                    qualifiedReferences = qualifiedRefs.map { reference -> content.qualifiedReferenceFacts(reference) },
                ),
            ),
    )
}

private fun String.tableReferenceFacts(reference: String): SqlTableReferenceFacts {
    val range = rangeFor(reference)
    val tokens = reference.split(Regex("\\s+")).filter { token -> token.isNotBlank() }
    val alias = tokens.dropWhile { token -> !token.equals("as", ignoreCase = true) }.getOrNull(1)
    val name = tokens.first().substringAfterLast('.')
    return SqlTableReferenceFacts(range = range, name = name, alias = alias)
}

private fun String.qualifiedReferenceFacts(reference: String): SqlQualifiedReferenceFacts {
    val range = rangeFor(reference)
    return SqlQualifiedReferenceFacts(
        range = range,
        qualifier = reference.substringBefore('.'),
        name = reference.substringAfter('.'),
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
