package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class SpaceAfterCommaRuleTest {
    @Test
    fun `reports safe fix in sq named query projection`() {
        val diagnostics =
            SpaceAfterCommaRule().diagnostics(
                """
                selectAll:
                SELECT id,name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.single().replacement)
        SpaceAfterCommaRule().assertAllFixes(
            """
            selectAll:
            SELECT id,name
            FROM player;
            """.asSqlDelightFile(),
            """
            selectAll:
            SELECT id, name
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `collapses repeated spaces after comma in insert query`() {
        val content =
            """
            insertPlayer:
            INSERT INTO player(id,   name, score)
            VALUES (?, ?, ?);
            """.asSqlDelightFile()

        assertEquals(" ", SpaceAfterCommaRule().singleReplacement(content))
        SpaceAfterCommaRule().assertAllFixes(
            content,
            """
            insertPlayer:
            INSERT INTO player(id, name, score)
            VALUES (?, ?, ?);
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `accepts comma followed by one space in clean sq file`() {
        SpaceAfterCommaRule().assertDiagnosticCount(cleanPlayerSq, 0)
    }

    @Test
    fun `accepts comma followed by newline in sq projection`() {
        val content =
            """
            selectAll:
            SELECT id,
              name,
              score
            FROM player;
            """.asSqlDelightFile()

        SpaceAfterCommaRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `accepts trailing comma before closing parenthesis in sqlite sq`() {
        val content =
            """
            insertPlayer:
            INSERT INTO player(id,)
            VALUES (?,);
            """.asSqlDelightFile()

        SpaceAfterCommaRule().assertDiagnosticCount(content, 0)
    }

    @Test
    fun `ignores comments and strings in sq files`() {
        val diagnostics =
            SpaceAfterCommaRule().diagnostics(
                """
                selectLiteral:
                -- SELECT id,name
                SELECT 'id,name';
                """.asSqlDelightFile(),
            )

        assertEquals(0, diagnostics.size)
    }

    @Test
    fun `ignores quoted identifiers`() {
        SpaceAfterCommaRule().assertDiagnosticCount(
            """
            selectQuoted:
            SELECT "id,name", `id,name`, [id,name]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
