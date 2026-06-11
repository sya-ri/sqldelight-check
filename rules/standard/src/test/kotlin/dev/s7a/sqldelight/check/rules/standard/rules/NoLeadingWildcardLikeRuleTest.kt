package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoLeadingWildcardLikeRuleTest {
    @Test
    fun `reports percent prefix wildcard literal`() {
        val diagnostics =
            NoLeadingWildcardLikeRule().diagnostics(
                """
                findPlayers:
                SELECT id
                FROM player
                WHERE name LIKE '%son';
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(4, diagnostics.single().range?.start?.line)
    }

    @Test
    fun `reports underscore prefix wildcard literal`() {
        NoLeadingWildcardLikeRule().assertDiagnosticCount(
            """
            findPlayers:
            SELECT id
            FROM player
            WHERE name LIKE '_son';
            """.asSqlDelightFile(),
            1,
        )
    }

    @Test
    fun `accepts suffix wildcard literal`() {
        NoLeadingWildcardLikeRule().assertDiagnosticCount(
            """
            findPlayers:
            SELECT id
            FROM player
            WHERE name LIKE 'son%';
            """.asSqlDelightFile(),
            0,
        )
    }
}
