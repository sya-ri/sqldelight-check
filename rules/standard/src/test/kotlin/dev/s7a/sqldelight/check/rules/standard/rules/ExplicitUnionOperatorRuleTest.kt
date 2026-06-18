package dev.s7a.sqldelight.check.rules.standard.rules

import dev.s7a.sqldelight.check.api.FixSafety
import kotlin.test.Test
import kotlin.test.assertEquals

class ExplicitUnionOperatorRuleTest {
    @Test
    fun `reports union without all or distinct`() {
        val content =
            """
            selectAllNames:
            SELECT name FROM active_player
            UNION
            SELECT name FROM archived_player;
            """.asSqlDelightFile()
        val diagnostics =
            ExplicitUnionOperatorRule().diagnostics(
                content,
            )

        assertEquals(1, diagnostics.size)
        assertEquals(FixSafety.Safe, diagnostics.single().fixes.single().safety)
        assertEquals(" DISTINCT", diagnostics.single().fixes.single().edits.single().replacement)
        ExplicitUnionOperatorRule().assertAllFixes(
            content,
            """
            selectAllNames:
            SELECT name FROM active_player
            UNION DISTINCT
            SELECT name FROM archived_player;
            """.asSqlDelightFile(),
        )
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
