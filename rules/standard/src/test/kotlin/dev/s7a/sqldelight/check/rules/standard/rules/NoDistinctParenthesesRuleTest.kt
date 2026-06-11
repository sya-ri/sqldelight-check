package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoDistinctParenthesesRuleTest {
    @Test
    fun `reports safe fix for distinct identifier parentheses`() {
        val diagnostics =
            NoDistinctParenthesesRule().diagnostics(
                """
                selectNames:
                SELECT DISTINCT(name)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.first().replacement)
    }

    @Test
    fun `reports safe fix for dotted identifier in migration files`() {
        val diagnostics =
            NoDistinctParenthesesRule().diagnostics(
                """
                INSERT INTO player_name_snapshot(name)
                SELECT DISTINCT(player.name)
                FROM player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.first().replacement)
    }

    @Test
    fun `reports safe fix for distinct star`() {
        val diagnostics =
            NoDistinctParenthesesRule().diagnostics(
                """
                selectRows:
                SELECT DISTINCT(*)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" ", diagnostics.single().fixes.single().edits.first().replacement)
    }

    @Test
    fun `safe fix preserves existing whitespace before parentheses`() {
        val diagnostics =
            NoDistinctParenthesesRule().diagnostics(
                """
                selectNames:
                SELECT DISTINCT (name)
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("", diagnostics.single().fixes.single().edits.first().replacement)
    }

    @Test
    fun `reports no fix for complex and multiline expressions`() {
        val diagnostics =
            NoDistinctParenthesesRule().diagnostics(
                """
                selectLowerNames:
                SELECT DISTINCT(LOWER(name))
                FROM player;

                selectMultiline:
                SELECT DISTINCT(
                  name
                )
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(2, diagnostics.size)
        assertEquals(0, diagnostics[0].fixes.size)
        assertEquals(0, diagnostics[1].fixes.size)
    }

    @Test
    fun `accepts distinct without parentheses`() {
        NoDistinctParenthesesRule().assertDiagnosticCount(
            """
            selectNames:
            SELECT DISTINCT name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoDistinctParenthesesRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- SELECT DISTINCT(name)
            SELECT 'DISTINCT(name)', "DISTINCT(name)", `DISTINCT(name)`, [DISTINCT(name)]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
