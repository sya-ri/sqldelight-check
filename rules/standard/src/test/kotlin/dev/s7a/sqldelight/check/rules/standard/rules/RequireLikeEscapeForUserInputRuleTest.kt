package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class RequireLikeEscapeForUserInputRuleTest {
    @Test
    fun `reports parameterized like without escape`() {
        val diagnostics =
            RequireLikeEscapeForUserInputRule().diagnostics(
                """
                searchPlayers:
                SELECT id, name
                FROM player
                WHERE name LIKE :namePattern;
                """.asSqlDelightFile(),
            )

        assertEquals(1, diagnostics.size)
        assertEquals(
            "Parameterized LIKE predicates should specify ESCAPE so user input wildcards are explicit.",
            diagnostics.single().message,
        )
    }

    @Test
    fun `accepts parameterized like with escape`() {
        RequireLikeEscapeForUserInputRule().assertDiagnosticCount(
            """
            searchPlayers:
            SELECT id, name
            FROM player
            WHERE name LIKE :namePattern ESCAPE '\';
            """.asSqlDelightFile(),
            expected = 0,
        )
    }

    @Test
    fun `accepts literal patterns`() {
        RequireLikeEscapeForUserInputRule().assertDiagnosticCount(
            """
            searchPlayers:
            SELECT id, name
            FROM player
            WHERE name LIKE 'A%';
            """.asSqlDelightFile(),
            expected = 0,
        )
    }
}
