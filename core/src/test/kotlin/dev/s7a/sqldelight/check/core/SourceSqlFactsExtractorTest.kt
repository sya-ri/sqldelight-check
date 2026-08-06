package dev.s7a.sqldelight.check.core

import dev.s7a.sqldelight.check.api.SourceFile
import dev.s7a.sqldelight.check.api.SourcePosition
import dev.s7a.sqldelight.check.api.SourceRange
import dev.s7a.sqldelight.check.api.DialectId
import dev.s7a.sqldelight.check.api.SqlDialect
import dev.s7a.sqldelight.check.api.SqlDialectSourcePattern
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatternRole.TableReferenceBoundary
import dev.s7a.sqldelight.check.api.SqlDialectSourcePatterns
import dev.s7a.sqldelight.check.rule.api.SqlStatementKind
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.system.measureNanoTime

/**
 * Tests for the source-only SQL fact extractor used before parser-backed facts
 * are available.
 */
class SourceSqlFactsExtractorTest {
    @Test
    fun `extracts labeled sq select facts`() {
        val content =
            """
            selectPlayers:
            SELECT player.id AS player_id, team.name AS team_name
            FROM player
            INNER JOIN team ON team.id = player.team_id;
            """.trimIndent()

        val statement = extract(content).statements.single()

        assertEquals(SqlStatementKind.Select, statement.kind)
        assertEquals(listOf("player", "team"), statement.tableReferences.map { reference -> reference.name })
        assertEquals(listOf("player_id", "team_name"), statement.select?.resultColumns?.map { column -> column.alias })
        assertEquals(listOf(false, false), statement.select?.resultColumns?.map { column -> column.wildcard })
        assertEquals(listOf("INNER JOIN"), statement.joins.map { join -> join.kind })
        assertEquals("team", statement.joins.single().table.name)
        assertEquals(
            listOf("player.id", "team.name", "team.id", "player.team_id"),
            statement.qualifiedReferences.map { reference -> reference.qualifier + "." + reference.name },
        )
    }

    @Test
    fun `extracts with select and derived table facts`() {
        val content =
            """
            activePlayers:
            WITH recent AS (
              SELECT player_id
              FROM player_event
            )
            SELECT recent.player_id, teams.*
            FROM (
              SELECT id
              FROM player
            ) AS recent
            LEFT OUTER JOIN team AS teams ON teams.id = recent.player_id;
            """.trimIndent()

        val statement = extract(content).statements.single()

        assertEquals(SqlStatementKind.Select, statement.kind)
        assertEquals(listOf("recent.player_id", "teams.*"), statement.select?.resultColumns?.map { column -> content.textIn(column.range).trim() })
        assertEquals(listOf(false, true), statement.select?.resultColumns?.map { column -> column.wildcard })
        assertEquals(listOf(null, "team"), statement.tableReferences.map { reference -> reference.name })
        assertEquals(listOf("recent", "teams"), statement.tableReferences.map { reference -> reference.alias })
        assertEquals(listOf(true, false), statement.tableReferences.map { reference -> reference.subquery })
        assertEquals(listOf("LEFT OUTER JOIN"), statement.joins.map { join -> join.kind })
    }

    @Test
    fun `extracts statement kinds without splitting comments or quoted text`() {
        val content =
            """
            SELECT ';' AS marker;
            -- DELETE FROM ignored;
            /* UPDATE ignored SET name = 'x'; */
            INSERT INTO player(id, name) VALUES (?, ?);
            UPDATE player SET name = 'name;still string' WHERE id = ?;
            DELETE FROM player WHERE id = ?;
            CREATE TABLE team(id INTEGER);
            ALTER TABLE team ADD COLUMN name TEXT;
            DROP TABLE old_team;
            PRAGMA foreign_keys = ON;
            """.trimIndent()

        val facts = extract(content)

        assertEquals(
            listOf(
                SqlStatementKind.Select,
                SqlStatementKind.Insert,
                SqlStatementKind.Update,
                SqlStatementKind.Delete,
                SqlStatementKind.Create,
                SqlStatementKind.Alter,
                SqlStatementKind.Drop,
                SqlStatementKind.Other,
            ),
            facts.statements.map { statement -> statement.kind },
        )
    }

    @Test
    fun `extracts top-level comma table references`() {
        val content =
            """
            SELECT player.id, team.name
            FROM player AS p, team AS t
            WHERE p.team_id = t.id;
            """.trimIndent()

        val statement = extract(content).statements.single()

        assertEquals(listOf("player", "team"), statement.tableReferences.map { reference -> reference.name })
        assertEquals(listOf("p", "t"), statement.tableReferences.map { reference -> reference.alias })
        assertTrue(statement.joins.isEmpty())
        assertEquals(
            listOf("player.id", "team.name", "p.team_id", "t.id"),
            statement.qualifiedReferences.map { reference -> reference.qualifier + "." + reference.name },
        )
    }

