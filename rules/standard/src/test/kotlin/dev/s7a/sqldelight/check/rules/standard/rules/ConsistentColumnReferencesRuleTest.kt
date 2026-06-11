package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentColumnReferencesRuleTest {
    @Test
    fun `reports group by clauses that mix ordinal and named references`() {
        val diagnostics =
            ConsistentColumnReferencesRule().diagnostics(
                """
                selectScores:
                SELECT name, team_id, SUM(score)
                FROM player
                GROUP BY 1, team_id;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports order by clauses that mix ordinal and named references`() {
        val diagnostics =
            ConsistentColumnReferencesRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name, score
                FROM player
                ORDER BY 1 DESC, name;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports mixed references in migration files`() {
        val diagnostics =
            ConsistentColumnReferencesRule().diagnostics(
                """
                INSERT INTO player_rank_snapshot(player_id, rank_name)
                SELECT id, name
                FROM player
                ORDER BY id, 2;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts all ordinal and all named references`() {
        ConsistentColumnReferencesRule().assertDiagnosticCount(
            """
            selectGroupedByOrdinal:
            SELECT team_id, name, SUM(score)
            FROM player
            GROUP BY 1, 2
            ORDER BY 2 DESC, 1 ASC;

            selectGroupedByName:
            SELECT team_id, LOWER(name), SUM(score)
            FROM player
            GROUP BY team_id, LOWER(name)
            ORDER BY team_id, LOWER(name) DESC;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `accepts order by ordinal references with nulls placement`() {
        ConsistentColumnReferencesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT id, name, score
            FROM player
            ORDER BY 1 NULLS LAST, 2 DESC NULLS FIRST;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `stops group by before order by`() {
        ConsistentColumnReferencesRule().assertDiagnosticCount(
            """
            selectPlayers:
            SELECT team_id, name, SUM(score)
            FROM player
            GROUP BY 1
            ORDER BY name;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `keeps commas inside expressions in a single reference`() {
        val diagnostics =
            ConsistentColumnReferencesRule().diagnostics(
                """
                selectPlayers:
                SELECT id, name, score
                FROM player
                ORDER BY 1, COALESCE(name, 'unknown');
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ConsistentColumnReferencesRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- GROUP BY 1, name
            -- ORDER BY 1, name
            SELECT 'GROUP BY 1, name', "ORDER BY 1, name", `GROUP BY 1, name`, [ORDER BY 1, name]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `conservatively skips order by clauses inside parentheses`() {
        ConsistentColumnReferencesRule().assertDiagnosticCount(
            """
            selectPlayerRanks:
            SELECT ROW_NUMBER() OVER (ORDER BY 1, name) AS rank
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
