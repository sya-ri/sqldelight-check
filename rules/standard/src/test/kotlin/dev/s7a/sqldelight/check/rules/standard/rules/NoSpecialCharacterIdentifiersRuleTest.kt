package dev.s7a.sqldelight.check.rules.standard.rules

import kotlin.test.Test
import kotlin.test.assertEquals

class NoSpecialCharacterIdentifiersRuleTest {
    @Test
    fun `reports quoted identifiers with special characters`() {
        val diagnostics =
            NoSpecialCharacterIdentifiersRule().diagnostics(
                """
                CREATE TABLE "player score" (
                  id INTEGER NOT NULL PRIMARY KEY
                );

                selectScore:
                SELECT `total-score`
                FROM "player score";
                """.asSqlDelightFile(),
            )

        assertEquals(3, diagnostics.size)
        assertEquals("Identifiers should avoid special characters.", diagnostics.first().message)
    }

    @Test
    fun `accepts portable quoted identifiers`() {
        NoSpecialCharacterIdentifiersRule().assertDiagnosticCount(
            """
            CREATE TABLE "player_score" (
              "id" INTEGER NOT NULL PRIMARY KEY
            );
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `ignores single quoted strings and comments`() {
        NoSpecialCharacterIdentifiersRule().assertDiagnosticCount(
            """
            selectLiteral:
            -- "player score"
            SELECT 'player score'
            FROM player;
            """.asSqlDelightFile(),
            0,
        )
    }

    @Test
    fun `handles escaped quoted identifier delimiters`() {
        NoSpecialCharacterIdentifiersRule().assertDiagnosticCount(
            """
            selectEscaped:
            SELECT "player""score"
            FROM player;
            """.asSqlDelightFile(),
            1,
        )
    }
}