    @Test
    fun `ignores tokens inside quoted identifiers and comments`() {
        val content =
            """
            SELECT [from.join] AS bracketed, "team.name" AS quoted, `player.id` AS backticked
            FROM player
            -- JOIN ignored ON ignored.id = player.id
            WHERE player.name = 'team.id';
            """.trimIndent()

        val statement = extract(content).statements.single()

        assertEquals(listOf("player"), statement.tableReferences.map { reference -> reference.name })
        assertTrue(statement.joins.isEmpty())
        assertEquals(listOf("player.name"), statement.qualifiedReferences.map { reference -> reference.qualifier + "." + reference.name })
        assertFalse(statement.select?.resultColumns.orEmpty().any { column -> column.wildcard })
    }

    @Test
    fun `uses dialect source patterns for table reference boundaries`() {
        val content =
            """
            SELECT player.id
            FROM player SAMPLE bucket
            WHERE player.id = ?;
            """.trimIndent()
        val dialect =
            SqlDialect(
                ids = setOf(DialectId("sample")),
                sourcePatterns =
                    SqlDialectSourcePatterns(
                        patterns =
                            SqlDialectSourcePatterns.SourceScannerDefault.patterns +
                                SqlDialectSourcePattern.parse("SAMPLE", TableReferenceBoundary),
                    ),
            )

        val statement = extract(content, dialect).statements.single()

        assertEquals(listOf("player"), statement.tableReferences.map { reference -> reference.name })
        assertEquals(listOf(null), statement.tableReferences.map { reference -> reference.alias })
    }

    @Test
    fun `large source facts extraction exposes the performance regression`() {
        val small = performanceWorkload(16)
        val large = performanceWorkload(64)

        repeat(2) {
            extract(small)
            extract(large)
        }
        val smallNanos = measureNanoTime { extract(small) }
        val largeNanos = measureNanoTime { extract(large) }

        assertTrue(
            actual = largeNanos < smallNanos * 8 + 50_000_000L,
            message = "small=${smallNanos / 1_000_000}ms large=${largeNanos / 1_000_000}ms",
        )
    }

    @Test
    fun `wide select facts extraction scales proportionally with file size`() {
        val small = wideSelectWorkload(32)
        val large = wideSelectWorkload(256)

        repeat(3) {
            extract(small)
            extract(large)
        }
        val smallNanos = measureNanoTime { extract(small) }
        val largeNanos = measureNanoTime { extract(large) }

        // 8x more statements: O(N) → 8x, O(N²) → 64x.
        // Allow up to 16x to absorb JVM variability while still catching quadratic growth.
        assertTrue(
            actual = largeNanos < smallNanos * 16,
            message = "small=${smallNanos / 1_000_000}ms large=${largeNanos / 1_000_000}ms",
        )
    }

    private fun extract(
        content: String,
        dialect: SqlDialect = SqlDialect(ids = setOf(DialectId("default"))),
    ) = SourceSqlFactsExtractor.extract(SourceFile(path = "src/main/sqldelight/com/example/Test.sq", content = content), dialect)

    private fun wideSelectWorkload(statementCount: Int): String =
        buildString {
            repeat(statementCount) { index ->
                append("query$index:\n")
                append("SELECT ")
                append((1..20).joinToString(", ") { col -> "t.col${col}_field AS alias${col}" })
                append("\nFROM some_large_table AS t\n")
                append("JOIN other_table AS o ON o.id = t.other_id\n")
                append("JOIN third_table AS r ON r.t_id = t.id;\n")
            }
        }

    private fun performanceWorkload(statementCount: Int): String =
        buildString {
            repeat(statementCount) { index ->
                append(
                    """
                    query$index:
                    SELECT player.id, team.name, (SELECT max(score) FROM scores WHERE scores.player_id = player.id) AS best_score, player.name
                    FROM player
                    LEFT OUTER JOIN team ON team.id = player.team_id
                    WHERE player.id IN (SELECT player_id FROM roster WHERE active = 1)
                      AND player.name IN (?, ?, ?, ?);
                    """.trimIndent(),
                )
                append('\n')
            }
        }

    private fun String.textIn(range: SourceRange): String = substring(range.start.toOffsetIn(this), range.end.toOffsetIn(this))

    private fun SourcePosition.toOffsetIn(content: String): Int {
        var currentLine = 1
        var currentColumn = 1
        content.forEachIndexed { index, character ->
            if (currentLine == line && currentColumn == column) return index
            if (character == '\n') {
                currentLine++
                currentColumn = 1
            } else {
                currentColumn++
            }
        }
        return content.length
    }
}
