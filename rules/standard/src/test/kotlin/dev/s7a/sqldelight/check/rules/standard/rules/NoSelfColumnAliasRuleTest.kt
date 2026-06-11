package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoSelfColumnAliasRuleTest {
    @Test
    fun `reports aliases that repeat simple column names`() {
        val diagnostics =
            NoSelfColumnAliasRule().diagnostics(
                """
                selectPlayers:
                SELECT name AS name
                FROM player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Column aliases should not repeat the source column name.", diagnostics.single().message)
    }

    @Test
    fun `reports aliases that repeat qualified column names`() {
        NoSelfColumnAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT player.name AS name
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts meaningful aliases`() {
        NoSelfColumnAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT name AS playerName
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts computed expressions with repeated alias names`() {
        NoSelfColumnAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT lower(name) AS name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts result columns without aliases`() {
        NoSelfColumnAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT name
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores quoted identifiers`() {
        NoSelfColumnAliasRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT "display_name" AS "display_name"
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
