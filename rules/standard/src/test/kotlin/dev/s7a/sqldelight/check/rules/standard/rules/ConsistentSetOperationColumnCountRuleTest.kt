package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ConsistentSetOperationColumnCountRuleTest {
    @Test
    fun `reports union branches with different column counts`() {
        val diagnostics =
            ConsistentSetOperationColumnCountRule().diagnostics(
                """
                selectAllPlayers:
                SELECT id, name
                FROM active_player
                UNION ALL
                SELECT id
                FROM archived_player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Set operation SELECT lists should return the same number of columns.", diagnostics.single().message)
    }

    @Test
    fun `accepts matching set operation column counts`() {
        ConsistentSetOperationColumnCountRule().assertDiagnosticCount(
            """
            selectAllPlayers:
            SELECT id, name
            FROM active_player
            UNION DISTINCT
            SELECT id, name
            FROM archived_player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `reports chained set operation mismatches`() {
        ConsistentSetOperationColumnCountRule().assertDiagnosticCount(
            """
            selectAllPlayers:
            SELECT id, name
            FROM active_player
            UNION ALL
            SELECT id, name
            FROM archived_player
            EXCEPT
            SELECT id
            FROM banned_player;
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts select literals without from clauses`() {
        ConsistentSetOperationColumnCountRule().assertDiagnosticCount(
            """
            selectNumbers:
            SELECT 1, 'one'
            UNION ALL
            SELECT 2, 'two';
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores wildcard branches because their result count is unknown`() {
        ConsistentSetOperationColumnCountRule().assertDiagnosticCount(
            """
            selectAllPlayers:
            SELECT *
            FROM active_player
            UNION ALL
            SELECT id
            FROM archived_player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores set operator words in comments and strings`() {
        ConsistentSetOperationColumnCountRule().assertDiagnosticCount(
            """
            selectPlayers:
            -- SELECT id, name FROM active_player UNION SELECT id FROM archived_player
            SELECT 'UNION'
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
