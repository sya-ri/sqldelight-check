package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class UniqueTableAliasesRuleTest {
    @Test
    fun `reports duplicate table aliases in one statement`() {
        val diagnostics =
            UniqueTableAliasesRule().diagnostics(
                """
                selectPlayers:
                SELECT p.id
                FROM player AS p
                JOIN team AS p ON p.id = player.team_id;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Table aliases should be unique within a statement.", diagnostics.single().message)
    }

    @Test
    fun `reports duplicate aliases across comma separated references`() {
        UniqueTableAliasesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM player AS p, team AS p;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts duplicate aliases in separate statements`() {
        UniqueTableAliasesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM player AS p;

            selectTeams:
            SELECT p.id
            FROM team AS p;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts unique table and subquery aliases`() {
        UniqueTableAliasesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id, ranked.id
            FROM player AS p
            JOIN (SELECT id FROM player) AS ranked ON ranked.id = p.id;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `does not compare nested aliases with outer statements`() {
        UniqueTableAliasesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT p.id
            FROM player AS p
            WHERE EXISTS (
              SELECT 1
              FROM team AS p
            );
            """.asSqlDelightFile(),
            0,
        )
    }
}
