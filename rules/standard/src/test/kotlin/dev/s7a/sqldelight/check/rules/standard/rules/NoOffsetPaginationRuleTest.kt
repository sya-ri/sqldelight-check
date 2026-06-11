package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoOffsetPaginationRuleTest {
    @Test
    fun `reports offset pagination`() {
        val diagnostics =
            NoOffsetPaginationRule().diagnostics(
                """
                listPlayers:
                SELECT id, name
                FROM player
                ORDER BY id
                LIMIT :limit OFFSET :offset;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals("Prefer keyset pagination over OFFSET pagination for stable and scalable paging.", diagnostics.single().message)
    }

    @Test
    fun `accepts keyset pagination without offset`() {
        NoOffsetPaginationRule().assertDiagnosticCount(
            """
            listPlayersAfter:
            SELECT id, name
            FROM player
            WHERE id > :lastSeenId
            ORDER BY id
            LIMIT :limit;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `ignores comments strings and quoted identifiers`() {
        NoOffsetPaginationRule().assertDiagnosticCount(
            """
            listPlayers:
            -- OFFSET :offset
            SELECT 'OFFSET', "offset", `offset`, [offset]
            FROM player;
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
