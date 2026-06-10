package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class ExplicitUnionOperatorRuleTest {
    @Test
    fun `reports union without all or distinct`() {
        val diagnostics =
            ExplicitUnionOperatorRule().diagnostics(
                """
                selectAllNames:
                SELECT name FROM active_player
                UNION
                SELECT name FROM archived_player;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(0, diagnostics.single().fixes.size)
    }

    @Test
    fun `reports bare union in migration files`() {
        val diagnostics =
            ExplicitUnionOperatorRule().diagnostics(
                """
                INSERT INTO player_name_snapshot(name)
                SELECT name FROM active_player
                UNION
                SELECT name FROM archived_player;
                """.asSqlDelightFile(),
                path = MIGRATION_SQM_PATH,
            )

        assertEquals(1, diagnostics.size)
    }

    @Test
    fun `accepts explicit union all and union distinct`() {
        ExplicitUnionOperatorRule().assertDiagnosticCount(
            """
            selectAllNames:
            SELECT name FROM active_player
            UNION ALL
            SELECT name FROM archived_player
            UNION DISTINCT
            SELECT name FROM pending_player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        ExplicitUnionOperatorRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- UNION
            SELECT 'UNION', "UNION", `UNION`, [UNION]
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }
}
