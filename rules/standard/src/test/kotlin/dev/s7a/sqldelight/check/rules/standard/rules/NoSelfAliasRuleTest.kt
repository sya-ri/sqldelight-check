package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelfAliasRuleTest {
    @Test
    fun `reports table aliases that repeat table names`() {
        val content =
            """
            selectPlayers:
            SELECT player.id
            FROM player AS player;
            """.asSqlDelightFile()
        val diagnostics = NoSelfAliasRule().diagnostics(content)

        assertEquals(1, diagnostics.size)
        assertEquals("Table aliases should not repeat the table name.", diagnostics.single().message)
        assertEquals(FixSafety.Unsafe, diagnostics.single().fixes.single().safety)
        NoSelfAliasRule().assertAllFixes(
            content,
            """
            selectPlayers:
            SELECT player.id
            FROM player;
            """.asSqlDelightFile(),
        )
    }

    @Test
    fun `reports implicit self aliases on joins`() {
        NoSelfAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM player p
            JOIN team team ON team.id = p.team_id;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `uses final segment of dotted table names`() {
        NoSelfAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM main.player AS player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts useful aliases and comments`() {
        NoSelfAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            -- FROM player AS player
            SELECT p.id
            FROM player AS p
            JOIN team AS t ON t.id = p.team_id;
            """.asSqlDelightFile(),
            0,
        )
    }
}
